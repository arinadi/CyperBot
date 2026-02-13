package com.zero.sentinel.telemetry

import android.view.accessibility.AccessibilityEvent
import android.util.Log

import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppTracker(private val repository: LogRepository) {

    private var lastApp: String = ""
    private val scope = CoroutineScope(Dispatchers.IO)

    fun handle(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""

        if (packageName != lastApp) {
            Log.i("SentinelAppTracker", "App Switch: $packageName ($className)")
            
            scope.launch {
                repository.insertLog("APP_USAGE", packageName, "Launched: $className")
            }
            
            lastApp = packageName
        }
    }
}
