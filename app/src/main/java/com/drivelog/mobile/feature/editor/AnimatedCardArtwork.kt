package com.drivelog.mobile.feature.editor

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.drivelog.mobile.core.model.CardDraft
import com.drivelog.mobile.core.model.CardLayout
import com.drivelog.mobile.core.model.toSettings
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.journey.cardMapPoints
import com.drivelog.mobile.feature.journey.smoothRoute
import com.drivelog.mobile.feature.journey.tracedRoutePath
import kotlin.math.cos
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

internal fun CardDraft.animatedPreviewSettings(record: RecordDto): JSONObject = toSettings().apply {
    if (layout == CardLayout.ROUTE && showRoute && cardMapPoints(record, trimEnds).size >= 2)
        put("previewHideRouteStroke", true)
    if (layout == CardLayout.SPLIT) put("previewHideFrameCorners", true)
}

internal data class PopupLayer(val image: ImageBitmap, val left: Int, val top: Int)
internal data class CardPreview(val layout: CardLayout, val image: ImageBitmap,
    val layers: List<PopupLayer> = emptyList())

internal fun popupLayer(bitmap: Bitmap, layout: CardLayout, index: Int): PopupLayer {
    val regions = when (layout) {
        CardLayout.STAT -> listOf(
            intArrayOf(65, 115, 1060, 470),
            intArrayOf(70, 485, 1040, 655),
            intArrayOf(70, 1085, 1050, 1290),
        )
        CardLayout.JOURNAL -> listOf(
            intArrayOf(40, 625, 650, 800),
            intArrayOf(40, 745, 700, 910),
            intArrayOf(40, 930, 1040, 1145),
            intArrayOf(40, 1145, 1040, 1235),
        )
        else -> emptyList()
    }
    val (left, top, right, bottom) = regions[index]
    val x = (left * bitmap.width / 1080f).roundToInt()
    val y = (top * bitmap.height / 1440f).roundToInt()
    val width = (right * bitmap.width / 1080f).roundToInt() - x
    val height = (bottom * bitmap.height / 1440f).roundToInt() - y
    return PopupLayer(Bitmap.createBitmap(bitmap, x, y, width, height).asImageBitmap(), x, y)
}

@Composable
internal fun AnimatedCardArtwork(layout: CardLayout, record: RecordDto, draft: CardDraft,
    preview: CardPreview, modifier: Modifier = Modifier) {
    val reveal = remember(record.id, layout) { Animatable(0f) }
    LaunchedEffect(preview.image, layout) {
        reveal.snapTo(0f)
        when (layout) {
            CardLayout.STAT, CardLayout.JOURNAL -> reveal.snapTo(1f)
            CardLayout.ROUTE -> reveal.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            CardLayout.SPLIT -> reveal.animateTo(1f, tween(1050, easing = FastOutSlowInEasing))
            CardLayout.PHOTO -> reveal.snapTo(1f)
        }
    }
    val popupProgress = remember(preview.layers) { preview.layers.map { Animatable(0f) } }
    LaunchedEffect(preview.layers) {
        popupProgress.forEach { it.snapTo(0f) }
        popupProgress.forEachIndexed { index, progress ->
            launch {
                delay(index * 160L)
                progress.animateTo(1f, spring(dampingRatio = .52f, stiffness = 380f))
            }
        }
    }
    val ink = if (draft.blackTemplate) Color.Black else Color.White
    Box(modifier) {
        Image(preview.image, "편집 중인 실제 카드", Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        if (preview.layers.isNotEmpty()) Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / 1200f
            val sy = size.height / 1600f
            preview.layers.forEachIndexed { index, layer ->
                val phase = popupProgress[index].value
                if (phase > 0f) {
                    val scale = .78f + .22f * phase
                    val width = layer.image.width * sx
                    val height = layer.image.height * sy
                    val left = layer.left * sx + width * (1f - scale) / 2f
                    val top = layer.top * sy + height * (1f - scale) / 2f
                    drawImage(layer.image,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize((width * scale).roundToInt().coerceAtLeast(1),
                            (height * scale).roundToInt().coerceAtLeast(1)),
                        alpha = phase.coerceIn(0f, 1f))
                }
            }
        }
        if (layout == CardLayout.ROUTE && draft.showRoute) {
            val projected = remember(record.route, draft.trimEnds) {
                projectCardRoute(cardMapPoints(record, draft.trimEnds))
            }
            Canvas(Modifier.fillMaxSize()) {
                if (projected.size < 2) return@Canvas
                val sx = size.width / 1080f
                val sy = size.height / 1440f
                val route = projected.map { Offset(it.x * sx, it.y * sy) }
                val total = route.zipWithNext().sumOf { (a, b) -> (b - a).getDistance().toDouble() }
                drawPath(tracedRoutePath(route, reveal.value, total), ink,
                    style = Stroke(13f * sx, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawCircle(ink, 17f * sx, route.first())
                if (reveal.value >= 1f) drawCircle(ink, 21f * sx, route.last(),
                    style = Stroke(7f * sx))
            }
        }
        if (layout == CardLayout.SPLIT) Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / 1080f
            val sy = size.height / 1440f
            val corners = listOf(
                Triple(84f to 185f, 1f to 1f, -1f to -1f),
                Triple(996f to 185f, -1f to 1f, 1f to -1f),
                Triple(996f to 1356f, -1f to -1f, 1f to 1f),
            )
            corners.forEachIndexed { index, (origin, arms, entrance) ->
                val phase = ((reveal.value - index * .22f) / .56f).coerceIn(0f, 1f)
                if (phase > 0f) {
                    val dx = entrance.first * (1f - phase) * 24f
                    val dy = entrance.second * (1f - phase) * 24f
                    val x = (origin.first + dx) * sx
                    val y = (origin.second + dy) * sy
                    val color = ink.copy(alpha = phase)
                    val arm = 72f * (.68f + .32f * phase)
                    rotate(entrance.first * (1f - phase) * 38f, Offset(x, y)) {
                        drawLine(color, Offset(x, y), Offset(x + arms.first * arm * sx, y), 9f * sx)
                        drawLine(color, Offset(x, y), Offset(x, y + arms.second * arm * sy), 9f * sx)
                    }
                }
            }
        }
    }
}

private fun projectCardRoute(points: List<com.drivelog.mobile.data.RoutePoint>): List<Offset> {
    if (points.size < 2) return emptyList()
    val k = cos(Math.toRadians(points.map { it.latitude }.average()))
    val xs = points.map { it.longitude * k }
    val ys = points.map { -it.latitude }
    val minX = xs.min()
    val minY = ys.min()
    val dx = (xs.max() - minX).coerceAtLeast(1e-7)
    val dy = (ys.max() - minY).coerceAtLeast(1e-7)
    val scale = minOf(780.0 / dx, 315.0 / dy)
    val ox = 150.0 + (780.0 - dx * scale) / 2
    val oy = 535.0 + (315.0 - dy * scale) / 2
    return smoothRoute(points.indices.map { index ->
        Offset((ox + (xs[index] - minX) * scale).toFloat(),
            (oy + (ys[index] - minY) * scale).toFloat())
    })
}
