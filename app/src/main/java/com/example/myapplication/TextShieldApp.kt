package com.example.myapplication

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Preloads the on-device model as soon as the process starts (whether launched by the user
 * opening the app, or by [SmsReceiver]/[MessageNotificationListener] waking the process up in the
 * background), so [SmsAnalysisService] doesn't have to pay model-init cost when a message arrives.
 */
class TextShieldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (!ModelDownloader.isModelDownloaded(this)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!PhishingDetector.isReady) {
                    PhishingDetector.initialize(applicationContext)
                    Log.d("TextShieldDebug", "model preloaded at app start")
                }
            } catch (e: Exception) {
                Log.e("TextShieldDebug", "model preload failed", e)
            }
        }
    }
}
