package com.drivelog.mobile.feature.wash

import org.json.JSONObject
import java.time.LocalDate

/** Only the new Wash response is translated here; existing API DTOs stay untouched. */
object WashResponseMapper {
    fun fromJson(json: JSONObject): WashUiState {
        val region = json.optString("regionName", "서울").ifBlank { "서울" }
        if (json.optString("availability") == "ERROR") return WashUiStates.error.copy(regionName = region)
        val source = if (json.optString("source") == "KMA") WashDataSourceStatus.KMA
            else WashDataSourceStatus.UNAVAILABLE
        val days = json.optJSONArray("dailyForecasts")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull()
                    ?: return@mapNotNull null
                val weather = runCatching { WashWeatherType.valueOf(item.optString("weatherType")) }
                    .getOrDefault(WashWeatherType.UNKNOWN)
                WashDayForecast(date, weather,
                    if (item.isNull("precipitationProbability")) null
                    else item.optInt("precipitationProbability").takeIf { it in 0..100 },
                    item.optBoolean("highlighted", false))
            }
        }.orEmpty()
        val status = runCatching {
            WashRecommendationStatus.valueOf(json.optString("recommendationStatus"))
        }.getOrDefault(WashRecommendationStatus.UNKNOWN)
        return WashUiState(
            regionName = region,
            recommendationStatus = status,
            headline = json.optString("headline", "이번 주 날씨 정보가 없어요"),
            summary = json.optString("summary", "예보가 준비되면 세차 추천을 확인할 수 있어요."),
            homeSummary = json.optString("homeSummary", "아직 날씨 정보를 확인할 수 없어요"),
            reason = json.optString("reason", ""),
            dailyForecasts = days,
            dataSourceStatus = source,
            forecastIssuedAt = json.optString("forecastIssuedAt").takeUnless { json.isNull("forecastIssuedAt") },
            lastUpdatedAt = json.optString("lastUpdatedAt").takeUnless { json.isNull("lastUpdatedAt") },
            stale = json.optBoolean("stale", false)
        )
    }
}
