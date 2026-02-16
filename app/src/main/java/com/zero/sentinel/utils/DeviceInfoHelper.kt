package com.zero.sentinel.utils

import android.os.Build

object DeviceInfoHelper {

    fun getDeviceInfo(): String {
        return """
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Brand: ${Build.BRAND}
            Device Name: ${Build.DEVICE}
            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
            Board: ${Build.BOARD}
            Bootloader: ${Build.BOOTLOADER}
            Display: ${Build.DISPLAY}
            Fingerprint: ${Build.FINGERPRINT}
            Hardware: ${Build.HARDWARE}
            Host: ${Build.HOST}
            ID: ${Build.ID}
            Product: ${Build.PRODUCT}
            Time: ${System.currentTimeMillis()}
            User: ${Build.USER}
        """.trimIndent()
    }
    
    fun getShortDeviceInfo(): String {
         return "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"
    }

    fun getSafeDeviceName(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}".replace(" ", "_").replace(Regex("[^a-zA-Z0-9_]"), "")
    }
}
