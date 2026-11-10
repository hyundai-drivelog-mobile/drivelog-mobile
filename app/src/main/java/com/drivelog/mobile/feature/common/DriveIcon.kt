package com.drivelog.mobile.feature.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.R

enum class DriveIconType { CAR, BELL, HOME, LIST, PERSON, ROUTE, AWARD, LOCK, CLOCK, FUEL, CHEVRON }

@Composable fun DriveIcon(type: DriveIconType, color: Color, modifier: Modifier = Modifier) {
    val image = when (type) {
        DriveIconType.CAR -> R.drawable.icon_vehicle_connection
        DriveIconType.ROUTE -> R.drawable.icon_recommended_course
        DriveIconType.AWARD -> R.drawable.icon_achievement
        DriveIconType.BELL -> R.drawable.icon_notification
        DriveIconType.HOME -> R.drawable.icon_tab_home
        DriveIconType.LIST -> R.drawable.icon_tab_records
        DriveIconType.PERSON -> R.drawable.icon_tab_profile
        else -> null
    }
    if (image != null) {
        Image(painterResource(image), null, modifier.size(24.dp), colorFilter = ColorFilter.tint(color))
        return
    }
    Canvas(modifier.size(24.dp)) {
        val sx = size.width / 24f; val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color, p(x1,y1), p(x2,y2), 1.8.dp.toPx(), cap = StrokeCap.Round)
        when (type) {
            DriveIconType.CAR -> {
                val body = Path().apply { moveTo(3*sx,15*sy); lineTo(5*sx,9*sy); lineTo(7*sx,7*sy); lineTo(17*sx,7*sy); lineTo(19*sx,9*sy); lineTo(21*sx,15*sy); close() }
                drawPath(body, color, style = Stroke(1.8.dp.toPx()))
                line(4f,15f,4f,18f); line(20f,15f,20f,18f); line(4f,18f,20f,18f)
                line(7f,15f,8f,15f); line(16f,15f,17f,15f)
            }
            DriveIconType.BELL -> {
                val bell = Path().apply { moveTo(6*sx,17*sy); lineTo(8*sx,14*sy); lineTo(8*sx,10*sy); quadraticTo(8*sx,5*sy,12*sx,5*sy); quadraticTo(16*sx,5*sy,16*sx,10*sy); lineTo(16*sx,14*sy); lineTo(18*sx,17*sy); close() }
                drawPath(bell,color,style=Stroke(1.8.dp.toPx())); line(10f,20f,14f,20f)
            }
            DriveIconType.HOME -> {
                line(3f,11f,12f,4f); line(12f,4f,21f,11f); line(6f,10f,6f,20f); line(18f,10f,18f,20f); line(6f,20f,18f,20f)
            }
            DriveIconType.LIST -> {
                listOf(6f,12f,18f).forEach { y -> line(8f,y,21f,y); drawCircle(color,1.5.dp.toPx(),p(4f,y)) }
            }
            DriveIconType.PERSON -> {
                drawCircle(color,3.6.dp.toPx(),p(12f,8f),style=Stroke(1.8.dp.toPx()))
                val shoulders = Path().apply { moveTo(4*sx,20*sy); quadraticTo(4*sx,14*sy,12*sx,14*sy); quadraticTo(20*sx,14*sy,20*sx,20*sy) }
                drawPath(shoulders,color,style=Stroke(1.8.dp.toPx()))
            }
            DriveIconType.ROUTE -> {
                drawCircle(color,2.dp.toPx(),p(5f,18f),style=Stroke(1.6.dp.toPx()))
                drawCircle(color,2.dp.toPx(),p(19f,6f),style=Stroke(1.6.dp.toPx()))
                line(7f,18f,12f,18f); line(12f,18f,12f,7f); line(12f,7f,17f,7f)
            }
            DriveIconType.AWARD -> {
                drawCircle(color,5.dp.toPx(),p(12f,9f),style=Stroke(1.8.dp.toPx()))
                line(9f,14f,8f,21f); line(8f,21f,12f,18f); line(12f,18f,16f,21f); line(16f,21f,15f,14f)
            }
            DriveIconType.LOCK -> {
                drawRect(color, p(5f,10f), androidx.compose.ui.geometry.Size(14*sx,11*sy))
                drawArc(color,180f,180f,false,p(8f,3f),androidx.compose.ui.geometry.Size(8*sx,12*sy),style=Stroke(1.8.dp.toPx()))
            }
            DriveIconType.CLOCK -> {
                drawCircle(color,8*sx,p(12f,13f),style=Stroke(1.8.dp.toPx()))
                line(10f,2f,14f,2f); line(12f,13f,12f,8f); line(12f,13f,16f,13f)
            }
            DriveIconType.FUEL -> {
                drawRect(color,p(4f,4f),androidx.compose.ui.geometry.Size(10*sx,16*sy),style=Stroke(1.8.dp.toPx()))
                line(5f,10f,13f,10f); line(14f,12f,18f,12f); line(18f,12f,18f,19f); line(18f,19f,21f,19f); line(21f,19f,21f,7f); line(21f,7f,18f,4f)
            }
            DriveIconType.CHEVRON -> { line(9f,5f,16f,12f); line(16f,12f,9f,19f) }
        }
    }
}
