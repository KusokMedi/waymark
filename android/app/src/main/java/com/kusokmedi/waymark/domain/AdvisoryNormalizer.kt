package com.kusokmedi.waymark.domain

data class RawAdvisoryInput(
    val objectId: Long?,
    val globalId: String?,
    val name: String?,
    val iso3: String?,
    val rawLevelCode: Int?,
    val advisoryUrl: String?,
    val advisoryDateMillis: Long?,
    val sourceEditDateMillis: Long?,
    val assistedDepartureStatus: Int?,
    val assistedDepartureText: String?,
    val assistedDepartureDate: String?,
)

data class NormalizedRecord(
    val sourceRecordId: String,
    val destinationName: String,
    val isoAlpha3: String?,
    val isoAlpha2: String?,
    val rawLevelCode: Int?,
    val level: AdvisoryLevel?,
    val containsHigherRiskAreas: Boolean,
    val officialLabel: String?,
    val advisoryUrl: String?,
    val advisoryDate: String?,
    val sourceEditDate: String?,
    val assistedDepartureStatus: Int?,
    val assistedDepartureText: String?,
    val assistedDepartureDate: String?,
    val recordKind: RecordKind,
    val contentHash: String,
)

enum class RecordKind { COUNTRY, REGION }

data class CountryAggregate(
    val id: String,
    val isoAlpha3: String?,
    val isoAlpha2: String?,
    val name: String,
    val level: AdvisoryLevel?,
    val containsHigherRiskAreas: Boolean,
    val officialLabel: String?,
    val advisoryUrl: String?,
    val advisoryDate: String?,
    val assistedDepartureStatus: Int?,
    val assistedDepartureText: String?,
    val assistedDepartureDate: String?,
    val records: List<NormalizedRecord>,
)

object AdvisoryNormalizer {
    fun normalize(input: RawAdvisoryInput, fallbackIndex: Int = 0): NormalizedRecord? {
        val name = input.name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val iso3 = input.iso3?.trim()?.uppercase()?.takeIf { it.matches(Regex("[A-Z]{3}")) }
        val iso2 = IsoCountries.alpha2(iso3)
        val mapping = AdvisoryLevels.map(input.rawLevelCode)
        val url = validateUrl(input.advisoryUrl)
        val advisoryDate = millisToUtcDate(input.advisoryDateMillis)
        val sourceRecordId = input.globalId?.takeIf { it.isNotBlank() }
            ?: input.objectId?.let { "arcgis-$it" }
            ?: "fallback-${iso3 ?: "name"}-${name.lowercase()}-$fallbackIndex"

        val hash = contentHash(
            level = mapping.level,
            higherRisk = mapping.containsHigherRiskAreas,
            label = mapping.officialLabel,
            url = url,
            date = advisoryDate,
            departed = input.assistedDepartureStatus,
            departureText = input.assistedDepartureText?.trim()?.lowercase(),
            departureDate = input.assistedDepartureDate?.trim(),
        )

        return NormalizedRecord(
            sourceRecordId = sourceRecordId,
            destinationName = name,
            isoAlpha3 = iso3,
            isoAlpha2 = iso2,
            rawLevelCode = input.rawLevelCode,
            level = mapping.level,
            containsHigherRiskAreas = mapping.containsHigherRiskAreas,
            officialLabel = mapping.officialLabel,
            advisoryUrl = url,
            advisoryDate = advisoryDate,
            sourceEditDate = millisToUtcIso(input.sourceEditDateMillis),
            assistedDepartureStatus = input.assistedDepartureStatus,
            assistedDepartureText = input.assistedDepartureText?.trim()?.takeIf { it.isNotEmpty() },
            assistedDepartureDate = input.assistedDepartureDate?.trim()?.takeIf { it.isNotEmpty() },
            recordKind = RecordKind.COUNTRY,
            contentHash = hash,
        )
    }

    fun groupIntoCountries(records: List<NormalizedRecord>): List<CountryAggregate> {
        val groups = records.groupBy { it.isoAlpha3 ?: "name:${it.destinationName.lowercase()}" }
        return groups.map { (key, group) ->
            val iso3 = group.firstOrNull()?.isoAlpha3
            val officialName = IsoCountries.name(iso3)
            val countryRecord = group.firstOrNull {
                it.isoAlpha3 != null && officialName != null &&
                    it.destinationName.equals(officialName, ignoreCase = true)
            }
            val classified = group.map { record ->
                if (group.size == 1 || record === countryRecord) {
                    record.copy(recordKind = RecordKind.COUNTRY)
                } else {
                    record.copy(recordKind = RecordKind.REGION)
                }
            }

            val level = countryRecord?.level ?: classified
                .mapNotNull { it.level }
                .distinct()
                .singleOrNull()

            CountryAggregate(
                id = iso3 ?: key,
                isoAlpha3 = iso3,
                isoAlpha2 = group.firstOrNull()?.isoAlpha2 ?: IsoCountries.alpha2(iso3),
                name = countryRecord?.destinationName ?: officialName ?: group.first().destinationName,
                level = level,
                containsHigherRiskAreas = classified.any { it.containsHigherRiskAreas },
                officialLabel = countryRecord?.officialLabel
                    ?: if (level == null) classified.firstOrNull { it.officialLabel != null }?.officialLabel else null,
                advisoryUrl = countryRecord?.advisoryUrl ?: classified.firstOrNull { it.advisoryUrl != null }?.advisoryUrl,
                advisoryDate = countryRecord?.advisoryDate ?: classified.firstOrNull { it.advisoryDate != null }?.advisoryDate,
                assistedDepartureStatus = countryRecord?.assistedDepartureStatus,
                assistedDepartureText = countryRecord?.assistedDepartureText,
                assistedDepartureDate = countryRecord?.assistedDepartureDate,
                records = classified.sortedBy { it.destinationName },
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun validateUrl(value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            val uri = java.net.URI(trimmed)
            if (uri.scheme == "https" || uri.scheme == "http") uri.toString() else null
        } catch (_: Exception) {
            null
        }
    }

    private fun millisToUtcDate(millis: Long?): String? {
        if (millis == null) return null
        return try {
            java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun millisToUtcIso(millis: Long?): String? {
        if (millis == null) return null
        return try {
            java.time.Instant.ofEpochMilli(millis).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun contentHash(
        level: AdvisoryLevel?,
        higherRisk: Boolean,
        label: String?,
        url: String?,
        date: String?,
        departed: Int?,
        departureText: String?,
        departureDate: String?,
    ): String {
        val payload = listOf(level?.number, higherRisk, label, url, date, departed, departureText, departureDate)
            .joinToString("|") { it?.toString() ?: "" }
        return payload.hashCode().toString(16) + "-" + payload.length
    }
}
