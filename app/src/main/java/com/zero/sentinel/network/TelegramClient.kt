package com.zero.sentinel.network

import android.content.Context
import android.util.Log
import com.zero.sentinel.data.EncryptedPrefsManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramClient(context: Context) {

    private val prefs = EncryptedPrefsManager(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getBaseUrl(): String? {
        val token = prefs.getBotToken()
        return if (token != null) "https://api.telegram.org/bot$token" else null
    }

    fun sendMessage(text: String) {
        val baseUrl = getBaseUrl() ?: return
        val chatId = prefs.getChatId() ?: return

        val requestBody = okhttp3.FormBody.Builder()
            .add("chat_id", chatId)
            .add("text", text)
            .add("parse_mode", "Markdown")
            .build()

        val request = Request.Builder()
            .url("$baseUrl/sendMessage")
            .post(requestBody)
            .build()
        
        Log.d("TelegramClient", "Sending message to chat_id: $chatId, content: $text")

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("TelegramClient", "SendMessage failed: ${response.code} - $errorBody")
                } else {
                    Log.d("TelegramClient", "SendMessage success: ${response.code}")
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramClient", "Failed to send message", e)
        }
    }

    fun sendDocument(file: File): Boolean {
        val baseUrl = getBaseUrl() ?: return false
        val chatId = prefs.getChatId() ?: return false

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("document", file.name, file.asRequestBody("text/plain".toMediaTypeOrNull()))
            .build()

        val request = Request.Builder()
            .url("$baseUrl/sendDocument")
            .post(requestBody)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e("TelegramClient", "Upload failed: ${response.code} - $errorBody")
                    false
                } else {
                    Log.d("TelegramClient", "Upload success: ${response.code}")
                    true
                }
            }
        } catch (e: IOException) {
            Log.e("TelegramClient", "Failed to send document", e)
            false
        }
    }

    // Stub for now - will implement polling logic next
    fun pollUpdates(offset: Long): String? {
        val baseUrl = getBaseUrl() ?: return null
        // Timout 50s for long polling
        val url = "$baseUrl/getUpdates?offset=$offset&timeout=50" 
        
        val request = Request.Builder().url(url).build()
        
        return try {
            client.newCall(request).execute().use { response ->
                 if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: IOException) {
            Log.e("TelegramClient", "Polling failed", e)
            null
        }
    }
}
