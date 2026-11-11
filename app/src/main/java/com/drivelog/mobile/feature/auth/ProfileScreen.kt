package com.drivelog.mobile.feature.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.feature.common.DriveButton
import com.drivelog.mobile.feature.common.ScreenTopBar
import com.drivelog.mobile.feature.common.VehicleConnectionCard
import com.drivelog.mobile.ui.theme.DriveColors

private data class InfoSection(val title: String, val body: String)
private data class InfoGroup(val title: String, val intro: String, val sections: List<InfoSection>)

/** Bundled text remains readable without a browser or network connection. */
private val infoGroups = listOf(
    InfoGroup("서비스 이용약관", "기록과 카드를 이용할 때 알아둘 내용", listOf(
        InfoSection("서비스", "연결한 차량의 드라이브 기록과 스탬프를 확인하고, 사진을 더해 드라이브 카드를 만들거나 기기에 이미지로 저장할 수 있습니다. 차량 연결이 없는 경우에는 체험 데이터를 볼 수 있습니다."),
        InfoSection("기록과 카드", "기록과 장소 정보는 연결된 차량에서 전달된 내용을 기준으로 표시합니다. 경로와 거리 등은 수집 환경에 따라 실제 주행과 다를 수 있습니다. 카드에 직접 추가한 사진과 문구는 본인이 사용할 권한이 있는 자료를 사용해 주세요."),
        InfoSection("연결과 이용", "연결 코드가 있어야 본인의 차량 기록을 동기화할 수 있습니다. 연결이 끊기거나 네트워크를 사용할 수 없으면 새 기록을 받지 못할 수 있습니다. 기기에 저장한 카드 이미지는 기기의 파일 관리 기능으로 관리합니다."),
        InfoSection("외부 자료", "지도 화면에는 외부 지도 제공자의 자료가 사용됩니다. 지도 요청과 출처는 아래 ‘지도·출처 안내’에서 확인할 수 있습니다.")
    )),
    InfoGroup("개인정보와 데이터 사용", "차량 연결부터 휴대폰 보관까지", listOf(
        InfoSection("연결과 동기화", "차량에서 휴대폰 연결을 선택한 경우 목적지, 경로 좌표, 주행 시각·거리·시간 등의 기록이 DRIVELOG 서버로 전달될 수 있습니다. 휴대폰 앱은 연결 코드로 인증한 뒤 해당 기록과 스탬프를 받아 표시합니다."),
        InfoSection("휴대폰에 저장되는 항목", "연결 인증 정보와 내려받은 기록·스탬프, 선택한 사진과 카드 편집 내용은 이 앱의 저장 공간에 보관됩니다. 카드 이미지를 저장하면 사용자가 선택한 기기 위치에 파일이 생성됩니다."),
        InfoSection("사진과 카드 설정", "사진과 카드 설정을 저장하면 연결된 기록에 반영하기 위해 서버로 전송됩니다. 카드 이미지를 기기에 내보내는 작업은 사용자가 선택한 저장 위치에 파일을 만듭니다."),
        InfoSection("삭제와 문의", "휴대폰에서 앱 데이터나 저장한 이미지를 지우는 것과 서버에 보관된 기록을 지우는 것은 별개의 작업입니다. 서버 기록의 열람·삭제 문의는 앱을 제공한 운영자에게 요청해야 합니다.")
    )),
    InfoGroup("권한 안내", "앱에서 사용하는 권한과 기기 기능", listOf(
        InfoSection("인터넷과 백그라운드 동기화", "연결 코드 인증, 드라이브 기록·스탬프 동기화와 지도 조각을 받는 데 네트워크를 사용합니다. 연결된 기록은 앱이 닫힌 동안에도 주기적으로 확인할 수 있으며, 기기를 다시 켠 뒤 동기화 작업을 이어갈 수 있습니다."),
        InfoSection("알림", "새 기록에 사진을 추가하라는 안내를 표시할 수 있습니다. 알림을 허용하지 않아도 기록과 카드 기능을 사용할 수 있습니다."),
        InfoSection("사진 선택과 이미지 저장", "사진은 기기의 사진 선택기를 통해 사용자가 고른 항목만 가져옵니다. 카드 저장에는 기기의 파일 저장 화면을 사용합니다. 사진 전체에 대한 접근 권한은 요청하지 않습니다.")
    )),
    InfoGroup("지도·출처 안내", "지도 제공자와 표시 범위", listOf(
        InfoSection("지도 제공", "지도 화면은 OpenFreeMap 지도 조각을 사용합니다. 요청한 지도 영역과 네트워크 주소가 지도 제공자에게 전달될 수 있습니다. 기록의 경로선은 앱에서 그립니다."),
        InfoSection("출처", "OpenFreeMap (openfreemap.org) · © OpenMapTiles (openmaptiles.org) · OpenStreetMap contributors (openstreetmap.org/copyright)"),
        InfoSection("표시 범위", "지도와 스탬프는 실제 입장이나 체류를 증명하지 않습니다. 기록의 위치와 지도 화면은 수신 상태에 따라 차이가 날 수 있습니다.")
    ))
)

