package com.drivelog.mobile.feature.journey

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.data.RoutePoint
import com.drivelog.mobile.feature.common.DrivePhoto
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshot
import org.maplibre.android.snapshotter.MapSnapshotter

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"
private const val STYLE_VERSION = "v3"
private const val GAP_MS = 60_000L
private const val RECENT_MAP_ATTRIBUTION = "OpenFreeMap\n© OpenMapTiles\nData from OpenStreetMap"
private const val MAP_ATTRIBUTION = "OpenFreeMap · © OpenMapTiles · Data from OpenStreetMap"

private fun validRoute(route: List<RoutePoint>) = route.filter {
    it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -85.0..85.0 && it.longitude in -180.0..180.0
}

internal fun cardMapPoints(record: RecordDto, trimEnds: Boolean): List<RoutePoint> {
    val points = validRoute(record.route)
    if (!trimEnds || points.size < 12) return points
    val cut = max(1, (points.size * .1).roundToInt())
    return points.drop(cut).dropLast(cut)
}

internal data class DetailRouteMap(val bitmap: Bitmap, val route: List<Offset>)

/** One snapshotter and one request at a time, as in the vehicle card cache. */
internal class RecentDriveMapCache(context: Context) {
    companion object {
        @Volatile private var instance: RecentDriveMapCache? = null

        fun shared(context: Context): RecentDriveMapCache = instance ?: synchronized(this) {
            instance ?: RecentDriveMapCache(context.applicationContext).also { instance = it }
        }
    }

    private val context = context.applicationContext
    private val directory = File(this.context.cacheDir, "recent-drive-maps-$STYLE_VERSION").apply { mkdirs() }
    private val lock = Mutex()
    private var style: String? = null
    private var snapshotter: MapSnapshotter? = null
    private var snapshotSize: Pair<Int, Int>? = null

