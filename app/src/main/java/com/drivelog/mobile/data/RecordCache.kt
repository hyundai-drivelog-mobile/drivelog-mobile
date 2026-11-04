package com.drivelog.mobile.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class RecordCache(private val file: File) {
    fun read(): List<RecordDto> = runCatching {
        val array = JSONArray(file.readText())
        List(array.length()) { RecordDto.parse(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    fun write(records: List<RecordDto>) {
        val array = JSONArray()
        records.forEach { r -> array.put(JSONObject().put("id", r.id).put("placeName", r.placeName)
            .put("startedAtMillis", r.startedAtMillis).put("endedAtMillis", r.endedAtMillis).put("elapsedSeconds", r.elapsedSeconds)
            .put("distanceKm", r.distanceKm ?: JSONObject.NULL).put("distanceSource", r.distanceSource)
            .put("status", r.status).put("simulated", r.simulated).put("hasPhoto", r.hasPhoto)
            .put("title", r.title ?: JSONObject.NULL).put("cardJson", r.cardJson ?: JSONObject.NULL)
            .put("route", JSONArray().apply { r.route.forEach { put(JSONArray().put(it.latitude).put(it.longitude).put(it.epochMillis)) } })) }
        val temp = File(file.parentFile, "records.tmp")
        temp.writeText(array.toString()); check(temp.renameTo(file))
    }
}
