package com.zero.sentinel.utils

import java.io.File
import java.io.RandomAccessFile
import android.util.Log

object SecureDelete {

    fun deleteSecurely(file: File): Boolean {
        if (!file.exists()) return false

        try {
            val length = file.length()
            val raf = RandomAccessFile(file, "rwd")
            
            // Pass 1: Overwrite with random data (optional, skipping for speed/stealth balance)
            
            // Pass 2: Overwrite with zeros
            val data = ByteArray(1024) { 0 }
            var written = 0L
            raf.seek(0)
            while (written < length) {
                val toWrite = minOf(data.size.toLong(), length - written).toInt()
                raf.write(data, 0, toWrite)
                written += toWrite
            }
            
            raf.close()
            
            // Final Delete
            return file.delete()
        } catch (e: Exception) {
            Log.e("SecureDelete", "Failed to securely delete file: ${file.name}", e)
            // Fallback to normal delete
            return file.delete()
        }
    }
}
