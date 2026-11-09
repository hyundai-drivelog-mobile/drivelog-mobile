package com.drivelog.mobile.data

import android.content.Context
import androidx.work.*
import com.drivelog.mobile.BuildConfig
import java.util.concurrent.TimeUnit

class RecordSyncWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val vault = TokenStore(applicationContext)
        val token = try { vault.read() } catch (_: Exception) { return Result.failure() } ?: return Result.success()
        val account = RecordArrivals.account(token)
        val prefs = applicationContext.getSharedPreferences("sync-$account", Context.MODE_PRIVATE)
        if (prefs.getBoolean("unauthorized", false)) return Result.success()
        if (System.currentTimeMillis() < prefs.getLong("retryAt", 0)) return Result.success()
        return try {
            val records = DriveLogApi(BuildConfig.API_URL) { token }.journey()
            if (vault.read() != token) return Result.success()
            RecordArrivals.accept(applicationContext, account, records).forEach {
                if (vault.read() == token) RecordArrivals.notify(applicationContext, account, it)
            }
            val stamps = DriveLogApi(BuildConfig.API_URL) { token }.stamps()
            if (vault.read() != token) return Result.success()
            val directory = java.io.File(applicationContext.noBackupFilesDir, account).apply { mkdirs() }
            StampCache(java.io.File(directory, "stamps.json")).write(stamps)
            Result.success()
        } catch (e: ApiFailure) {
            when (e.status) {
                401 -> { prefs.edit().putBoolean("unauthorized", true).commit(); Result.success() }
                429 -> { prefs.edit().putLong("retryAt", System.currentTimeMillis() + (e.retryAfterSeconds ?: 60).coerceAtMost(86400) * 1000).commit(); Result.retry() }
                in 500..599 -> Result.retry()
                else -> Result.failure()
            }
        } catch (_: java.io.IOException) { Result.retry() }
        catch (_: Exception) { Result.failure() }
    }
    companion object {
        fun schedule(context: Context) {
            val work = PeriodicWorkRequestBuilder<RecordSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("record-sync", ExistingPeriodicWorkPolicy.KEEP, work)
        }
    }
}
