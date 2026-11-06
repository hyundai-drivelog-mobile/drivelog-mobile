package com.drivelog.mobile

import com.drivelog.mobile.core.util.CropMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {
    @Test fun cropAlwaysCoversFrameAtPanExtremes() {
        for (zoom in listOf(1f, 1.75f, 3f)) {
            for (panX in listOf(-1f, 0f, 1f)) for (panY in listOf(-1f, 0f, 1f)) {
                val p = CropMath.place(1600f, 900f, 1080f, 1440f, zoom, panX, panY)
                assertTrue(p.left <= 0.001f)
                assertTrue(p.top <= 0.001f)
                assertTrue(p.left + p.width >= 1079.999f)
                assertTrue(p.top + p.height >= 1439.999f)
            }
        }
    }

    @Test fun resetIsCenteredAndZoomIsCapped() {
        val centered = CropMath.place(800f, 1200f, 1080f, 1440f, 1f, 0f, 0f)
        assertEquals((1080f - centered.width) / 2f, centered.left, .001f)
        assertEquals((1440f - centered.height) / 2f, centered.top, .001f)
        val capped = CropMath.place(800f, 1200f, 1080f, 1440f, 4f, 0f, 0f)
        val max = CropMath.place(800f, 1200f, 1080f, 1440f, 3f, 0f, 0f)
        assertEquals(max.width, capped.width, .001f)
    }
}
