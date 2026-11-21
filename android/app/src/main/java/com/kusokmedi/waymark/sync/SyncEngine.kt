package com.kusokmedi.waymark.sync

import android.content.Context
import android.util.Log
import com.kusokmedi.waymark.data.NetworkMonitor
import com.kusokmedi.waymark.data.local.AdvisoryEntity
import com.kusokmedi.waymark.data.local.CountryEntity
import com.kusokmedi.waymark.data.local.HistoryEntity
import com.kusokmedi.waymark.data.local.SettingsRepository
import com.kusokmedi.waymark.data.local.SyncStateEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.remote.ArcGisClient
import com.kusokmedi.waymark.data.remote.SourceException
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.AdvisoryNormalizer
import com.kusokmedi.waymark.domain.ChangeDetector
import com.kusokmedi.waymark.domain.ChangeType
import com.kusokmedi.waymark.domain.CountrySnapshot
import com.kusokmedi.waymark.domain.RecordKind
import com.kusokmedi.waymark.widget.WaymarkWidgetUpdater
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

sealed interface SyncOutcome {
    data class Success(val changeCount: Int, val notModified: Boolean = false) : SyncOutcome
    data class Offline(val message: String) : SyncOutcome
    data class Failure(val message: String) : SyncOutcome
}

/**
 * Core synchronization engine for Travel Advisory data.
 * 
 * **IMPORTANT**: Must be initialized with Application Context to prevent memory leaks.
 * Never pass Activity or Service context to the constructor.
 */
