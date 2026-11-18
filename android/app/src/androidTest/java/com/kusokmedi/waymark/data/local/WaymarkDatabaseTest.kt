package com.kusokmedi.waymark.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaymarkDatabaseTest {
    private lateinit var db: WaymarkDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WaymarkDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun storesCountryAdvisoryAndWatchlist() = runBlocking {
        db.countryDao().upsertAll(listOf(CountryEntity("FIN", "FI", "FIN", "Finland", 1)))
        db.advisoryDao().upsertAll(
            listOf(
                AdvisoryEntity(
                    sourceRecordId = "g-1",
                    countryId = "FIN",
                    destinationName = "Finland",
                    isoAlpha3 = "FIN",
                    rawLevelCode = 10,
                    levelNumber = 1,
                    containsHigherRiskAreas = true,
                    officialLabel = "Exercise Normal Precautions",
                    advisoryUrl = "https://example.com",
                    advisoryDate = "2026-03-13",
                    sourceEditDate = null,
                    assistedDepartureStatus = null,
                    assistedDepartureText = null,
                    assistedDepartureDate = null,
                    recordKind = "COUNTRY",
                    contentHash = "hash",
                    fetchedAt = 1,
                )
            )
        )
        db.watchlistDao().upsert(WatchlistEntity("FIN", 0, true, true, 1))

        val country = db.countryDao().getById("FIN")
        assertNotNull(country)
        val advisories = db.advisoryDao().getForCountry("FIN")
        assertEquals(1, advisories.size)
        assertEquals(1, advisories.single().levelNumber)
        assertEquals(true, advisories.single().containsHigherRiskAreas)
        assertEquals(1, db.watchlistDao().observeAll().first().size)
    }

    @Test
    fun widgetConfigurationsAreIndependent() = runBlocking {
        db.widgetConfigDao().upsert(WidgetConfigEntity(1, "compact", "FIN", true, true, true, true, "surface", 1))
        db.widgetConfigDao().upsert(WidgetConfigEntity(2, "grid", "SWE,NOR", false, true, false, true, "transparent", 2))
        assertEquals("compact", db.widgetConfigDao().get(1)?.preset)
        assertEquals("SWE,NOR", db.widgetConfigDao().get(2)?.countryIds)
        assertEquals(2, db.widgetConfigDao().getAll().size)
    }

    @Test
    fun historyFingerprintIsDeduplicated() = runBlocking {
        val event = HistoryEntity(
            countryId = "FIN",
            eventType = "WORSENED",
            previousLevel = 1,
            newLevel = 2,
            title = "Finland advisory worsened",
            message = "Level 1 to Level 2",
            occurredAt = 1,
            fingerprint = "FIN|WORSENED|1->2",
        )
        db.countryDao().upsertAll(listOf(CountryEntity("FIN", "FI", "FIN", "Finland", 1)))
        db.historyDao().insertIgnore(event)
        db.historyDao().insertIgnore(event.copy(id = 0))
        assertEquals(1, db.historyDao().getAll().size)
    }

    @Test
    fun deletingCountryCascades() = runBlocking {
        db.countryDao().upsertAll(listOf(CountryEntity("FIN", "FI", "FIN", "Finland", 1)))
        db.watchlistDao().upsert(WatchlistEntity("FIN", 0, false, false, 1))
        db.countryDao().deleteAll()
        assertNull(db.watchlistDao().get("FIN"))
    }
}
