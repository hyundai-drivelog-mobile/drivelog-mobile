package com.drivelog.mobile.feature.link

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.feature.auth.UsageInfoScreen
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*

@Composable fun VehicleLinkScreen(busy: Boolean, error: String, onLink: (String) -> Unit, onDemo: () -> Unit,
    existingConnectionPending: Boolean = false, onExistingConsent: () -> Unit = {}) {
    var code by remember { mutableStateOf("") }
    var consentCode by remember { mutableStateOf<String?>(null) }
    var showExistingConsent by remember { mutableStateOf(existingConnectionPending) }
    var showInfo by remember { mutableStateOf(false) }
    BackHandler(showInfo) { showInfo = false }
    BackHandler(consentCode != null && !showInfo) { consentCode = null }
    BackHandler(showExistingConsent && existingConnectionPending && !showInfo) { showExistingConsent = false }
    if (showInfo) {
        UsageInfoScreen(onBack = { showInfo = false })
        return
    }
    if (showExistingConsent && existingConnectionPending) {
        PhoneLinkConsentScreen(busy, onAgree = onExistingConsent,
            onBack = { showExistingConsent = false }, onDetails = { showInfo = true })
        return
    }
    if (consentCode != null) {
        PhoneLinkConsentScreen(busy, onAgree = {
            val approvedCode = consentCode ?: return@PhoneLinkConsentScreen
            consentCode = null
            onLink(approvedCode)
        }, onBack = { consentCode = null }, onDetails = { showInfo = true })
        return
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DriveDimens.Page)) {
        Spacer(Modifier.height(36.dp))
        Text(stringResource(R.string.link_title), style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.link_description), style = MaterialTheme.typography.bodyLarge, color = DriveColors.Gray700)
        Spacer(Modifier.height(120.dp))
        Spacer(Modifier.height(14.dp))
        SegmentedCodeInput(code) { code = it }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = DriveColors.Black, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(28.dp))
        DriveButton(stringResource(R.string.link_action), {
            if (code == "000000") onDemo() else consentCode = code
        }, Modifier.fillMaxWidth(), code.length == 6 && !busy)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable private fun PhoneLinkConsentScreen(busy: Boolean, onAgree: () -> Unit, onBack: () -> Unit,
    onDetails: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("휴대폰 연결", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page)) {
            Spacer(Modifier.height(16.dp))
            Text("연결 전 확인해 주세요", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text("차량에서 서버 전송에 동의한 기록을 이 휴대폰에서 조회합니다.",
                style = MaterialTheme.typography.bodyLarge, color = DriveColors.Gray700)
            TextButton(onClick = onDetails) { Text("이용 안내 및 약관 전체 보기") }
            Spacer(Modifier.height(30.dp))
            ConsentSection("가져오는 정보", "연결 코드로 인증하면 목적지와 경로 좌표, 주행 시각·거리·시간, 카드 제목·설정과 스탬프를 서버에서 받아요. 차량에서 별도로 선택한 기존 기록만 함께 전송됩니다.")
            ConsentSection("사용 목적과 보관", "받은 기록은 드라이브 목록·지도·카드·스탬프를 표시하는 데 사용해요. 인증 정보와 내려받은 기록은 휴대폰 앱 저장 공간에 보관됩니다. 내 정보에서 연결을 해제하면 휴대폰 사본을 지울 수 있고, 서버 사본은 별도 삭제 요청이 필요해요.")
            ConsentSection("사진과 지도", "사진과 카드 설정을 저장하면 서버 기록에도 반영됩니다. 지도 화면은 OpenFreeMap의 지도 조각을 받아 요청한 지도 영역과 네트워크 주소가 제공자에게 전달될 수 있어요.")
            ConsentSection("동의하지 않을 권리", "동의하지 않으면 연결 코드를 서버에 보내지 않고 차량 기록도 가져오지 않아요. 체험 데이터는 연결 없이 볼 수 있습니다.")
            Spacer(Modifier.height(4.dp))
            DriveButton("위 내용을 확인하고 연결에 동의", onAgree, Modifier.fillMaxWidth(), enabled = !busy)
            Spacer(Modifier.height(10.dp))
            DriveOutlineButton("동의하지 않고 돌아가기", onBack, Modifier.fillMaxWidth())
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable private fun ConsentSection(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
    Spacer(Modifier.height(24.dp))
}

@Preview @Composable private fun LinkPreview() { DRIVELOGMobileTheme { VehicleLinkScreen(false, "", {}, {}) } }
