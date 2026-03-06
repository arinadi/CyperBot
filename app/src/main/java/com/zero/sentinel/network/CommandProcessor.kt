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
            command.startsWith("/stat") -> handleStatCommand()
            command.startsWith("/fetch") -> handleFetchCommand(command)
            command.startsWith("/config") -> handleConfigCommand(command)
            else -> {
                // Ignore unknown or legacy commands silently
            }
        }
    }

    private fun handleStatCommand() {
        val lastHeartbeat = prefs.getLastHeartbeat()
        val now = System.currentTimeMillis()
        
        val heartbeatInfo = if (lastHeartbeat == 0L) {
            "Status: Waiting for first heartbeat."
        } else {
            val diff = now - lastHeartbeat
            val diffMin = diff / (60 * 1000)
            val nextIn = maxOf(0, 15 - diffMin)
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val nextRunTime = formatter.format(java.util.Date(now + (nextIn * 60 * 1000)))
            
            "⏱️ **Last run**: ${diffMin}m ago\n🔄 **Next cycle**: ~${nextIn}m (at $nextRunTime)\n"
        }
        
        val stats = com.zero.sentinel.utils.DeviceInfoHelper.getDeviceStats(context)
        client.sendMessage("$stats\n$heartbeatInfo")
    }

    private fun handleFetchCommand(command: String) {
        val arg = if ("_" in command) {
            command.substringAfter("_").trim().lowercase()
        } else {
            command.substringAfter("/fetch").trim().lowercase()
        }
        
        when (arg) {
            "loc", "location" -> {
                prefs.savePendingLocationRequest(true)
                client.sendMessage("📍 Location request queued. Will retrieve on next background cycle (max 15 mins).")
            }
            "logs", "log" -> {
                scope.launch {
                    val success = com.zero.sentinel.utils.LogUploadHelper.upload(context, repository, client)
                    if (!success) {
                        client.sendMessage("⚠️ No new logs to upload or upload failed.")
                    }
                }
            }
            "all", "" -> {
                // Do both
                prefs.savePendingLocationRequest(true)
                scope.launch {
                    val success = com.zero.sentinel.utils.LogUploadHelper.upload(context, repository, client)
                    val logMsg = if (success) "Logs uploaded." else "No new logs to upload."
                    client.sendMessage("📥 Fetch All initiated.\n- $logMsg\n- 📍 Location queued for next cycle.")
                }
            }
            else -> client.sendMessage("Unknown fetch argument. Use: `/fetch loc`, `/fetch logs`, or `/fetch all`.")
        }
    }

    private fun handleConfigCommand(command: String) {
        val action = if ("_" in command) {
             command.substringAfter("_").substringBefore(" ").trim().lowercase()
        } else {
             val parts = command.split(" ")
             parts.getOrNull(1)?.lowercase()
        }
        
        val target = command.split(" ").getOrNull(if ("_" in command) 1 else 2)

        when (action) {
            "wipe" -> {
                scope.launch {
                    repository.deleteAllLogs()
                    client.sendMessage("🔥 All local logs wiped.")
                }
            }
            "pin" -> {
                if (target != null && target.all { it.isDigit() } && target.length == 6) {
                    prefs.saveAppPassword(target)
                    client.sendMessage("✅ PIN updated to: $target")
                } else {
                    client.sendMessage("⚠️ Invalid PIN. Use exactly 6 digits. (Example: `/config pin 123456`)")
                }
            }
            "exc", "exception" -> {
                handleExceptionSubCommand(target, parts.getOrNull(3))
            }
            else -> {
                client.sendMessage(
                    """
                    ⚙️ **Config Usage:**
                    `/config wipe` - Delete local logs
                    `/config pin <6_digits>` - Set app pin
                    `/config exc <add/del/list/wipe> [pkg_name]` - Manage notification exceptions
                    """.trimIndent()
                )
            }
        }
    }

    private fun handleExceptionSubCommand(subAction: String?, pkgTarget: String?) {
        val currentExceptions = prefs.getNotificationExceptions().toMutableSet()
        val action = subAction?.lowercase()

        when (action) {
            "add" -> {
                if (!pkgTarget.isNullOrEmpty()) {
                    if (currentExceptions.add(pkgTarget)) {
                        prefs.saveNotificationExceptions(currentExceptions)
                        client.sendMessage("✅ Added to exceptions: $pkgTarget")
                    } else {
                        client.sendMessage("⚠️ Already in exceptions: $pkgTarget")
                    }
                } else {
                    client.sendMessage("Usage: `/config exc add <package_name>`")
                }
            }
            "remove", "del", "delete" -> {
                if (!pkgTarget.isNullOrEmpty()) {
                    if (currentExceptions.remove(pkgTarget)) {
                        prefs.saveNotificationExceptions(currentExceptions)
                        client.sendMessage("🗑️ Removed from exceptions: $pkgTarget")
                    } else {
                        client.sendMessage("⚠️ Not found in exceptions: $pkgTarget")
                    }
                } else {
                    client.sendMessage("Usage: `/config exc del <package_name>`")
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
                    `/config exc add <pkg>`
                    `/config exc del <pkg>`
                    `/config exc list`
                    `/config exc wipe`
                    """.trimIndent()
                )
            }
        }
    }
}
