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

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tv_status)
        enableButton = findViewById(R.id.btn_enable_accessibility)

        enableButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                
                // Show simple toast guide
                Toast.makeText(this, "Find 'Zero Sentinel' and enable it.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Service already active!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "System Monitor Status: ACTIVE"
            enableButton.isEnabled = false
            enableButton.text = "Service Enabled"
        } else {
            statusText.text = "System Monitor Status: Inactive"
            enableButton.isEnabled = true
            enableButton.text = "Enable Accessibility Service"
        }
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
}
