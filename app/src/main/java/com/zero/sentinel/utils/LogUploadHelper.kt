package com.zero.sentinel.utils

import android.content.Context
import android.util.Log
import com.zero.sentinel.data.repository.LogRepository
import com.zero.sentinel.network.TelegramClient
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUploadHelper {

    private const val TAG = "LogUploadHelper"

    /**
     * Shared logic for uploading logs from DB to Telegram.
     * @return true if upload succeeded, false otherwise.
     */
    suspend fun upload(
        context: Context,
        repository: LogRepository,
        client: TelegramClient
    ): Boolean {
        // 1. Fetch from DB
        val logs = repository.getAllLogs()
        if (logs.isEmpty()) {
            Log.i(TAG, "No logs to upload. Skipping.")
            return false
        }

        // 2. Prepare Markdown File
        val deviceName = DeviceInfoHelper.getSafeDeviceName()
        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
        val fileName = "logs_${deviceName}_${timeStamp}.md"
        val file = File(context.cacheDir, fileName)

        try {
            FileWriter(file).use { writer ->
                // Write Markdown Header
                writer.append("# Sentinel Logs - $deviceName\n\n")
                writer.append("| Time | Type | Content |\n")
                writer.append("| --- | --- | --- |\n")

                logs.forEach { log ->
                    val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                    // Clean content to avoid breaking MD table
                    val cleanContent = log.content.replace("|", "\\|").replace("\n", " ")
                    writer.append("| $date | ${log.type} | $cleanContent |\n")
                }
            }

            // 3. Upload to Telegram
            val success = client.sendDocument(file)

            // 4. Cleanup
            if (success) {
                repository.deleteAllLogs()
                Log.i(TAG, "✅ Upload Success. Cleared ${logs.size} logs from DB.")
            } else {
                Log.w(TAG, "⚠️ Upload Failed. Logs retained in DB.")
            }
            
            SecureDelete.deleteSecurely(file)
            return success

        } catch (e: Exception) {
            Log.e(TAG, "Log upload process failed", e)
            if (file.exists()) {
                file.delete()
            }
            return false
        }
    }
}
