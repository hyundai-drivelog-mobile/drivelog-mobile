package com.drivelog.mobile.feature.wash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.designsystem.picker.WheelPickerBottomSheet
import com.drivelog.mobile.feature.common.BackButton
import com.drivelog.mobile.ui.theme.DRIVELOGMobileTheme
import com.drivelog.mobile.ui.theme.DriveColors
import java.time.DayOfWeek
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WashDetailScreen(
    state: WashUiState,
    selectedRegion: WashRegion,
    onRegionSelected: (WashRegion) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(DriveColors.White)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = DriveDimens.Page),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack)
            Text(
                "오늘의 세차", Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            RegionSelector(selectedRegion, onRegionSelected)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = DriveDimens.Page)
        ) {
            Spacer(Modifier.height(36.dp))
            WashHero(state)
            Spacer(Modifier.height(32.dp))
            if (state.dailyForecasts.isNotEmpty()) {
                WeeklyForecastStrip(state.dailyForecasts)
                Spacer(Modifier.height(36.dp))
            }
            if (state.reason.isNotBlank() && !state.isLoading && state.errorMessage == null) {
                WashReason(state.reason)
                Spacer(Modifier.height(40.dp))
            }
            if (state.dataSourceStatus == WashDataSourceStatus.KMA) {
                HorizontalDivider(color = DriveColors.Gray200)
                Spacer(Modifier.height(20.dp))
                val issuedAt = state.forecastIssuedAt?.let(::displayKst)
                val updatedAt = state.lastUpdatedAt?.let(::displayKst)
                val notice = buildString {
                    append("기상청 제공")
                    if (issuedAt != null) append(" · 예보 기준 $issuedAt")
                    if (updatedAt != null) append("\n최근 확인 $updatedAt")
                    if (state.stale) append("\n이전에 받아온 예보를 표시하고 있어요.")
                }
                Text(
                    notice,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal, lineHeight = 18.sp
                    ),
                    color = DriveColors.TertiaryText
                )
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun RegionSelector(selected: WashRegion, onSelected: (WashRegion) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
            .clickable { expanded = true }.padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center) {
            Text(selected.label, style = MaterialTheme.typography.labelMedium,
                color = DriveColors.Black, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        if (expanded) {
            WheelPickerBottomSheet("지역 선택", selected, WashRegion.entries,
                { it.label },
                onConfirm = {
                    expanded = false
                    onSelected(it)
                },
                onDismiss = { expanded = false })
        }
    }
}

@Composable
private fun WashHero(state: WashUiState) {
    val headline = when {
        state.isLoading -> "날씨 정보를\n확인하고 있어요"
        state.errorMessage != null -> state.errorMessage
        else -> state.headline
    }
    val summary = if (state.isLoading) "" else state.summary
    val weather = when (state.recommendationStatus) {
        WashRecommendationStatus.RECOMMENDED -> WashWeatherType.SUNNY
        WashRecommendationStatus.POSTPONE -> WashWeatherType.RAINY
        WashRecommendationStatus.UNKNOWN -> WashWeatherType.CLOUDY
    }
    Box(Modifier.fillMaxWidth().height(206.dp)) {
        Box(
            Modifier.align(Alignment.CenterEnd).padding(top = 10.dp)
                .size(160.dp).background(DriveColors.Gray100, CircleShape),
            contentAlignment = Alignment.TopEnd
        ) {
            WashWeatherIcon(weather, Modifier.padding(top = 20.dp, end = 20.dp).size(44.dp),
                DriveColors.Black)
        }
        Image(
            painter = painterResource(R.drawable.wash_hero_car_cutout),
            contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).width(188.dp).height(128.dp),
            contentScale = ContentScale.Fit
        )
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth(.75f).padding(top = 12.dp)) {
            Text(headline,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold
                ),
                color = DriveColors.Black,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(summary, Modifier.fillMaxWidth(.67f),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = DriveColors.SecondaryText, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
            }
        }
        if (state.isLoading) {
            CircularProgressIndicator(
                Modifier.align(Alignment.BottomStart).size(22.dp),
                color = DriveColors.Black, strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun WashReason(reason: String) {
    Text("추천 이유", style = MaterialTheme.typography.titleMedium, color = DriveColors.Black)
    Spacer(Modifier.height(14.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                .background(DriveColors.Gray100),
            contentAlignment = Alignment.Center
        ) {
            WashDropIcon(Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            reason, Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 23.sp),
            color = DriveColors.SecondaryText
        )
    }
}

@Composable
private fun WeeklyForecastStrip(days: List<WashDayForecast>) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(DriveColors.Gray100).padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        days.take(7).forEach { day ->
            Column(
                Modifier.weight(1f).heightIn(min = 126.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (day.highlighted) Color(0xFFE8E8E8) else Color.Transparent)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    day.date.dayOfWeek.koreanLabel(),
                    style = MaterialTheme.typography.labelSmall, color = DriveColors.Black
                )
                WashWeatherIcon(day.weatherType, Modifier.size(24.dp))
                Text(
                    day.precipitationProbability?.let { "$it%" } ?: "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.highlighted) DriveColors.Black else DriveColors.SecondaryText,
                    maxLines = 1
                )
            }
        }
    }
}

private fun displayKst(value: String): String? = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.of("Asia/Seoul"))
        .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN))
}.getOrNull()

private fun DayOfWeek.koreanLabel(): String = when (this) {
    DayOfWeek.MONDAY -> "월"
    DayOfWeek.TUESDAY -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY -> "목"
    DayOfWeek.FRIDAY -> "금"
    DayOfWeek.SATURDAY -> "토"
    DayOfWeek.SUNDAY -> "일"
}

@Preview @Composable private fun WashRecommendedPreview() {
    DRIVELOGMobileTheme {
        WashDetailScreen(WashPreviewData.recommended, WashRegion.SEOUL, {}, {})
    }
}

@Preview @Composable private fun WashPostponePreview() {
    DRIVELOGMobileTheme {
        WashDetailScreen(WashPreviewData.postpone, WashRegion.SEOUL, {}, {})
    }
}
