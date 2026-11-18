package com.kusokmedi.waymark.backup

import androidx.room.withTransaction
import com.kusokmedi.waymark.BuildConfig
import com.kusokmedi.waymark.data.local.AdvisoryEntity
import com.kusokmedi.waymark.data.local.CountryEntity
import com.kusokmedi.waymark.data.local.HistoryEntity
import com.kusokmedi.waymark.data.local.SettingsRepository
import com.kusokmedi.waymark.data.local.ThemeMode
import com.kusokmedi.waymark.data.local.WatchlistEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.data.local.WidgetConfigEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val BACKUP_SCHEMA_VERSION = 1
const val BACKUP_APP_ID = "com.kusokmedi.waymark"

@Serializable
data class BackupFile(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val appId: String = BACKUP_APP_ID,
    val appVersion: String = "",
    val exportedAt: String = "",
    val countries: List<BackupCountry> = emptyList(),
    val advisories: List<BackupAdvisory> = emptyList(),
    val watchlist: List<BackupWatchlist> = emptyList(),
    val history: List<BackupHistory> = emptyList(),
    val widgets: List<BackupWidget> = emptyList(),
    val settings: BackupSettings? = null,
)

@Serializable
data class BackupCountry(
    val id: String,
    val isoAlpha2: String? = null,
    val isoAlpha3: String? = null,
    val name: String,
    val updatedAt: Long = 0,
)

@Serializable
data class BackupAdvisory(
    val sourceRecordId: String,
    val countryId: String,
    val destinationName: String,
    val isoAlpha3: String? = null,
    val rawLevelCode: Int? = null,
    val levelNumber: Int? = null,
    val containsHigherRiskAreas: Boolean = false,
    val officialLabel: String? = null,
    val advisoryUrl: String? = null,
    val advisoryDate: String? = null,
    val sourceEditDate: String? = null,
    val assistedDepartureStatus: Int? = null,
    val assistedDepartureText: String? = null,
    val assistedDepartureDate: String? = null,
    val recordKind: String = "COUNTRY",
    val contentHash: String = "",
    val fetchedAt: Long = 0,
)

@Serializable
data class BackupWatchlist(
    val countryId: String,
    val position: Int = 0,
    val isFavorite: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val addedAt: Long = 0,
)

@Serializable
data class BackupHistory(
    val countryId: String,
    val eventType: String,
    val previousLevel: Int? = null,
    val newLevel: Int? = null,
    val title: String,
    val message: String,
    val occurredAt: Long = 0,
    val fingerprint: String,
)

@Serializable
data class BackupWidget(
    val widgetId: Int,
    val preset: String = "large",
    val countryIds: String = "",
    val showDate: Boolean = true,
    val showLevel: Boolean = true,
    val showFlag: Boolean = true,
    val showCountryName: Boolean = true,
    val backgroundStyle: String = "surface",
    val columns: Int = 1,
    val widgetType: String = "watchlist",
    val singleCountryId: String? = null,
)

@Serializable
data class BackupSettings(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val notificationsEnabled: Boolean = false,
    val syncIntervalHours: Int = 12,
    val automaticSync: Boolean = true,
)

data class ImportResult(
    val successful: Boolean,
    val message: String,
    val countries: Int = 0,
    val advisories: Int = 0,
    val watchlist: Int = 0,
    val history: Int = 0,
    val widgets: Int = 0,
    val warnings: List<String> = emptyList(),
)

