package com.zero.sentinel

import android.app.Application
import com.zero.sentinel.data.AppDatabase
import com.zero.sentinel.data.repository.LogRepository

class ZeroSentinelApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { LogRepository(database.logDao()) }
    val prefsManager by lazy { com.zero.sentinel.data.EncryptedPrefsManager(this) }

    override fun onCreate() {
        super.onCreate()
        // Initialize logic here
    }
}
