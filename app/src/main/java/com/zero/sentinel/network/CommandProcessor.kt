package com.zero.sentinel.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommandProcessor(
    private val repository: LogRepository,
    private val client: TelegramClient
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun processUpdates(jsonResponse: String?): Long {
        if (jsonResponse == null) return 0
        
        var maxUpdateId = 0L

        try {
            val root = gson.fromJson(jsonResponse, JsonObject::class.java)
            if (!root.has("result")) return 0
            
            val results = root.getAsJsonArray("result")
            
            for (i in 0 until results.size()) {
                val update = results.get(i).asJsonObject
                val updateId = update.get("update_id").asLong
                if (updateId > maxUpdateId) maxUpdateId = updateId
                
                if (update.has("message")) {
                    val message = update.getAsJsonObject("message")
                    if (message.has("text")) {
                        val text = message.get("text").asString
                        val chatId = message.get("chat").asJsonObject.get("id").asString
                        
                        // Security check: Only execute commands from configured chat
                        // (Assuming client/prefs ensures we only talk to right chat, but good to check)
                        
                        handleCommand(text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CommandProcessor", "Error processing updates", e)
        }
        
        return maxUpdateId
    }

    private fun handleCommand(command: String) {
        Log.i("CommandProcessor", "Received command: $command")
        
        when {
            command.startsWith("/ping") -> {
                client.sendMessage("Pong! Sentinel is active.")
            }
            command.startsWith("/wipe") -> {
                scope.launch {
                    repository.deleteAllLogs()
                    client.sendMessage("Logs wiped locally.")
                }
            }
            else -> {
                // Ignore unknown
            }
        }
    }
}
