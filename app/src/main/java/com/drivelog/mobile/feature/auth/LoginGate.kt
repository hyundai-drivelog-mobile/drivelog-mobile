package com.drivelog.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*

@Composable fun LoginGate(onDismiss: () -> Unit, onProvider: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(DriveDimens.Page)) {
        Eyebrow("MEMBERSHIP / COMING SOON")
        Spacer(Modifier.height(22.dp))
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.login_description), style = MaterialTheme.typography.bodyLarge, color = DriveColors.Gray700)
        Spacer(Modifier.height(32.dp))
        DriveButton(stringResource(R.string.kakao_start), { onProvider("Kakao") }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        DriveOutlineButton(stringResource(R.string.naver_start), { onProvider("Naver") }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        DriveGhostButton(stringResource(R.string.later), onDismiss, Modifier.fillMaxWidth())
    }
}

@Preview @Composable private fun LoginPreview() { DRIVELOGMobileTheme { LoginGate({}, {}) } }
