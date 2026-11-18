package com.kusokmedi.waymark.data.remote

import com.kusokmedi.waymark.domain.RawAdvisoryInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class FetchResult(
    val records: List<RawAdvisoryInput>,
    val etag: String?,
    val lastModified: String?,
    val notModified: Boolean,
)

class SourceException(message: String, cause: Throwable? = null) : IOException(message, cause)

class ArcGisClient(
    private val http: OkHttpClient = defaultClient(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun fetchAll(
        etag: String? = null,
        lastModified: String? = null,
        pageSize: Int = 2000,
    ): FetchResult = withContext(Dispatchers.IO) {
        val records = mutableListOf<RawAdvisoryInput>()
        var offset = 0
        var responseEtag: String? = null
        var responseLastModified: String? = null

        for (page in 0 until MAX_PAGES) {
            val requestBuilder = Request.Builder()
                .url(queryUrl(offset, pageSize))
                .get()
                .header("Accept", "application/json")
            if (page == 0) {
                etag?.let { requestBuilder.header("If-None-Match", it) }
                lastModified?.let { requestBuilder.header("If-Modified-Since", it) }
            }

            val response = try {
                http.newCall(requestBuilder.build()).execute()
            } catch (e: IOException) {
                throw SourceException("Network request failed: ${e.message}", e)
            }

            var pageComplete = false
            response.use { res ->
                if (page == 0) {
                    responseEtag = res.header("ETag")
                    responseLastModified = res.header("Last-Modified")
                }
                if (res.code == 304 && page == 0) {
                    return@withContext FetchResult(emptyList(), responseEtag, responseLastModified, true)
                }
                if (!res.isSuccessful) {
                    throw SourceException("Source request failed: HTTP ${res.code}")
                }
                val body = res.body?.string() ?: throw SourceException("Empty source response")
                val root = try {
                    json.parseToJsonElement(body)
                } catch (e: Exception) {
                    throw SourceException("Malformed source response", e)
                }
                val objectRoot = root as? JsonObject ?: throw SourceException("Unexpected source response shape")
                val error = objectRoot["error"] as? JsonObject
                if (error != null) {
                    val message = error.string("message") ?: "ArcGIS error"
                    throw SourceException(message)
                }
                val features = (objectRoot["features"] as? JsonArray)?.mapNotNull { feature ->
                    parseFeature(feature)
                } ?: emptyList()
                records += features
                val exceeded = (objectRoot["exceededTransferLimit"] as? JsonPrimitive)?.booleanOrNull ?: false
                if (!exceeded || features.isEmpty()) {
                    pageComplete = true
                } else {
                    offset += features.size
                }
            }
            if (pageComplete) break
        }

        FetchResult(records, responseEtag, responseLastModified, notModified = false)
    }

    private fun parseFeature(element: JsonElement): RawAdvisoryInput? {
        val feature = element as? JsonObject ?: return null
        val attributes = feature["attributes"] as? JsonObject ?: return null
        return RawAdvisoryInput(
            objectId = attributes.long("OBJECTID"),
            globalId = attributes.string("GlobalID"),
            name = attributes.string("NAME"),
            iso3 = attributes.string("ISO_3"),
            rawLevelCode = attributes.int("LEVEL_"),
            advisoryUrl = attributes.string("URL"),
            advisoryDateMillis = attributes.long("ADVDATE"),
            sourceEditDateMillis = attributes.long("EditDate"),
            assistedDepartureStatus = attributes.int("AD_OD_Status"),
            assistedDepartureText = attributes.string("AD_OD_Text"),
            assistedDepartureDate = attributes.string("AD_OD_Date"),
        )
    }

    private fun queryUrl(offset: Int, pageSize: Int) = "$SERVICE_URL/$LAYER_ID/query".toHttpUrl().newBuilder()
        .addQueryParameter("where", "NAME IS NOT NULL")
        .addQueryParameter("outFields", "*")
        .addQueryParameter("returnGeometry", "false")
        .addQueryParameter("resultOffset", offset.toString())
        .addQueryParameter("resultRecordCount", pageSize.toString())
        .addQueryParameter("orderByFields", "OBJECTID")
        .addQueryParameter("f", "json")
        .build()

    companion object {
        const val SERVICE_URL =
            "https://services6.arcgis.com/R6wlO6UHmSzqm9Vs/ArcGIS/rest/services/Travel_Advisory_Levels__%282024%29_View_Layer/FeatureServer"
        const val LAYER_ID = 54
        private const val MAX_PAGES = 50

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}

private fun JsonObject.string(key: String): String? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    val primitive = value as? JsonPrimitive ?: return null
    return primitive.content.takeIf { it.isNotBlank() && it != "null" }
}

private fun JsonObject.long(key: String): Long? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    val primitive = value as? JsonPrimitive ?: return null
    return primitive.longOrNull ?: primitive.doubleOrNull?.toLong()
}

private fun JsonObject.int(key: String): Int? = long(key)?.toInt()

private fun JsonObject.double(key: String): Double? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    return (value as? JsonPrimitive)?.doubleOrNull
}
