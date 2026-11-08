package com.drivelog.mobile.feature.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
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
import com.drivelog.mobile.feature.journey.RecentDriveMapCache
import com.drivelog.mobile.feature.journey.cardMapPoints
import com.drivelog.mobile.ui.theme.DriveColors
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun CardCanvas(record: RecordDto, draft: CardDraft, photo: File?, photoVersion: Int,
    modifier: Modifier = Modifier, onCropChange: ((CardDraft) -> Unit)? = null,
    onPhotoClick: (() -> Unit)? = null) {
    val photoExists = photo?.exists() == true
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapCache = remember(context) { RecentDriveMapCache.shared(context) }
    val source = remember(photo?.path, photoVersion, photoExists) { if (photoExists) photo!! else com.drivelog.mobile.feature.common.DrivePhotoAsset.file(context) }
    val photoBounds = remember(photo?.path, photoVersion) {
        photo?.takeIf { it.exists() }?.let {
            BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { options -> BitmapFactory.decodeFile(it.path, options) }
        }
    }
    val rendered by produceState<ImageBitmap?>(null, record.id, draft, photoVersion, photoExists) {
        val map = if (!photoExists) {
            val points = cardMapPoints(record, draft.showRoute && draft.trimEnds)
            if (points.size >= 2) mapCache.load(record, points, 900, 1200, includeRoute = draft.showRoute) else null
        } else null
        value = withContext(Dispatchers.IO) {
            Cards.render(record, source, draft.title.ifBlank { record.placeName }, draft.toSettings(), map,
                draft.blackElements).asImageBitmap()
        }
    }
    val currentDraft by rememberUpdatedState(draft)
    val currentChange by rememberUpdatedState(onCropChange)
    val currentPhotoClick by rememberUpdatedState(onPhotoClick)
    BoxWithConstraints(modifier.aspectRatio(3f / 4f).clip(RoundedCornerShape(18.dp)).background(DriveColors.Gray100)) {
        if (rendered != null) Image(rendered!!, "편집 중인 실제 카드", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        else com.drivelog.mobile.feature.common.DrivePhoto(modifier = Modifier.fillMaxSize())
        if (onCropChange != null || onPhotoClick != null) {
            val transform = if (photoExists && onCropChange != null) Modifier.pointerInput(photo?.path, draft.layout) {
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
                        currentChange?.invoke(current.copy(zoom = nextZoom, panX = x, panY = y))
                    }
                }
            } else Modifier
            Box(Modifier.fillMaxSize().then(transform).pointerInput(photoExists) {
                detectTapGestures { position ->
                    val element = defaultCardElementAt(position, size.width, size.height, currentDraft)
                    if (element != null && currentChange != null) {
                        val colors = currentDraft.blackElements
                        currentChange?.invoke(currentDraft.copy(blackElements =
                            if (element in colors) colors - element else colors + element))
                    } else if (!photoExists) currentPhotoClick?.invoke()
                }
            })
        }
    }
}

private fun defaultCardElementAt(position: Offset, width: Int, height: Int, draft: CardDraft): CardElement? {
    if (draft.layout != CardLayout.PHOTO || width <= 0 || height <= 0) return null
    val x = position.x * 1080f / width
    val y = position.y * 1440f / height
    if (x in 35f..370f && y in 35f..130f) return CardElement.BRAND
    if (draft.showDate && x in 690f..1045f && y in 35f..125f) return CardElement.DATE
    if (draft.showPlace && x in 35f..1045f && y in 1050f..1180f) return CardElement.PLACE
    if (x !in 80f..1000f || y !in 1180f..1350f) return null
    val metrics = buildList {
        if (draft.showDistance) add(CardElement.DISTANCE)
        if (draft.showTime) add(CardElement.TIME)
        add(CardElement.EFFICIENCY)
    }
    return metrics[((x - 80f) / 920f * metrics.size).toInt().coerceIn(0, metrics.lastIndex)]
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
