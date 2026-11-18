package com.kusokmedi.waymark.data

import androidx.room.withTransaction
import com.kusokmedi.waymark.data.local.AdvisoryEntity
import com.kusokmedi.waymark.data.local.CountryEntity
import com.kusokmedi.waymark.data.local.WatchlistEntity
import com.kusokmedi.waymark.data.local.WaymarkDatabase
import com.kusokmedi.waymark.domain.AdvisoryLevel
import com.kusokmedi.waymark.domain.AdvisoryLevels
import com.kusokmedi.waymark.domain.CountryCard
import com.kusokmedi.waymark.domain.CountryDetailData
import com.kusokmedi.waymark.domain.HistoryItem
import com.kusokmedi.waymark.domain.RecordKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AdvisoryRepository(private val db: WaymarkDatabase) {
    private val countries = db.countryDao()
    private val advisories = db.advisoryDao()
    private val watchlist = db.watchlistDao()
    private val history = db.historyDao()
    private val syncState = db.syncStateDao()

    val allCountries: Flow<List<CountryCard>> = combine(
        countries.observeAll(),
        advisories.observeAll(),
        watchlist.observeAll(),
    ) { countryList, advisoryList, watchList ->
        buildCards(countryList, advisoryList, watchList)
    }

    val watchlistCountries: Flow<List<CountryCard>> = allCountries.map { cards ->
        cards.filter { it.inWatchlist }.sortedBy { it.position }
    }

    val historyItems: Flow<List<HistoryItem>> = combine(
        history.observeAll(),
        countries.observeAll(),
    ) { events, countryList ->
        val byId = countryList.associateBy { it.id }
        events.map { event ->
            val country = byId[event.countryId]
            HistoryItem(event, country?.name ?: event.countryId, country?.isoAlpha2)
        }
    }

    val syncStateFlow = syncState.observe()

    fun observeCountry(countryId: String): Flow<CountryDetailData?> = combine(
        countries.observeAll(),
        advisories.observeForCountry(countryId),
        watchlist.observeAll(),
        history.observeForCountry(countryId),
        syncState.observe(),
    ) { countryList, records, watchList, events, state ->
        val country = countryList.firstOrNull { it.id == countryId }
        if (country == null && records.isEmpty()) return@combine null
        val effective = country ?: CountryEntity(
            id = countryId,
            isoAlpha2 = records.firstOrNull()?.let { com.kusokmedi.waymark.domain.IsoCountries.alpha2(it.isoAlpha3) },
            isoAlpha3 = records.firstOrNull()?.isoAlpha3,
            name = records.firstOrNull()?.destinationName ?: countryId,
            updatedAt = 0,
        )
        CountryDetailData(
            country = effective,
            records = records,
            watchlist = watchList.firstOrNull { it.countryId == countryId },
            history = events,
            lastSyncAt = state?.lastSuccessAt,
        )
    }

    suspend fun search(query: String): List<CountryEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return countries.getAll()
        return countries.search(trimmed)
    }

    suspend fun countryCardsByIds(ids: List<String>): List<CountryCard> {
        if (ids.isEmpty()) return emptyList()
        val countryList = countries.getByIds(ids)
        val advisoryList = advisories.getForCountries(ids)
        val watchList = watchlist.getAll().filter { it.countryId in ids }
        val cards = buildCards(countryList, advisoryList, watchList).associateBy { it.id }
        return ids.mapNotNull { cards[it] }
    }

    suspend fun getCountry(id: String): CountryEntity? = countries.getById(id)

    suspend fun addToWatchlist(countryId: String) {
        if (countries.getById(countryId) == null) return
        db.withTransaction {
            if (watchlist.get(countryId) != null) return@withTransaction
            val position = (watchlist.getAll().maxOfOrNull { it.position } ?: -1) + 1
            watchlist.upsert(
                WatchlistEntity(
                    countryId = countryId,
                    position = position,
                    isFavorite = false,
                    notificationsEnabled = false,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    suspend fun removeFromWatchlist(countryId: String) {
        watchlist.delete(countryId)
    }

    suspend fun setFavorite(countryId: String, favorite: Boolean) {
        watchlist.setFavorite(countryId, favorite)
    }

    suspend fun setNotifications(countryId: String, enabled: Boolean) {
        watchlist.setNotifications(countryId, enabled)
    }

    suspend fun reorderWatchlist(ids: List<String>) {
        db.withTransaction {
            val existing = watchlist.getAll().associateBy { it.countryId }
            ids.forEachIndexed { index, id ->
                existing[id]?.let { watchlist.upsert(it.copy(position = index)) }
            }
        }
    }

    suspend fun clearHistory() {
        history.deleteAll()
    }

    private fun buildCards(
        countryList: List<CountryEntity>,
        advisoryList: List<AdvisoryEntity>,
        watchList: List<WatchlistEntity>,
    ): List<CountryCard> {
        val recordsByCountry = advisoryList.groupBy { it.countryId }
        val watchByCountry = watchList.associateBy { it.countryId }
        return countryList.map { country ->
            val records = recordsByCountry[country.id].orEmpty()
            val countryRecord = records.firstOrNull { it.recordKind == RecordKind.COUNTRY.name }
            val level = countryRecord?.level
                ?: records.mapNotNull { it.level }.distinct().singleOrNull()
            val watch = watchByCountry[country.id]
            CountryCard(
                id = country.id,
                name = country.name,
                isoAlpha2 = country.isoAlpha2,
                isoAlpha3 = country.isoAlpha3,
                level = level,
                containsHigherRiskAreas = records.any { it.containsHigherRiskAreas },
                officialLabel = countryRecord?.officialLabel
                    ?: level?.let { AdvisoryLevels.label(it) }
                    ?: records.firstOrNull { it.officialLabel != null }?.officialLabel,
                advisoryDate = countryRecord?.advisoryDate
                    ?: records.firstOrNull { it.advisoryDate != null }?.advisoryDate,
                advisoryUrl = countryRecord?.advisoryUrl
                    ?: records.firstOrNull { it.advisoryUrl != null }?.advisoryUrl,
                recordCount = records.size,
                regionCount = records.count { it.recordKind == RecordKind.REGION.name },
                isFavorite = watch?.isFavorite ?: false,
                inWatchlist = watch != null,
                notificationsEnabled = watch?.notificationsEnabled ?: false,
                position = watch?.position ?: Int.MAX_VALUE,
            )
        }.sortedBy { it.name.lowercase() }
    }
}

fun List<CountryCard>.sortedBySeverity(): List<CountryCard> = sortedWith(
    compareBy<CountryCard> { card -> card.level?.let { 5 - it.number } ?: 99 }
        .thenByDescending { it.containsHigherRiskAreas }
        .thenBy { it.name.lowercase() }
)

fun advisoryLevelOrNull(number: Int?): AdvisoryLevel? = AdvisoryLevel.fromNumber(number)
