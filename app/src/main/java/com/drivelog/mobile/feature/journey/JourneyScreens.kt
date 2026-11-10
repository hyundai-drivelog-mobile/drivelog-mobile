package com.drivelog.mobile.feature.journey

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.designsystem.picker.MonthWheelPickerBottomSheet
import com.drivelog.mobile.core.designsystem.picker.YearWheelPickerBottomSheet
import com.drivelog.mobile.core.model.DemoJourneys
import com.drivelog.mobile.core.util.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.data.RoutePoint
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import kotlin.math.ceil

private enum class DrivePeriod(val label: String) { WEEK("주"), MONTH("월"), YEAR("년"), ALL("전체") }

private fun periodStart(period: DrivePeriod, anchor: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = anchor }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    when (period) {
        DrivePeriod.WEEK -> cal.add(Calendar.DAY_OF_YEAR, -((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7))
        DrivePeriod.MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
        DrivePeriod.YEAR -> cal.set(Calendar.DAY_OF_YEAR, 1)
        DrivePeriod.ALL -> return Long.MIN_VALUE
    }
    return cal.timeInMillis
}

private fun periodEnd(period: DrivePeriod, start: Long): Long {
    if (period == DrivePeriod.ALL) return Long.MAX_VALUE
    return Calendar.getInstance().apply {
        timeInMillis = start
        when (period) {
            DrivePeriod.WEEK -> add(Calendar.DAY_OF_YEAR, 7)
            DrivePeriod.MONTH -> add(Calendar.MONTH, 1)
            DrivePeriod.YEAR -> add(Calendar.YEAR, 1)
            DrivePeriod.ALL -> Unit
        }
    }.timeInMillis
}

private fun dateAt(year: Int, month: Int): Long = Calendar.getInstance().apply {
    clear()
    set(year, month, 1)
}.timeInMillis

private fun periodLabel(period: DrivePeriod, anchor: Long, records: List<RecordDto>): String {
    val cal = Calendar.getInstance().apply { timeInMillis = anchor }
    return when (period) {
        DrivePeriod.YEAR -> "${cal.get(Calendar.YEAR)}년"
        DrivePeriod.MONTH -> String.format(Locale.KOREA, "%d년 %02d월", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        DrivePeriod.WEEK -> "이번 주"
        DrivePeriod.ALL -> {
            val years = records.map { it.year() }.distinct().sorted()
            when (years.size) {
                0 -> "기록 없음"
                1 -> "${years.first()}년"
                else -> "${years.first()}~${years.last()}년"
            }
        }
    }
}

private fun periodChoices(period: DrivePeriod, records: List<RecordDto>, anchor: Long): List<Long> {
    val recordedYears = records.map { it.year() }.distinct().ifEmpty {
        listOf(Calendar.getInstance().get(Calendar.YEAR))
    }
    val years = (recordedYears.min()..recordedYears.max()).toList()
    return when (period) {
        DrivePeriod.YEAR -> years.map { dateAt(it, Calendar.JANUARY) }.sortedDescending()
        DrivePeriod.MONTH -> years.flatMap { year -> (0..11).map { month -> dateAt(year, month) } }.sortedDescending()
        DrivePeriod.WEEK -> emptyList()
        DrivePeriod.ALL -> emptyList()
    }
}

private fun RecordDto.year(): Int = Calendar.getInstance().apply { timeInMillis = startedAtMillis }.get(Calendar.YEAR)
private fun distance(value: Double): String = String.format(Locale.KOREA, "%,.1f", value)
private fun duration(seconds: Long): String = if (seconds >= 3600) "${seconds / 3600}시간 ${(seconds % 3600) / 60}분" else "${seconds / 60}분"

@Composable private fun DriveOverview(records: List<RecordDto>, period: DrivePeriod, anchor: Long,
    allRecords: List<RecordDto>, onRequestPicker: () -> Unit) {
    val totalKm = records.sumOf { it.distanceKm ?: 0.0 }
    val measured = records.filter { it.distanceKm != null }
    Row(Modifier.clickable(enabled = period == DrivePeriod.YEAR || period == DrivePeriod.MONTH, onClick = onRequestPicker),
        verticalAlignment = Alignment.CenterVertically) {
        Text(periodLabel(period, anchor, allRecords), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), color = DriveColors.Gray700)
        if (period == DrivePeriod.YEAR || period == DrivePeriod.MONTH) {
            Spacer(Modifier.width(5.dp))
            DriveIcon(DriveIconType.CHEVRON, DriveColors.Gray700, Modifier.size(17.dp).rotate(90f))
        }
    }
    Spacer(Modifier.height(18.dp))
    Text(if (records.isEmpty()) "0.0" else if (measured.isNotEmpty()) distance(totalKm) else "—", style = MaterialTheme.typography.displayLarge.copy(fontSize = 56.sp, lineHeight = 62.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic),
        color = DriveColors.Black, maxLines = 1)
    Spacer(Modifier.height(8.dp))
    Text("킬로미터", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), color = DriveColors.Gray500)
    Spacer(Modifier.height(26.dp))
    val summaryWidths = listOf(.25f, .29f, .46f)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("${records.size}",
            if (records.isEmpty()) "0.0" else if (measured.isEmpty()) "—" else distance(measured.sumOf { it.distanceKm ?: 0.0 } / measured.size),
            duration(records.sumOf { it.elapsedSeconds })).forEachIndexed { index, value ->
            Text(value, Modifier.weight(summaryWidths[index]).alignByBaseline(), style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf("드라이브 수", "평균 거리", "총 운행시간").forEachIndexed { index, label ->
            Text(label, Modifier.weight(summaryWidths[index]).alignByBaseline(), style = MaterialTheme.typography.labelSmall,
                color = DriveColors.Gray500, maxLines = 1)
        }
    }
    Spacer(Modifier.height(38.dp))
    val byYear = records.filter { it.distanceKm != null }.groupBy { it.year() }.toSortedMap()
    if (byYear.isEmpty()) Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val left = 51.dp.toPx()
        val width = (size.width - left - 18.dp.toPx()).coerceAtLeast(0f)
        (0..5).forEach { tick ->
            val x = left + width * tick / 5f
            drawLine(DriveColors.Gray200, Offset(x, 0f), Offset(x, size.height - 18.dp.toPx()), strokeWidth = .5.dp.toPx())
        }
    }
    else YearDistanceChart(byYear.mapValues { (_, yearly) -> yearly.sumOf { it.distanceKm ?: 0.0 } }, period, anchor)
}

