package com.kusokmedi.waymark.domain

import com.kusokmedi.waymark.data.local.AdvisoryEntity
import com.kusokmedi.waymark.data.local.CountryEntity
import com.kusokmedi.waymark.data.local.HistoryEntity
import com.kusokmedi.waymark.data.local.WatchlistEntity

data class CountryCard(
    val id: String,
    val name: String,
    val isoAlpha2: String?,
    val isoAlpha3: String?,
    val level: AdvisoryLevel?,
    val containsHigherRiskAreas: Boolean,
    val officialLabel: String?,
    val advisoryDate: String?,
    val advisoryUrl: String?,
    val recordCount: Int,
    val regionCount: Int,
    val isFavorite: Boolean,
    val inWatchlist: Boolean,
    val notificationsEnabled: Boolean,
    val position: Int,
)

data class CountryDetailData(
    val country: CountryEntity,
    val records: List<AdvisoryEntity>,
    val watchlist: WatchlistEntity?,
    val history: List<HistoryEntity>,
    val lastSyncAt: Long?,
) {
    val countryRecord: AdvisoryEntity?
        get() = records.firstOrNull { it.recordKind == RecordKind.COUNTRY.name }

    val regions: List<AdvisoryEntity>
        get() = records.filter { it.recordKind == RecordKind.REGION.name }

    val level: AdvisoryLevel?
        get() = countryRecord?.level ?: records.mapNotNull { it.level }.distinct().singleOrNull()

    val containsHigherRiskAreas: Boolean
        get() = records.any { it.containsHigherRiskAreas }

    val advisoryDate: String?
        get() = countryRecord?.advisoryDate ?: records.firstOrNull { it.advisoryDate != null }?.advisoryDate

    val advisoryUrl: String?
        get() = countryRecord?.advisoryUrl ?: records.firstOrNull { it.advisoryUrl != null }?.advisoryUrl
}

data class HistoryItem(
    val event: HistoryEntity,
    val countryName: String,
    val countryIso2: String?,
)
