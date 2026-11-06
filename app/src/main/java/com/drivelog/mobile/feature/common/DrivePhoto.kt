package com.drivelog.mobile.feature.common

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

/** Shared branded fallback, also used as the editor and filter source image. */
object DrivePhotoAsset {
    fun bitmap(): Bitmap = Bitmap.createBitmap(600,800,Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(48,48,48))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE; textSize = 54f
            typeface = Typeface.create("sans-serif",Typeface.BOLD); textAlign = Paint.Align.CENTER
        }
    }
    fun filterBitmap(): Bitmap = bitmap().also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(0f,0f,600f,800f,
                intArrayOf(0xff738c9a.toInt(),0xffc1aa87.toInt(),0xff434a47.toInt()),null,android.graphics.Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f,0f,600f,800f,paint)
        paint.shader=null; paint.color=android.graphics.Color.WHITE; paint.textSize=54f
        paint.typeface=Typeface.create("sans-serif",Typeface.BOLD); paint.textAlign=Paint.Align.CENTER
    }
    fun file(context: android.content.Context): File = File(context.cacheDir,"drivelog-default-v2.png").also { file ->
        if (!file.exists()) { val bitmap=bitmap(); file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
    }
}
@Composable fun DrivePhoto(photo: File? = null, version: Int = 0, modifier: Modifier = Modifier) {
    val image = remember(photo?.path,version) {
        (photo?.takeIf { it.exists() }?.let { android.graphics.BitmapFactory.decodeFile(it.path) } ?: DrivePhotoAsset.bitmap()).asImageBitmap()
    }
    Image(image,"DRIVELOG",modifier,contentScale=ContentScale.Crop)
}
