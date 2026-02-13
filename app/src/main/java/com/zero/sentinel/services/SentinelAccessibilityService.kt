package com.zero.sentinel.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class SentinelAccessibilityService : AccessibilityService() {

    private lateinit var telemetryProcessor: com.zero.sentinel.telemetry.TelemetryProcessor

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Sentinel", "Service Connected")
        
        val app = applicationContext as com.zero.sentinel.ZeroSentinelApp
        telemetryProcessor = com.zero.sentinel.telemetry.TelemetryProcessor(app.repository)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (::telemetryProcessor.isInitialized) {
            event?.let {
                telemetryProcessor.processEvent(it)
            }
        }
    }

    override fun onInterrupt() {
        Log.w("Sentinel", "Service Interrupted")
    }
}
