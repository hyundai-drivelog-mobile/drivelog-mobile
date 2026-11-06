package com.drivelog.mobile.feature.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.model.*
import com.drivelog.mobile.core.util.CropMath
import com.drivelog.mobile.core.util.displayDate
import com.drivelog.mobile.core.util.displayDistance
import com.drivelog.mobile.core.util.displayDuration
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.Cards
import com.drivelog.mobile.ui.theme.DriveColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun CardCanvas(record: RecordDto, draft: CardDraft, photo: File?, photoVersion: Int,
    modifier: Modifier = Modifier, onCropChange: ((CardDraft) -> Unit)? = null,
    onPhotoClick: (() -> Unit)? = null) {
    val photoExists = photo?.exists() == true
    val context = androidx.compose.ui.platform.LocalContext.current
    val source = remember(photo?.path, photoVersion, photoExists) { if (photoExists) photo!! else com.drivelog.mobile.feature.common.DrivePhotoAsset.file(context) }
    val photoBounds = remember(photo?.path, photoVersion) {
        photo?.takeIf { it.exists() }?.let {
            BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { options -> BitmapFactory.decodeFile(it.path, options) }
        }
    }
    val rendered by produceState<ImageBitmap?>(null, record.id, draft, photoVersion, photoExists) {
        value = withContext(Dispatchers.IO) {
            Cards.render(record, source, draft.title.ifBlank { record.placeName }, draft.toSettings()).asImageBitmap()
        }
    }
    BoxWithConstraints(modifier.aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp)).background(DriveColors.Gray100)) {
        if (rendered != null) Image(rendered!!, "편집 중인 실제 카드", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        else com.drivelog.mobile.feature.common.DrivePhoto(modifier = Modifier.fillMaxSize())
        if (!photoExists && onPhotoClick != null) Box(Modifier.fillMaxSize().clickable(onClick = onPhotoClick))
        if (photoExists && onCropChange != null) {
            val currentDraft by rememberUpdatedState(draft)
            val currentChange by rememberUpdatedState(onCropChange)
            val photoArea = Modifier.fillMaxSize()
            Box(photoArea.pointerInput(photo?.path, draft.layout) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    val current = currentDraft
                    val nextZoom = (current.zoom * gestureZoom).coerceIn(1f, 3f)
                    if (photoBounds != null && photoBounds.outWidth > 0 && photoBounds.outHeight > 0) {
                        val placement = CropMath.place(photoBounds.outWidth.toFloat(), photoBounds.outHeight.toFloat(),
                            size.width.toFloat(), size.height.toFloat(), nextZoom, current.panX, current.panY)
                        val xSpace = (placement.width - size.width).coerceAtLeast(0f)
                        val ySpace = (placement.height - size.height).coerceAtLeast(0f)
                        val x = if (xSpace > 1f) (current.panX + 2f * pan.x / xSpace).coerceIn(-1f, 1f) else 0f
                        val y = if (ySpace > 1f) (current.panY + 2f * pan.y / ySpace).coerceIn(-1f, 1f) else 0f
                        currentChange(current.copy(zoom = nextZoom, panX = x, panY = y))
                    }
                }
            })
        }
    }
}

@Composable fun PhotoFilterThumbnail(photo: File?, photoVersion: Int, filter: CardFilter,
    modifier: Modifier = Modifier) {
    val bitmap = remember(photo?.path, photoVersion) {
        (photo?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) } ?: com.drivelog.mobile.feature.common.DrivePhotoAsset.filterBitmap()).asImageBitmap()
    }
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(DriveColors.Gray100), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, "${filter.label} 사진 필터", Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, colorFilter = filterColor(filter))
        else Text("사진 없음", style = MaterialTheme.typography.labelSmall, color = DriveColors.TertiaryText)
    }
}

private fun filterColor(filter: CardFilter): ColorFilter? {
    if (filter == CardFilter.ORIGINAL) return null
    val saturation = when (filter) {
        CardFilter.SOFT -> .72f; CardFilter.FILM -> .62f; CardFilter.MONO -> 0f; else -> 1f
    }
    val matrix = ColorMatrix().apply { setToSaturation(saturation) }
    if (filter == CardFilter.WARM) { matrix[0, 0] = 1.06f; matrix[2, 2] = .88f }
    return ColorFilter.colorMatrix(matrix)
}
