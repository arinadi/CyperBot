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
    
    fun getDeviceStats(context: android.content.Context): String {
        val sb = StringBuilder()
        sb.append("📱 **DEVICE STATUS**\n")
        sb.append("Model: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        sb.append("OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n\n")

        try {
            // --- Battery ---
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                
                val statusStr = when (status) {
                    android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging ⚡"
                    android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Unplugged"
                    android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full 🔋"
                    android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }
                sb.append("🔋 **Battery**: $level% ($statusStr)\n")
            }

            // --- Network ---
            val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected) {
                if (activeNetwork.type == android.net.ConnectivityManager.TYPE_WIFI) {
                    val wm = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                    val info = wm.connectionInfo
                    val ssid = info.ssid?.replace("\"", "") ?: "Unknown SSID"
                    sb.append("🛜 **Network**: Wi-Fi ($ssid)\n")
                } else if (activeNetwork.type == android.net.ConnectivityManager.TYPE_MOBILE) {
                    sb.append("🛜 **Network**: Cellular Data\n")
                } else {
                    sb.append("🛜 **Network**: Connected (${activeNetwork.typeName})\n")
                }
            } else {
                sb.append("🛜 **Network**: Disconnected\n")
            }

            // --- Ringer Mode ---
            val am = context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
            val ringerStr = when (am.ringerMode) {
                android.media.AudioManager.RINGER_MODE_SILENT -> "Silent 🔇"
                android.media.AudioManager.RINGER_MODE_VIBRATE -> "Vibrate 📳"
                android.media.AudioManager.RINGER_MODE_NORMAL -> "Normal 🔊"
                else -> "Unknown"
            }
            sb.append("🔕 **Sound Profile**: $ringerStr\n")

        } catch (e: Exception) {
            sb.append("⚠️ **Diagnostics Error**: ${e.message}\n")
        }

        sb.append("\n⏰ Checked: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        return sb.toString()
    }

    fun getShortDeviceInfo(): String {
         return "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"
    }

    fun getSafeDeviceName(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
    }
}
