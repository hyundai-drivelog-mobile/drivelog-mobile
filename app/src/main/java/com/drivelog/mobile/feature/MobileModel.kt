package com.drivelog.mobile.feature

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.drivelog.mobile.BuildConfig
import com.drivelog.mobile.MainActivity
import com.drivelog.mobile.data.*
import com.drivelog.mobile.feature.common.DrivePhotoAsset
import com.drivelog.mobile.feature.journey.RecentDriveMapCache
import com.drivelog.mobile.feature.journey.cardMapPoints
import com.drivelog.mobile.core.model.CardElement
import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject
import org.json.JSONArray

class MobileModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val vault = TokenStore(context)
    private var token = runCatching { vault.read() }.getOrNull()
    private val api = DriveLogApi(BuildConfig.API_URL) { token }
    var connected by mutableStateOf(token != null); private set
    var records by mutableStateOf(emptyList<RecordDto>()); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    var selected by mutableStateOf<String?>(null)
    var photoVersion by mutableIntStateOf(0); private set
    private var retryAt = 0L
    init { if (connected) RecordSyncWorker.schedule(context); viewModelScope.launch { records = withContext(Dispatchers.IO) { RecordCache(File(directory(), "records.json")).read() } } }
    private fun scopeId() = MessageDigest.getInstance("SHA-256").digest((token ?: "unlinked").toByteArray()).joinToString("") { "%02x".format(it) }
    private fun directory() = File(context.noBackupFilesDir, scopeId()).apply { mkdirs() }
    fun photo(id: String) = File(directory(), safeId(id) + ".jpg")
    private fun safeId(id: String) = MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }
    fun draft(id: String): JSONObject = runCatching { JSONObject(File(directory(), safeId(id) + ".json").readText()) }.getOrDefault(JSONObject())
    fun saveDraft(id: String, title: String, settings: JSONObject, blackElements: Set<CardElement> = emptySet()) {
        File(directory(), safeId(id) + ".json").writeText(JSONObject().put("title", title).put("settings", settings)
            .put("blackElements", JSONArray(blackElements.map { it.name })).toString())
    }
    private fun task(block: suspend () -> Unit) {
        if (busy) return
        if (System.currentTimeMillis() < retryAt) { message = "잠시 후 다시 시도해주세요."; return }
        busy = true; message = ""
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                message = when ((e as? ApiFailure)?.status) {
                    401 -> { connected = false; "연결이 만료되었습니다. 다시 연결해주세요." }
                    404 -> "연결 코드가 잘못되었거나 만료되었습니다."
                    409 -> "이미 사용된 연결 코드입니다."
                    413 -> "사진 용량이 너무 큽니다. 다른 사진을 선택해주세요."
                    429 -> { retryAt = System.currentTimeMillis() + ((e as ApiFailure).retryAfterSeconds ?: 60) * 1000; "요청이 많습니다. 잠시 후 다시 시도해주세요." }
                    else -> "작업을 완료하지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
                }
            } finally { busy = false }
        }
    }
    fun claim(code: String) = task {
        val next = withContext(Dispatchers.IO) { api.claim(code).also { vault.write(it) } }
        if (next != token) { records = emptyList(); selected = null; context.getSystemService(NotificationManager::class.java).cancelAll() }
        token = next; connected = true; context.getSharedPreferences("sync-${scopeId()}", Context.MODE_PRIVATE).edit().remove("unauthorized").apply(); RecordSyncWorker.schedule(context); syncNow()
    }
    fun sync() { if (connected) task { syncNow() } }
    private suspend fun syncNow() {
        val fresh = withContext(Dispatchers.IO) { api.journey() }
        val newRecords = withContext(Dispatchers.IO) { RecordArrivals.accept(context, scopeId(), fresh) }
        withContext(Dispatchers.IO) { RecordCache(File(directory(), "records.json")).write(fresh) }
        records = fresh
        if (newRecords.isNotEmpty()) {
            message = "새 기록 ${newRecords.size}건에 사진을 추가해보세요."
            newRecords.forEach { notify(it) }
        }
    }
    private fun notify(record: RecordDto) = RecordArrivals.notify(context, scopeId(), record)
    fun openIntent(intent: Intent) { if (intent.getStringExtra("account") == scopeId()) selected = intent.getStringExtra("recordId") }
    fun loadPhoto(record: RecordDto) { if (record.hasPhoto && !photo(record.id).exists()) task {
        withContext(Dispatchers.IO) { photo(record.id).writeBytes(api.photo(record.id)) }; photoVersion++
    } }
    fun importPhoto(id: String, uri: Uri) = task {
        withContext(Dispatchers.IO) { Cards.importPhoto(context, uri, photo(id)) }; photoVersion++
    }
    fun save(id: String, title: String, settings: JSONObject, blackElements: Set<CardElement> = emptySet()) = task {
        withContext(Dispatchers.IO) {
            saveDraft(id, title, settings, blackElements)
            api.uploadPhoto(id, photo(id).readBytes(), "image/jpeg")
            api.saveCard(id, title, settings.toString())
        }
        syncNow(); message = "사진과 카드 설정을 저장했습니다."
    }
    fun export(uri: Uri, record: RecordDto, title: String, settings: JSONObject,
        blackElements: Set<CardElement> = emptySet()) = task {
        val selectedPhoto = photo(record.id)
        val map = if (!selectedPhoto.exists()) {
            val points = cardMapPoints(record, settings.optBoolean("showRoute", true) && settings.optBoolean("trimEnds", true))
            if (points.size >= 2) RecentDriveMapCache.shared(context).load(record, points, 900, 1200,
                includeRoute = settings.optBoolean("showRoute", true)) else null
        } else null
        withContext(Dispatchers.IO) {
            val source = if (selectedPhoto.exists()) selectedPhoto else DrivePhotoAsset.file(context)
            val bitmap = Cards.render(record, source, title, settings, map, blackElements)
            try { context.contentResolver.openOutputStream(uri)?.use { check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) } ?: error("저장 실패") }
            finally { bitmap.recycle() }
        }
        selected = null; message = "카드를 저장했습니다."
    }
}



