package com.zero.sentinel.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefsManager(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback: Clear corrupted prefs or use standard prefs to prevent crash
            e.printStackTrace()
            // In a real scenario, we might delete the file and start fresh
            // For now, we'll try to use standard prefs as a temporary fallback to keep app alive
            // OR re-throw if critical. Better: delete and retry once.
            
            // Attempt to clear data and retry (simplified for this context)
            context.deleteSharedPreferences("secret_shared_prefs")
            
            val retryMasterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                
            EncryptedSharedPreferences.create(
                context,
                "secret_shared_prefs",
                retryMasterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    fun saveBotToken(token: String) {
        sharedPreferences.edit().putString("BOT_TOKEN", token).apply()
    }

    fun getBotToken(): String? {
        return sharedPreferences.getString("BOT_TOKEN", null)
    }

    fun saveChatId(chatId: String) {
        sharedPreferences.edit().putString("CHAT_ID", chatId).apply()
    }

    fun getChatId(): String? {
        return sharedPreferences.getString("CHAT_ID", null)
    }
    
    fun isConfigured(): Boolean {
        return !getBotToken().isNullOrEmpty() && !getChatId().isNullOrEmpty()
    }

    fun saveAppPassword(password: String) {
        sharedPreferences.edit().putString("APP_PASSWORD", password).apply()
    }

    fun getAppPassword(): String {
        return sharedPreferences.getString("APP_PASSWORD", "123123") ?: "123123"
    }

    fun saveLastUpdateId(updateId: Long) {
        sharedPreferences.edit().putLong("LAST_UPDATE_ID", updateId).apply()
    }

    fun getLastUpdateId(): Long {
        return sharedPreferences.getLong("LAST_UPDATE_ID", 0)
    }
}
