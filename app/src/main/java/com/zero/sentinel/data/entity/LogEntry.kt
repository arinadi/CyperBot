package com.zero.sentinel.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val type: String, // KEYSTROKE, APP_USAGE, SCREEN_CONTENT
    val packageName: String,
    val content: String,
    val isEncrypted: Boolean = false // Will be used in Phase 2
)
