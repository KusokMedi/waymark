package com.kusokmedi.waymark.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.NormalizedRecord
import com.kusokmedi.waymark.domain.RecordKind

@Entity(tableName = "countries")
data class CountryEntity(
    @PrimaryKey val id: String,
    val isoAlpha2: String?,
    val isoAlpha3: String?,
    val name: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "advisories",
    primaryKeys = ["sourceRecordId"],
    foreignKeys = [
        ForeignKey(
            entity = CountryEntity::class,
            parentColumns = ["id"],
            childColumns = ["countryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("countryId")],
)
data class AdvisoryEntity(
    val sourceRecordId: String,
    val countryId: String,
    val destinationName: String,
    val isoAlpha3: String?,
    val rawLevelCode: Int?,
    val levelNumber: Int?,
    val containsHigherRiskAreas: Boolean,
    val officialLabel: String?,
    val advisoryUrl: String?,
    val advisoryDate: String?,
    val sourceEditDate: String?,
    val assistedDepartureStatus: Int?,
    val assistedDepartureText: String?,
    val assistedDepartureDate: String?,
    val recordKind: String,
    val contentHash: String,
    val fetchedAt: Long,
) {
    val level: AdvisoryLevel? get() = AdvisoryLevel.fromNumber(levelNumber)

    companion object {
        fun from(record: NormalizedRecord, countryId: String, fetchedAt: Long) = AdvisoryEntity(
            sourceRecordId = record.sourceRecordId,
            countryId = countryId,
            destinationName = record.destinationName,
            isoAlpha3 = record.isoAlpha3,
            rawLevelCode = record.rawLevelCode,
            levelNumber = record.level?.number,
            containsHigherRiskAreas = record.containsHigherRiskAreas,
            officialLabel = record.officialLabel,
            advisoryUrl = record.advisoryUrl,
            advisoryDate = record.advisoryDate,
            sourceEditDate = record.sourceEditDate,
            assistedDepartureStatus = record.assistedDepartureStatus,
            assistedDepartureText = record.assistedDepartureText,
            assistedDepartureDate = record.assistedDepartureDate,
            recordKind = record.recordKind.name,
            contentHash = record.contentHash,
            fetchedAt = fetchedAt,
        )
    }

    fun toDomain(): NormalizedRecord = NormalizedRecord(
        sourceRecordId = sourceRecordId,
        destinationName = destinationName,
        isoAlpha3 = isoAlpha3,
        isoAlpha2 = null,
        rawLevelCode = rawLevelCode,
        level = level,
        containsHigherRiskAreas = containsHigherRiskAreas,
        officialLabel = officialLabel ?: rawLevelCode?.let { AdvisoryLevels.map(it).officialLabel },
        advisoryUrl = advisoryUrl,
        advisoryDate = advisoryDate,
        sourceEditDate = sourceEditDate,
        assistedDepartureStatus = assistedDepartureStatus,
        assistedDepartureText = assistedDepartureText,
        assistedDepartureDate = assistedDepartureDate,
        recordKind = runCatching { RecordKind.valueOf(recordKind) }.getOrDefault(RecordKind.COUNTRY),
        contentHash = contentHash,
    )
}

@Entity(
    tableName = "watchlist",
    foreignKeys = [
        ForeignKey(
            entity = CountryEntity::class,
            parentColumns = ["id"],
            childColumns = ["countryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["countryId"], unique = true)],
)
data class WatchlistEntity(
    @PrimaryKey val countryId: String,
    val position: Int,
    val isFavorite: Boolean,
    val notificationsEnabled: Boolean,
    val addedAt: Long,
)

@Entity(
    tableName = "history_events",
    foreignKeys = [
        ForeignKey(
            entity = CountryEntity::class,
            parentColumns = ["id"],
            childColumns = ["countryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("countryId"),
        Index("occurredAt"),
        Index(value = ["fingerprint"], unique = true)
    ],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val countryId: String,
    val eventType: String,
    val previousLevel: Int?,
    val newLevel: Int?,
    val title: String,
    val message: String,
    val occurredAt: Long,
    val fingerprint: String,
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastSuccessAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val status: String = "idle",
    val message: String? = null,
    val etag: String? = null,
    val lastModified: String? = null,
    val recordCount: Int = 0,
    val firstSyncCompleted: Boolean = false,
)

@Entity(tableName = "widget_configs")
data class WidgetConfigEntity(
    @PrimaryKey val widgetId: Int,
    val preset: String = "watchlist",
    val countryIds: String = "",
    val showDate: Boolean = false,
    val showLevel: Boolean = true,
    val showFlag: Boolean = true,
    val showCountryName: Boolean = true,
    val backgroundStyle: String = "surface",
    val backgroundAlpha: Int = 100,
    val columns: Int = 1,
    val updatedAt: Long = 0,
    val widgetType: String = "watchlist",
    val singleCountryId: String? = null,
    val cornerRadius: Int = 12,
)
