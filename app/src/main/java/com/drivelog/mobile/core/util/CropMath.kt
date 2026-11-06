package com.drivelog.mobile.core.util

import kotlin.math.max

data class CropPlacement(val left: Float, val top: Float, val width: Float, val height: Float)

/** Normalized pan stays within image overflow, so the photo always covers its frame. */
object CropMath {
    fun place(imageWidth: Float, imageHeight: Float, frameWidth: Float, frameHeight: Float,
        zoom: Float, panX: Float, panY: Float): CropPlacement {
        val safeZoom = zoom.coerceIn(1f, 3f)
        val scale = max(frameWidth / imageWidth, frameHeight / imageHeight) * safeZoom
        val width = imageWidth * scale
        val height = imageHeight * scale
        return CropPlacement(
            left = (frameWidth - width) / 2f + (width - frameWidth) / 2f * panX.coerceIn(-1f, 1f),
            top = (frameHeight - height) / 2f + (height - frameHeight) / 2f * panY.coerceIn(-1f, 1f),
            width = width,
            height = height
        )
    }
}
