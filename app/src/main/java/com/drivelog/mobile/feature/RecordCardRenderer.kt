package com.drivelog.mobile.feature

import android.graphics.*
import com.drivelog.mobile.data.RecordDto
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/** Native port of the source project's server/static/m/card.js record templates. */
object RecordCardRenderer {
    fun render(r: RecordDto, photo: File, title: String, o: JSONObject): Bitmap {
        val result = Bitmap.createBitmap(1080, 1350, Bitmap.Config.ARGB_8888)
        val c = Canvas(result)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val journal = o.optString("template", "PHOTO") == "JOURNAL"
        val dark = journal && o.optString("tone") == "mono-dark"
        val ink = if (dark || !journal) Color.WHITE else Color.rgb(14,14,15)
        fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, width: Float = 968f) {
            p.shader = null; p.colorFilter = null; p.color = ink; p.textSize = size
            p.typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            var display = value
            if (p.measureText(display) > width) {
                while (display.isNotEmpty() && p.measureText("$display…") > width) display = display.dropLast(1)
                display += "…"
            }
            c.drawText(display, x, y, p)
        }
        c.drawColor(if (dark) Color.BLACK else Color.WHITE)
        val rect = if (journal) RectF(40f,232f,1040f,922f) else RectF(0f,0f,1080f,1350f)
        val bitmap = BitmapFactory.decodeFile(photo.path) ?: error("사진을 다시 선택해주세요")
        try {
            val crop = o.optJSONObject("crop") ?: JSONObject()
            val zoom = crop.optDouble("zoom",1.0).takeIf { it.isFinite() }?.coerceIn(1.0,4.0) ?: 1.0
            val scale = max(rect.width()/bitmap.width,rect.height()/bitmap.height) * zoom.toFloat()
            val w = bitmap.width*scale; val h=bitmap.height*scale
            fun pan(key: String) = crop.optDouble(key,0.0).takeIf { it.isFinite() }?.coerceIn(-1.0,1.0)?.toFloat() ?: 0f
            val x=rect.left+(rect.width()-w)/2+(w-rect.width())/2*pan("x")
            val y=rect.top+(rect.height()-h)/2+(h-rect.height())/2*pan("y")
            if (o.optString("tone","color") != "color") p.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            c.save(); c.clipRect(rect); c.drawBitmap(bitmap,null,RectF(x,y,x+w,y+h),p); c.restore(); p.colorFilter=null
        } finally { bitmap.recycle() }
        if (!journal) {
            p.shader=LinearGradient(0f,0f,0f,420f,0x73000000,Color.TRANSPARENT,Shader.TileMode.CLAMP)
            c.drawRect(0f,0f,1080f,420f,p)
            p.shader=LinearGradient(0f,930f,0f,1350f,Color.TRANSPARENT,0xB8000000.toInt(),Shader.TileMode.CLAMP)
            c.drawRect(0f,930f,1080f,1350f,p); p.shader=null
        }
        val distance=r.distanceKm?.let { "%.1f km".format(Locale.KOREA,it) } ?: "거리 정보 없음"
        val label=when(r.distanceSource) { "VEHICLE" -> "차량 주행거리"; "GPS_ESTIMATED" -> "추정 거리"; else -> "거리" }
        val duration="${r.elapsedSeconds/60}분 ${r.elapsedSeconds%60}초"
        val date=SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(Date(r.startedAtMillis))
        if (journal) {
            text("A DAY AWAY",56f,84f,24f)
            text(title,52f,186f,76f,true)
            if (o.optBoolean("showPlace",true)) text(r.placeName,50f,1288f,38f,true,570f)
            if (o.optBoolean("showDistance",true)) { text(label,676f,990f,26f); text(distance,672f,1110f,64f,true,360f) }
            if (o.optBoolean("showTime",true)) text(duration,676f,1190f,34f,true,360f)
            if (o.optBoolean("showDate",true)) text(date,676f,1240f,28f,false,360f)
        } else {
            if (o.optBoolean("showDistance",true)) { text(label,60f,92f,30f); text(distance,56f,262f,140f,true) }
            text(title,56f,1200f,76f,true)
            val meta=listOfNotNull(if(o.optBoolean("showPlace",true)) r.placeName else null,if(o.optBoolean("showTime",true)) duration else null,if(o.optBoolean("showDate",true)) date else null)
            text(meta.joinToString(" | "),60f,1274f,30f,false,850f)
        }
        if (o.optBoolean("showRoute",true)) {
            var route=r.route.filter { it.latitude.isFinite() && it.longitude.isFinite() && it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
            if (o.optBoolean("trimEnds",true) && route.size>=12) { val cut=max(1,(route.size*.1).roundToInt()); route=route.drop(cut).dropLast(cut) }
            val box=if(journal) RectF(50f,960f,600f,1210f) else RectF(70f,340f,1010f,960f)
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
        val note=listOfNotNull(if(r.status=="PARTIAL") "불완전한 기록" else null,if(r.simulated) "시뮬레이션 데이터" else null).joinToString(" · ")
        text(note,50f,1326f,22f)
        return result
    }
}
