package com.drivelog.mobile.feature.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.model.DemoJourneys
import com.drivelog.mobile.core.util.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.feature.journey.DriveRouteMapImage
import com.drivelog.mobile.feature.journey.RecentDriveMapCache
import com.drivelog.mobile.feature.wash.WashSparkleCarIcon
import com.drivelog.mobile.ui.theme.*
import java.io.File

@Composable fun HomeScreen(records: List<RecordDto>, demo: Boolean, connected: Boolean,
    photoFor: (String) -> File?, photoVersion: Int, onList: () -> Unit, onDetail: (String) -> Unit,
    onEditor: (String) -> Unit, onLogin: () -> Unit, onVehicle: () -> Unit, onNotifications: () -> Unit,
    onWash: () -> Unit) {
    val recent = records.maxByOrNull { it.startedAtMillis }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = DriveDimens.Page, end = DriveDimens.Page, bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.brand), Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = (-0.8).sp))
                IconButton(onNotifications, Modifier.size(48.dp)) {
                    DriveIcon(DriveIconType.BELL, DriveColors.Black)
                }
            }
            Spacer(Modifier.height(10.dp))
            VehicleConnectionCard(connected, demo, onVehicle)
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("최근 드라이브", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                TextButton(onList) {
                    Text(stringResource(R.string.all_records), style = MaterialTheme.typography.labelMedium, color = DriveColors.SecondaryText)
                    Spacer(Modifier.width(4.dp))
                    DriveIcon(DriveIconType.CHEVRON, DriveColors.SecondaryText, Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            if (recent == null) EmptyRecentDrive(onList) else RecentDriveHero(recent, photoFor(recent.id), photoVersion,
                { onDetail(recent.id) })
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoreFeature("추천 코스 공유", DriveIconType.ROUTE, Modifier.weight(1f), onLogin)
                MoreFeature("도전과제", DriveIconType.AWARD, Modifier.weight(1f), onLogin)
            }
            Spacer(Modifier.height(16.dp))
            HomeWashEntryCard(onWash)
        }
    }
}

@Composable private fun HomeWashEntryCard(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(20.dp))
        .background(DriveColors.Gray100).clickable(onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        WashSparkleCarIcon(Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text("오늘의 세차", Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = DriveColors.Black, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        DriveIcon(DriveIconType.CHEVRON, DriveColors.Gray500, Modifier.size(20.dp))
    }
}

@Composable private fun VehicleConnectionCard(connected: Boolean, demo: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 96.dp).clip(RoundedCornerShape(18.dp))
        .background(DriveColors.Gray100).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(DriveColors.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            DriveIcon(DriveIconType.CAR, DriveColors.Black)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(if (connected) "차량이 연결되어 있어요" else "차량 연결하기",
                style = MaterialTheme.typography.labelLarge, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Text(when { demo -> "데모 기록 사용 중"; connected -> "DRIVELOG 차량 연결됨"; else -> "차량 화면의 6자리 코드를 입력하세요" },
                style = MaterialTheme.typography.labelMedium, color = DriveColors.SecondaryText, maxLines = 2)
        }
        DriveIcon(DriveIconType.CHEVRON, DriveColors.SecondaryText, Modifier.size(20.dp))
    }
}

@Composable private fun RecentDriveHero(record: RecordDto, photo: File?, photoVersion: Int,
    onDetail: () -> Unit) {
    val context = LocalContext.current
    val mapCache = remember(context) { RecentDriveMapCache.shared(context) }
    Box(Modifier.fillMaxWidth().heightIn(min = 250.dp).height(270.dp).clip(RoundedCornerShape(20.dp))
        .background(Color(0xff303030)).clickable(onClick = onDetail)) {
        DriveRouteMapImage(record, mapCache, photo, photoVersion, Modifier.fillMaxSize(), creditAlignment = Alignment.TopStart)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .08f),
            Color.Black.copy(alpha = .13f), Color.Black.copy(alpha = .83f)))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp)) {
            Text(record.displayDate(), style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray200)
            Spacer(Modifier.height(4.dp))
            Text(record.placeName, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineMedium, color = DriveColors.White)
            Spacer(Modifier.height(5.dp))
            Text("${record.displayDistance()}  |  ${record.displayDuration()}",
                style = MaterialTheme.typography.labelMedium, color = DriveColors.Gray200, maxLines = 2)
        }
    }
}

@Composable private fun EmptyRecentDrive(onList: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = 250.dp).background(DriveColors.Gray100, RoundedCornerShape(20.dp))
        .padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.empty_records), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_records_hint), style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
        Spacer(Modifier.height(12.dp))
        DriveGhostButton(stringResource(R.string.all_records), onList)
    }
}

@Composable private fun MoreFeature(title: String, icon: DriveIconType, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 126.dp).clip(RoundedCornerShape(17.dp))
        .background(DriveColors.Gray100).clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            DriveIcon(icon, DriveColors.Black, Modifier.size(22.dp))
            DriveIcon(DriveIconType.LOCK, DriveColors.Black, Modifier.size(16.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text("준비 중", style = MaterialTheme.typography.labelSmall, color = DriveColors.TertiaryText)
        }
    }
}

@Preview @Composable private fun HomePreview() { DRIVELOGMobileTheme {
    HomeScreen(DemoJourneys.records, true, false, { null }, 0, {}, {}, {}, {}, {}, {}, {})
} }
