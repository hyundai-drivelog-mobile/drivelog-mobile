package com.drivelog.mobile.core.designsystem.picker

import android.animation.ValueAnimator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.stopScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val SheetBlack = Color(0xFF111111)
private val NeonlessSelected = Color(0xFF222222)
private val NearGray = Color(0xFF999999)
private val FarGray = Color(0xFFC9C9C9)
private val SelectionGray = Color(0xFFF0F0F2)

@Composable
fun YearWheelPickerBottomSheet(
    selectedYear: Int,
    availableYears: List<Int>,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    WheelPickerBottomSheet("연도 선택", selectedYear, availableYears, { "${it}년" }, onConfirm, onDismiss)
}

@Composable
fun MonthWheelPickerBottomSheet(
    selectedMonth: Long,
    availableMonths: List<Long>,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    WheelPickerBottomSheet("월 선택", selectedMonth, availableMonths,
        { SimpleDateFormat("yyyy년 MM월", Locale.KOREA).format(Date(it)) }, onConfirm, onDismiss)
}

@Composable
private fun <T> WheelPickerBottomSheet(
    title: String,
    selectedValue: T,
    availableValues: List<T>,
    label: (T) -> String,
    onConfirm: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val values = remember(availableValues, selectedValue) { (availableValues + selectedValue).distinct() }
    val initialIndex = remember(values, selectedValue) { values.indexOf(selectedValue).coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState, snapPosition = SnapPosition.Center)
    val scope = rememberCoroutineScope()
    val motionEnabled = remember { ValueAnimator.areAnimatorsEnabled() }
    val openMillis = if (motionEnabled) 300 else 0
    val closeMillis = if (motionEnabled) 240 else 0
    var visible by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var confirming by remember { mutableStateOf(false) }
    var resultIndex by remember { mutableStateOf<Int?>(null) }
    val centerIndex by remember(listState, values) {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
                ?.coerceIn(values.indices) ?: initialIndex
        }
    }
    var pendingIndex by remember { mutableIntStateOf(initialIndex) }
    LaunchedEffect(centerIndex) { pendingIndex = centerIndex }
    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(closing) {
        if (closing) {
            delay(closeMillis.toLong())
            val index = resultIndex
            if (index == null) onDismiss() else onConfirm(values[index])
        }
    }
    fun close(index: Int? = null) {
        if (closing) return
        resultIndex = index
        closing = true
        visible = false
    }

    Dialog(onDismissRequest = { close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val view = LocalView.current
        SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible, enter = fadeIn(tween(if (motionEnabled) 180 else 0, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(if (motionEnabled) 180 else 0)), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable { close() })
            }
            AnimatedVisibility(visible,
                enter = slideInVertically(animationSpec = tween(openMillis, easing = FastOutSlowInEasing), initialOffsetY = { it }),
                exit = slideOutVertically(animationSpec = tween(closeMillis, easing = FastOutSlowInEasing), targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)) {
                BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(Color.White).clickable { }) {
                    val wheelHeight = minOf(200.dp, (maxHeight - 180.dp).coerceAtLeast(120.dp))
                    val itemHeight = 40.dp
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 20.dp)) {
                        Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = SheetBlack)
                            TextButton(onClick = { close() }, enabled = !closing) { Text("취소", color = NearGray) }
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(wheelHeight), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxWidth().height(46.dp).background(SelectionGray, RoundedCornerShape(9.dp)))
                            LazyColumn(
                                state = listState,
                                flingBehavior = fling,
                                contentPadding = PaddingValues(vertical = (wheelHeight - itemHeight) / 2),
                                modifier = Modifier.fillMaxSize()
                                    .semantics { contentDescription = "$title, 현재 ${label(values[pendingIndex])}" }
                            ) {
                                itemsIndexed(values) { index, value ->
                                    val distance by remember(index, listState) {
                                        derivedStateOf {
                                            val info = listState.layoutInfo
                                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                                            if (item == null || item.size == 0) 2f else {
                                                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                                abs(item.offset + item.size / 2f - center) / item.size
                                            }
                                        }
                                    }
                                    val proximity = distance.coerceIn(0f, 2f)
                                    val scale = 1f - .07f * proximity
                                    val opacity = 1f - .3f * proximity
                                    Text(label(value), Modifier.fillMaxWidth().height(itemHeight)
                                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = opacity }
                                        .clickable(enabled = !closing) {
                                            scope.launch { listState.animateScrollToItem(index) }
                                        }
                                        .semantics {
                                            contentDescription = label(value)
                                            selected = index == pendingIndex
                                        }
                                        .wrapContentHeight(Alignment.CenterVertically),
                                        textAlign = TextAlign.Center,
                                        fontSize = 21.sp,
                                        fontWeight = if (proximity < .5f) FontWeight.SemiBold else FontWeight.Medium,
                                        color = when {
                                            proximity < .5f -> NeonlessSelected
                                            proximity < 1.5f -> NearGray
                                            else -> FarGray
                                        })
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Button(onClick = {
                            if (!closing && !confirming) {
                                confirming = true
                                scope.launch {
                                    listState.stopScroll()
                                    val finalIndex = centerIndex
                                    listState.animateScrollToItem(finalIndex)
                                    pendingIndex = finalIndex
                                    close(finalIndex)
                                }
                            }
                        }, enabled = !closing && !confirming,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SheetBlack, contentColor = Color.White)) {
                            Text("선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
