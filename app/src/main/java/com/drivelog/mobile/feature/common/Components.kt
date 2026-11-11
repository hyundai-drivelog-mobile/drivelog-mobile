package com.drivelog.mobile.feature.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.util.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.ui.theme.DriveColors

@Composable fun DriveButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.height(DriveDimens.ButtonHeight), enabled = enabled,
        shape = RoundedCornerShape(DriveDimens.ButtonRadius),
        colors = ButtonDefaults.buttonColors(containerColor = DriveColors.Black, contentColor = DriveColors.White,
            disabledContainerColor = DriveColors.Gray300, disabledContentColor = DriveColors.Gray700)) {
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable fun DriveOutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.height(DriveDimens.ButtonHeight), shape = RoundedCornerShape(DriveDimens.ButtonRadius),
        border = BorderStroke(1.dp, DriveColors.Black), colors = ButtonDefaults.outlinedButtonColors(contentColor = DriveColors.Black)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable fun DriveGhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick, modifier) { Text(label, color = DriveColors.Gray700, style = MaterialTheme.typography.labelLarge) }
}

@Composable fun ScreenTopBar(title: String, onBack: (() -> Unit)? = null, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = DriveDimens.Page), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) BackButton(onBack)
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) DriveGhostButton(action, onAction)
    }
}

@Composable fun BackButton(onClick: () -> Unit) {
    IconButton(onClick) {
        DriveIcon(DriveIconType.CHEVRON, DriveColors.Black, Modifier.size(24.dp).rotate(180f))
    }
}


@Composable fun Eyebrow(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray500)
}

@Composable fun SectionHeading(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) DriveGhostButton(action, onAction)
    }
}

@Composable fun VehicleConnectionCard(connected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 88.dp).clip(RoundedCornerShape(20.dp))
        .background(DriveColors.Gray100).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        DriveIcon(DriveIconType.CAR, DriveColors.Black, Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(if (connected) "차량이 연결되어 있어요" else "차량 연결하기", Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = DriveColors.Black, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        DriveIcon(DriveIconType.CHEVRON, DriveColors.Gray500, Modifier.size(20.dp))
    }
}

@Composable fun RecordCard(record: RecordDto, photo: java.io.File? = null, photoVersion: Int = 0, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(104.dp).border(1.dp, DriveColors.Gray200, RoundedCornerShape(DriveDimens.CardRadius))
        .clickable(onClick = onClick).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        DrivePhoto(photo, photoVersion, Modifier.size(78.dp,82.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(record.placeName, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            Spacer(Modifier.height(3.dp))
            Text(record.displayDate(), style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray700)
            Spacer(Modifier.height(5.dp))
            Text("${record.displayDistance()}  ${record.displayDuration()}", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
    }
}

@Composable fun FeatureCard(title: String, description: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(DriveColors.Black, RoundedCornerShape(DriveDimens.CardRadius))
        .clickable(onClick = onClick).padding(24.dp)) {
        Text("DRIVELOG CARD  /  01", style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray300)
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(4.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = DriveColors.Gray300)
        Spacer(Modifier.height(22.dp))
        Text("시작하기  ↗", style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable fun LockedFeatureCard(title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().border(1.dp, DriveColors.Gray200, RoundedCornerShape(18.dp))
        .clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(DriveColors.Gray100, CircleShape), contentAlignment = Alignment.Center) {
            Text("⌁", style = MaterialTheme.typography.titleLarge, color = DriveColors.Gray500)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray500)
        }
        Text("↗", color = DriveColors.Gray500)
    }
}

@Composable fun EditorOptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (selected) DriveColors.Black else DriveColors.White,
        border = BorderStroke(1.dp, if (selected) DriveColors.Black else DriveColors.Gray300)) {
        Text(label, Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            color = if (selected) DriveColors.White else DriveColors.Black, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable fun StatusBadge(label: String) {
    Text(label, Modifier.background(DriveColors.Gray100, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
        style = MaterialTheme.typography.labelSmall, color = DriveColors.Gray700)
}

@Composable fun SegmentedCodeInput(value: String, onValueChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        BasicTextField(value = value, onValueChange = { onValueChange(it.filter(Char::isDigit).take(6)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true,
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = Color.Transparent),
            modifier = Modifier.fillMaxWidth(), decorationBox = { inner ->
                Box {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(6) { index ->
                            val active = index == value.length.coerceAtMost(5)
                            Box(Modifier.weight(1f).aspectRatio(0.85f).border(if (active) 2.dp else 1.dp,
                                if (active) DriveColors.Black else DriveColors.Gray300, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(value.getOrNull(index)?.toString() ?: "", textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                    Box(Modifier.size(1.dp)) { inner() }
                }
            })
    }
}