class BackupManager(
    private val context: android.content.Context,
    private val db: WaymarkDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportJson(): String = json.encodeToString(BackupFile.serializer(), backupFile())

    suspend fun exportZip(): ByteArray {
        val payload = exportJson().toByteArray(Charsets.UTF_8)
        val manifest = """{"schemaVersion":$BACKUP_SCHEMA_VERSION,"file":"waymark-backup.json"}"""
            .toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("waymark-backup.json"))
            zip.write(payload)
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    suspend fun restore(input: ByteArray, replaceExisting: Boolean): ImportResult {
        return try {
            val raw = if (isZip(input)) readZip(input) else input.toString(Charsets.UTF_8)
            val file = json.decodeFromString<BackupFile>(raw)
            validateAndRestore(file, replaceExisting)
        } catch (e: Exception) {
            ImportResult(false, "Backup could not be imported: ${e.message ?: "invalid file"}")
        }
    }

    private suspend fun backupFile(): BackupFile {
        val current = settings.settings.first()
        return BackupFile(
            appVersion = BuildConfig.VERSION_NAME,
            exportedAt = Instant.now().toString(),
            countries = db.countryDao().getAll().map {
                BackupCountry(it.id, it.isoAlpha2, it.isoAlpha3, it.name, it.updatedAt)
            },
            advisories = db.advisoryDao().getAll().map {
                BackupAdvisory(
                    sourceRecordId = it.sourceRecordId,
                    countryId = it.countryId,
                    destinationName = it.destinationName,
                    isoAlpha3 = it.isoAlpha3,
                    rawLevelCode = it.rawLevelCode,
                    levelNumber = it.levelNumber,
                    containsHigherRiskAreas = it.containsHigherRiskAreas,
                    officialLabel = it.officialLabel,
                    advisoryUrl = it.advisoryUrl,
                    advisoryDate = it.advisoryDate,
                    sourceEditDate = it.sourceEditDate,
                    assistedDepartureStatus = it.assistedDepartureStatus,
                    assistedDepartureText = it.assistedDepartureText,
                    assistedDepartureDate = it.assistedDepartureDate,
                    recordKind = it.recordKind,
                    contentHash = it.contentHash,
                    fetchedAt = it.fetchedAt,
                )
            },
            watchlist = db.watchlistDao().getAll().map {
                BackupWatchlist(it.countryId, it.position, it.isFavorite, it.notificationsEnabled, it.addedAt)
            },
            history = db.historyDao().getAll().map {
                BackupHistory(
                    countryId = it.countryId,
                    eventType = it.eventType,
                    previousLevel = it.previousLevel,
                    newLevel = it.newLevel,
                    title = it.title,
                    message = it.message,
                    occurredAt = it.occurredAt,
                    fingerprint = it.fingerprint,
                )
            },
            widgets = db.widgetConfigDao().getAll().map {
                BackupWidget(
                    widgetId = it.widgetId,
                    preset = it.preset,
                    countryIds = it.countryIds,
                    showDate = it.showDate,
                    showLevel = it.showLevel,
                    showFlag = it.showFlag,
                    showCountryName = it.showCountryName,
                    backgroundStyle = it.backgroundStyle,
                    columns = it.columns,
                    widgetType = it.widgetType,
                    singleCountryId = it.singleCountryId,
                )
            },
            settings = BackupSettings(
                themeMode = current.themeMode.name,
                dynamicColor = current.dynamicColor,
                notificationsEnabled = current.notificationsEnabled,
                syncIntervalHours = current.syncIntervalHours,
                automaticSync = current.automaticSync,
            ),
        )
    }

    private suspend fun validateAndRestore(file: BackupFile, replaceExisting: Boolean): ImportResult {
        if (file.schemaVersion != BACKUP_SCHEMA_VERSION) {
            return ImportResult(false, "Unsupported backup schema version ${file.schemaVersion}.")
        }
        if (file.appId != BACKUP_APP_ID) {
            return ImportResult(false, "This backup was not created by Waymark.")
        }
        if (file.countries.isEmpty() && file.advisories.isEmpty()) {
            return ImportResult(false, "The backup does not contain any country data.")
        }

        val warnings = mutableListOf<String>()
        val backupIds = linkedSetOf<String>()
        val validCountries = file.countries.filter { country ->
            if (country.id.isBlank() || country.name.isBlank()) {
                warnings += "Skipped a country with a missing identifier or name."
                false
            } else if (!backupIds.add(country.id)) {
                warnings += "Skipped duplicate country ${country.id}."
                false
            } else {
                true
            }
        }
        val existingIds = if (replaceExisting) emptySet() else db.countryDao().getAll().map { it.id }.toSet()
        val allowedCountryIds = backupIds + existingIds

        val validAdvisories = file.advisories.filter { advisory ->
            val ok = advisory.sourceRecordId.isNotBlank() &&
                advisory.countryId.isNotBlank() &&
                advisory.countryId in allowedCountryIds &&
                advisory.destinationName.isNotBlank() &&
                (advisory.levelNumber == null || advisory.levelNumber in 1..4)
            if (!ok) warnings += "Skipped an invalid advisory record for ${advisory.countryId}."
            ok
        }.distinctBy { it.sourceRecordId }

        val validWatchlist = file.watchlist.filter { entry ->
            val ok = entry.countryId.isNotBlank() && entry.countryId in allowedCountryIds
            if (!ok) warnings += "Skipped a watchlist entry for an unknown country."
            ok
        }.distinctBy { it.countryId }

        val validHistory = file.history.filter { event ->
            val ok = event.countryId.isNotBlank() &&
                event.countryId in allowedCountryIds &&
                event.fingerprint.isNotBlank() &&
                event.title.isNotBlank()
            if (!ok) warnings += "Skipped an invalid history event."
            ok
        }.distinctBy { it.fingerprint }

        val validWidgets = file.widgets.filter { it.widgetId > 0 }.distinctBy { it.widgetId }

        db.withTransaction {
            if (replaceExisting) {
                db.advisoryDao().deleteAll()
                db.watchlistDao().deleteAll()
                db.historyDao().deleteAll()
                db.widgetConfigDao().deleteAll()
                db.countryDao().deleteAll()
            }
            if (validCountries.isNotEmpty()) {
                db.countryDao().upsertAll(
                    validCountries.map {
                        CountryEntity(
                            id = it.id,
                            isoAlpha2 = it.isoAlpha2,
                            isoAlpha3 = it.isoAlpha3,
                            name = it.name,
                            updatedAt = it.updatedAt.takeIf { value -> value > 0 } ?: System.currentTimeMillis(),
                        )
                    }
                )
            }
            if (validAdvisories.isNotEmpty()) {
                db.advisoryDao().upsertAll(
                    validAdvisories.map {
                        AdvisoryEntity(
                            sourceRecordId = it.sourceRecordId,
                            countryId = it.countryId,
                            destinationName = it.destinationName,
                            isoAlpha3 = it.isoAlpha3,
                            rawLevelCode = it.rawLevelCode,
                            levelNumber = it.levelNumber,
                            containsHigherRiskAreas = it.containsHigherRiskAreas,
                            officialLabel = it.officialLabel,
                            advisoryUrl = it.advisoryUrl,
                            advisoryDate = it.advisoryDate,
                            sourceEditDate = it.sourceEditDate,
                            assistedDepartureStatus = it.assistedDepartureStatus,
                            assistedDepartureText = it.assistedDepartureText,
                            assistedDepartureDate = it.assistedDepartureDate,
                            recordKind = it.recordKind,
                            contentHash = it.contentHash,
                            fetchedAt = it.fetchedAt.takeIf { value -> value > 0 } ?: System.currentTimeMillis(),
                        )
                    }
                )
            }
            if (validWatchlist.isNotEmpty()) {
                db.watchlistDao().upsertAll(
                    validWatchlist.map {
                        WatchlistEntity(
                            countryId = it.countryId,
                            position = it.position,
                            isFavorite = it.isFavorite,
                            notificationsEnabled = it.notificationsEnabled,
                            addedAt = it.addedAt.takeIf { value -> value > 0 } ?: System.currentTimeMillis(),
                        )
                    }
                )
            }
            validHistory.forEach {
                db.historyDao().insertIgnore(
                    HistoryEntity(
                        countryId = it.countryId,
                        eventType = it.eventType,
                        previousLevel = it.previousLevel,
                        newLevel = it.newLevel,
                        title = it.title,
                        message = it.message,
                        occurredAt = it.occurredAt.takeIf { value -> value > 0 } ?: System.currentTimeMillis(),
                        fingerprint = it.fingerprint,
                    )
                )
            }
            if (validWidgets.isNotEmpty()) {
                db.widgetConfigDao().upsertAll(
                    validWidgets.map {
                        WidgetConfigEntity(
                            widgetId = it.widgetId,
                            preset = it.preset,
                            countryIds = it.countryIds,
                            showDate = it.showDate,
                            showLevel = it.showLevel,
                            showFlag = it.showFlag,
                            showCountryName = it.showCountryName,
                            backgroundStyle = it.backgroundStyle,
                            columns = it.columns.coerceIn(1, 3),
                            updatedAt = System.currentTimeMillis(),
                            widgetType = it.widgetType.ifBlank { "watchlist" },
                            singleCountryId = it.singleCountryId,
                        )
                    }
                )
            }
        }

        file.settings?.let { restored ->
            runCatching { ThemeMode.valueOf(restored.themeMode) }.getOrNull()?.let { settings.setThemeMode(it) }
            settings.setDynamicColor(restored.dynamicColor)
            settings.setNotificationsEnabled(restored.notificationsEnabled)
            settings.setSyncIntervalHours(restored.syncIntervalHours)
            settings.setAutomaticSync(restored.automaticSync)
        }
        settings.setOnboardingCompleted(true)

        return ImportResult(
            successful = true,
            message = "Backup imported.",
            countries = validCountries.size,
            advisories = validAdvisories.size,
            watchlist = validWatchlist.size,
            history = validHistory.size,
            widgets = validWidgets.size,
            warnings = warnings,
        )
    }

    private fun isZip(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    private fun readZip(bytes: ByteArray): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".json") && entry.name != "manifest.json") {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                entry = zip.nextEntry
            }
        }
        throw IllegalArgumentException("The archive does not contain a Waymark backup.")
    }
}

// A small extension used by the widget restore path and tests.
fun WidgetConfigEntity.countryIdList(): List<String> =
    countryIds.split(',').map { it.trim() }.filter { it.isNotEmpty() }
