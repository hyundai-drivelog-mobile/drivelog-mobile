package com.drivelog.mobile.feature

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.drivelog.mobile.data.RecordDto
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

object Cards {
    fun settings(raw: String?): JSONObject = try { JSONObject(raw ?: "{}") } catch (_: Exception) { JSONObject() }
    fun importPhoto(context: Context, uri: Uri, file: File) {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1.0, 2048.0 / max(info.size.width, info.size.height))
            decoder.setTargetSize(max(1, (info.size.width * scale).toInt()), max(1, (info.size.height * scale).toInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        bitmap.recycle()
        check(temp.length() <= 15 * 1024 * 1024)
        check(temp.renameTo(file))
    }
    fun render(context: Context, record: RecordDto, photo: File, title: String, settings: JSONObject,
        mapBackground: Bitmap? = null, blackTemplate: Boolean = false): Bitmap =
        RecordCardRenderer.render(context, record, photo, title, settings, mapBackground, blackTemplate)
}
