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
}
