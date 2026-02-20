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
    
    fun getHardwareInfo(context: android.content.Context): String {
        val sb = StringBuilder()
        sb.append("🛡️ **HARDWARE STATUS**\n\n")

        try {
            // --- Battery ---
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                val status = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
                val health = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_HEALTH, -1)
                val temp = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1) / 10.0
                
                val statusStr = when (status) {
                    android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging ⚡"
                    android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }

                val healthStr = when (health) {
                    android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good ✅"
                    android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat 🔥"
                    android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead 💀"
                    android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    else -> "Average"
                }

                sb.append("🔋 **Battery**: $level% ($statusStr)\n")
                sb.append("🌡️ **Temp**: $temp°C | Health: $healthStr\n\n")
            }

            // --- Storage ---
            val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalSize = (totalBlocks * blockSize) / (1024 * 1024 * 1024).toDouble()
            val freeSize = (availableBlocks * blockSize) / (1024 * 1024 * 1024).toDouble()
            
            sb.append("💾 **Storage**: ${String.format("%.2f", freeSize)} GB free / ${String.format("%.2f", totalSize)} GB total\n")

            // --- RAM ---
            val actManager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memInfo = android.app.ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalMem = memInfo.totalMem / (1024 * 1024)
            val availMem = memInfo.availMem / (1024 * 1024)
            sb.append("🧠 **RAM**: $availMem MB free / $totalMem MB\n\n")

            // --- Uptime ---
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val days = uptimeMillis / (24 * 3600 * 1000)
            val hours = (uptimeMillis % (24 * 3600 * 1000)) / (3600 * 1000)
            val minutes = (uptimeMillis % (3600 * 1000)) / (60 * 1000)
            
            sb.append("⏱️ **Uptime**: ${days}d ${hours}h ${minutes}m\n")

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
