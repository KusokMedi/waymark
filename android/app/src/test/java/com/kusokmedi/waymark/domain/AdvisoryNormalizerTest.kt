package com.kusokmedi.waymark.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisoryNormalizerTest {
    private fun input(
        name: String? = "Finland",
        iso3: String? = "FIN",
        level: Int? = 1,
        url: String? = "https://travel.state.gov/content/travel/en/traveladvisories/traveladvisories/finland-travel-advisory.html",
        date: Long? = 1773374400000,
        globalId: String? = "g-1",
        objectId: Long? = 1,
        edit: Long? = null,
        adStatus: Int? = null,
        adText: String? = null,
        adDate: String? = null,
    ) = RawAdvisoryInput(
        objectId = objectId,
        globalId = globalId,
        name = name,
        iso3 = iso3,
        rawLevelCode = level,
        advisoryUrl = url,
        advisoryDateMillis = date,
        sourceEditDateMillis = edit,
        assistedDepartureStatus = adStatus,
        assistedDepartureText = adText,
        assistedDepartureDate = adDate,
    )

    @Test
    fun normalizesKnownFields() {
        val record = AdvisoryNormalizer.normalize(input())!!
        assertEquals("g-1", record.sourceRecordId)
        assertEquals("Finland", record.destinationName)
        assertEquals("FIN", record.isoAlpha3)
        assertEquals("FI", record.isoAlpha2)
        assertEquals(1, record.rawLevelCode)
        assertEquals(AdvisoryLevel.LEVEL_1, record.level)
        assertEquals("2026-03-13", record.advisoryDate)
        assertNotNull(record.advisoryUrl)
    }

    @Test
    fun missingNameIsSkipped() {
        assertNull(AdvisoryNormalizer.normalize(input(name = null)))
        assertNull(AdvisoryNormalizer.normalize(input(name = "   ")))
    }

    @Test
    fun unknownCodeIsPreservedAndNotLevelOne() {
        val record = AdvisoryNormalizer.normalize(input(level = 99))!!
        assertEquals(99, record.rawLevelCode)
        assertNull(record.level)
        assertEquals("Unsupported advisory code: 99", record.officialLabel)
    }

    @Test
    fun missingLevelIsNotLevelOne() {
        val record = AdvisoryNormalizer.normalize(input(level = null, date = null, url = null))!!
        assertNull(record.level)
        assertNull(record.advisoryDate)
        assertNull(record.advisoryUrl)
    }

    @Test
    fun invalidUrlIsDiscarded() {
        val record = AdvisoryNormalizer.normalize(input(url = "not a url"))!!
        assertNull(record.advisoryUrl)
    }

    @Test
    fun dateConversionUsesUtc() {
        // 2026-03-13T00:00:00Z
        val record = AdvisoryNormalizer.normalize(input(date = 1773360000000))!!
        assertEquals("2026-03-13", record.advisoryDate)
    }

    @Test
    fun sourceRecordIdFallsBackToObjectId() {
        val record = AdvisoryNormalizer.normalize(input(globalId = null, objectId = 42))!!
        assertEquals("arcgis-42", record.sourceRecordId)
    }

    @Test
    fun groupsRegionsUnderTheCountryRecord() {
        val records = listOf(
            AdvisoryNormalizer.normalize(input(name = "Mexico", iso3 = "MEX", level = null, objectId = 1, globalId = "m"))!!,
            AdvisoryNormalizer.normalize(input(name = "Jalisco", iso3 = "MEX", level = 3, objectId = 2, globalId = "j"))!!,
            AdvisoryNormalizer.normalize(input(name = "Yucatán", iso3 = "MEX", level = 1, objectId = 3, globalId = "y"))!!,
        )
        val grouped = AdvisoryNormalizer.groupIntoCountries(records)
        val mexico = grouped.first { it.isoAlpha3 == "MEX" }
        assertEquals("Mexico", mexico.name)
        assertEquals(3, mexico.records.size)
        assertEquals(2, mexico.records.count { it.recordKind == RecordKind.REGION })
        assertEquals(1, mexico.records.count { it.recordKind == RecordKind.COUNTRY })
    }

    @Test
    fun aggregateLevelStaysUnknownWhenRecordsDisagreeAndNoCountryRecordExists() {
        val records = listOf(
            AdvisoryNormalizer.normalize(input(name = "Alpha", iso3 = "AAA", level = 1, objectId = 1, globalId = "a"))!!,
            AdvisoryNormalizer.normalize(input(name = "Beta", iso3 = "AAA", level = 3, objectId = 2, globalId = "b"))!!,
        )
        val grouped = AdvisoryNormalizer.groupIntoCountries(records)
        assertNull(grouped.single().level)
    }

    @Test
    fun containsHigherRiskFlagIsPreserved() {
        val record = AdvisoryNormalizer.normalize(input(level = 30))!!
        assertTrue(record.containsHigherRiskAreas)
        assertEquals(AdvisoryLevel.LEVEL_3, record.level)
    }

    @Test
    fun assistedDepartureMetadataIsPreserved() {
        val record = AdvisoryNormalizer.normalize(
            input(adStatus = 2, adText = "Authorized departure", adDate = "2026-01-02")
        )!!
        assertEquals(2, record.assistedDepartureStatus)
        assertEquals("Authorized departure", record.assistedDepartureText)
        assertEquals("2026-01-02", record.assistedDepartureDate)
    }
}
