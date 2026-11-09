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
import com.drivelog.mobile.feature.journey.cardMapPoints
import com.drivelog.mobile.ui.theme.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun FinalPreviewScreen(record: RecordDto, draft: CardDraft, photo: File?, photoVersion: Int,
    onBack: () -> Unit, onSavePng: () -> Unit) {
    val rendered by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, record.id, draft, photoVersion) {
        value = if (photo?.exists() == true) withContext(Dispatchers.IO) {
            runCatching { Cards.render(record, photo, draft.title.ifBlank { record.placeName }, draft.toSettings(),
                blackTemplate = draft.blackTemplate).asImageBitmap() }.getOrNull()
        } else null
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.preview), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = DriveDimens.Page),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Spacer(Modifier.height(18.dp))
            Box(Modifier.widthIn(max = 430.dp).fillMaxWidth().aspectRatio(3f / 4f)) {
                if (rendered != null) Image(rendered!!, "최종 PNG 미리보기", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                else CardCanvas(record, draft, photo, photoVersion, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(16.dp))
            DriveButton(stringResource(R.string.save_image), onSavePng, Modifier.widthIn(max = 430.dp).fillMaxWidth(),
                photo?.exists() == true || cardMapPoints(record, false).size >= 2)
            Spacer(Modifier.height(10.dp))
            DriveGhostButton(stringResource(R.string.keep_editing), onBack, Modifier.widthIn(max = 430.dp).fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview @Composable private fun FinalPreview() { DRIVELOGMobileTheme {
    FinalPreviewScreen(DemoJourneys.records.first(), CardDraft(), null, 0, {}, {})
} }
