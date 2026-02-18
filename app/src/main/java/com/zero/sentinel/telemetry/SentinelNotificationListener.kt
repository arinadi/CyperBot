package com.zero.sentinel.telemetry

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zero.sentinel.ZeroSentinelApp
import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SentinelNotificationListener : NotificationListenerService() {

    private lateinit var repository: LogRepository
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as ZeroSentinelApp
        repository = app.repository
        Log.d("SentinelNLS", "Notification Listener Created")
    }

    private var lastNotificationHash: Int = 0

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            val packageName = notification.packageName
            val extras = notification.notification.extras
            val title = extras.getString("android.title") ?: "No Title"
            val text = extras.getCharSequence("android.text")?.toString() ?: "No Text"
            
            // 1. Check Exception List
            val prefs = com.zero.sentinel.data.EncryptedPrefsManager(applicationContext)
            val exceptions = prefs.getNotificationExceptions()
            if (exceptions.contains(packageName)) {
                return
            }

            // 2. Identify Source (Emoji Mapping)
            val appLabel = getAppLabel(packageName)
            
            // 3. Format Content (Single Line)
            val cleanTitle = title.replace("\n", " ").trim()
            val cleanText = text.replace("\n", " ").trim()
            val content = "$appLabel $cleanTitle: $cleanText"
            
            val currentHash = (packageName + content).hashCode()

            if (currentHash == lastNotificationHash) {
                Log.v("SentinelNLS", "Duplicate notification ignored: $packageName - $content")
                return
            }
            lastNotificationHash = currentHash

            Log.i("SentinelNLS", "Notification: $content")

            scope.launch {
                repository.insertLog(
                    type = "N",
                    packageName = packageName,
                    content = content
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We generally don't care about removals for this use case
    }

    private fun getAppLabel(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "[WA] 💬"
            packageName.contains("telegram") -> "[TG] ✈️"
            packageName.contains("instagram") -> "[IG] 📸"
            packageName.contains("facebook") || packageName.contains("katana") -> "[FB] 📘"
            packageName.contains("youtube") -> "[YT] ▶️"
            packageName.contains("tiktok") -> "[TT] 🎵"
            packageName.contains("snapchat") -> "[SC] 👻"
            packageName.contains("twitter") || packageName.contains("x.android") -> "[X] 🐦"
            packageName.contains("gmail") || packageName.contains("android.gm") -> "[Mail] 📧"
            packageName.contains("bank") || packageName.contains("finance") || packageName.contains("dana") || packageName.contains("ovo") || packageName.contains("gopay") -> "[Bank] 💰"
            packageName.contains("shopee") || packageName.contains("tokopedia") -> "[Shop] 🛍️"
            else -> "[$packageName]"
        }
    }
}
