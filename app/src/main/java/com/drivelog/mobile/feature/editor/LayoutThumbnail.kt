package com.drivelog.mobile.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.model.CardLayout
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.DrivePhoto
import java.io.File

@Composable fun LayoutThumbnail(layout: CardLayout, record: RecordDto, photo: File?, photoVersion: Int,
    modifier: Modifier = Modifier, blackTemplate: Boolean = false) {
    Box(modifier.aspectRatio(3f/4f).clip(RoundedCornerShape(9.dp))) {
        DrivePhoto(photo,photoVersion,Modifier.fillMaxSize())
        val ink = if (blackTemplate) Color.Black else Color.White
        Canvas(Modifier.fillMaxSize()) {
            fun bar(x: Float, y: Float, width: Float, thickness: Float = 3f) = drawLine(ink,
                Offset(size.width*x,size.height*y),Offset(size.width*(x+width),size.height*y),thickness.dp.toPx())
            when (layout) {
                CardLayout.PHOTO -> { bar(.10f,.74f,.62f,5f); bar(.10f,.82f,.42f,5f); bar(.10f,.93f,.75f) }
                CardLayout.STAT -> {
                    bar(.10f,.12f,.54f)
                    drawLine(ink,Offset(size.width*.13f,size.height*.60f),Offset(size.width*.13f,size.height*.94f),2.dp.toPx())
                    bar(.22f,.73f,.64f,7f); bar(.22f,.88f,.44f,4f)
                }
                CardLayout.ROUTE -> {
                    val route=Path().apply { moveTo(size.width*.14f,size.height*.70f)
                        cubicTo(size.width*.31f,size.height*.63f,size.width*.33f,size.height*.45f,
                            size.width*.50f,size.height*.52f)
                        cubicTo(size.width*.70f,size.height*.58f,size.width*.72f,size.height*.31f,
                            size.width*.88f,size.height*.30f) }
                    drawPath(route,ink,style=Stroke(3.dp.toPx()))
                    bar(.12f,.90f,.76f)
                }
                CardLayout.JOURNAL -> {
                    bar(.10f,.66f,.55f,7f); bar(.10f,.75f,.70f,7f)
                    bar(.10f,.82f,.33f); bar(.10f,.89f,.58f); bar(.10f,.95f,.75f)
                }
                CardLayout.SPLIT -> {
                    bar(.10f,.77f,.45f); bar(.10f,.85f,.62f,6f); bar(.10f,.94f,.70f)
                    drawLine(ink,Offset(size.width*.10f,size.height*.10f),Offset(size.width*.25f,size.height*.10f),2.dp.toPx())
                    drawLine(ink,Offset(size.width*.10f,size.height*.10f),Offset(size.width*.10f,size.height*.23f),2.dp.toPx())
                    drawLine(ink,Offset(size.width*.75f,size.height*.90f),Offset(size.width*.90f,size.height*.90f),2.dp.toPx())
                }
            }
        }
    }
}
