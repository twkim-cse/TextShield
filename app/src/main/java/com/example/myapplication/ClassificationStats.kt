package com.example.myapplication

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "classification_stats"

object ClassificationStats {
    private fun currentMonthKey(): String =
        SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun record(context: Context, isPhishing: Boolean) {
        val month = currentMonthKey()
        val key = if (isPhishing) "${month}_phishing" else "${month}_safe"
        val prefs = prefs(context)
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    /** Returns (phishingCount, safeCount) for the current month. */
    fun getMonthlyStats(context: Context): Pair<Int, Int> {
        val month = currentMonthKey()
        val prefs = prefs(context)
        val phishing = prefs.getInt("${month}_phishing", 0)
        val safe = prefs.getInt("${month}_safe", 0)
        return phishing to safe
    }
}
