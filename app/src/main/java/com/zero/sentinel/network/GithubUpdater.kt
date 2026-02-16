package com.zero.sentinel.network

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class GithubUpdater(private val context: Context) {

    private val client = OkHttpClient()
    private val repoOwner = "arinadi"
    private val repoName = "CyperBot"
    private val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    suspend fun checkAndInstallUpdate() {
        withContext(Dispatchers.IO) {
            try {
                // 1. Get Latest Release
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("GithubUpdater", "Failed to check update: ${response.code}")
                    return@withContext
                }

                val json = JsonParser.parseString(response.body?.string()).asJsonObject
                val latestTag = json.get("tag_name").asString
                
                // version tag usually starts with 'v', e.g., v1.45...
                // versionName is 1.45...
                // Simple check: if tag != v+currentVersion
                
                val cleanTag = latestTag.removePrefix("v")
                
                if (cleanTag != currentVersion) {
                    Log.i("GithubUpdater", "Update found: $latestTag (Current: $currentVersion)")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Downloading update: $latestTag", Toast.LENGTH_LONG).show()
                    }
                    
                    // 2. Find APK asset
                    val assets = json.getAsJsonArray("assets")
                    var apkUrl: String? = null
                    var apkName: String? = null
                    
                    for (asset in assets) {
                        val name = asset.asJsonObject.get("name").asString
                        if (name.endsWith("-release.apk")) {
                            apkUrl = asset.asJsonObject.get("browser_download_url").asString
                            apkName = name
                            break
                        }
                    }

                    if (apkUrl != null && apkName != null) {
                        downloadAndInstall(apkUrl, apkName)
                    } else {
                        Log.e("GithubUpdater", "No release APK found in assets")
                    }
                } else {
                    Log.i("GithubUpdater", "App is up to date")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "App is up to date", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("GithubUpdater", "Update check failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun downloadAndInstall(url: String, fileName: String) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                Log.e("GithubUpdater", "Download failed")
                return
            }

            val file = File(context.externalCacheDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(response.body?.bytes())
            fos.close()

            Log.i("GithubUpdater", "Download complete: ${file.absolutePath}")
            
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download complete. Installing...", Toast.LENGTH_LONG).show()
                installApk(file)
            }

        } catch (e: Exception) {
            Log.e("GithubUpdater", "Download failed", e)
        }
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GithubUpdater", "Install failed", e)
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
