package com.zero.sentinel.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.zero.sentinel.ui.MainActivity

object StealthManager {

    fun hideAppIcon(context: Context) {
        val componentName = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun showAppIcon(context: Context) {
        val componentName = ComponentName(context, MainActivity::class.java)
        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
