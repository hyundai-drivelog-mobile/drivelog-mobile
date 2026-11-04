package com.drivelog.mobile

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.lifecycle.ViewModelProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.drivelog.mobile.feature.*
import com.drivelog.mobile.ui.theme.DRIVELOGMobileTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    private lateinit var model: MobileModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[MobileModel::class.java]
        model.openIntent(intent)
        setContent { DRIVELOGMobileTheme { MobileScreen(model) } }
    }
    override fun onResume() { super.onResume(); if (::model.isInitialized) model.sync() }
}

@Composable
fun MobileScreen(model: MobileModel) {
    var code by remember { mutableStateOf("") }
    var addVehicle by remember { mutableStateOf(false) }
    var notificationAllowed by remember { mutableStateOf(true) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationAllowed = it }
    Scaffold { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DRIVELOG", style = MaterialTheme.typography.headlineMedium)
            if (model.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (model.message.isNotBlank()) Text(model.message)
            if (!model.connected || addVehicle) {
                Text("차량 연결", style = MaterialTheme.typography.titleLarge)
                Text("차량에 표시된 6자리 연결 코드를 입력해주세요.")
                OutlinedTextField(code, { code = it.filter { c -> c in '0'..'9' }.take(6) }, label = { Text("연결 코드") }, singleLine = true)
                Button(enabled = code.length == 6 && !model.busy, onClick = { model.claim(code); addVehicle = false }) { Text("연결") }
            } else {
                Row { TextButton(onClick = { model.sync() }, enabled = !model.busy) { Text("새로고침") }; TextButton(onClick = { addVehicle = true }) { Text("차량 추가 연결") } }
                TextButton(onClick = { if (Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }) { Text("사진 추가 알림 허용") }
                if (!notificationAllowed) Text("알림이 꺼져 있습니다. 앱에서 새 기록을 확인할 수 있습니다.")
                val record = model.records.find { it.id == model.selected }
                if (record != null) {
                    TextButton(onClick = { model.selected = null }) { Text("내 기록으로") }
                    key(record.id) { Editor(model, record) }
                } else {
                    if (model.records.isEmpty() && !model.busy) Text("아직 주행 기록이 없습니다.")
                    model.records.forEach { item ->
                        OutlinedCard(onClick = { model.selected = item.id; model.loadPhoto(item) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(item.placeName, style = MaterialTheme.typography.titleMedium)
                                Text(item.distanceKm?.let { "%.1f km".format(it) } ?: "거리 정보 없음")
                                Text("${item.elapsedSeconds / 60}분 · ${if (item.simulated) "시뮬레이션" else "실제 주행"}")
                                if (item.status == "PARTIAL") Text("불완전한 기록")
                                Text(if (item.hasPhoto) "사진 있음" else "사진 추가하기")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Editor(model: MobileModel, record: com.drivelog.mobile.data.RecordDto) {
    val draft = remember { model.draft(record.id) }
    var title by remember { mutableStateOf(draft.optString("title", record.title ?: record.placeName).take(40)) }
    var raw by remember { mutableStateOf((draft.optJSONObject("settings") ?: Cards.settings(record.cardJson)).toString()) }
    val settings = remember(raw) { Cards.settings(raw) }
    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) model.importPhoto(record.id, uri) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri -> if (uri != null) model.export(uri, record, title, settings) }
    LaunchedEffect(Unit) { model.loadPhoto(record) }
    LaunchedEffect(raw, title) { withContext(Dispatchers.IO) { model.saveDraft(record.id, title, settings) } }
    LaunchedEffect(raw, title, model.photoVersion) {
        if (model.photo(record.id).exists()) preview = withContext(Dispatchers.IO) { runCatching { Cards.render(record, model.photo(record.id), title, settings) }.getOrNull() }
    }
    Text(record.placeName, style = MaterialTheme.typography.titleLarge)
    Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(record.startedAtMillis)))
    Text("${record.elapsedSeconds / 60}분 · ${record.distanceKm?.let { "$it km" } ?: "거리 정보 없음"}")
    if (record.distanceSource == "GPS_ESTIMATED") Text("GPS 추정 거리")
    Text(if (record.route.size < 2) "경로 정보 부족" else "관측 경로 ${record.route.size}개 지점")
    Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !model.busy) { Text("대표 사진 선택·교체") }
    preview?.let { Image(it.asImageBitmap(), "저장할 카드 미리보기", Modifier.fillMaxWidth().height(400.dp)) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("PHOTO" to "사진형", "JOURNAL" to "저널형").forEach { (value, label) ->
            FilterChip(selected = settings.optString("template", "PHOTO") == value, onClick = { raw = JSONObject(raw).put("template", value).toString() }, label = { Text(label) })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("color" to "컬러", "mono" to "흑백", "mono-dark" to "다크 흑백").forEach { (value, label) ->
            FilterChip(selected = settings.optString("tone", "color") == value, onClick = { raw = JSONObject(raw).put("tone", value).toString() }, label = { Text(label) })
        }
    }
    OutlinedTextField(title, { title = it.take(40) }, label = { Text("제목 (40자)") })
    listOf("showRoute" to "경로", "showDistance" to "거리", "showTime" to "시간", "showDate" to "날짜", "showPlace" to "장소", "trimEnds" to "경로 양 끝 줄이기").forEach { (key, label) ->
        Row { Checkbox(settings.optBoolean(key, true), { raw = JSONObject(raw).put(key, it).toString() }); Text(label) }
    }
    listOf("zoom" to "확대", "x" to "좌우 이동", "y" to "상하 이동").forEach { (key, label) ->
        val crop = settings.optJSONObject("crop") ?: JSONObject()
        Text(label)
        Slider(crop.optDouble(key, if (key == "zoom") 1.0 else 0.0).toFloat().coerceIn(if (key == "zoom") 1f..4f else -1f..1f), {
            raw = JSONObject(raw).put("crop", crop.put(key, it.toDouble())).toString()
        }, valueRange = if (key == "zoom") 1f..4f else -1f..1f)
    }
    Button(onClick = { model.save(record.id, title, settings) }, enabled = preview != null && !model.busy) { Text("사진·편집 서버 저장") }
    Button(onClick = { exporter.launch("DRIVELOG-${record.id}.png") }, enabled = preview != null && !model.busy) { Text("PNG 저장") }
}


