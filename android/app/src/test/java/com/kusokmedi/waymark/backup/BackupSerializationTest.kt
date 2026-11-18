package com.kusokmedi.waymark.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSerializationTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun roundTripsJsonBackup() {
        val file = BackupFile(
            appVersion = "1.0.0",
            exportedAt = "2026-11-18T00:00:00Z",
            countries = listOf(BackupCountry("FIN", "FI", "FIN", "Finland", 1)),
            advisories = listOf(
                BackupAdvisory(
                    sourceRecordId = "g-1",
                    countryId = "FIN",
                    destinationName = "Finland",
                    isoAlpha3 = "FIN",
                    rawLevelCode = 1,
                    levelNumber = 1,
                    advisoryDate = "2026-03-13",
                    recordKind = "COUNTRY",
                    contentHash = "abc",
                )
            ),
            watchlist = listOf(BackupWatchlist("FIN", 0, true, true, 1)),
            history = listOf(
                BackupHistory(
                    countryId = "FIN",
                    eventType = "WORSENED",
                    previousLevel = 1,
                    newLevel = 2,
                    title = "Finland advisory worsened",
                    message = "Level 1 to Level 2",
                    occurredAt = 1,
                    fingerprint = "FIN|WORSENED|1->2",
                )
            ),
            widgets = listOf(BackupWidget(10, "list", "FIN,SWE", true, true, true, true, "surface", 1)),
            settings = BackupSettings(),
        )
        val encoded = json.encodeToString(BackupFile.serializer(), file)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)
        assertEquals(file.countries, decoded.countries)
        assertEquals(file.advisories, decoded.advisories)
        assertEquals(file.watchlist, decoded.watchlist)
        assertEquals(file.history, decoded.history)
        assertEquals(file.widgets, decoded.widgets)
        assertEquals(file.settings, decoded.settings)
    }

    @Test
    fun ignoresUnknownFutureFields() {
        val raw = """
            {
              "schemaVersion": 1,
              "appId": "com.kusokmedi.waymark",
              "futureField": {"nested": true},
              "countries": [{"id":"FIN","name":"Finland","newField":42}]
            }
        """.trimIndent()
        val decoded = json.decodeFromString(BackupFile.serializer(), raw)
        assertEquals("FIN", decoded.countries.single().id)
        assertEquals("Finland", decoded.countries.single().name)
    }

    @Test
    fun defaultSchemaIsCurrent() {
        assertTrue(BackupFile().schemaVersion == BACKUP_SCHEMA_VERSION)
        assertEquals(BACKUP_APP_ID, BackupFile().appId)
    }
}
