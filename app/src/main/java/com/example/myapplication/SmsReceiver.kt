package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!ModelDownloader.isModelDownloaded(context)) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "알 수 없음"
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        if (body.isBlank()) return

        // Hand off to a foreground service: goAsync() only gives us a few seconds, which isn't
        // enough for a cold model load + inference.
        SmsAnalysisService.start(context, sender, body)
    }
}