    suspend fun load(record: RecordDto, points: List<RoutePoint>, width: Int, height: Int,
        includeRoute: Boolean = true): Bitmap? = lock.withLock {
        if (points.size < 2) return@withLock null
        val target = cacheFile(record, points, width, height, includeRoute)
        withContext(Dispatchers.IO) { BitmapFactory.decodeFile(target.path) }?.let { return@withLock it }
        try {
            val mapStyle = style ?: withContext(Dispatchers.IO) { loadStyle() }?.also { style = it } ?: return@withLock null
            val bitmap = withTimeoutOrNull(30_000L) { capture(points, width, height, mapStyle, includeRoute) } ?: return@withLock null
            withContext(Dispatchers.IO) {
                val temp = File(directory, target.name + ".tmp")
                temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                if (!temp.renameTo(target)) temp.delete()
            }
            bitmap
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    suspend fun loadDetail(record: RecordDto, points: List<RoutePoint>, width: Int, height: Int): DetailRouteMap? = lock.withLock {
        if (points.size < 2) return@withLock null
        val target = cacheFile(record, points, width, height, includeRoute = false)
        val coordinates = File(directory, target.nameWithoutExtension + ".json")
        val saved = withContext(Dispatchers.IO) {
            runCatching {
                val image = BitmapFactory.decodeFile(target.path) ?: return@runCatching null
                try {
                    val json = JSONArray(coordinates.readText())
                    val route = (0 until json.length()).map { index ->
                        val pair = json.getJSONArray(index)
                        Offset(pair.getDouble(0).toFloat(), pair.getDouble(1).toFloat())
                    }
                    if (route.size == points.size) DetailRouteMap(image, route)
                    else { image.recycle(); null }
                } catch (error: Exception) {
                    image.recycle()
                    throw error
                }
            }.getOrNull()
        }
        if (saved != null) return@withLock saved
        try {
            val mapStyle = style ?: withContext(Dispatchers.IO) { loadStyle() }?.also { style = it } ?: return@withLock null
            val frame = withTimeoutOrNull(30_000L) {
                withContext(Dispatchers.Main) {
                    val snapshot = captureSnapshot(points, width, height, mapStyle) ?: return@withContext null
                    DetailRouteMap(snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, false), points.map {
                        snapshot.pixelForLatLng(LatLng(it.latitude, it.longitude)).let { pixel -> Offset(pixel.x, pixel.y) }
                    })
                }
            } ?: return@withLock null
            withContext(Dispatchers.IO) {
                val temp = File(directory, target.name + ".tmp")
                temp.outputStream().use { frame.bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                if (!temp.renameTo(target)) temp.delete()
                coordinates.writeText(JSONArray().apply {
                    frame.route.forEach { put(JSONArray().put(it.x.toDouble()).put(it.y.toDouble())) }
                }.toString())
            }
            frame
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheFile(record: RecordDto, points: List<RoutePoint>, width: Int, height: Int,
        includeRoute: Boolean): File {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update("$STYLE_VERSION|${record.id}|$width|$height|$includeRoute".toByteArray())
        points.forEach { digest.update("|${it.latitude},${it.longitude},${it.epochMillis}".toByteArray()) }
        val name = digest.digest().joinToString("") { "%02x".format(it) }
        return File(directory, "$name.jpg")
    }

    private fun loadStyle(): String? = runCatching {
        val connection = URL(STYLE_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            check(connection.responseCode in 200..299)
            val provider = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            JSONObject().put("version", 8).put("sources", provider.getJSONObject("sources"))
                .put("layers", JSONArray(DARK_LAYERS)).toString()
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private suspend fun capture(points: List<RoutePoint>, width: Int, height: Int, style: String,
        includeRoute: Boolean): Bitmap? = withContext(Dispatchers.Main) {
            val snapshot = captureSnapshot(points, width, height, style) ?: return@withContext null
            if (includeRoute) drawRoute(snapshot, points) else snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

    private suspend fun captureSnapshot(points: List<RoutePoint>, width: Int, height: Int, style: String): MapSnapshot? {
            MapLibre.getInstance(context)
            val south = points.minOf { it.latitude }; val north = points.maxOf { it.latitude }
            val west = points.minOf { it.longitude }; val east = points.maxOf { it.longitude }
            val latPad = max((north - south) * .22, (.035 - (north - south)) / 2)
            val lonPad = max((east - west) * .22, (.035 - (east - west)) / 2)
            val bounds = LatLngBounds.Builder()
                .include(LatLng((south - latPad).coerceAtLeast(-85.0), (west - lonPad).coerceAtLeast(-180.0)))
                .include(LatLng((north + latPad).coerceAtMost(85.0), (east + lonPad).coerceAtMost(180.0)))
                .build()
            val renderer = snapshotter?.also {
                if (snapshotSize != width to height) it.setSize(width, height)
                it.setRegion(bounds)
            } ?: MapSnapshotter(context, MapSnapshotter.Options(width, height).withPixelRatio(1f)
                .withRegion(bounds).withLogo(false).withAttribution(false).withStyleBuilder(Style.Builder().fromJson(style)))
                .also { snapshotter = it }
            snapshotSize = width to height
            return awaitSnapshot(renderer) ?: run {
                delay(1_000)
                awaitSnapshot(renderer)
            }
    }

    private suspend fun awaitSnapshot(renderer: MapSnapshotter): MapSnapshot? =
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { renderer.cancel() }
            renderer.start({ result -> if (continuation.isActive) continuation.resume(result) },
                { if (continuation.isActive) continuation.resume(null) })
        }

    private fun drawRoute(snapshot: MapSnapshot, points: List<RoutePoint>): Bitmap {
        val bitmap = snapshot.bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(bitmap)
        val stroke = minOf(minOf(bitmap.width, bitmap.height) * .045f,
            context.resources.displayMetrics.density * 5f)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val pixels = points.map { snapshot.pixelForLatLng(LatLng(it.latitude, it.longitude)) }
        for (index in 1 until points.size) {
            val gap = points[index].epochMillis - points[index - 1].epochMillis
            if (gap < 0) continue
            ink.pathEffect = if (gap > GAP_MS) DashPathEffect(floatArrayOf(stroke * 1.5f, stroke * 2f), 0f) else null
            canvas.drawLine(pixels[index - 1].x, pixels[index - 1].y, pixels[index].x, pixels[index].y, ink)
        }
        ink.pathEffect = null
        ink.style = Paint.Style.FILL
        val start = pixels.first()
        ink.color = 0xFF131417.toInt(); canvas.drawCircle(start.x, start.y, stroke * 1.6f, ink)
        ink.color = AndroidColor.WHITE; canvas.drawCircle(start.x, start.y, stroke, ink)
        val end = pixels.last()
        canvas.drawCircle(end.x, end.y, stroke * 1.6f, ink)
        ink.color = 0xFF131417.toInt(); canvas.drawCircle(end.x, end.y, stroke, ink)
        return bitmap
    }
}

private fun road(id: String, classes: String, color: String, narrow: Double, wide: Double) = """
    {"id":"$id","type":"line","source":"openmaptiles","source-layer":"transportation",
     "filter":["in","class",$classes],"layout":{"line-cap":"round","line-join":"round"},
     "paint":{"line-color":"$color","line-width":["interpolate",["exponential",1.5],["zoom"],10,$narrow,16,$wide],
              "line-opacity":["interpolate",["linear"],["zoom"],6,0.35,10,0.75,12,1]}}
"""

private val DARK_LAYERS = """[
    {"id":"land","type":"background","paint":{"background-color":"#1B1C1F"}},
    {"id":"green","type":"fill","source":"openmaptiles","source-layer":"landcover","filter":["in","class","wood","grass"],"paint":{"fill-color":"#17181B"}},
    {"id":"water","type":"fill","source":"openmaptiles","source-layer":"water","filter":["!=","brunnel","tunnel"],"paint":{"fill-color":"#08090A"}},
    ${road("road-minor", "\"minor\",\"service\"", "#3A3C41", .35, 2.0)},
    ${road("road-mid", "\"secondary\",\"tertiary\"", "#5E6168", .7, 4.5)},
    ${road("road-major", "\"primary\",\"trunk\"", "#8F9198", 1.1, 7.0)},
    ${road("road-motorway", "\"motorway\"", "#B4B6BC", 1.6, 8.0)}
]"""

/** The same recorded-route map serves the list, home hero, and detail preview at their existing sizes. */
@Composable
internal fun DriveRouteMapImage(
    record: RecordDto, cache: RecentDriveMapCache, photo: File?, photoVersion: Int,
    modifier: Modifier = Modifier, compactCredit: Boolean = false,
    creditAlignment: Alignment = Alignment.BottomStart, showEmptyMessage: Boolean = false,
) {
    val points = remember(record.route) { validRoute(record.route) }
    val view = LocalView.current
    var mapSize by remember(record.id) { mutableStateOf(IntSize.Zero) }
    var visible by remember(record.id) { mutableStateOf(false) }
    var bitmap by remember(record.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(record.id, points, visible, mapSize) {
        bitmap = if (visible && points.size >= 2 && mapSize.width > 0 && mapSize.height > 0)
            cache.load(record, points, mapSize.width, mapSize.height) else null
    }
    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        mapSize = coordinates.size
        visible = bounds.bottom > 0f && bounds.top < view.rootView.height
    }.background(Color(0xFF131417)).semantics {
        contentDescription = if (bitmap != null) "주행 경로 지도" else "주행 경로"
    }) {
        when {
            bitmap != null -> Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            points.size < 2 && photo?.exists() == true -> DrivePhoto(photo, photoVersion, Modifier.fillMaxSize())
            else -> RouteMapPlaceholder(points, Modifier.fillMaxSize())
        }
        if (bitmap != null) Text(if (compactCredit) RECENT_MAP_ATTRIBUTION else MAP_ATTRIBUTION,
            Modifier.align(creditAlignment).fillMaxWidth()
                .padding(horizontal = if (compactCredit) 2.dp else 10.dp, vertical = if (compactCredit) 1.dp else 8.dp),
            color = Color.White, fontSize = if (compactCredit) 5.5.sp else 9.sp,
            lineHeight = if (compactCredit) 6.5.sp else 11.sp, maxLines = if (compactCredit) 3 else 1)
        if (showEmptyMessage && points.size < 2 && photo?.exists() != true) Text("경로 정보 없음",
            Modifier.align(Alignment.Center), color = Color.White)
    }
}

@Composable
internal fun RecentDriveMapThumbnail(
    record: RecordDto, cache: RecentDriveMapCache, photo: File?, photoVersion: Int,
    modifier: Modifier = Modifier,
) = DriveRouteMapImage(record, cache, photo, photoVersion, modifier, compactCredit = true)

@Composable
internal fun DetailDriveRouteMap(record: RecordDto, cache: RecentDriveMapCache, modifier: Modifier = Modifier) {
    val points = remember(record.route) { validRoute(record.route) }
    val view = LocalView.current
    var mapSize by remember(record.id) { mutableStateOf(IntSize.Zero) }
    var visible by remember(record.id) { mutableStateOf(false) }
    var frame by remember(record.id) { mutableStateOf<DetailRouteMap?>(null) }
    var loaded by remember(record.id) { mutableStateOf(false) }
    val trace = remember(record.id) { Animatable(0f) }
    LaunchedEffect(record.id, points, visible, mapSize) {
        if (visible && points.size >= 2 && mapSize.width > 0 && mapSize.height > 0) {
            loaded = false
            frame = cache.loadDetail(record, points, mapSize.width, mapSize.height)
            loaded = true
        } else {
            frame = null
            loaded = false
        }
    }
    val route = remember(frame, loaded, mapSize, points) {
        when {
            frame != null -> smoothRoute(frame!!.route)
            loaded && mapSize.width > 0 && mapSize.height > 0 ->
                smoothRoute(fitRoute(points, mapSize.width.toFloat(), mapSize.height.toFloat()))
            else -> emptyList()
        }
    }
    val totalLength = remember(route) { route.zipWithNext().sumOf { (a, b) -> (b - a).getDistance().toDouble() } }
    LaunchedEffect(route) {
        trace.snapTo(0f)
        if (route.size >= 2) trace.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
    }
    Box(modifier.onGloballyPositioned { coordinates ->
        val bounds = coordinates.boundsInWindow()
        mapSize = coordinates.size
        visible = bounds.bottom > 0f && bounds.top < view.rootView.height
    }.background(Color(0xFF131417)).semantics { contentDescription = "주행 경로 지도" }) {
        val current = frame
        if (current != null) {
            Image(current.bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            RouteMapPlaceholder(emptyList(), Modifier.fillMaxSize())
            if (points.size < 2) Text("경로 정보 없음", Modifier.align(Alignment.Center), color = Color.White)
        }
        Canvas(Modifier.fillMaxSize()) {
            if (route.size < 2) return@Canvas
            var remaining = totalLength * trace.value
            val path = Path().apply { moveTo(route.first().x, route.first().y) }
            for (index in 1 until route.size) {
                val from = route[index - 1]
                val to = route[index]
                val length = (to - from).getDistance().toDouble()
                if (remaining >= length) {
                    path.lineTo(to.x, to.y)
                    remaining -= length
                } else {
                    if (length > 0) {
                        val part = (remaining / length).toFloat().coerceIn(0f, 1f)
                        path.lineTo(from.x + (to.x - from.x) * part, from.y + (to.y - from.y) * part)
                    }
                    break
                }
            }
            val stroke = minOf(size.minDimension * .045f, 5.dp.toPx())
            drawPath(path, Color.White, style = Stroke(stroke, cap = StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawCircle(Color(0xFF131417), stroke * 1.6f, route.first())
            drawCircle(Color.White, stroke, route.first())
            if (trace.value >= 1f) {
                drawCircle(Color.White, stroke * 1.6f, route.last())
                drawCircle(Color(0xFF131417), stroke, route.last())
            }
        }
        if (current != null) Text(MAP_ATTRIBUTION, Modifier.align(Alignment.BottomStart).fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp), color = Color.White,
            fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1)
    }
}

private fun fitRoute(points: List<RoutePoint>, width: Float, height: Float): List<Offset> {
    if (points.size < 2) return emptyList()
    val k = cos(Math.toRadians(points.map { it.latitude }.average()))
    val xs = points.map { it.longitude * k }
    val ys = points.map { -it.latitude }
    val minX = xs.min(); val minY = ys.min()
    val spanX = (xs.max() - minX).coerceAtLeast(1e-6)
    val spanY = (ys.max() - minY).coerceAtLeast(1e-6)
    val scale = minOf(width * .78 / spanX, height * .78 / spanY)
    val ox = (width - spanX * scale) / 2
    val oy = (height - spanY * scale) / 2
    return points.map { Offset((ox + (it.longitude * k - minX) * scale).toFloat(),
        (oy + (-it.latitude - minY) * scale).toFloat()) }
}

private fun smoothRoute(points: List<Offset>): List<Offset> {
    if (points.size < 3) return points
    val result = ArrayList<Offset>(points.size * 4)
    result.add(points.first())
    for (index in 0 until points.lastIndex) {
        val p0 = points[(index - 1).coerceAtLeast(0)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[(index + 2).coerceAtMost(points.lastIndex)]
        for (step in 1..4) {
            val t = step / 4f
            val t2 = t * t
            val t3 = t2 * t
            fun coordinate(a: Float, b: Float, c: Float, d: Float) = .5f *
                (2f * b + (-a + c) * t + (2f * a - 5f * b + 4f * c - d) * t2 +
                    (-a + 3f * b - 3f * c + d) * t3)
            result.add(Offset(coordinate(p0.x, p1.x, p2.x, p3.x),
                coordinate(p0.y, p1.y, p2.y, p3.y)))
        }
    }
    return result
}

@Composable
private fun RouteMapPlaceholder(points: List<RoutePoint>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF131417))
        val spacing = 9.dp.toPx()
        var y = spacing / 2
        while (y < size.height) {
            var x = spacing / 2
            while (x < size.width) {
                drawCircle(Color.White.copy(alpha = .10f), .6.dp.toPx(), Offset(x, y))
                x += spacing
            }
            y += spacing
        }
        if (points.size < 2) return@Canvas
        val k = cos(Math.toRadians(points.map { it.latitude }.average()))
        val xs = points.map { it.longitude * k }
        val ys = points.map { -it.latitude }
        val minX = xs.min(); val minY = ys.min()
        val spanX = (xs.max() - minX).coerceAtLeast(1e-6)
        val spanY = (ys.max() - minY).coerceAtLeast(1e-6)
        val scale = minOf(size.width * .78 / spanX, size.height * .78 / spanY)
        val ox = (size.width - spanX * scale) / 2
        val oy = (size.height - spanY * scale) / 2
        val pixels = points.map { Offset((ox + (it.longitude * k - minX) * scale).toFloat(), (oy + (-it.latitude - minY) * scale).toFloat()) }
        val stroke = 2.dp.toPx()
        for (index in 1 until points.size) {
            if (points[index].epochMillis < points[index - 1].epochMillis) continue
            drawLine(Color.White, pixels[index - 1], pixels[index], strokeWidth = stroke, cap = StrokeCap.Round,
                pathEffect = if (points[index].epochMillis - points[index - 1].epochMillis > GAP_MS)
                    PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2)) else null)
        }
        drawCircle(Color.White, stroke * 1.5f, pixels.first())
        drawCircle(Color.White, stroke * 1.6f, pixels.last())
        drawCircle(Color(0xFF131417), stroke, pixels.last())
    }
}
