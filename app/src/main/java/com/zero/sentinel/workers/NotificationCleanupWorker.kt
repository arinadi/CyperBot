package com.zero.sentinel.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zero.sentinel.ZeroSentinelApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val repository = (appContext.applicationContext as ZeroSentinelApp).repository

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d("NotificationCleanupWorker", "Starting cleanup of logs older than 24 hours")
            
            // Calculate cutoff time: 24 hours ago
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            
            // Delete logs older than 24 hours
            repository.deleteLogsOlderThan(cutoffTime)
            
            Log.i("NotificationCleanupWorker", "✅ Cleanup completed successfully")
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e("NotificationCleanupWorker", "Error during cleanup", e)
            return@withContext Result.retry()
        }
    }
}
