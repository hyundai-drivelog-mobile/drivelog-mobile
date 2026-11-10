package com.drivelog.mobile.feature.wash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.drivelog.mobile.R
import com.drivelog.mobile.ui.theme.DriveColors

@Composable fun WashSparkleCarIcon(modifier: Modifier = Modifier, color: Color = DriveColors.Black) {
    Image(painterResource(R.drawable.icon_today_wash), null, modifier.size(28.dp),
        colorFilter = ColorFilter.tint(color))
}

@Composable fun WashDropIcon(modifier: Modifier = Modifier, color: Color = DriveColors.Black) {
    Canvas(modifier.size(24.dp)) {
        val unit = size.minDimension / 24f
        val drop = Path().apply {
            moveTo(12f * unit, 2f * unit)
            cubicTo(9f * unit, 6f * unit, 4f * unit, 11f * unit, 4f * unit, 15f * unit)
            cubicTo(4f * unit, 19.5f * unit, 7.6f * unit, 22f * unit, 12f * unit, 22f * unit)
            cubicTo(16.4f * unit, 22f * unit, 20f * unit, 19.5f * unit, 20f * unit, 15f * unit)
            cubicTo(20f * unit, 11f * unit, 15f * unit, 6f * unit, 12f * unit, 2f * unit)
            close()
        }
        drawPath(drop, color, style = Stroke(1.8.dp.toPx()))
    }
}

@Composable fun WashWeatherIcon(type: WashWeatherType, modifier: Modifier = Modifier,
    color: Color = DriveColors.Black) {
    Canvas(modifier.size(24.dp)) {
        val unit = size.minDimension / 24f
        val stroke = 1.8.dp.toPx()
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        when (type) {
            WashWeatherType.UNKNOWN -> drawLine(color, point(8f, 12f), point(16f, 12f),
                stroke, StrokeCap.Round)
            WashWeatherType.SUNNY -> {
                drawCircle(color, 4.2f * unit, point(12f, 12f), style = Stroke(stroke))
                listOf(
                    12f to 1f, 12f to 23f, 1f to 12f, 23f to 12f,
                    4.2f to 4.2f, 19.8f to 4.2f, 4.2f to 19.8f, 19.8f to 19.8f
                ).forEach { (x, y) ->
                    val dx = x - 12f
                    val dy = y - 12f
                    val length = kotlin.math.sqrt(dx * dx + dy * dy)
                    drawLine(color, point(12f + dx * 7f / length, 12f + dy * 7f / length),
                        point(12f + dx * 10f / length, 12f + dy * 10f / length), stroke, StrokeCap.Round)
                }
            }
            WashWeatherType.CLOUDY -> drawCloud(color, unit, stroke)
            WashWeatherType.RAINY -> {
                drawCloud(color, unit, stroke)
                listOf(8f, 12f, 16f).forEach { x ->
                    drawLine(color, point(x, 18f), point(x - 1f, 22f), stroke, StrokeCap.Round)
                }
            }
        }
    }
}

private fun DrawScope.drawCloud(color: Color, unit: Float, stroke: Float) {
    val cloud = Path().apply {
        moveTo(6f * unit, 18f * unit)
        cubicTo(3f * unit, 18f * unit, 2f * unit, 16f * unit, 2f * unit, 13.5f * unit)
        cubicTo(2f * unit, 11f * unit, 4f * unit, 9f * unit, 6.5f * unit, 9f * unit)
        cubicTo(7.5f * unit, 5f * unit, 11f * unit, 4f * unit, 14f * unit, 6f * unit)
        cubicTo(15.2f * unit, 6.8f * unit, 16f * unit, 8f * unit, 16.3f * unit, 9.2f * unit)
        cubicTo(20f * unit, 9f * unit, 22f * unit, 11f * unit, 22f * unit, 13.6f * unit)
        cubicTo(22f * unit, 16.4f * unit, 20f * unit, 18f * unit, 17f * unit, 18f * unit)
        close()
    }
    drawPath(cloud, color, style = Stroke(stroke, cap = StrokeCap.Round))
}
