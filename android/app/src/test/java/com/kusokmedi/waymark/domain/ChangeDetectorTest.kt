package com.kusokmedi.waymark.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeDetectorTest {
    private fun snapshot(
        level: AdvisoryLevel? = AdvisoryLevel.LEVEL_1,
        regions: Set<String> = emptySet(),
        detailHash: String = "details-a",
        name: String = "Finland",
    ) = CountrySnapshot(
        countryId = "FIN",
        name = name,
        level = level,
        containsHigherRiskAreas = false,
        advisoryDate = "2026-03-13",
        officialLabel = level?.let(AdvisoryLevels::label),
        regionNames = regions,
        detailHash = detailHash,
    )

    @Test
    fun firstSyncCreatesNoEvents() {
        val events = ChangeDetector.detect(previous = null, current = snapshot(), firstSync = true)
        assertTrue(events.isEmpty())
    }

    @Test
    fun worseningIsDetectedOnce() {
        val events = ChangeDetector.detect(
            previous = snapshot(AdvisoryLevel.LEVEL_1),
            current = snapshot(AdvisoryLevel.LEVEL_3),
            firstSync = false,
        )
        assertEquals(1, events.size)
        assertEquals(ChangeType.WORSENED, events.single().type)
        assertTrue(events.single().message.contains("Reconsider Travel"))
    }

    @Test
    fun improvementIsDetected() {
        val events = ChangeDetector.detect(
            previous = snapshot(AdvisoryLevel.LEVEL_4),
            current = snapshot(AdvisoryLevel.LEVEL_2),
            firstSync = false,
        )
        assertEquals(ChangeType.IMPROVED, events.single().type)
    }

    @Test
    fun newRegionIsDetected() {
        val events = ChangeDetector.detect(
            previous = snapshot(regions = setOf("Jalisco")),
            current = snapshot(regions = setOf("Jalisco", "Yucatán")),
            firstSync = false,
        )
        assertEquals(ChangeType.REGIONAL, events.single().type)
        assertTrue(events.single().message.contains("Yucatán"))
    }

    @Test
    fun detailChangeIsDetectedWhenLevelAndRegionsAreStable() {
        val events = ChangeDetector.detect(
            previous = snapshot(detailHash = "a"),
            current = snapshot(detailHash = "b"),
            firstSync = false,
        )
        assertEquals(ChangeType.DETAILS, events.single().type)
    }

    @Test
    fun noEventWhenNothingChanged() {
        val events = ChangeDetector.detect(snapshot(), snapshot(), firstSync = false)
        assertTrue(events.isEmpty())
    }

    @Test
    fun unknownLevelsDoNotProduceMisleadingLevelChanges() {
        val events = ChangeDetector.detect(
            previous = snapshot(level = null),
            current = snapshot(level = null),
            firstSync = false,
        )
        assertTrue(events.isEmpty())
    }

    @Test
    fun fingerprintIsStableForTheSameChange() {
        val first = ChangeDetector.detect(snapshot(AdvisoryLevel.LEVEL_1), snapshot(AdvisoryLevel.LEVEL_2), false)
        val second = ChangeDetector.detect(snapshot(AdvisoryLevel.LEVEL_1), snapshot(AdvisoryLevel.LEVEL_2), false)
        assertEquals(first.single().fingerprint, second.single().fingerprint)
    }
}