@Composable private fun YearDistanceChart(values: Map<Int, Double>, period: DrivePeriod, anchor: Long) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(period, anchor, values) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing))
    }
    val maxKm = values.values.maxOrNull() ?: 0.0
    val step = (ceil(maxKm / 50.0) * 10.0).coerceAtLeast(10.0)
    val chartMax = step * 5.0
    values.forEach { (year, km) ->
        Row(Modifier.fillMaxWidth().height(38.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(year.toString(), Modifier.width(43.dp), style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray500)
            Spacer(Modifier.width(8.dp))
            BoxWithConstraints(Modifier.weight(1f).height(20.dp)) {
                val fraction = (km / chartMax).toFloat().coerceIn(0f, 1f) * reveal.value
                val plotWidth = maxWidth - 18.dp
                Canvas(Modifier.fillMaxSize()) {
                    val chartWidth = size.width - 18.dp.toPx()
                    (0..5).forEach { tick ->
                        val x = chartWidth * tick / 5f
                        drawLine(DriveColors.Gray200, Offset(x, 0f), Offset(x, size.height), strokeWidth = .5.dp.toPx())
                    }
                    val length = chartWidth * fraction
                    val dash = 10.dp.toPx()
                    val gap = 4.dp.toPx()
                    val band = 6.dp.toPx()
                    val stagger = (dash + gap) / 2f
                    for (track in 0..1) {
                        var x = if (track == 0) 0f else -stagger
                        val y = if (track == 0) 1.dp.toPx() else 10.dp.toPx()
                        while (x < length) {
                            val left = x.coerceAtLeast(0f)
                            val right = (x + dash).coerceAtMost(length)
                            if (right > left) {
                                val slant = 2.dp.toPx().coerceAtMost((right - left) / 3f)
                                val tread = Path().apply {
                                    moveTo(left + slant, y)
                                    lineTo(right, y)
                                    lineTo(right - slant, y + band)
                                    lineTo(left, y + band)
                                    close()
                                }
                                drawPath(tread, Color(0xFFC8FF19))
                            }
                            x += dash + gap
                        }
                    }
                }
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_car_top), null,
                    Modifier.offset(x = plotWidth * fraction, y = (-2).dp).size(18.dp).rotate(90f), tint = Color.Unspecified)
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(start = 51.dp, end = 18.dp, top = 8.dp).height(18.dp)) {
        (0..5).forEach { tick ->
            val offset = when (tick) {
                0 -> 0.dp
                5 -> maxWidth - 40.dp
                else -> maxWidth * (tick / 5f) - 20.dp
            }
            Text(String.format(Locale.KOREA, "%.0f", tick * step),
                Modifier.offset(x = offset).width(40.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = DriveColors.Gray500,
                textAlign = when (tick) {
                    0 -> androidx.compose.ui.text.style.TextAlign.Start
                    5 -> androidx.compose.ui.text.style.TextAlign.End
                    else -> androidx.compose.ui.text.style.TextAlign.Center
                })
        }
    }
}

@Composable private fun ActivityStats(records: List<RecordDto>) {
    Text("전체 활동 통계", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(20.dp))
    val measured = records.mapNotNull { it.distanceKm }
    val stats = listOf(
        "누적 드라이브" to "${records.size}회",
        "평균 주행 거리" to if (records.isEmpty()) "0.0 km" else if (measured.isEmpty()) "—" else "${distance(measured.average())} km",
        "최장 주행 거리" to if (records.isEmpty()) "0.0 km" else if (measured.isEmpty()) "—" else "${distance(measured.max())} km",
        "평균 운행시간" to if (records.isEmpty()) "—" else duration(records.sumOf { it.elapsedSeconds } / records.size)
    )
    stats.forEach { (label, value) ->
        Column(Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = DriveColors.Gray500)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
        HorizontalDivider(color = DriveColors.Gray200)
    }
}

@Composable private fun DashboardDriveCard(record: RecordDto, photo: java.io.File?, photoVersion: Int,
    mapCache: RecentDriveMapCache, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
        .border(.5.dp, DriveColors.Gray300, RoundedCornerShape(20.dp))
        .background(DriveColors.White).clickable(onClick = onClick).padding(12.dp)) {
        Row(Modifier.fillMaxWidth().height(92.dp), verticalAlignment = Alignment.CenterVertically) {
            RecentDriveMapThumbnail(record, mapCache, photo, photoVersion,
                Modifier.size(92.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(record.displayDate(), style = MaterialTheme.typography.bodyMedium,
                    color = DriveColors.Gray700, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(record.placeName, Modifier.padding(end = 20.dp),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DriveColors.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DriveRoadIcon(Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(record.distanceKm?.let { "${distance(it)} km" } ?: "—",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = DriveColors.Black, maxLines = 1)
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(1.dp).height(17.dp).background(DriveColors.Gray300))
                    Spacer(Modifier.width(8.dp))
                    DriveIcon(DriveIconType.CLOCK, DriveColors.Black, Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(duration(record.elapsedSeconds),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        color = DriveColors.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        DriveIcon(DriveIconType.CHEVRON, DriveColors.Black,
            Modifier.align(Alignment.CenterEnd).size(18.dp))
    }
}

@Composable private fun DriveRoadIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 1.5.dp.toPx()
        drawLine(DriveColors.Black, Offset(size.width * .30f, size.height * .08f),
            Offset(size.width * .04f, size.height * .92f), stroke)
        drawLine(DriveColors.Black, Offset(size.width * .70f, size.height * .08f),
            Offset(size.width * .96f, size.height * .92f), stroke)
        drawLine(DriveColors.Black, Offset(size.width * .50f, size.height * .10f),
            Offset(size.width * .50f, size.height * .92f), stroke,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2)))
    }
}

@Composable private fun DemoBadge(modifier: Modifier = Modifier) {
    Box(modifier.size(66.dp).background(DriveColors.Black, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_car), null, Modifier.size(28.dp), tint = Color(0xFFC8FF19))
            Text("DEMO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = DriveColors.White)
        }
    }
}

@Composable private fun DemoAchievement() {
    Text("달성 기록", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth().height(104.dp).border(.5.dp, DriveColors.Gray300, RoundedCornerShape(16.dp))
        .background(DriveColors.White, RoundedCornerShape(16.dp))
        .padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("3회 연속 기록", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("드라이브를 이어간 순간", style = MaterialTheme.typography.labelMedium, color = DriveColors.Gray500)
            Text("데모 달성 기록", style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray500)
        }
        DemoBadge()
    }
}

@Composable private fun DemoDriveLevel(totalKm: Double) {
    Text("드라이브 레벨", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(16.dp))
    Box(Modifier.fillMaxWidth().background(DriveColors.Black, RoundedCornerShape(18.dp)).padding(horizontal = 22.dp, vertical = 30.dp)) {
        Column {
            Text("${distance(totalKm)} km", style = MaterialTheme.typography.headlineMedium, color = DriveColors.White)
            Text("누적 주행 거리", style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray300)
            Spacer(Modifier.height(64.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val progress = (totalKm / 1000.0).coerceIn(0.0, 1.0)
                repeat(5) { index ->
                    Box(Modifier.weight(1f).height(8.dp).background(DriveColors.Gray700, RoundedCornerShape(50))) {
                        val filled = (progress * 5 - index).toFloat().coerceIn(0f, 1f)
                        if (filled > 0f) Box(Modifier.fillMaxWidth(filled).fillMaxHeight()
                            .background(Color(0xFFC8FF19), RoundedCornerShape(50)))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("다음 레벨까지 ${distance((1000.0 - totalKm).coerceAtLeast(0.0))}km 남음",
                style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray300)
        }
    }
}

@Composable fun JourneyListScreen(records: List<RecordDto>, onBack: () -> Unit, onDetail: (String) -> Unit, onRefresh: () -> Unit, photoFor: (String) -> java.io.File? = { null }, photoVersion: Int = 0) {
    val context = LocalContext.current
    val mapCache = remember(context) { RecentDriveMapCache.shared(context) }
    var selectedPeriod by rememberSaveable { mutableStateOf(DrivePeriod.ALL) }
    val latestAnchor = records.maxOfOrNull { it.startedAtMillis } ?: System.currentTimeMillis()
    var selectedAnchor by rememberSaveable(latestAnchor) { mutableLongStateOf(latestAnchor) }
    var pickerPeriod by remember { mutableStateOf<DrivePeriod?>(null) }
    Column(Modifier.fillMaxSize()) {
        var searching by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = DriveDimens.Page), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BackButton(onBack)
            Text("드라이브 기록", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton({ searching = !searching }) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_search), "기록 검색", tint = DriveColors.Gray500) }
        }
        if (searching) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = DriveDimens.Page), singleLine = true, placeholder = { Text("코스명 검색") })
        Row(Modifier.fillMaxWidth().padding(horizontal = DriveDimens.Page).border(.5.dp, DriveColors.Gray300, RoundedCornerShape(50))
            .background(DriveColors.White, RoundedCornerShape(50))) {
            DrivePeriod.entries.forEach { period ->
                val active = period == selectedPeriod
                Box(Modifier.weight(1f).height(32.dp).background(if (active) DriveColors.Black else Color.Transparent, RoundedCornerShape(50))
                    .clickable {
                        selectedPeriod = period
                        selectedAnchor = if (period == DrivePeriod.WEEK) System.currentTimeMillis() else latestAnchor
                    }, contentAlignment = Alignment.Center) {
                    Text(period.label, style = MaterialTheme.typography.labelLarge,
                        color = if (active) DriveColors.White else DriveColors.Gray700)
                }
            }
        }
        val start = periodStart(selectedPeriod, selectedAnchor)
        val end = periodEnd(selectedPeriod, start)
        val periodRecords = records.filter { it.startedAtMillis >= start && it.startedAtMillis < end }
        val visibleRecords = periodRecords.filter { it.placeName.contains(query, ignoreCase = true) }.sortedByDescending { it.startedAtMillis }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page)) {
            Spacer(Modifier.height(30.dp))
            DriveOverview(periodRecords, selectedPeriod, selectedAnchor, records, { pickerPeriod = selectedPeriod })
            Spacer(Modifier.height(40.dp))
            ActivityStats(periodRecords)
            Spacer(Modifier.height(40.dp))
            Text("최근 드라이브", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            if (visibleRecords.isEmpty()) {
                Text(if (records.isEmpty()) stringResource(R.string.empty_records_hint) else when (selectedPeriod) {
                    DrivePeriod.YEAR -> "선택한 연도의 주행 기록이 없어요."
                    DrivePeriod.MONTH -> "선택한 월의 주행 기록이 없어요."
                    DrivePeriod.WEEK -> "이번 주의 주행 기록이 없어요."
                    DrivePeriod.ALL -> "조건에 맞는 기록이 없습니다"
                },
                    style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
            } else visibleRecords.forEach { record ->
                DashboardDriveCard(record, photoFor(record.id), photoVersion, mapCache) { onDetail(record.id) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(30.dp))
            DemoAchievement()
            Spacer(Modifier.height(34.dp))
            DemoDriveLevel(records.sumOf { it.distanceKm ?: 0.0 })
            Spacer(Modifier.height(32.dp))
        }
    }
    when (pickerPeriod) {
        DrivePeriod.YEAR -> YearWheelPickerBottomSheet(
            selectedYear = Calendar.getInstance().apply { timeInMillis = selectedAnchor }.get(Calendar.YEAR),
            availableYears = periodChoices(DrivePeriod.YEAR, records, selectedAnchor).map {
                Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)
            }.sorted(),
            onConfirm = { selectedAnchor = dateAt(it, Calendar.JANUARY); pickerPeriod = null },
            onDismiss = { pickerPeriod = null }
        )
        DrivePeriod.MONTH -> MonthWheelPickerBottomSheet(
            selectedMonth = periodStart(DrivePeriod.MONTH, selectedAnchor),
            availableMonths = periodChoices(DrivePeriod.MONTH, records, selectedAnchor).sorted(),
            onConfirm = { selectedAnchor = it; pickerPeriod = null },
            onDismiss = { pickerPeriod = null }
        )
        else -> Unit
    }
}

