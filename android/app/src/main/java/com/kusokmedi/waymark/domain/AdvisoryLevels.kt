package com.kusokmedi.waymark.domain

/**
 * Advisory levels published by the U.S. Department of State.
 *
 * The numeric value is the public "Level 1..4" label, not the raw ArcGIS code.
 * Raw codes 10, 20 and 30 are special: they share the base level but denote
 * countries that contain areas with a higher security risk. See [AdvisoryLevels].
 */
enum class AdvisoryLevel(val number: Int) {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4);

    companion object {
        fun fromNumber(value: Int?): AdvisoryLevel? = entries.firstOrNull { it.number == value }
    }
}

data class LevelMapping(
    val level: AdvisoryLevel?,
    val containsHigherRiskAreas: Boolean,
    val officialLabel: String?,
)

object AdvisoryLevels {
    const val LEVEL_1_LABEL = "Exercise Normal Precautions"
    const val LEVEL_2_LABEL = "Exercise Increased Caution"
    const val LEVEL_3_LABEL = "Reconsider Travel"
    const val LEVEL_4_LABEL = "Do Not Travel"

    // Explicit mapping. Never derive by arithmetic or by dividing the raw code.
    private val rawCodes: Map<Int, LevelMapping> = mapOf(
        1 to LevelMapping(AdvisoryLevel.LEVEL_1, false, LEVEL_1_LABEL),
        10 to LevelMapping(
            AdvisoryLevel.LEVEL_1,
            true,
            "$LEVEL_1_LABEL - Contains Areas with Higher Security Risk",
        ),
        2 to LevelMapping(AdvisoryLevel.LEVEL_2, false, LEVEL_2_LABEL),
        20 to LevelMapping(
            AdvisoryLevel.LEVEL_2,
            true,
            "$LEVEL_2_LABEL - Contains Areas with Higher Security Risk",
        ),
        3 to LevelMapping(AdvisoryLevel.LEVEL_3, false, LEVEL_3_LABEL),
        30 to LevelMapping(
            AdvisoryLevel.LEVEL_3,
            true,
            "$LEVEL_3_LABEL - Contains Areas with Higher Security Risk",
        ),
        4 to LevelMapping(AdvisoryLevel.LEVEL_4, false, LEVEL_4_LABEL),
    )

    fun map(rawCode: Int?): LevelMapping {
        if (rawCode == null) return LevelMapping(null, false, null)
        return rawCodes[rawCode] ?: LevelMapping(
            level = null,
            containsHigherRiskAreas = false,
            officialLabel = "Unsupported advisory code: $rawCode",
        )
    }

    fun isKnown(rawCode: Int?): Boolean = rawCode != null && rawCodes.containsKey(rawCode)

    /** Higher number means more severe. Unknown values are not assigned a severity. */
    fun severity(level: AdvisoryLevel?): Int? = level?.number

    fun label(level: AdvisoryLevel?): String = when (level) {
        AdvisoryLevel.LEVEL_1 -> LEVEL_1_LABEL
        AdvisoryLevel.LEVEL_2 -> LEVEL_2_LABEL
        AdvisoryLevel.LEVEL_3 -> LEVEL_3_LABEL
        AdvisoryLevel.LEVEL_4 -> LEVEL_4_LABEL
        null -> "Unknown"
    }

    fun shortLabel(level: AdvisoryLevel?): String = when (level) {
        null -> "Unknown level"
        else -> "Level ${level.number}"
    }
}
