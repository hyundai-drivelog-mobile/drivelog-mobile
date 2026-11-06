package com.drivelog.mobile.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.ui.theme.DRIVELOGMobileTheme
import com.drivelog.mobile.ui.theme.DriveColors
import kotlinx.coroutines.delay

@Composable fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) { delay(1700); onFinished() }
    Box(Modifier.fillMaxSize().background(DriveColors.Black)) {
        Text(stringResource(R.string.brand), Modifier.align(Alignment.Center), style = MaterialTheme.typography.displayLarge, color = DriveColors.White)
        LinearProgressIndicator(Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.35f).padding(bottom = 72.dp).height(2.dp),
            color = DriveColors.White, trackColor = DriveColors.Gray700)
    }
}

@Preview @Composable private fun SplashPreview() { DRIVELOGMobileTheme { SplashScreen({}) } }
