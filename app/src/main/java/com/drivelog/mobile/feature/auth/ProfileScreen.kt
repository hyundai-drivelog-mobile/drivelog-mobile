package com.drivelog.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.feature.common.DriveButton
import com.drivelog.mobile.feature.common.DriveOutlineButton
import com.drivelog.mobile.ui.theme.DriveColors

@Composable fun ProfileScreen(connected: Boolean, demo: Boolean, onVehicle: () -> Unit, onLogin: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(DriveDimens.Page)) {
        Text("내 정보", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(26.dp))
        Column(Modifier.fillMaxWidth().background(DriveColors.Gray100, RoundedCornerShape(18.dp)).padding(20.dp)) {
            Text(if (connected) "차량이 연결되어 있어요" else "차량 연결하기", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(if (demo) "데모 기록 사용 중" else if (connected) "DRIVELOG 차량 연결됨" else "연결된 차량이 없습니다",
                style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
        }
        Spacer(Modifier.height(14.dp))
        DriveOutlineButton("차량 연결 화면", onVehicle, Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
        Text("회원 기능", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("코스 공유와 배지는 준비 중입니다.", style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
        Spacer(Modifier.height(18.dp))
        DriveButton("로그인 안내", onLogin, Modifier.fillMaxWidth())
    }
}
