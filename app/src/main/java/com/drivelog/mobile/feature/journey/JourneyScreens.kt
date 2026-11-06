package com.drivelog.mobile.feature.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.model.DemoJourneys
import com.drivelog.mobile.core.util.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.data.RoutePoint
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*

@Composable fun JourneyListScreen(records: List<RecordDto>, onBack: () -> Unit, onDetail: (String) -> Unit, onRefresh: () -> Unit, photoFor: (String) -> java.io.File? = { null }, photoVersion: Int = 0) {
    Column(Modifier.fillMaxSize()) {
        var searching by remember { mutableStateOf(false) }
        var query by remember { mutableStateOf("") }
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = DriveDimens.Page), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            BackButton(onBack)
            Text("드라이브 기록", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            IconButton({ searching = !searching }) { Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_search), "기록 검색", tint = DriveColors.Gray500) }
        }
        if (searching) OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = DriveDimens.Page), singleLine = true, placeholder = { Text("코스명 검색") })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DriveDimens.Page)) {
            if (records.isEmpty()) {
                Text(stringResource(R.string.empty_records), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.empty_records_hint), style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
            } else records.filter { it.placeName.contains(query, ignoreCase = true) }.forEach { record ->
                RecordCard(record, photoFor(record.id), photoVersion) { onDetail(record.id) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable fun JourneyDetailScreen(record: RecordDto, hasPhoto: Boolean, onBack: () -> Unit, onPhoto: () -> Unit, onEditor: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("드라이브 기록", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page)) {
            Spacer(Modifier.height(22.dp))
            Eyebrow(record.displayDate())
            Spacer(Modifier.height(12.dp))
            Text(record.placeName, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(14.dp))
            Spacer(Modifier.height(24.dp))
            RoutePreview(record.route, Modifier.fillMaxWidth().height(220.dp))
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
