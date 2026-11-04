package com.drivelog.mobile.data

import android.app.*
import android.content.*
import android.net.Uri
import com.drivelog.mobile.MainActivity
import java.security.MessageDigest

object RecordArrivals {
    fun account(token: String) = MessageDigest.getInstance("SHA-256").digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
    @Synchronized fun accept(context: Context, account: String, records: List<RecordDto>): List<RecordDto> {
        val prefs = context.getSharedPreferences("arrivals-$account", Context.MODE_PRIVATE)
        val seen = prefs.getStringSet("seen", null)
        val ids = ArrivalPolicy.unseen(seen, records.map { it.id to it.hasPhoto })
        check(prefs.edit().putStringSet("seen", (seen.orEmpty() + records.map { it.id }).toSet()).commit())
        return records.filter { it.id in ids }
    }
    fun notify(context: Context, account: String, record: RecordDto) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("photos", "사진 추가 안내", NotificationManager.IMPORTANCE_DEFAULT))
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("drivelog://record/$account/${record.id}"))
            .putExtra("recordId", record.id).putExtra("account", account)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            manager.notify(record.id, 1, Notification.Builder(context, "photos").setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("주행 기록에 사진을 추가해보세요").setContentText(record.placeName)
                .setContentIntent(pending).setAutoCancel(true).build())
        } catch (_: SecurityException) { /* In-app records remain available when permission is denied. */ }
    }
}
