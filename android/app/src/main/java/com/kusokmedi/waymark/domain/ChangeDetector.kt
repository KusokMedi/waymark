package com.kusokmedi.waymark.domain

enum class ChangeType { WORSENED, IMPROVED, REGIONAL, DETAILS }

data class CountrySnapshot(
    val countryId: String,
    val name: String,
    val level: AdvisoryLevel?,
    val containsHigherRiskAreas: Boolean,
    val advisoryDate: String?,
    val officialLabel: String?,
    val regionNames: Set<String>,
    val detailHash: String,
)

data class AdvisoryChange(
    val type: ChangeType,
    val countryId: String,
    val countryName: String,
    val previousLevel: AdvisoryLevel?,
    val newLevel: AdvisoryLevel?,
    val title: String,
    val message: String,
    val fingerprint: String,
)

object ChangeDetector {
    fun snapshotOf(country: CountryAggregate): CountrySnapshot {
        val regions = country.records.filter { it.recordKind == RecordKind.REGION }
        return CountrySnapshot(
            countryId = country.id,
            name = country.name,
            level = country.level,
            containsHigherRiskAreas = country.containsHigherRiskAreas,
            advisoryDate = country.advisoryDate,
            officialLabel = country.officialLabel,
            regionNames = regions.map { it.destinationName }.toSet(),
            detailHash = country.records.map { it.contentHash }.sorted().joinToString(","),
        )
    }

    fun detect(previous: CountrySnapshot?, current: CountrySnapshot, firstSync: Boolean): List<AdvisoryChange> {
        // The first synchronization establishes a baseline. It must not produce events.
        if (firstSync || previous == null) return emptyList()

        val changes = mutableListOf<AdvisoryChange>()

        val previousSeverity = AdvisoryLevels.severity(previous.level)
        val currentSeverity = AdvisoryLevels.severity(current.level)
        if (previousSeverity != null && currentSeverity != null && previousSeverity != currentSeverity) {
            val type = if (currentSeverity > previousSeverity) ChangeType.WORSENED else ChangeType.IMPROVED
            val verb = if (type == ChangeType.WORSENED) "worsened" else "improved"
            changes += AdvisoryChange(
                type = type,
                countryId = current.countryId,
                countryName = current.name,
                previousLevel = previous.level,
                newLevel = current.level,
                title = "${current.name} advisory $verb",
                message = "${AdvisoryLevels.shortLabel(previous.level)} to ${AdvisoryLevels.shortLabel(current.level)}: " +
                    AdvisoryLevels.label(current.level),
                fingerprint = fingerprint(current, type, "${previous.level?.number}->${current.level?.number}"),
            )
        }

        val newRegions = current.regionNames - previous.regionNames
        if (newRegions.isNotEmpty()) {
            changes += AdvisoryChange(
                type = ChangeType.REGIONAL,
                countryId = current.countryId,
                countryName = current.name,
                previousLevel = previous.level,
                newLevel = current.level,
                title = "New regional advisory for ${current.name}",
                message = newRegions.sorted().joinToString(", "),
                fingerprint = fingerprint(current, ChangeType.REGIONAL, newRegions.sorted().joinToString(",")),
            )
        }

        val levelChanged = previousSeverity != currentSeverity
        val regionsChanged = newRegions.isNotEmpty() || (previous.regionNames - current.regionNames).isNotEmpty()
        if (previous.detailHash != current.detailHash && !levelChanged && !regionsChanged) {
            changes += AdvisoryChange(
                type = ChangeType.DETAILS,
                countryId = current.countryId,
                countryName = current.name,
                previousLevel = previous.level,
                newLevel = current.level,
                title = "${current.name} advisory details changed",
                message = "The official advisory text or metadata was updated.",
                fingerprint = fingerprint(current, ChangeType.DETAILS, current.detailHash),
            )
        }

        return changes
    }

    private fun fingerprint(snapshot: CountrySnapshot, type: ChangeType, suffix: String): String =
        "${snapshot.countryId}|${type.name}|$suffix"
}
