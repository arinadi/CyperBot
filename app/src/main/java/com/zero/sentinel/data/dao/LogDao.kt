package com.zero.sentinel.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zero.sentinel.data.entity.LogEntry

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntry)

    @Query("SELECT * FROM logs ORDER BY timestamp ASC")
    suspend fun getAllLogs(): List<LogEntry>

    @Query("DELETE FROM logs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    @Query("DELETE FROM logs")
    suspend fun deleteAll()
}
