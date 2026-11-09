package com.drivelog.mobile.data

import org.json.JSONArray
import java.io.File

data class StampDto(val id: String, val name: String, val region: String, val area: String,
    val motto: String, val recordId: String, val visitedAtMillis: Long,
    val art: String = id, val tilt: Float = 0f) {
    companion object {
        fun parse(array: JSONArray): List<StampDto> = List(array.length()) { index ->
            val visit = array.getJSONObject(index)
            val place = visit.getJSONObject("place")
            StampDto(place.getString("id"), place.getString("name"), place.getString("region"),
                place.getString("area"), place.getString("motto"), visit.getString("recordId"), visit.getLong("visitedAtMillis"),
                place.optString("art", place.getString("id")),
                place.optDouble("tilt", 0.0).toFloat().takeIf { it.isFinite() } ?: 0f)
        }
    }
}

/** Stored in the token-scoped directory, just like records. Failed requests never replace it. */
class StampCache(private val file: File) {
    fun read(): List<StampDto> = runCatching { StampDto.parse(JSONArray(file.readText())) }.getOrDefault(emptyList())
    fun write(array: JSONArray) {
        StampDto.parse(array)
        val temp = File(file.parentFile, "stamps.tmp")
        temp.writeText(array.toString())
        check(temp.renameTo(file))
    }
}
