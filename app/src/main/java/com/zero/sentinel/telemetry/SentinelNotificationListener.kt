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

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn?.let { notification ->
            val packageName = notification.packageName
            val extras = notification.notification.extras
            val title = extras.getString("android.title") ?: "No Title"
            val text = extras.getCharSequence("android.text")?.toString() ?: "No Text"

            Log.i("SentinelNLS", "Notification: $packageName - $title: $text")

            scope.launch {
                repository.insertLog(
                    type = "NOTIFICATION",
                    packageName = packageName,
                    content = "$title: $text"
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // We generally don't care about removals for this use case
    }
}
