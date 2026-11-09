package com.drivelog.mobile.data

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** Blocking transport. Repository callers must dispatch onto an IO executor. */
class DriveLogApi(baseUrl: String, private val token: () -> String?) {
    private val base = baseUrl.trimEnd('/')
    init {
        val uri = URI(base)
        require(uri.scheme == "https" || (uri.scheme == "http" && uri.host == "10.0.2.2"))
        require(uri.userInfo == null && uri.query == null && uri.fragment == null)
    }

    fun claim(code: String): String {
        require(code.matches(Regex("[0-9]{6}")))
        return JSONObject(String(request("POST", "/link/claim",
            JSONObject().put("code", code).toString().toByteArray()), Charsets.UTF_8))
            .getString("accessToken").also { require(it.isNotBlank()) }
    }

    fun journey(): List<RecordDto> {
        val records = JSONObject(String(request("GET", "/journey"), Charsets.UTF_8)).getJSONArray("records")
        return List(records.length()) { RecordDto.parse(records.getJSONObject(it)) }
    }

    fun stamps(): org.json.JSONArray = org.json.JSONArray(String(request("GET", "/stamps"), Charsets.UTF_8))

    fun washRecommendation(region: String): JSONObject {
        require(region.matches(Regex("[A-Z]+")))
        return JSONObject(String(request("GET", "/wash/recommendation?region=$region"), Charsets.UTF_8))
    }

    fun photo(id: String): ByteArray = request("GET", path(id, "photo"))
    fun uploadPhoto(id: String, bytes: ByteArray, mime: String) {
        require(mime == "image/jpeg" || mime == "image/png")
        require(bytes.isNotEmpty() && bytes.size <= 15 * 1024 * 1024)
        request("PUT", path(id, "photo"), bytes, mime)
    }
    fun deletePhoto(id: String) { request("DELETE", path(id, "photo")) }
    fun saveCard(id: String, title: String, cardJson: String): RecordDto {
        require(title.length <= 40 && cardJson.length <= 2000)
        JSONObject(cardJson)
        val body = JSONObject().put("title", title).put("cardJson", cardJson)
        return RecordDto.parse(JSONObject(String(request("PUT", path(id, "card"),
            body.toString().toByteArray()), Charsets.UTF_8)))
    }

    private fun path(id: String, suffix: String): String {
        require(id.matches(Regex("[a-zA-Z0-9-]+")))
        return "/records/$id/$suffix"
    }

    private fun request(method: String, path: String, body: ByteArray? = null,
                        mime: String = "application/json"): ByteArray {
        val connection = URI("$base/api/v1$path").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            token()?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", mime)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw ApiFailure(status,
                connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceAtLeast(1))
            return if (status == 204) byteArrayOf() else connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 20 * 1024 * 1024) throw IOException("Response too large")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally { connection.disconnect() }
    }
}

class ApiFailure(val status: Int, val retryAfterSeconds: Long?) : IOException("Request failed ($status)")

data class RoutePoint(val latitude: Double, val longitude: Double, val epochMillis: Long)

data class RecordDto(
    val id: String, val placeName: String, val startedAtMillis: Long,
    val endedAtMillis: Long, val elapsedSeconds: Long, val distanceKm: Double?,
    val distanceSource: String, val status: String, val simulated: Boolean,
    val route: List<RoutePoint>, val title: String?, val cardJson: String?, val hasPhoto: Boolean
) {
    companion object {
        fun parse(json: JSONObject): RecordDto {
            val points = json.getJSONArray("route")
            return RecordDto(json.getString("id"), json.getString("placeName"),
                json.getLong("startedAtMillis"), json.getLong("endedAtMillis"),
                json.getLong("elapsedSeconds"), if (json.isNull("distanceKm")) null else json.getDouble("distanceKm"),
                json.getString("distanceSource"), json.getString("status"), json.getBoolean("simulated"),
                List(points.length()) { index ->
                    val point = points.getJSONArray(index)
                    RoutePoint(point.getDouble(0), point.getDouble(1), point.getLong(2))
                }, if (json.isNull("title")) null else json.getString("title"),
                if (json.isNull("cardJson")) null else json.getString("cardJson"), json.getBoolean("hasPhoto"))
        }
    }
}

