package com.drivelog.mobile.core.model

import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.data.RoutePoint
import java.util.concurrent.TimeUnit

data class JourneyUiState(val records: List<RecordDto> = emptyList(), val loading: Boolean = false, val demo: Boolean = false)

enum class CardFilter(val label: String, val tone: String) {
    ORIGINAL("Original", "color"), SOFT("Soft", "soft"), WARM("Warm", "warm"),
    FILM("Film", "film"), MONO("Mono", "mono")
}

enum class CardLayout(val label: String) {
    PHOTO("PHOTO"), JOURNAL("JOURNAL"), SPLIT("SPLIT"), STAT("STAT"), ROUTE("ROUTE")
}

data class CardDraft(
    val title: String = "",
    val filter: CardFilter = CardFilter.ORIGINAL,
    val layout: CardLayout = CardLayout.PHOTO,
    val showRoute: Boolean = true,
    val showDistance: Boolean = true,
    val showTime: Boolean = true,
    val showDate: Boolean = true,
    val showPlace: Boolean = true,
    val showEfficiency: Boolean = false,
    val trimEnds: Boolean = true,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
)

object DemoJourneys {
    private val now = System.currentTimeMillis()
    val records = listOf(
        journey("demo-1", "양양 해변", 96.4, 112, 1, false),
        journey("demo-2", "북악 스카이웨이", 32.8, 48, 4, false),
        journey("demo-3", "남한산성", 51.2, 67, 9, true)
    )

    private fun journey(id: String, place: String, distance: Double, minutes: Long, daysAgo: Long, simulated: Boolean): RecordDto {
        val start = now - TimeUnit.DAYS.toMillis(daysAgo)
        val points = listOf(
            RoutePoint(37.55, 127.0, start), RoutePoint(37.58, 127.03, start + 600_000),
            RoutePoint(37.57, 127.08, start + 1_200_000), RoutePoint(37.61, 127.12, start + 1_800_000),
            RoutePoint(37.60, 127.17, start + 2_400_000)
        )
        return RecordDto(id, place, start, start + minutes * 60_000, minutes * 60, distance,
            "GPS_ESTIMATED", "COMPLETE", simulated, points, null, null, false)
    }
}
