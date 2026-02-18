package com.zero.sentinel.utils

import android.os.Build

object DeviceInfoHelper {

    fun getDeviceInfo(context: android.content.Context? = null): String {
        val sb = StringBuilder()
        sb.append("📱 **DEVICE INFO**\n")
        sb.append("Model: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})\n")
        sb.append("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        sb.append("Brand: ${Build.BRAND}\n")
        sb.append("Device: ${Build.DEVICE}\n")
        sb.append("Board: ${Build.BOARD}\n")
        sb.append("Host: ${Build.HOST}\n")
        sb.append("Fingerprint: ${Build.FINGERPRINT}\n")

        if (context != null) {
            try {
                // Battery
                val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                val batteryLevel = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                sb.append("🔋 Battery: $batteryLevel%\n")

                // Memory
                val actManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memInfo = android.app.ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val totalMem = memInfo.totalMem / (1024 * 1024)
                val availMem = memInfo.availMem / (1024 * 1024)
                sb.append("💾 RAM: $availMem MB free / $totalMem MB total\n")

            } catch (e: Exception) {
                sb.append("⚠️ Error reading system metrics: ${e.message}\n")
            }
        }
        sb.append("⏰ Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        return sb.toString()
    }
    
    fun getShortDeviceInfo(): String {
         return "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"
    }

    fun getSafeDeviceName(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
    }
}
