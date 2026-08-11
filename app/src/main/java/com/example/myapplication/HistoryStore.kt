package com.example.myapplication

import android.content.Context

private const val PREFS_NAME = "classification_history"
private const val KEY_ENTRIES = "entries"
private const val MAX_ENTRIES = 20
private const val ENTRY_SEPARATOR = "@@ENTRY@@"
private const val FIELD_SEPARATOR = "@@FIELD@@"

data class HistoryEntry(
    val timestampMillis: Long,
    val verdict: Verdict,
    val snippet: String,
    val reason: String,
)

object HistoryStore {
    // Guards against two entries getting the exact same millisecond timestamp (e.g. SmsReceiver
    // and MessageNotificationListener both processing the same message at once), which would
    // otherwise produce a duplicate LazyColumn key and crash the History screen.
    private val lastTimestamp = java.util.concurrent.atomic.AtomicLong(0)

    private fun nextUniqueTimestamp(): Long {
        synchronized(lastTimestamp) {
            val now = System.currentTimeMillis()
            val next = if (now > lastTimestamp.get()) now else lastTimestamp.get() + 1
            lastTimestamp.set(next)
            return next
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(context: Context, verdict: Verdict, fullText: String, reason: String) {
        if (verdict == Verdict.UNKNOWN) return
        val snippet =
            fullText.trim().take(30).replace(ENTRY_SEPARATOR, " ").replace(FIELD_SEPARATOR, " ")
        val cleanReason = reason.trim().replace(ENTRY_SEPARATOR, " ").replace(FIELD_SEPARATOR, " ")
        val entry =
            "${nextUniqueTimestamp()}$FIELD_SEPARATOR${verdict.name}$FIELD_SEPARATOR$snippet$FIELD_SEPARATOR$cleanReason"

        val prefs = prefs(context)
        val existing = prefs.getString(KEY_ENTRIES, "") ?: ""
        val entries =
            if (existing.isEmpty()) mutableListOf() else existing.split(ENTRY_SEPARATOR).toMutableList()
        entries.add(0, entry)
        while (entries.size > MAX_ENTRIES) entries.removeAt(entries.size - 1)
        prefs.edit().putString(KEY_ENTRIES, entries.joinToString(ENTRY_SEPARATOR)).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply()
    }

    fun remove(context: Context, timestampMillis: Long) {
        val prefs = prefs(context)
        val existing = prefs.getString(KEY_ENTRIES, "") ?: return
        if (existing.isEmpty()) return
        val entries =
            existing.split(ENTRY_SEPARATOR).filterNot { line ->
                line.split(FIELD_SEPARATOR).firstOrNull()?.toLongOrNull() == timestampMillis
            }
        prefs.edit().putString(KEY_ENTRIES, entries.joinToString(ENTRY_SEPARATOR)).apply()
    }

    fun getAll(context: Context): List<HistoryEntry> {
        val existing = prefs(context).getString(KEY_ENTRIES, "") ?: return emptyList()
        if (existing.isEmpty()) return emptyList()
        val seenTimestamps = mutableSetOf<Long>()
        return existing.split(ENTRY_SEPARATOR).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR)
            if (parts.size < 3) return@mapNotNull null
            var timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
            // Defend against legacy entries saved before timestamps were guaranteed unique.
            while (!seenTimestamps.add(timestamp)) timestamp++
            val verdict = runCatching { Verdict.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val reason = if (parts.size >= 4) parts[3] else ""
            HistoryEntry(timestamp, verdict, parts[2], reason)
        }
    }
}
