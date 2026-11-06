package com.drivelog.mobile.feature

import android.graphics.*
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
    fun render(r: RecordDto, photo: File, title: String, o: JSONObject): Bitmap {
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
        val ink = Color.WHITE
        fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, width: Float = 968f,
                 align: Paint.Align = Paint.Align.LEFT) {
            p.shader = null; p.colorFilter = null; p.color = ink; p.textSize = size
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
        val bitmap = BitmapFactory.decodeFile(photo.path) ?: error("사진을 다시 선택해주세요")
        try {
            val crop = o.optJSONObject("crop") ?: JSONObject()
            val zoom = crop.optDouble("zoom",1.0).takeIf { it.isFinite() }?.coerceIn(1.0,3.0) ?: 1.0
            fun pan(key: String) = crop.optDouble(key,0.0).takeIf { it.isFinite() }?.coerceIn(-1.0,1.0)?.toFloat() ?: 0f
            val placement = CropMath.place(bitmap.width.toFloat(), bitmap.height.toFloat(),
                rect.width(), rect.height(), zoom.toFloat(), pan("x"), pan("y"))
            val x = rect.left + placement.left
            val y = rect.top + placement.top
            val tone = o.optString("tone", "color")
            val matrix = when (tone) {
                "mono", "mono-dark" -> ColorMatrix().apply { setSaturation(0f) }
                "soft" -> ColorMatrix().apply { setSaturation(.72f) }
                "film" -> ColorMatrix().apply { setSaturation(.62f) }
                "warm" -> ColorMatrix(floatArrayOf(1.06f,0f,0f,0f,0f, 0f,1f,0f,0f,0f, 0f,0f,.88f,0f,0f, 0f,0f,0f,1f,0f))
                else -> null
            }
            if (matrix != null) p.colorFilter = ColorMatrixColorFilter(matrix)
            c.save(); c.clipRect(rect); c.drawBitmap(bitmap,null,RectF(x,y,x+placement.width,y+placement.height),p); c.restore(); p.colorFilter=null
        } finally { bitmap.recycle() }
        val distance=r.distanceKm?.let { "%.1f km".format(Locale.KOREA,it) } ?: "거리 정보 없음"
        val label=when(r.distanceSource) { "VEHICLE" -> "차량 주행거리"; "GPS_ESTIMATED" -> "추정 거리"; else -> "거리" }
        val duration="${r.elapsedSeconds/60}분 ${r.elapsedSeconds%60}초"
        val date=SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(r.startedAtMillis))
        val titleY = when { journal -> 180f; split -> 760f; stat -> 210f; routeFocus -> 190f; else -> 1130f }
        if (o.optBoolean("showPlace",true)) text(title,56f,titleY,if(stat) 94f else 76f,true)
        val metricSize = 46f
        p.textSize = metricSize
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val halfTime = p.measureText(r.displayDuration()) / 2f
        val leftDivider = 540f - halfTime - 24f
        val rightDivider = 540f + halfTime + 24f
        if (o.optBoolean("showTime",true)) text(r.displayDuration(),540f,1300f,metricSize,true,340f,Paint.Align.CENTER)
        if (o.optBoolean("showDistance",true)) text(distance,leftDivider-24f,1300f,metricSize,true,leftDivider-80f,Paint.Align.RIGHT)
        if (o.optBoolean("showDate",true)) text(date,rightDivider+24f,1300f,metricSize,true,1000f-rightDivider)
        if (o.optBoolean("showDistance",true) && o.optBoolean("showTime",true)) text("|",leftDivider,1300f,38f,false,30f,Paint.Align.CENTER)
        if (o.optBoolean("showTime",true) && o.optBoolean("showDate",true)) text("|",rightDivider,1300f,38f,false,30f,Paint.Align.CENTER)
        if (o.optBoolean("showRoute",true)) {
            var route=r.route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            if (o.optBoolean("trimEnds",true) && route.size>=12) { val cut=max(1,(route.size*.1).roundToInt()); route=route.drop(cut).dropLast(cut) }
            val box=when {
                journal -> RectF(70f,350f,1010f,950f)
                split -> RectF(70f,840f,1010f,1150f)
                stat -> RectF(70f,300f,1010f,770f)
                routeFocus -> RectF(70f,240f,1010f,1010f)
                else -> RectF(70f,340f,1010f,960f)
            }
            if(route.size<2) text("경로 정보 부족",box.left,box.centerY(),26f,false,box.width()) else {
                val k=cos(Math.toRadians(route.map { it.latitude }.average()))
                val xs=route.map { it.longitude*k }; val ys=route.map { -it.latitude }
                val minX=xs.min(); val minY=ys.min(); val dx=max(1e-7,xs.max()-minX); val dy=max(1e-7,ys.max()-minY)
                val scale=min(box.width()/dx,box.height()/dy)
                val ox=box.left+(box.width()-dx*scale)/2; val oy=box.top+(box.height()-dy*scale)/2
                p.color=ink; p.strokeWidth=if(journal) 6f else 8f; p.strokeCap=Paint.Cap.ROUND
                for(i in 1 until route.size) {
                    val gap=route[i].epochMillis-route[i-1].epochMillis
                    if(gap<0) continue
                    p.pathEffect=if(gap>60000) DashPathEffect(floatArrayOf(10f,20f),0f) else null
                    c.drawLine((ox+(xs[i-1]-minX)*scale).toFloat(),(oy+(ys[i-1]-minY)*scale).toFloat(),(ox+(xs[i]-minX)*scale).toFloat(),(oy+(ys[i]-minY)*scale).toFloat(),p)
                }
                p.pathEffect=null
            }
        }
        if (o.optBoolean("showEfficiency", false)) text("연비/전비 정보 없음",56f,1300f,23f)
        val note=listOfNotNull(if(r.status=="PARTIAL") "불완전한 기록" else null,if(r.simulated) "시뮬레이션 데이터" else null).joinToString(" · ")
        text(note,50f,1380f,22f)
        return result
    }
}
