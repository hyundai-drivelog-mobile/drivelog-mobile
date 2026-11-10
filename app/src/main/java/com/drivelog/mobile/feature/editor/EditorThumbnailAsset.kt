package com.drivelog.mobile.feature.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.drivelog.mobile.R
import java.io.File

/** Bundled photo used by editor previews and cards without a selected photo. */
internal object EditorThumbnailAsset {
    @Volatile private var copiedFile: File? = null

    fun bitmap(context: Context): Bitmap = BitmapFactory.decodeResource(context.resources,
        R.drawable.edit_default, BitmapFactory.Options().apply { inSampleSize = 4 })
        ?: error("Could not decode editor thumbnail photo")

    fun file(context: Context): File = synchronized(this) {
        copiedFile?.takeIf { it.exists() } ?: File(context.cacheDir, "edit-default-thumbnail.png").also { file ->
            context.resources.openRawResource(R.drawable.edit_default).use { source ->
                file.outputStream().use { target -> source.copyTo(target) }
            }
            copiedFile = file
        }
    }
}
