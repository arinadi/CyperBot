package com.zero.sentinel

import android.app.Application

class ZeroSentinelApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { LogRepository(database.logDao()) }

    override fun onCreate() {
        super.onCreate()
        // Initialize logic here
    }
}
