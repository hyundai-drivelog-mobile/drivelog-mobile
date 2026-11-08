package com.drivelog.mobile.feature

import android.graphics.*
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.core.model.CardElement
import com.drivelog.mobile.core.util.CropMath
import com.drivelog.mobile.core.util.displayDuration
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/** Native port of the source project's server/static/m/card.js record templates. */
object RecordCardRenderer {
    fun render(r: RecordDto, photo: File, title: String, o: JSONObject, mapBackground: Bitmap? = null,
        blackElements: Set<CardElement> = emptySet()): Bitmap {
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
        var ink = Color.WHITE
        fun elementInk(element: CardElement) = if (template == "PHOTO" && element in blackElements) Color.BLACK else ink
        fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, width: Float = 968f,
                 align: Paint.Align = Paint.Align.LEFT, color: Int = ink) {
            p.shader = null; p.colorFilter = null; p.color = color; p.textSize = size
            p.textAlign = align
            p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            var display = value
            if (p.measureText(display) > width) {
                while (display.isNotEmpty() && p.measureText("$display…") > width) display = display.dropLast(1)
                display += "…"
            }
            c.drawText(display, x, y, p)
        }
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
        fun panel(color: Int, l: Float, t: Float, r: Float, b: Float) {
            p.color = color; p.shader = null; p.colorFilter = null
            c.drawRect(l, t, r, b, p)
        }
        when {
            journal -> { panel(Color.rgb(247,244,236),0f,885f,1080f,1440f); ink=Color.rgb(25,25,25) }
            split -> { panel(Color.rgb(17,17,17),545f,0f,1080f,1440f); panel(Color.WHITE,545f,0f,552f,1440f) }
            stat -> panel(Color.argb(225,15,15,15),36f,695f,1044f,1420f)
            routeFocus -> panel(Color.argb(210,8,13,19),0f,0f,1080f,1440f)
            else -> panel(Color.argb(145,0,0,0),0f,1050f,1080f,1440f)
        }
        p.color = if (journal) Color.WHITE else elementInk(CardElement.BRAND)
        p.textSize = 44f; p.textAlign = Paint.Align.LEFT
        p.typeface = Typeface.create(Typeface.DEFAULT,Typeface.BOLD_ITALIC)
        c.drawText("DRIVELOG",56f,92f,p)
        val distance=r.distanceKm?.let { "%.1f km".format(Locale.KOREA,it) } ?: "정보 없음"
        val label=when(r.distanceSource) { "VEHICLE" -> "차량 주행거리"; "GPS_ESTIMATED" -> "추정 거리"; else -> "거리" }
        val duration="${r.elapsedSeconds/60}분 ${r.elapsedSeconds%60}초"
        val date=SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(r.startedAtMillis))
        if (o.optBoolean("showDate",true)) {
            val saved = ink; if (journal) ink = Color.WHITE
            text(date,1024f,90f,30f,false,380f,Paint.Align.RIGHT, elementInk(CardElement.DATE)); ink = saved
        }
        val titleY = when { journal -> 1025f; split -> 235f; stat -> 840f; routeFocus -> 265f; else -> 1150f }
        val titleX = if (split) 595f else 56f
        if (o.optBoolean("showPlace",true)) text(title,titleX,titleY,if(split) 60f else 76f,true,
            if(split) 430f else 968f, color = elementInk(CardElement.PLACE))
        if (journal) { text("DRIVE JOURNAL",56f,950f,25f,true); panel(ink,56f,1065f,1024f,1068f) }
        if (split) text("THE DRIVE / 01",595f,155f,25f,true)
        if (stat) text("DRIVE DATA",56f,760f,27f,true)
        if (routeFocus) text("THE ROUTE",56f,170f,30f,true)
        if (mapBackground == null && o.optBoolean("showRoute",true)) {
            var route=r.route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            if (o.optBoolean("trimEnds",true) && route.size>=12) { val cut=max(1,(route.size*.1).roundToInt()); route=route.drop(cut).dropLast(cut) }
            val box=when {
                journal -> RectF(650f,1080f,1010f,1190f)
                split -> RectF(595f,360f,1020f,1030f)
                stat -> RectF(650f,910f,1010f,1120f)
                routeFocus -> RectF(100f,350f,980f,1080f)
                else -> RectF(100f,180f,980f,990f)
            }
            if(route.size<2) { if (routeFocus) text("경로 정보 없음",box.centerX(),box.centerY(),28f,false,box.width(),Paint.Align.CENTER) } else {
                val k=cos(Math.toRadians(route.map { it.latitude }.average()))
                val xs=route.map { it.longitude*k }; val ys=route.map { -it.latitude }
                val minX=xs.min(); val minY=ys.min(); val dx=max(1e-7,xs.max()-minX); val dy=max(1e-7,ys.max()-minY)
                val scale=min(box.width()/dx,box.height()/dy)
                val ox=box.left+(box.width()-dx*scale)/2; val oy=box.top+(box.height()-dy*scale)/2
                p.color=ink; p.strokeWidth=if(routeFocus) 14f else if(journal) 5f else 8f; p.strokeCap=Paint.Cap.ROUND
                for(i in 1 until route.size) {
                    val gap=route[i].epochMillis-route[i-1].epochMillis
                    if(gap<0) continue
                    p.pathEffect=if(gap>60000) DashPathEffect(floatArrayOf(10f,20f),0f) else null
                    c.drawLine((ox+(xs[i-1]-minX)*scale).toFloat(),(oy+(ys[i-1]-minY)*scale).toFloat(),(ox+(xs[i]-minX)*scale).toFloat(),(oy+(ys[i]-minY)*scale).toFloat(),p)
                }
                p.pathEffect=null
            }
        }
        val metrics = listOfNotNull(
            if (o.optBoolean("showDistance",true)) Triple("distance","거리",distance) else null,
            if (o.optBoolean("showTime",true)) Triple("time","시간",r.displayDuration()) else null,
            Triple("fuel","연비","정보 없음")
        )
        val left = if (split) 580f else 80f
        val right = 1000f
        val baseline = if (journal) 1280f else 1300f
        metrics.forEachIndexed { index, metric ->
            val center = left+(right-left)*(index+.5f)/metrics.size
            val y = baseline-85f
            val metricInk = elementInk(when (metric.first) {
                "distance" -> CardElement.DISTANCE
                "time" -> CardElement.TIME
                else -> CardElement.EFFICIENCY
            })
            p.color=metricInk; p.style=Paint.Style.STROKE; p.strokeWidth=4f; p.strokeCap=Paint.Cap.ROUND
            when(metric.first) {
                "distance" -> { c.drawRoundRect(center-22f,y-14f,center+22f,y+10f,5f,5f,p); c.drawCircle(center-12f,y+13f,5f,p); c.drawCircle(center+12f,y+13f,5f,p) }
                "time" -> { c.drawCircle(center,y,22f,p); c.drawLine(center,y,center,y-13f,p); c.drawLine(center,y,center+11f,y+6f,p) }
                else -> { c.drawRoundRect(center-16f,y-22f,center+12f,y+22f,4f,4f,p); c.drawLine(center-16f,y-6f,center+12f,y-6f,p) }
            }
            p.style=Paint.Style.FILL
            text(metric.second,center,baseline-24f,25f,false,210f,Paint.Align.CENTER, metricInk)
            text(metric.third,center,baseline+28f,if(split) 33f else 43f,true,(right-left)/metrics.size-25f,Paint.Align.CENTER, metricInk)
        }
        val note=listOfNotNull(if(r.status=="PARTIAL") "불완전한 기록" else null,if(r.simulated) "시뮬레이션 데이터" else null).joinToString(" · ")
        text(note,50f,1380f,22f)
        if (mapBackground != null) text("OpenFreeMap · © OpenMapTiles · Data from OpenStreetMap", 50f, 1410f, 16f)
        return result
    }
}