@Composable fun ProfileScreen(connected: Boolean, demo: Boolean, onVehicle: () -> Unit, onLogin: () -> Unit,
    busy: Boolean, onUnlink: () -> Unit) {
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var confirmUnlink by rememberSaveable { mutableStateOf(false) }
    BackHandler(showInfo) { showInfo = false }
    if (showInfo) {
        UsageInfoScreen(onBack = { showInfo = false })
    } else {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(DriveDimens.Page)) {
            Text("내 정보", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(28.dp))
            VehicleConnectionCard(connected, onVehicle)
            Spacer(Modifier.height(32.dp))
            Text("회원 기능", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("코스 공유와 배지는 준비 중입니다.", style = MaterialTheme.typography.bodyMedium,
                color = DriveColors.SecondaryText)
            Spacer(Modifier.height(18.dp))
            DriveButton("로그인 안내", onLogin, Modifier.fillMaxWidth())
            Spacer(Modifier.height(38.dp))
            Text("안내 및 약관", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Button(onClick = { showInfo = true }, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = DriveColors.Black, contentColor = DriveColors.White)) {
                Text("이용 안내 및 약관", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
            if (connected) {
                Spacer(Modifier.height(34.dp))
                Text("연결 관리", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("연결을 해제하면 이 휴대폰의 기록·사진·카드 설정과 연결 정보가 삭제됩니다. 서버 사본은 별도로 요청해야 합니다.",
                    style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = { confirmUnlink = true }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(DriveDimens.ButtonHeight),
                    shape = RoundedCornerShape(DriveDimens.ButtonRadius)) {
                    Text("연결 해제", color = DriveColors.Black)
                }
            }
        }
    }
    if (confirmUnlink) AlertDialog(
        onDismissRequest = { confirmUnlink = false },
        title = { Text("연결을 해제할까요?") },
        text = { Text("이 휴대폰의 연결 정보와 저장된 기록·사진·카드 설정을 지웁니다. 서버 기록은 자동으로 삭제되지 않습니다.") },
        confirmButton = { TextButton(onClick = { confirmUnlink = false; onUnlink() }) { Text("해제") } },
        dismissButton = { TextButton(onClick = { confirmUnlink = false }) { Text("취소") } }
    )
}

@Composable fun UsageInfoScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("이용 안내 및 약관", onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = DriveDimens.Page)) {
            Spacer(Modifier.height(18.dp))
            Text("안심하고 드라이브를\n기록할 수 있도록", style = MaterialTheme.typography.headlineMedium,
                color = DriveColors.Black)
            Spacer(Modifier.height(10.dp))
            Text("서비스 이용, 데이터 처리, 권한과 지도 출처를 한곳에 모았습니다.",
                style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth().background(DriveColors.Gray100, RoundedCornerShape(18.dp))
                .padding(20.dp)) {
                Text("먼저 확인해 주세요", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Text("차량 연결 시 기록이 서버를 통해 휴대폰으로 전달됩니다. 사진과 카드 설정을 저장하면 서버 기록에도 반영됩니다. 앱 데이터 삭제와 서버 기록 삭제는 별개입니다.",
                    style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
            }
            Spacer(Modifier.height(34.dp))
            infoGroups.forEachIndexed { index, group ->
                InfoGroupContent(group)
                if (index < infoGroups.lastIndex) {
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = DriveColors.Gray200)
                    Spacer(Modifier.height(30.dp))
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable private fun InfoGroupContent(group: InfoGroup) {
    Text(group.title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = DriveColors.Black)
    Spacer(Modifier.height(6.dp))
    Text(group.intro, style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray700)
    Spacer(Modifier.height(22.dp))
    group.sections.forEach { section ->
        Text(section.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = DriveColors.Black)
        Spacer(Modifier.height(7.dp))
        Text(section.body, style = MaterialTheme.typography.bodyMedium, color = DriveColors.SecondaryText)
        Spacer(Modifier.height(22.dp))
    }
}
