package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "phishing_alerts"

object PhishingNotifier {
    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "피싱 문자 경고",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "TextShield가 피싱으로 판단한 문자를 알려드려요" }
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyPhishing(context: Context, sender: String, snippet: String, reason: String) {
        ensureChannel(context)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("⚠️ 피싱 의심 문자 발견")
                .setContentText("$sender: $snippet")
                .setStyle(NotificationCompat.BigTextStyle().bigText("$sender\n$snippet\n\n근거: $reason"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
