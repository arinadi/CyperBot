package com.zero.sentinel.data.repository

import com.zero.sentinel.data.dao.LogDao
import com.zero.sentinel.data.entity.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogRepository(private val logDao: LogDao) {

    suspend fun insertLog(type: String, packageName: String, content: String) {
        withContext(Dispatchers.IO) {
            val log = LogEntry(
                timestamp = System.currentTimeMillis(),
                type = type,
                packageName = packageName,
                content = content
            )
            logDao.insert(log)
        }
    }

    suspend fun getAllLogs(): List<com.zero.sentinel.data.entity.LogEntry> {
        return withContext(Dispatchers.IO) {
            logDao.getAllLogs()
        }
    }

    suspend fun deleteAllLogs() {
        withContext(Dispatchers.IO) {
            logDao.deleteAll()
        }
    }

    suspend fun deleteLogsOlderThan(timestamp: Long) {
        withContext(Dispatchers.IO) {
            logDao.deleteLogsOlderThan(timestamp)
        }
    }
}
