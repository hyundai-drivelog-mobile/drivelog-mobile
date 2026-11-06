package com.drivelog.mobile.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.ui.theme.DriveColors

enum class MainTab { HOME, RECORDS, PROFILE }

@Composable fun AppBottomBar(selected: MainTab, onHome: () -> Unit, onRecords: () -> Unit, onProfile: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DriveColors.White).navigationBarsPadding()) {
        HorizontalDivider(color = DriveColors.Gray200, thickness = 1.dp)
        Row(Modifier.fillMaxWidth().height(66.dp)) {
            BottomItem("홈", DriveIconType.HOME, selected == MainTab.HOME, onHome, Modifier.weight(1f))
            BottomItem("기록", DriveIconType.LIST, selected == MainTab.RECORDS, onRecords, Modifier.weight(1f))
            BottomItem("내 정보", DriveIconType.PERSON, selected == MainTab.PROFILE, onProfile, Modifier.weight(1f))
        }
    }
}

@Composable private fun BottomItem(label: String, icon: DriveIconType, selected: Boolean,
    onClick: () -> Unit, modifier: Modifier) {
    val color: Color = if (selected) DriveColors.Black else DriveColors.TertiaryText
    Column(modifier.fillMaxHeight().clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        DriveIcon(icon, color, Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
