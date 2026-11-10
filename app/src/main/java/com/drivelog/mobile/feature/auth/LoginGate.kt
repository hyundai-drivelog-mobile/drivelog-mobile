package com.drivelog.mobile.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*

@Composable fun LoginGate(onDismiss: () -> Unit, onProvider: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(DriveDimens.Page)) {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(68.dp))
        DriveButton(stringResource(R.string.kakao_start), { onProvider("Kakao") }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedButton({ onProvider("Naver") }, Modifier.fillMaxWidth().height(DriveDimens.ButtonHeight),
            shape = RoundedCornerShape(DriveDimens.ButtonRadius), border = BorderStroke(1.dp, DriveColors.Black),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DriveColors.Black)) {
            Text(stringResource(R.string.naver_start),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        DriveGhostButton(stringResource(R.string.later), onDismiss, Modifier.fillMaxWidth())
    }
}

@Preview @Composable private fun LoginPreview() { DRIVELOGMobileTheme { LoginGate({}, {}) } }
