package com.drivelog.mobile.feature.editor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.core.model.CardLayout
import com.drivelog.mobile.data.RecordDto
import com.drivelog.mobile.feature.common.DrivePhoto
import java.io.File

@Composable fun LayoutThumbnail(layout: CardLayout, record: RecordDto, photo: File?, photoVersion: Int, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(3f/4f)) {
        DrivePhoto(photo,photoVersion,Modifier.fillMaxSize())
        val alignment = when(layout) {
            CardLayout.PHOTO -> Alignment.BottomStart
            CardLayout.JOURNAL, CardLayout.STAT, CardLayout.ROUTE -> Alignment.TopStart
            CardLayout.SPLIT -> Alignment.CenterStart
        }
        Column(Modifier.align(alignment).fillMaxWidth().padding(7.dp)) {
            Box(Modifier.fillMaxWidth(if(layout == CardLayout.STAT) .95f else .7f).height(if(layout == CardLayout.STAT) 8.dp else 3.dp).background(Color.White))
            Spacer(Modifier.height(5.dp))
        }
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(7.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            repeat(3) { Box(Modifier.weight(1f).height(2.dp).background(Color.White)) }
        }
    }
}