@Composable fun JourneyDetailScreen(record: RecordDto, hasPhoto: Boolean, onBack: () -> Unit, onPhoto: () -> Unit, onEditor: () -> Unit) {
    val context = LocalContext.current
    val mapCache = remember(context) { RecentDriveMapCache.shared(context) }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("드라이브 기록", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page)) {
            Spacer(Modifier.height(22.dp))
            Eyebrow(record.displayDate())
            Spacer(Modifier.height(12.dp))
            Text(record.placeName, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(14.dp))
            Spacer(Modifier.height(24.dp))
            DetailDriveRouteMap(record, mapCache,
                Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(22.dp)))
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JourneyMetric(R.drawable.ic_car, "거리", record.displayDistance(), Modifier.weight(1f))
                JourneyMetric(R.drawable.ic_clock, "주행시간", record.displayDuration(), Modifier.weight(1f))
                JourneyMetric(R.drawable.ic_fuel, "연비 / 전비", "정보 없음", Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
            SectionHeading("주행 정보")
            Spacer(Modifier.height(12.dp))
            DetailRow("출발", record.displayTime(record.startedAtMillis))
            DetailRow("도착", record.displayTime(record.endedAtMillis))
            DetailRow("거리 기준", when (record.distanceSource) { "VEHICLE" -> "차량 계기판"; "GPS_ESTIMATED" -> "GPS 추정"; else -> "정보 없음" })
            DetailRow("경로 지점", "${record.route.size}개")
            DetailRow("연비 / 전비", "정보 없음")
            Spacer(Modifier.height(24.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = DriveDimens.Page, vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
            Button(onEditor, Modifier.fillMaxWidth().height(48.dp), colors = ButtonDefaults.buttonColors(containerColor = DriveColors.Black, contentColor = DriveColors.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.card_make), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable private fun JourneyMetric(icon: Int, label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(vertical = 12.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Icon(androidx.compose.ui.res.painterResource(icon), label, Modifier.size(26.dp), tint = DriveColors.Black)
        Spacer(Modifier.height(14.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider(color = DriveColors.Gray200)
}

@Composable fun RoutePreview(route: List<RoutePoint>, modifier: Modifier = Modifier) {
    Box(modifier.background(DriveColors.Gray100, androidx.compose.foundation.shape.RoundedCornerShape(22.dp))) {
        val valid = route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
        if (valid.size < 2) {
            Text("경로 정보 없음", Modifier.align(androidx.compose.ui.Alignment.Center), style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
        } else Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            val minX = valid.minOf { it.longitude }; val maxX = valid.maxOf { it.longitude }
            val minY = valid.minOf { it.latitude }; val maxY = valid.maxOf { it.latitude }
            val dx = (maxX - minX).coerceAtLeast(1e-7); val dy = (maxY - minY).coerceAtLeast(1e-7)
            fun point(p: RoutePoint) = Offset(
                size.width * (.08f + .84f * ((p.longitude - minX) / dx).toFloat()),
                size.height * (.08f + .84f * (1f - ((p.latitude - minY) / dy).toFloat()))
            )
            val path = Path().apply {
                valid.forEachIndexed { index, p ->
                    val xy = point(p)
                    if (index == 0) moveTo(xy.x, xy.y) else lineTo(xy.x, xy.y)
                }
            }
            drawPath(path, DriveColors.Black, style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawCircle(DriveColors.White, 7.dp.toPx(), point(valid.first()))
            drawCircle(DriveColors.Black, 7.dp.toPx(), point(valid.last()))
        }
    }
}

@Preview @Composable private fun ListPreview() { DRIVELOGMobileTheme { JourneyListScreen(DemoJourneys.records, {}, {}, {}) } }
@Preview @Composable private fun DetailPreview() { DRIVELOGMobileTheme { JourneyDetailScreen(DemoJourneys.records.first(), false, {}, {}, {}) } }
