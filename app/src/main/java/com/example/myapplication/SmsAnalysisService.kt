package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val ANALYSIS_CHANNEL_ID = "sms_analysis"
private const val ANALYSIS_NOTIFICATION_ID = 1001

private const val EXTRA_SENDER = "sender"
private const val EXTRA_BODY = "body"

/**
 * Runs the (potentially slow, especially on cold start) on-device classification outside of
 * [SmsReceiver]'s short-lived `goAsync()` window, which the OS kills after only a few seconds.
 */
class SmsAnalysisService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sender = intent?.getStringExtra(EXTRA_SENDER) ?: "알 수 없음"
        val body = intent?.getStringExtra(EXTRA_BODY)

        startForeground(ANALYSIS_NOTIFICATION_ID, buildAnalyzingNotification())

        if (body.isNullOrBlank()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        Log.d("TextShieldDebug", "onStartCommand sender=$sender bodyLen=${body?.length}")

        scope.launch {
            try {
                if (!PhishingDetector.isReady) {
                    Log.d("TextShieldDebug", "initializing model...")
                    PhishingDetector.initialize(applicationContext)
                    Log.d("TextShieldDebug", "model initialized")
                }
                val result = PhishingDetector.classify(body)
                val verdict = parseVerdict(result)
                Log.d("TextShieldDebug", "parsedVerdict=$verdict")
                if (verdict != Verdict.UNKNOWN) {
                    ClassificationStats.record(context = applicationContext, isPhishing = verdict == Verdict.PHISHING)
                    HistoryStore.add(
                        context = applicationContext,
                        verdict = verdict,
                        fullText = body,
                        reason = parseReason(result),
                    )
                    if (verdict == Verdict.PHISHING) {
                        Log.d("TextShieldDebug", "posting phishing notification")
                        PhishingNotifier.notifyPhishing(
                            context = applicationContext,
                            sender = sender,
                            snippet = body.take(50),
                            reason = parseReason(result),
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("TextShieldDebug", "analysis failed", e)
            } finally {
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildAnalyzingNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(ANALYSIS_CHANNEL_ID) == null) {
            val channel =
                NotificationChannel(
                    ANALYSIS_CHANNEL_ID,
                    "문자 분석 중",
                    NotificationManager.IMPORTANCE_MIN,
                ).apply { description = "TextShield가 수신된 문자를 분석하는 동안 표시돼요" }
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, ANALYSIS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("문자 분석 중...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        fun start(context: Context, sender: String, body: String) {
            val intent =
                Intent(context, SmsAnalysisService::class.java)
                    .putExtra(EXTRA_SENDER, sender)
                    .putExtra(EXTRA_BODY, body)
            context.startForegroundService(intent)
        }
    }
}
