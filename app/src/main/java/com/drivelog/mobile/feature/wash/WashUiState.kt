package com.drivelog.mobile.feature.wash

import java.time.LocalDate

enum class WashRecommendationStatus { RECOMMENDED, POSTPONE, UNKNOWN }
enum class WashWeatherType { SUNNY, CLOUDY, RAINY, UNKNOWN }
enum class WashDataSourceStatus { DEMO, KMA, UNAVAILABLE }

data class WashDayForecast(
    val date: LocalDate,
    val weatherType: WashWeatherType,
    val precipitationProbability: Int?,
    val highlighted: Boolean = false
)

data class WashUiState(
    val regionName: String,
    val recommendationStatus: WashRecommendationStatus,
    val headline: String,
    val summary: String,
    val homeSummary: String,
    val reason: String,
    val dailyForecasts: List<WashDayForecast>,
    val dataSourceStatus: WashDataSourceStatus,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val forecastIssuedAt: String? = null,
    val lastUpdatedAt: String? = null,
    val stale: Boolean = false
)

object WashUiStates {
    val loading = WashUiState("서울", WashRecommendationStatus.UNKNOWN, "", "",
        "날씨 정보를 확인하고 있어요", "", emptyList(), WashDataSourceStatus.UNAVAILABLE,
        isLoading = true)
    val error = WashUiState("서울", WashRecommendationStatus.UNKNOWN,
        "날씨 정보를 불러오지 못했어요", "잠시 후 다시 확인해 주세요.",
        "아직 날씨 정보를 확인할 수 없어요", "", emptyList(), WashDataSourceStatus.UNAVAILABLE,
        errorMessage = "날씨 정보를 불러오지 못했어요")
}
