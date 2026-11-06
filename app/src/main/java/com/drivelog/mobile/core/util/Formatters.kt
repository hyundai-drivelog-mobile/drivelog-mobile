package com.drivelog.mobile.core.util

import com.drivelog.mobile.data.RecordDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun RecordDto.displayDate(): String = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(startedAtMillis))
fun RecordDto.displayTime(millis: Long): String = SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(millis))
fun RecordDto.displayDistance(): String = distanceKm?.let { String.format(Locale.KOREA, "%.1f km", it) } ?: "—"
fun RecordDto.displayDuration(): String = "${elapsedSeconds / 3600}시간 ${(elapsedSeconds % 3600) / 60}분".let {
    if (elapsedSeconds < 3600) "${elapsedSeconds / 60}분" else it
}
