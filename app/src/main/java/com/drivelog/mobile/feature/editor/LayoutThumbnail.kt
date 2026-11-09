package com.drivelog.mobile.feature.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.model.CardDraft
import com.drivelog.mobile.core.model.CardLayout
import com.drivelog.mobile.core.model.toSettings
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.Cards
import com.drivelog.mobile.ui.theme.DriveColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun LayoutThumbnail(layout: CardLayout, record: RecordDto, draft: CardDraft,
    modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Picker previews always use the bundled example, regardless of the selected card photo.
    val previewDraft = draft.copy(layout = layout, zoom = 1f, panX = 0f, panY = 0f)
    val thumbnail by produceState<ImageBitmap?>(null, record, previewDraft, context) {
        value = withContext(Dispatchers.Default) {
            val source = EditorThumbnailAsset.file(context)
            val rendered = Cards.render(record, source,
                previewDraft.title.ifBlank { record.placeName }, previewDraft.toSettings(),
                blackTemplate = previewDraft.blackTemplate)
            try {
                Bitmap.createScaledBitmap(rendered, 144, 192, true).asImageBitmap()
            } finally {
                rendered.recycle()
            }
        }
    }
    Box(modifier.aspectRatio(3f / 4f).clip(RoundedCornerShape(9.dp))
        .background(DriveColors.Gray100)) {
        thumbnail?.let { image ->
            Image(image, layout.label, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
    }
}
