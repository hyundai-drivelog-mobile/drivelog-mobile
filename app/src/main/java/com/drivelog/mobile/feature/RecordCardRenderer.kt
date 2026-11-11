package com.drivelog.mobile.feature

import android.graphics.*
import android.content.Context
import com.drivelog.mobile.R
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.core.util.CropMath
import com.drivelog.mobile.core.util.displayDuration
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/** Native port of the source project's server/static/m/card.js record templates. */
object RecordCardRenderer {
    fun render(context: Context, r: RecordDto, photo: File, title: String, o: JSONObject, mapBackground: Bitmap? = null,
        blackTemplate: Boolean = false): Bitmap {
        val result = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        c.scale(1200f / 1080f, 1600f / 1440f)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val template = o.optString("template", "PHOTO")
        val journal = template == "JOURNAL"
        val split = template == "SPLIT"
        val stat = template == "STAT"
        val routeFocus = template == "ROUTE"
        val dark = journal && o.optString("tone") == "mono-dark"
        val ink = if (blackTemplate) Color.BLACK else Color.WHITE
        val hidePopup = o.optBoolean("previewHidePopupElements", false)
        val popupOnly = o.optBoolean("previewPopupOverlayOnly", false)
        val popupElement = o.optInt("previewPopupElement", -1)
        fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, width: Float = 968f,
                 align: Paint.Align = Paint.Align.LEFT, color: Int = ink) {
            p.shader = null; p.colorFilter = null; p.color = color; p.textSize = size
            p.style = Paint.Style.FILL
            p.textAlign = align
            p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            var display = value
            if (p.measureText(display) > width) {
                while (display.isNotEmpty() && p.measureText("$display…") > width) display = display.dropLast(1)
                display += "…"
            }
            c.drawText(display, x, y, p)
        }
        fun displayMetric(x: Float, baseline: Float, numberSize: Float, unitSize: Float) {
            val number = r.distanceKm?.let { "%.1f".format(Locale.KOREA, it) } ?: "—"
            p.shader = null; p.colorFilter = null; p.color = ink; p.textAlign = Paint.Align.LEFT
            p.style = Paint.Style.FILL
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            p.textSize = numberSize
            val width = p.measureText(number)
            c.drawText(number, x, baseline, p)
            if (r.distanceKm != null) {
                p.textSize = unitSize
                c.drawText("km", x + width + 12f, baseline - 14f, p)
            }
        }
        if (!popupOnly) {
            c.drawColor(if (dark) Color.BLACK else Color.WHITE)
            val rect = RectF(0f,0f,1080f,1440f)
            val bitmap = mapBackground ?: BitmapFactory.decodeFile(photo.path) ?: error("사진을 다시 선택해주세요")
            try {
                val crop = if (mapBackground == null) o.optJSONObject("crop") ?: JSONObject() else JSONObject()
                val zoom = crop.optDouble("zoom",1.0).takeIf { it.isFinite() }?.coerceIn(1.0,3.0) ?: 1.0
                fun pan(key: String) = crop.optDouble(key,0.0).takeIf { it.isFinite() }?.coerceIn(-1.0,1.0)?.toFloat() ?: 0f
                val placement = CropMath.place(bitmap.width.toFloat(), bitmap.height.toFloat(),
                    rect.width(), rect.height(), zoom.toFloat(), pan("x"), pan("y"))
                val x = rect.left + placement.left
                val y = rect.top + placement.top
                val tone = if (mapBackground == null) o.optString("tone", "color") else "color"
                val matrix = when (tone) {
                    "mono", "mono-dark" -> ColorMatrix().apply { setSaturation(0f) }
                    "soft" -> ColorMatrix().apply { setSaturation(.72f) }
                    "film" -> ColorMatrix().apply { setSaturation(.62f) }
                    "warm" -> ColorMatrix(floatArrayOf(1.06f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,.88f,0f,0f, 0f,0f,0f,1f,0f))
                    else -> null
                }
                if (matrix != null) p.colorFilter = ColorMatrixColorFilter(matrix)
                c.save(); c.clipRect(rect); c.drawBitmap(bitmap,null,RectF(x,y,x+placement.width,y+placement.height),p); c.restore(); p.colorFilter=null
            } finally { if (mapBackground == null) bitmap.recycle() }
        }
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 5f) {
            p.shader = null; p.colorFilter = null; p.color = ink; p.style = Paint.Style.STROKE; p.strokeWidth = width
            c.drawLine(x1, y1, x2, y2, p)
            p.style = Paint.Style.FILL
        }
        p.color = ink
        p.textSize = 42f; p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.create(Typeface.DEFAULT,Typeface.BOLD_ITALIC)
        if (!popupOnly) c.drawText("DRIVELOG",56f,96f,p)
        val distance=r.distanceKm?.let { "%.1f km".format(Locale.KOREA,it) } ?: "정보 없음"
        val date=SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(r.startedAtMillis))
        val editorialDate=SimpleDateFormat("MMM dd, yyyy",Locale.ENGLISH).format(Date(r.startedAtMillis)).uppercase(Locale.ENGLISH)
        val duration = r.displayDuration()
        if (split && !o.optBoolean("previewHideFrameCorners", false)) {
            val edge = 84f
            val top=185f
            line(edge,top,edge+72f,top,9f); line(edge,top,edge,top+72f,9f)
            line(1080f-edge-72f,top,1080f-edge,top,9f); line(1080f-edge,top,1080f-edge,top+72f,9f)
            line(1080f-edge-72f,1440f-edge,1080f-edge,1440f-edge,9f)
            line(1080f-edge,1440f-edge-72f,1080f-edge,1440f-edge,9f)
        }
        if (stat) {
            if (!hidePopup && popupElement in listOf(-1, 1)) {
                text("오늘도, 길 위에서 만난 새로운 풍경", 86f, 535f, 38f, false, 900f)
                if (o.optBoolean("showDate", true)) text(editorialDate, 86f, 620f, 38f, true, 900f)
            }
            if (!hidePopup && popupElement in listOf(-1, 2)) {
                if (o.optBoolean("showPlace", true)) {
                    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    p.textSize = 76f
                    val ruleWidth = p.measureText(title).coerceIn(220f, 900f)
                    line(86f, 1110f, 86f + ruleWidth, 1110f, 5f)
                    text(title, 86f, 1250f, 76f, true, 900f)
                }
            }
        }
        if (routeFocus) {
            if (!popupOnly) {
                line(348f, 99f, 752f, 99f, 3f)
                if (o.optBoolean("showDate", true)) text(editorialDate, 1030f, 110f, 35f, true, 260f, Paint.Align.RIGHT)
            }
            if (o.optBoolean("showPlace", true)) text("출발  →  $title", 86f, 345f, 82f, true, 940f)
            text("익숙한 도심을 지나, 새로운 풍경으로", 86f, 425f, 38f, false, 920f)
        }
        if (journal && !hidePopup) {
            if (popupElement in listOf(-1, 0)) text("Drive",56f,760f,112f,true)
            if (popupElement in listOf(-1, 1)) text("Journal",56f,865f,112f,true)
            if (popupElement in listOf(-1, 2)) {
                line(56f,965f,440f,965f,6f)
                if (o.optBoolean("showPlace",true)) text(title,56f,1120f,69f,true)
            }
        }
        if (split) {
            text("THE DRIVE",56f,1050f,38f,true)
            if (o.optBoolean("showPlace",true)) text(title,56f,1150f,81f,true)
        }
        if (!journal && !split && !stat && !routeFocus && o.optBoolean("showPlace",true)) {
            val words=title.trim().split(" ", limit=2)
            if (title.length > 7 && words.size == 2) {
                text(words[0],56f,1030f,125f,true,930f)
                text(words[1],56f,1140f,125f,true,930f)
            } else text(title,56f,1110f,125f,true,930f)
        }
        if (routeFocus) {
            var route=r.route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            if (o.optBoolean("trimEnds",true) && route.size>=12) { val cut=max(1,(route.size*.1).roundToInt()); route=route.drop(cut).dropLast(cut) }
            val box=RectF(150f,535f,930f,850f)
            if(route.size<2) {
                if (o.optBoolean("showRoute", true))
                    text("경로 정보 없음",box.centerX(),box.centerY(),28f,false,box.width(),Paint.Align.CENTER)
            } else {
                val k=cos(Math.toRadians(route.map { it.latitude }.average()))
                val xs=route.map { it.longitude*k }; val ys=route.map { -it.latitude }
                val minX=xs.min(); val minY=ys.min(); val dx=max(1e-7,xs.max()-minX); val dy=max(1e-7,ys.max()-minY)
                val scale=min(box.width()/dx,box.height()/dy)
                val ox=box.left+(box.width()-dx*scale)/2; val oy=box.top+(box.height()-dy*scale)/2
                val pixels=route.indices.map { index ->
                    PointF((ox+(xs[index]-minX)*scale).toFloat(),(oy+(ys[index]-minY)*scale).toFloat())
                }
                if (o.optBoolean("showRoute", true) && !o.optBoolean("previewHideRouteStroke", false)) {
                    p.color=ink; p.strokeWidth=13f; p.strokeCap=Paint.Cap.ROUND; p.style=Paint.Style.STROKE
                    for(i in 1 until route.size) {
                        val gap=route[i].epochMillis-route[i-1].epochMillis
                        if(gap<0) continue
                        val previous=pixels[(i-2).coerceAtLeast(0)]
                        val start=pixels[i-1]
                        val end=pixels[i]
                        val next=pixels[(i+1).coerceAtMost(pixels.lastIndex)]
                        val path=Path().apply {
                            moveTo(start.x,start.y)
                            cubicTo(start.x+(end.x-previous.x)/6f,start.y+(end.y-previous.y)/6f,
                                end.x-(next.x-start.x)/6f,end.y-(next.y-start.y)/6f,end.x,end.y)
                        }
                        c.drawPath(path,p)
                    }
                    p.pathEffect=null; p.style=Paint.Style.FILL
                    c.drawCircle(pixels.first().x,pixels.first().y,17f,p)
                    p.style=Paint.Style.STROKE; p.strokeWidth=7f
                    c.drawCircle(pixels.last().x,pixels.last().y,21f,p); p.style=Paint.Style.FILL
                }
                if (o.optBoolean("showRoute", true)) {
                    val start=pixels.first(); val end=pixels.last()
                    text("출발", start.x.coerceIn(86f, 820f), (start.y+65f).coerceAtMost(960f), 42f, false, 190f)
                    if (o.optBoolean("showPlace", true))
                        text(title, end.x.coerceIn(86f, 760f), (end.y+65f).coerceAtMost(960f), 42f, false, 260f)
                }
            }
        }
        val showDistance=o.optBoolean("showDistance",true)
        val showTime=o.optBoolean("showTime",true)
        fun clock(x: Float, y: Float, scale: Float = 1f) {
            c.save(); c.scale(scale,scale,x,y)
            p.color=ink; p.style=Paint.Style.STROKE; p.strokeWidth=5f
            c.drawCircle(x,y,24f,p); c.drawLine(x,y,x,y-13f,p); c.drawLine(x,y,x+11f,y+6f,p)
            p.style=Paint.Style.FILL; c.restore()
        }
        fun car(x: Float, y: Float, scale: Float = 1f) {
            c.save(); c.scale(scale,scale,x,y)
            p.color=ink; p.style=Paint.Style.STROKE; p.strokeWidth=5f
            c.drawRoundRect(x-25f,y-14f,x+25f,y+9f,5f,5f,p)
            c.drawCircle(x-14f,y+14f,5f,p); c.drawCircle(x+14f,y+14f,5f,p)
            p.style=Paint.Style.FILL; c.restore()
        }
        when {
            stat -> {
                if (!hidePopup && showDistance && popupElement in listOf(-1, 0))
                    displayMetric(78f, 435f, 300f, 72f)
            }
            routeFocus -> {
                if (showDistance) text(distance, 86f, 1330f, 98f, true, 900f)
            }
            journal -> {
                if (!hidePopup && popupElement in listOf(-1, 3)) {
                    val summary=listOfNotNull(if(o.optBoolean("showDate",true)) date else null,
                        if(showDistance) distance else null,if(showTime) duration else null).joinToString(" · ")
                    text(summary,56f,1210f,39f,false,960f)
                }
            }
            split -> {
                val summary=listOfNotNull(if(showDistance) distance else null,if(showTime) duration else null).joinToString(" · ")
                text(summary,56f,1235f,41f,false,960f)
            }
            else -> {
                if (showDistance) { car(82f,1240f,1.25f); text(distance,137f,1263f,52f,true,385f) }
                if (showTime) { clock(530f,1240f,1.25f); text(duration,585f,1263f,52f,true,430f) }
            }
        }
        val note=listOfNotNull(if(r.status=="PARTIAL") "불완전한 기록" else null,if(r.simulated) "시뮬레이션 데이터" else null).joinToString(" · ")
        if (!popupOnly) {
            text(note,50f,1380f,22f)
            if (mapBackground != null) text("OpenFreeMap · © OpenMapTiles · Data from OpenStreetMap", 50f, 1410f, 16f)
        }
        return result
    }
}
