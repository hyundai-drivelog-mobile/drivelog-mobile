package com.drivelog.mobile.data

import android.content.Context

/** Consent is scoped to the connection token, so another vehicle connection needs its own choice. */
class ConnectionConsent(context: Context) {
    private val prefs = context.getSharedPreferences("phone-connection-consent", Context.MODE_PRIVATE)

    private fun key(token: String) = "v1-${RecordArrivals.account(token)}"

    fun hasAgreed(token: String): Boolean = prefs.contains(key(token))

    fun agree(token: String) {
        check(prefs.edit().putLong(key(token), System.currentTimeMillis()).commit())
    }

    fun forget(token: String) { check(prefs.edit().remove(key(token)).commit()) }
}
