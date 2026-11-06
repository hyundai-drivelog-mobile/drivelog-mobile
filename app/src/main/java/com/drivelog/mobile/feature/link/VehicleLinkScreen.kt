package com.drivelog.mobile.feature.link

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
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*

@Composable fun VehicleLinkScreen(busy: Boolean, error: String, onLink: (String) -> Unit, onDemo: () -> Unit) {
    var code by remember { mutableStateOf("") }
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
        DriveButton(stringResource(R.string.link_action), { onLink(code) }, Modifier.fillMaxWidth(), code.length == 6 && !busy)
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.height(48.dp))
        HorizontalDivider(color = DriveColors.Gray200)
        Spacer(Modifier.height(16.dp))
        DriveOutlineButton(stringResource(R.string.demo_action), onDemo, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

@Preview @Composable private fun LinkPreview() { DRIVELOGMobileTheme { VehicleLinkScreen(false, "", {}, {}) } }
