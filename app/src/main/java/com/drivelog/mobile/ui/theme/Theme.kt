package com.drivelog.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val Day = lightColorScheme(
    primary = DriveColors.Black, onPrimary = DriveColors.White,
    secondary = DriveColors.Gray700, onSecondary = DriveColors.White,
    background = DriveColors.White, onBackground = DriveColors.Black,
    surface = DriveColors.White, onSurface = DriveColors.Black,
    surfaceVariant = DriveColors.Gray100, onSurfaceVariant = DriveColors.Gray700,
    outline = DriveColors.Gray300, error = DriveColors.Black
)

@Composable
fun DRIVELOGMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Day, typography = DriveTypography, content = content)
}

