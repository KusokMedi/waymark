package com.kusokmedi.waymark.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdvisoryLevelsTest {
    @Test
    fun mapsEveryKnownRawCodeExplicitly() {
        assertEquals(AdvisoryLevel.LEVEL_1, AdvisoryLevels.map(1).level)
        assertFalse(AdvisoryLevels.map(1).containsHigherRiskAreas)
        assertEquals(AdvisoryLevel.LEVEL_1, AdvisoryLevels.map(10).level)
        assertTrue(AdvisoryLevels.map(10).containsHigherRiskAreas)
        assertEquals(AdvisoryLevel.LEVEL_2, AdvisoryLevels.map(2).level)
        assertEquals(AdvisoryLevel.LEVEL_2, AdvisoryLevels.map(20).level)
        assertTrue(AdvisoryLevels.map(20).containsHigherRiskAreas)
        assertEquals(AdvisoryLevel.LEVEL_3, AdvisoryLevels.map(3).level)
        assertEquals(AdvisoryLevel.LEVEL_3, AdvisoryLevels.map(30).level)
        assertTrue(AdvisoryLevels.map(30).containsHigherRiskAreas)
        assertEquals(AdvisoryLevel.LEVEL_4, AdvisoryLevels.map(4).level)
        assertFalse(AdvisoryLevels.map(4).containsHigherRiskAreas)
    }

    @Test
    fun doesNotInterpretSpecialCodesAsNumericLevels() {
        // Raw 10 is Level 1, not an "extra level" and not Level 4.
        assertEquals(1, AdvisoryLevels.map(10).level?.number)
        assertEquals(1, AdvisoryLevels.map(20).level?.number?.minus(1))
        assertEquals(3, AdvisoryLevels.map(30).level?.number)
    }

    @Test
    fun unknownCodeStaysUnknown() {
        val mapping = AdvisoryLevels.map(99)
        assertNull(mapping.level)
        assertFalse(mapping.containsHigherRiskAreas)
        assertTrue(mapping.officialLabel!!.contains("Unsupported"))
    }

    @Test
    fun missingCodeStaysUnknown() {
        val mapping = AdvisoryLevels.map(null)
        assertNull(mapping.level)
        assertNull(mapping.officialLabel)
        assertEquals("Unknown", AdvisoryLevels.label(mapping.level))
    }

    @Test
    fun severityIsExplicit() {
        assertEquals(1, AdvisoryLevels.severity(AdvisoryLevel.LEVEL_1))
        assertEquals(4, AdvisoryLevels.severity(AdvisoryLevel.LEVEL_4))
        assertNull(AdvisoryLevels.severity(null))
    }

    @Test
    fun isoCatalogContainsCountries() {
        assertTrue(IsoCountries.all.size >= 200)
        assertEquals("FI", IsoCountries.alpha2("FIN"))
        assertEquals("FIN", IsoCountries.alpha3("FI"))
        assertEquals("Finland", IsoCountries.name("FIN"))
        assertNull(IsoCountries.alpha2("XXX"))
    }
}
