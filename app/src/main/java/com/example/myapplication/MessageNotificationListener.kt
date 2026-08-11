package com.example.myapplication

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.Collections

/**
 * Catches new-message notifications from ANY app (RCS/채팅+, KakaoTalk, Telegram, etc.), not just
 * plain SMS. Plain SMS is still handled faster/more reliably via [SmsReceiver]; this is the
 * fallback that covers everything SMS_RECEIVED can't see.
 */
class MessageNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return // ignore our own notifications
        val notification = sbn.notification
        if (notification.category != Notification.CATEGORY_MESSAGE) return

        val extras = notification.extras
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val body =
            (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_TEXT))
                ?.toString()
                ?.trim()
        if (sender.isNullOrBlank() || body.isNullOrBlank()) return
        if (!ModelDownloader.isModelDownloaded(applicationContext)) return

        val dedupeKey = "${sbn.packageName}:${sbn.postTime}:${body.hashCode()}"
        synchronized(seenKeys) {
            if (!seenKeys.add(dedupeKey)) return
            while (seenKeys.size > MAX_SEEN_KEYS) seenKeys.iterator().let { if (it.hasNext()) { it.next(); it.remove() } }
        }

        SmsAnalysisService.start(applicationContext, sender, body)
    }

    companion object {
        private const val MAX_SEEN_KEYS = 100
        private val seenKeys: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet())

        fun isEnabled(context: android.content.Context): Boolean {
            val enabledListeners =
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners",
                ) ?: return false
            return enabledListeners.contains(context.packageName)
        }
    }
}
