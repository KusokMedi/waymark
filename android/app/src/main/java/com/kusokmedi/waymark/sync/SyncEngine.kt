package com.kusokmedi.waymark.sync

import android.content.Context
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

sealed interface SyncOutcome {
    data class Success(val changeCount: Int, val notModified: Boolean = false) : SyncOutcome
    data class Offline(val message: String) : SyncOutcome
    data class Failure(val message: String) : SyncOutcome
}

class SyncEngine(
    private val context: Context,
    private val db: WaymarkDatabase,
    private val client: ArcGisClient,
    private val settings: SettingsRepository,
    private val notifier: ChangeNotifier,
) {
    private val mutex = Mutex()

    suspend fun synchronize(): SyncOutcome = mutex.withLock {
        if (!NetworkMonitor.isOnline(context)) {
            val state = db.syncStateDao().get() ?: SyncStateEntity()
            db.syncStateDao().upsert(
                state.copy(
                    lastAttemptAt = System.currentTimeMillis(),
                    status = "offline",
                    message = "No network connection",
                )
            )
            return SyncOutcome.Offline("No network connection. Showing saved data.")
        }

        val state = db.syncStateDao().get() ?: SyncStateEntity()
        val attemptAt = System.currentTimeMillis()
        db.syncStateDao().upsert(state.copy(lastAttemptAt = attemptAt, status = "running", message = null))
        WaymarkWidgetUpdater.updateAll(context)

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
                WaymarkWidgetUpdater.updateAll(context)
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

            val watchEntries = db.watchlistDao().getAll().associateBy { it.countryId }
            val trackedChanges = changes.filter { watchEntries.containsKey(it.countryId) }
            val now = System.currentTimeMillis()

            db.withTransaction {
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
            }

            val appSettings = settings.settings.first()
            if (appSettings.notificationsEnabled) {
                val enabledCountryIds = watchEntries.values
                    .filter { it.notificationsEnabled }
                    .map { it.countryId }
                    .toSet()
                notifier.notifyChanges(trackedChanges.filter { it.countryId in enabledCountryIds })
            }
            WaymarkWidgetUpdater.updateAll(context)
            SyncOutcome.Success(trackedChanges.size)
        } catch (e: CancellationException) {
            db.syncStateDao().upsert(
                state.copy(lastAttemptAt = attemptAt, status = "idle", message = "Synchronization cancelled")
            )
            WaymarkWidgetUpdater.updateAll(context)
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
            WaymarkWidgetUpdater.updateAll(context)
            notifier.notifySyncProblem(message)
            SyncOutcome.Failure(message)
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
