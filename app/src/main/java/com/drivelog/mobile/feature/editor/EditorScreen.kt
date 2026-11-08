package com.drivelog.mobile.feature.editor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.model.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.ui.theme.*
import java.io.File
import java.util.Locale

enum class EditorTab { FILTER, LAYOUT, INFO }

@Composable fun EditorScreen(record: RecordDto, draft: CardDraft, onDraft: (CardDraft) -> Unit, photo: File?, photoVersion: Int,
    onBack: () -> Unit, onPhoto: () -> Unit, onPreview: () -> Unit, onSaveSettings: () -> Unit, onSave: () -> Unit = onPreview) {
    var tab by remember(record.id) { mutableStateOf(EditorTab.FILTER) }
    BoxWithConstraints(Modifier.fillMaxSize().background(DriveColors.White)) {
        val panelHeight = if (maxHeight < 700.dp) 230.dp else 270.dp
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = DriveDimens.Page), verticalAlignment = Alignment.CenterVertically) {
                BackButton(onBack)
                Text(stringResource(R.string.editor_title), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Button(onSave, colors = ButtonDefaults.buttonColors(containerColor = DriveColors.Black, contentColor = DriveColors.White)) { Text("저장하기") }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val previewWidth = minOf(maxWidth - 48.dp, (maxHeight - 62.dp).coerceAtLeast(100.dp) * .75f, 350.dp)
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    CardCanvas(record, draft, photo, photoVersion, Modifier.width(previewWidth),
                        onCropChange = onDraft, onPhotoClick = onPhoto)
                    Spacer(Modifier.height(7.dp))
                    Row(Modifier.width(previewWidth), verticalAlignment = Alignment.CenterVertically) {
                        DriveGhostButton("미리보기", onPreview, Modifier.weight(1f))
                        DriveGhostButton(if (photo?.exists() == true) "사진 교체" else "사진 선택", onPhoto, Modifier.weight(1f))
                    }
                }
            }
            HorizontalDivider(color = DriveColors.Gray200)
            Column(Modifier.fillMaxWidth().height(panelHeight).background(DriveColors.White)) {
                Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    EditorTab.entries.forEach { item ->
                        val label = when (item) {
                            EditorTab.FILTER -> stringResource(R.string.tab_filter)
                            EditorTab.LAYOUT -> stringResource(R.string.tab_layout)
                            EditorTab.INFO -> stringResource(R.string.tab_info)
                        }
                        Column(Modifier.weight(1f).fillMaxHeight().clickable { tab = item },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(label, style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (tab == item) FontWeight.SemiBold else FontWeight.Medium),
                                color = if (tab == item) DriveColors.Black else DriveColors.TertiaryText)
                            Spacer(Modifier.height(5.dp))
                            Box(Modifier.width(22.dp).height(2.dp).background(if (tab == item) DriveColors.Black else Color.Transparent))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    when (tab) {
                        EditorTab.FILTER -> FilterPicker(draft, photo, photoVersion, onDraft)
                        EditorTab.LAYOUT -> LayoutPicker(record, draft, photo, photoVersion, onDraft)
                        EditorTab.INFO -> InfoControls(draft, onDraft)

                    }
                }
            }
        }
    }
}

@Composable private fun FilterPicker(draft: CardDraft, photo: File?, photoVersion: Int, onDraft: (CardDraft) -> Unit) {
    Column {
        Text("사진 분위기", Modifier.padding(horizontal = DriveDimens.Page), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = DriveDimens.Page), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            items(CardFilter.entries) { filter ->
                Column(Modifier.clickable { onDraft(draft.copy(filter = filter)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    PhotoFilterThumbnail(photo, photoVersion, filter, Modifier.size(72.dp, 82.dp)
                        .border(if (draft.filter == filter) 2.dp else 1.dp,
                            if (draft.filter == filter) DriveColors.Black else DriveColors.Border, RoundedCornerShape(11.dp)))
                    Spacer(Modifier.height(7.dp))
                    Text(filter.label, style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (draft.filter == filter) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (draft.filter == filter) DriveColors.Black else DriveColors.TertiaryText)
                }
            }
        }
    }
}

@Composable private fun LayoutPicker(record: RecordDto, draft: CardDraft, photo: File?, photoVersion: Int, onDraft: (CardDraft) -> Unit) {
    Column {
        Text("카드 레이아웃", Modifier.padding(horizontal = DriveDimens.Page), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = DriveDimens.Page), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            items(listOf(CardLayout.PHOTO, CardLayout.STAT, CardLayout.ROUTE, CardLayout.JOURNAL, CardLayout.SPLIT)) { layout ->
                Column(Modifier.clickable { onDraft(draft.copy(layout = layout)) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    LayoutThumbnail(layout, record, photo, photoVersion, Modifier.width(72.dp)
                        .border(if (draft.layout == layout) 2.dp else 1.dp,
                            if (draft.layout == layout) DriveColors.Black else DriveColors.Border, RoundedCornerShape(9.dp)),
                        blackTemplate = draft.blackTemplate)
                    Spacer(Modifier.height(7.dp))
                    Text(layout.label, style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (draft.layout == layout) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (draft.layout == layout) DriveColors.Black else DriveColors.TertiaryText)
                }
            }
        }
    }
}

@Composable private fun InfoControls(draft: CardDraft, onDraft: (CardDraft) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page)) {
        OutlinedTextField(draft.title, { onDraft(draft.copy(title = it.take(40))) }, label = { Text(stringResource(R.string.title_label)) },
            singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DriveColors.Black, unfocusedBorderColor = DriveColors.Border))
        Spacer(Modifier.height(8.dp))
        InfoSwitch(stringResource(R.string.route_label), draft.showRoute) { onDraft(draft.copy(showRoute = it)) }
        InfoSwitch(stringResource(R.string.distance_label), draft.showDistance) { onDraft(draft.copy(showDistance = it)) }
        InfoSwitch(stringResource(R.string.time_label), draft.showTime) { onDraft(draft.copy(showTime = it)) }
        InfoSwitch(stringResource(R.string.date_label), draft.showDate) { onDraft(draft.copy(showDate = it)) }
        InfoSwitch(stringResource(R.string.place_label), draft.showPlace) { onDraft(draft.copy(showPlace = it)) }
        InfoSwitch(stringResource(R.string.efficiency_label), draft.showEfficiency, "정보 없음", false) { onDraft(draft.copy(showEfficiency = it)) }
        InfoSwitch(stringResource(R.string.trim_label), draft.trimEnds) { onDraft(draft.copy(trimEnds = it)) }
    }
}

@Composable private fun InfoSwitch(label: String, checked: Boolean, note: String? = null,
    enabled: Boolean = true, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 51.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) DriveColors.Black else DriveColors.TertiaryText)
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = DriveColors.TertiaryText)
        }
        Switch(checked, onChecked, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DriveColors.White, checkedTrackColor = DriveColors.Black,
                uncheckedThumbColor = DriveColors.TertiaryText, uncheckedTrackColor = DriveColors.Gray200,
                checkedBorderColor = DriveColors.Black, uncheckedBorderColor = DriveColors.Gray300,
                disabledUncheckedThumbColor = DriveColors.Gray500, disabledUncheckedTrackColor = DriveColors.Gray200))
    }
}

@Preview @Composable private fun EditorPreview() { DRIVELOGMobileTheme {
    EditorScreen(DemoJourneys.records.first(), CardDraft(), {}, null, 0, {}, {}, {}, {})
} }
