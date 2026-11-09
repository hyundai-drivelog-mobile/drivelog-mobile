package com.drivelog.mobile.feature.wash

import java.time.LocalDate

/** Design-only values. No weather or location service is queried in Task 1. */
object WashPreviewData {
    private val monday = LocalDate.of(2026, 11, 9)

    val postpone = WashUiState(
        regionName = "서울",
        recommendationStatus = WashRecommendationStatus.POSTPONE,
        headline = "이번 주는\n세차를 미루세요",
        summary = "금요일과 토요일에 비가 예보되어 있어요.",
        homeSummary = "금요일·토요일 비 예보 · 세차 미루기",
        reason = "세차 직후 비가 올 가능성이 높아\n다음 맑은 날을 추천해요.",
        dailyForecasts = listOf(
            WashDayForecast(monday, WashWeatherType.SUNNY, 10),
            WashDayForecast(monday.plusDays(1), WashWeatherType.SUNNY, 0),
            WashDayForecast(monday.plusDays(2), WashWeatherType.SUNNY, 10),
            WashDayForecast(monday.plusDays(3), WashWeatherType.CLOUDY, 20),
            WashDayForecast(monday.plusDays(4), WashWeatherType.RAINY, 80),
            WashDayForecast(monday.plusDays(5), WashWeatherType.RAINY, 70),
            WashDayForecast(monday.plusDays(6), WashWeatherType.SUNNY, 20)
        ),
        dataSourceStatus = WashDataSourceStatus.DEMO
    )

    val recommended = postpone.copy(
        recommendationStatus = WashRecommendationStatus.RECOMMENDED,
        headline = "화요일에\n세차를 추천해요",
        summary = "화요일은 맑고 강수확률이 낮아요.",
        homeSummary = "화요일 세차 추천",
        reason = "화요일은 맑고 이번 주 강수확률이 가장 낮아요.",
        dailyForecasts = postpone.dailyForecasts.mapIndexed { index, day ->
            day.copy(weatherType = if (index == 3) WashWeatherType.CLOUDY else WashWeatherType.SUNNY,
                precipitationProbability = listOf(10, 0, 10, 20, 10, 10, 20)[index], highlighted = index == 1)
        }
    )

    val empty = postpone.copy(
        recommendationStatus = WashRecommendationStatus.UNKNOWN,
        headline = "이번 주 날씨 정보가 없어요",
        summary = "예보가 준비되면 세차 추천을 확인할 수 있어요.",
        homeSummary = "아직 날씨 정보를 확인할 수 없어요",
        reason = "",
        dailyForecasts = emptyList()
    )

    val loading = empty.copy(isLoading = true)
    val error = empty.copy(
        headline = "날씨 정보를 불러오지 못했어요",
        summary = "잠시 후 다시 확인해 주세요.",
        errorMessage = "날씨 정보를 불러오지 못했어요"
    )
}
