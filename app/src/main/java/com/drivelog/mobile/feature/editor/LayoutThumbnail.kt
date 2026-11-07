package com.drivelog.mobile.feature.editor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.model.CardLayout
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.DrivePhoto
import java.io.File

@Composable fun LayoutThumbnail(layout: CardLayout, record: RecordDto, photo: File?, photoVersion: Int, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(3f/4f).clip(RoundedCornerShape(9.dp))) {
        DrivePhoto(photo,photoVersion,Modifier.fillMaxSize())
        when (layout) {
            CardLayout.JOURNAL -> Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.38f).background(Color(0xfff7f4ec)))
            CardLayout.SPLIT -> Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.5f).background(Color(0xff111111)))
            CardLayout.STAT -> Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth(.92f).fillMaxHeight(.52f).background(Color(0xee111111)))
            CardLayout.ROUTE -> Box(Modifier.fillMaxSize().background(Color(0xcc0a1018)))
            else -> Unit
        }
        val ink = if (layout == CardLayout.JOURNAL) Color.Black else Color.White
        val alignment = when (layout) {
            CardLayout.JOURNAL -> Alignment.BottomStart
            CardLayout.SPLIT -> Alignment.CenterEnd
            CardLayout.STAT -> Alignment.BottomStart
            CardLayout.ROUTE -> Alignment.Center
            else -> Alignment.BottomStart
        }
        Column(Modifier.align(alignment).fillMaxWidth(if (layout == CardLayout.SPLIT) .48f else .9f).padding(7.dp)) {
            Box(Modifier.fillMaxWidth(if (layout == CardLayout.ROUTE) .85f else .72f).height(if (layout == CardLayout.ROUTE) 4.dp else 3.dp).background(ink))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth(.52f).height(2.dp).background(ink.copy(alpha = .75f)))
        }
    }
}
