package com.kusokmedi.waymark.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CountryDao {
    @Query("SELECT * FROM countries ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CountryEntity>>

    @Query("SELECT * FROM countries ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CountryEntity>

    @Query("SELECT * FROM countries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CountryEntity?

    @Query("SELECT * FROM countries WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<CountryEntity>

    @Query(
        "SELECT * FROM countries WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(isoAlpha2) = LOWER(:query) OR LOWER(isoAlpha3) = LOWER(:query) " +
            "ORDER BY name COLLATE NOCASE",
    )
    suspend fun search(query: String): List<CountryEntity>

    @Upsert
    suspend fun upsertAll(countries: List<CountryEntity>)

    @Query("SELECT COUNT(*) FROM countries")
    suspend fun count(): Int

    @Query("DELETE FROM countries")
    suspend fun deleteAll()
}

@Dao
interface AdvisoryDao {
    @Query("SELECT * FROM advisories WHERE countryId = :countryId ORDER BY destinationName COLLATE NOCASE")
    fun observeForCountry(countryId: String): Flow<List<AdvisoryEntity>>

    @Query("SELECT * FROM advisories ORDER BY countryId, destinationName COLLATE NOCASE")
    fun observeAll(): Flow<List<AdvisoryEntity>>

    @Query("SELECT * FROM advisories WHERE countryId = :countryId ORDER BY destinationName COLLATE NOCASE")
    suspend fun getForCountry(countryId: String): List<AdvisoryEntity>

    @Query("SELECT * FROM advisories ORDER BY countryId, destinationName COLLATE NOCASE")
    suspend fun getAll(): List<AdvisoryEntity>

    @Query("SELECT * FROM advisories WHERE countryId IN (:countryIds)")
    suspend fun getForCountries(countryIds: List<String>): List<AdvisoryEntity>

    @Upsert
    suspend fun upsertAll(advisories: List<AdvisoryEntity>)

    @Query("DELETE FROM advisories WHERE countryId IN (:countryIds)")
    suspend fun deleteForCountries(countryIds: List<String>)

    @Query("SELECT COUNT(*) FROM advisories")
    suspend fun count(): Int

    @Query("DELETE FROM advisories")
    suspend fun deleteAll()
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY position ASC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Query("SELECT * FROM watchlist ORDER BY position ASC")
    suspend fun getAll(): List<WatchlistEntity>

    @Query("SELECT * FROM watchlist WHERE countryId = :countryId LIMIT 1")
    suspend fun get(countryId: String): WatchlistEntity?

    @Upsert
    suspend fun upsert(entry: WatchlistEntity)

    @Upsert
    suspend fun upsertAll(entries: List<WatchlistEntity>)

    @Query("DELETE FROM watchlist WHERE countryId = :countryId")
    suspend fun delete(countryId: String)

    @Query("UPDATE watchlist SET notificationsEnabled = :enabled WHERE countryId = :countryId")
    suspend fun setNotifications(countryId: String, enabled: Boolean)

    @Query("UPDATE watchlist SET isFavorite = :favorite WHERE countryId = :countryId")
    suspend fun setFavorite(countryId: String, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM watchlist")
    suspend fun count(): Int

    @Query("DELETE FROM watchlist")
    suspend fun deleteAll()
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: HistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(events: List<HistoryEntity>): List<Long>

    @Query("SELECT * FROM history_events ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_events WHERE countryId = :countryId ORDER BY occurredAt DESC, id DESC")
    fun observeForCountry(countryId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history_events ORDER BY occurredAt DESC, id DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM history_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): HistoryEntity?

    @Query("SELECT MAX(occurredAt) FROM history_events")
    suspend fun latestEventAt(): Long?

    @Query("DELETE FROM history_events")
    suspend fun deleteAll()
}

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncStateEntity?

    @Upsert
    suspend fun upsert(state: SyncStateEntity)
    
    /**
     * Recovers stale "running" state that was interrupted by app crash or process termination.
     * Returns the recovered state if it was stale, or the original state if still valid.
     */
    suspend fun recoverStaleRunning(staleThresholdMs: Long = 30_000L): SyncStateEntity? {
        val state = get() ?: return null
        if (state.status != "running") return state
        
        val age = System.currentTimeMillis() - (state.lastAttemptAt ?: 0L)
        if (age < staleThresholdMs) return state
        
        val recovered = state.copy(
            status = "failed",
            message = state.message ?: "Synchronization was interrupted"
        )
        upsert(recovered)
        return recovered
    }
}

@Dao
interface WidgetConfigDao {
    @Query("SELECT * FROM widget_configs WHERE widgetId = :widgetId LIMIT 1")
    suspend fun get(widgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_configs ORDER BY widgetId")
    suspend fun getAll(): List<WidgetConfigEntity>

    @Upsert
    suspend fun upsert(config: WidgetConfigEntity)

    @Upsert
    suspend fun upsertAll(configs: List<WidgetConfigEntity>)

    @Query("DELETE FROM widget_configs WHERE widgetId = :widgetId")
    suspend fun delete(widgetId: Int)

    @Query("DELETE FROM widget_configs")
    suspend fun deleteAll()
}
