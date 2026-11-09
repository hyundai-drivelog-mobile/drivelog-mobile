package com.drivelog.mobile.feature.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.data.StampDto
import com.drivelog.mobile.feature.common.ScreenTopBar
import com.drivelog.mobile.ui.theme.DriveColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable fun StampScreen(stamps: List<StampDto>, busy: Boolean, loaded: Boolean,
    connected: Boolean, onBack: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("내 스탬프", onBack, if (busy) null else "새로고침", onRefresh)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn(contentPadding = PaddingValues(DriveDimens.Page), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("다녀온 곳 ${stamps.size}곳", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(when {
                    !connected -> "차량을 연결하면 차량에서 모은 스탬프를 볼 수 있어요."
                    !loaded -> "새로고침해 차량의 스탬프를 불러오세요."
                    stamps.isEmpty() -> "아직 모은 스탬프가 없어요. 명소에 도착해 주행 기록을 저장하면 표시돼요."
                    else -> "마지막으로 동기화한 방문 기록이에요. 오프라인에서도 볼 수 있어요."
                }, style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
            }
            items(stamps, key = { it.id }) { stamp ->
                Column(Modifier.fillMaxWidth().background(DriveColors.Gray100, RoundedCornerShape(18.dp)).padding(20.dp)) {
                    StampArt(stamp, collected = true, size = 96.dp,
                        modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                    Spacer(Modifier.height(16.dp))
                    Text("${stamp.region} · ${stamp.area}", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(stamp.name, style = MaterialTheme.typography.titleLarge)
                    Text(stamp.motto, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date(stamp.visitedAtMillis)) + " 첫 방문",
                        style = MaterialTheme.typography.labelMedium, color = DriveColors.SecondaryText)
                }
            }
        }
    }
}