class SyncEngine(
    private val context: Context,
    private val db: WaymarkDatabase,
    private val client: ArcGisClient,
    private val settings: SettingsRepository,
    private val notifier: ChangeNotifier,
) {
    private val mutex = Mutex()

    companion object {
        private const val TAG = "WaymarkSyncEngine"
        private val SYNC_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(150)
        private val MIN_SYNC_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30)
    }

    private suspend fun markFailedIfRunning(message: String) {
        mutex.withLock {
            val current = db.syncStateDao().get() ?: SyncStateEntity()
            if (current.status == "running") {
                db.syncStateDao().upsert(current.copy(status = "failed", message = message))
            }
        }
    }

    suspend fun synchronize(forceMobileData: Boolean = false, forceSync: Boolean = false): SyncOutcome {
        val outcome = try {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) {
                mutex.withLock { doSynchronize(forceMobileData, forceSync) }
            }
        } catch (e: Exception) {
            Log.w(TAG, "synchronize exception", e)
            markFailedIfRunning(e.message ?: "Synchronization was interrupted")
            WaymarkWidgetUpdater.forceUpdateAll(context)
            return SyncOutcome.Failure(e.message ?: "Synchronization was interrupted")
        }

        if (outcome != null) return outcome

        Log.w(TAG, "synchronize timed out or lock wait exceeded")
        markFailedIfRunning("Synchronization timed out")
        WaymarkWidgetUpdater.forceUpdateAll(context)
        return SyncOutcome.Failure("Synchronization timed out")
    }

    private suspend fun doSynchronize(forceMobileData: Boolean, forceSync: Boolean): SyncOutcome {
        var state = db.syncStateDao().recoverStaleRunning() ?: SyncStateEntity()
        val appSettings = settings.settings.first()
        val effectiveAllowMobileData = forceMobileData || appSettings.allowMobileData

        if (!NetworkMonitor.isAllowed(context, effectiveAllowMobileData)) {
            val offlineMessage = if (!NetworkMonitor.isOnline(context)) {
                "No network connection. Showing saved data."
            } else {
                "Mobile data is disabled for sync. Connect to Wi-Fi or allow mobile data in Settings."
            }
            db.syncStateDao().upsert(
                state.copy(
                    lastAttemptAt = System.currentTimeMillis(),
                    status = "offline",
                    message = if (!NetworkMonitor.isOnline(context)) "No network connection" else "Mobile data disabled",
                )
            )
            WaymarkWidgetUpdater.forceUpdateAll(context)
            return SyncOutcome.Offline(offlineMessage)
        }

        val attemptAt = System.currentTimeMillis()
        val lastSuccess = state.lastSuccessAt ?: 0L
        
        // Skip sync if recent success, unless forceSync is true
        if (!forceSync && state.firstSyncCompleted && attemptAt - lastSuccess < MIN_SYNC_INTERVAL_MS) {
            Log.d(TAG, "sync skipped: recent success ${attemptAt - lastSuccess}ms ago")
            // Keep UX responsive: show that we checked now, without a redundant network fetch.
            db.syncStateDao().upsert(
                state.copy(
                    lastAttemptAt = attemptAt,
                    lastSuccessAt = attemptAt,
                    status = "success",
                    message = null,
                )
            )
            WaymarkWidgetUpdater.forceUpdateAll(context)
            return SyncOutcome.Success(changeCount = 0, notModified = true)
        }

        db.syncStateDao().upsert(state.copy(lastAttemptAt = attemptAt, status = "running", message = null))
        WaymarkWidgetUpdater.forceUpdateAll(context)

        try {
            val fetched = client.fetchAll(etag = state.etag, lastModified = state.lastModified)
            if (fetched.notModified) {
                db.syncStateDao().upsert(
                    state.copy(
                        lastAttemptAt = attemptAt,
                        lastSuccessAt = attemptAt,
                        status = "success",
                        message = null,
                        firstSyncCompleted = true,
                    )
                )
                WaymarkWidgetUpdater.forceUpdateAll(context)
                return SyncOutcome.Success(changeCount = 0, notModified = true)
            }

            val normalized = fetched.records.mapIndexedNotNull { index, input ->
                AdvisoryNormalizer.normalize(input, index)
            }
            if (normalized.isEmpty()) {
                throw SourceException("The source returned no usable advisory records")
            }
            val aggregates = AdvisoryNormalizer.groupIntoCountries(normalized)
            if (aggregates.isEmpty()) {
                throw SourceException("The source returned an empty country catalogue")
            }

            val watchEntries = db.watchlistDao().getAll().associateBy { it.countryId }
            val now = System.currentTimeMillis()

            // Execute all data modifications and change detection inside transaction to prevent race conditions
            val trackedChanges = db.withTransaction {
                val existingCountries = db.countryDao().getAll()
                val existingAdvisories = db.advisoryDao().getAll().groupBy { it.countryId }
                val existingSnapshots = existingCountries.associate { country ->
                    country.id to snapshotOf(country, existingAdvisories[country.id].orEmpty())
                }
                val firstSync = existingCountries.isEmpty() || !state.firstSyncCompleted
                val changes = if (firstSync) {
                    emptyList()
                } else {
                    aggregates.flatMap { aggregate ->
                        ChangeDetector.detect(
                            previous = existingSnapshots[aggregate.id],
                            current = ChangeDetector.snapshotOf(aggregate),
                            firstSync = false,
                        )
                    }
                }
                val trackedChanges = changes.filter { watchEntries.containsKey(it.countryId) }
                
                // Upsert countries and advisories. Room @Upsert handles both insert and update.
                // We deleteAll advisories first to remove obsolete records from removed countries/regions.
                // This happens within a transaction, so no temporary empty state is visible to readers.
                db.countryDao().upsertAll(
                    aggregates.map { aggregate ->
                        CountryEntity(
                            id = aggregate.id,
                            isoAlpha2 = aggregate.isoAlpha2,
                            isoAlpha3 = aggregate.isoAlpha3,
                            name = aggregate.name,
                            updatedAt = now,
                        )
                    }
                )
                db.advisoryDao().deleteAll()
                db.advisoryDao().upsertAll(
                    aggregates.flatMap { aggregate ->
                        aggregate.records.map { AdvisoryEntity.from(it, aggregate.id, now) }
                    }
                )
                trackedChanges.forEach { change ->
                    db.historyDao().insertIgnore(
                        HistoryEntity(
                            countryId = change.countryId,
                            eventType = change.type.name,
                            previousLevel = change.previousLevel?.number,
                            newLevel = change.newLevel?.number,
                            title = change.title,
                            message = change.message,
                            occurredAt = now,
                            fingerprint = change.fingerprint,
                        )
                    )
                }
                db.syncStateDao().upsert(
                    SyncStateEntity(
                        id = 1,
                        lastSuccessAt = now,
                        lastAttemptAt = attemptAt,
                        status = "success",
                        message = null,
                        etag = fetched.etag ?: state.etag,
                        lastModified = fetched.lastModified ?: state.lastModified,
                        recordCount = normalized.size,
                        firstSyncCompleted = true,
                    )
                )
                
                trackedChanges // Return from transaction
            }

            if (appSettings.notificationsEnabled) {
                val enabledCountryIds = watchEntries.values
                    .filter { it.notificationsEnabled }
                    .map { it.countryId }
                    .toSet()
                notifier.notifyChanges(trackedChanges.filter { it.countryId in enabledCountryIds })
            }
            WaymarkWidgetUpdater.forceUpdateAll(context)
            return SyncOutcome.Success(trackedChanges.size)
        } catch (e: CancellationException) {
            db.syncStateDao().upsert(
                state.copy(lastAttemptAt = attemptAt, status = "idle", message = "Synchronization cancelled")
            )
            WaymarkWidgetUpdater.forceUpdateAll(context)
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "Synchronization failed"
            db.syncStateDao().upsert(
                state.copy(
                    lastAttemptAt = attemptAt,
                    status = "failed",
                    message = message,
                )
            )
            WaymarkWidgetUpdater.forceUpdateAll(context)
            notifier.notifySyncProblem(message)
            return SyncOutcome.Failure(message)
        }
    }

    private fun snapshotOf(country: CountryEntity, records: List<AdvisoryEntity>): CountrySnapshot {
        val countryRecord = records.firstOrNull { it.recordKind == RecordKind.COUNTRY.name }
        val level = countryRecord?.level ?: records.mapNotNull { it.level }.distinct().singleOrNull()
        return CountrySnapshot(
            countryId = country.id,
            name = country.name,
            level = level,
            containsHigherRiskAreas = records.any { it.containsHigherRiskAreas },
            advisoryDate = countryRecord?.advisoryDate ?: records.firstOrNull { it.advisoryDate != null }?.advisoryDate,
            officialLabel = countryRecord?.officialLabel
                ?: level?.let(AdvisoryLevels::label),
            regionNames = records.filter { it.recordKind == RecordKind.REGION.name }
                .map { it.destinationName }
                .toSet(),
            detailHash = records.map { it.contentHash }.sorted().joinToString(","),
        )
    }
}

fun HistoryEntity.changeType(): ChangeType? = runCatching { ChangeType.valueOf(eventType) }.getOrNull()
