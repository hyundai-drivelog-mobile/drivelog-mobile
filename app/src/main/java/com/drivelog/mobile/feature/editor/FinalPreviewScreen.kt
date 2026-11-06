package com.drivelog.mobile.feature.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R
import com.drivelog.mobile.core.designsystem.DriveDimens
import com.drivelog.mobile.core.model.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.*
import com.drivelog.mobile.feature.Cards
import com.drivelog.mobile.ui.theme.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun FinalPreviewScreen(record: RecordDto, draft: CardDraft, photo: File?, photoVersion: Int,
    saved: Boolean, onBack: () -> Unit, onSavePng: () -> Unit, onSaveSettings: () -> Unit) {
    val rendered by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, record.id, draft, photoVersion) {
        value = if (photo?.exists() == true) withContext(Dispatchers.IO) {
            runCatching { Cards.render(record, photo, draft.title.ifBlank { record.placeName }, draft.toSettings()).asImageBitmap() }.getOrNull()
        } else null
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.preview), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            if (saved) Text("저장했어요", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.widthIn(max = 430.dp).fillMaxWidth().aspectRatio(3f / 4f)) {
                if (rendered != null) Image(rendered!!, "최종 PNG 미리보기", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                else CardCanvas(record, draft, photo, photoVersion, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(DriveDimens.Page)) {
            DriveButton(stringResource(R.string.save_png), onSavePng, Modifier.fillMaxWidth(), photo?.exists() == true)
            Spacer(Modifier.height(10.dp))
            DriveOutlineButton(stringResource(R.string.keep_editing), onBack, Modifier.fillMaxWidth())
            DriveGhostButton(stringResource(R.string.save_settings), onSaveSettings, Modifier.fillMaxWidth())
        }
    }
}

@Preview @Composable private fun FinalPreview() { DRIVELOGMobileTheme {
    FinalPreviewScreen(DemoJourneys.records.first(), CardDraft(), null, 0, false, {}, {}, {})
} }
