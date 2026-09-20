package com.rahul.vibetube1.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rahul.vibetube1.R
import com.rahul.vibetube1.data.local.DownloadStatus
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CacheCleanupResult(
    val freedBytes: Long,
    val currentCacheSize: Long
)

@Singleton
class CacheCleanerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val downloadRepository: DownloadRepository
) {

    companion object {
        const val CHANNEL_ID = "zyvotube_cache_cleaner"
        const val NOTIFICATION_ID = 2005
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZyvoTube Cache Cleaner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications when temporary cache is cleaned"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun calculateCacheSize(): Long {
        var size = 0L
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            size += getDirSize(cacheDir)
        }
        val extCacheDir = context.externalCacheDir
        if (extCacheDir != null && extCacheDir.exists()) {
            size += getDirSize(extCacheDir)
        }
        return size
    }

    private fun getDirSize(dir: File): Long {
        var total = 0L
        val children = dir.listFiles() ?: return 0L
        for (child in children) {
            total += if (child.isDirectory) {
                getDirSize(child)
            } else {
                child.length()
            }
        }
        return total
    }

    suspend fun checkAndAutoClean(forceIfExceeded: Boolean = false): CacheCleanupResult? {
        val isEnabled = preferencesManager.isAutoCacheCleanerEnabled.first()
        if (!isEnabled && !forceIfExceeded) return null

        val thresholdMb = preferencesManager.cacheCleanerThresholdMb.first()
        val lastCleanupTime = preferencesManager.lastCacheCleanupTime.first()
        val currentSize = calculateCacheSize()
        val thresholdBytes = thresholdMb * 1024 * 1024L
        val now = System.currentTimeMillis()

        val isExceeded = currentSize >= thresholdBytes
        val isPeriodicDue = (now - lastCleanupTime) >= 24 * 60 * 60 * 1000L

        if (isExceeded || isPeriodicDue || forceIfExceeded) {
            PTLog.d("CacheCleanerManager", "Auto clean triggered: currentSize=$currentSize, thresholdBytes=$thresholdBytes, isExceeded=$isExceeded, isPeriodicDue=$isPeriodicDue")
            return performCleanup(isManualTrigger = false)
        }

        return null
    }

    suspend fun performCleanup(isManualTrigger: Boolean = false): CacheCleanupResult {
        PTLog.d("CacheCleanerManager", "Starting cache cleanup (isManualTrigger=$isManualTrigger)...")

        // Active download protection: Gather active/queued/paused video IDs and file paths
        val activeVideoIds = mutableSetOf<String>()
        val activePaths = mutableSetOf<String>()
        try {
            val allDownloads = downloadRepository.getAllDownloads().first()
            for (dl in allDownloads) {
                if (dl.status == DownloadStatus.DOWNLOADING ||
                    dl.status == DownloadStatus.WAITING ||
                    dl.status == DownloadStatus.PENDING ||
                    dl.status == DownloadStatus.PAUSED
                ) {
                    activeVideoIds.add(dl.videoId)
                    if (dl.filePath.isNotEmpty()) {
                        activePaths.add(dl.filePath)
                    }
                }
            }
        } catch (e: Exception) {
            PTLog.e("CacheCleanerManager", "Error querying active downloads for protection", e)
        }

        val userDownloadsDir = try {
            context.getExternalFilesDir(null)?.canonicalPath
        } catch (_: Exception) {
            context.getExternalFilesDir(null)?.absolutePath
        }

        val sizeBefore = calculateCacheSize()

        var freedBytes = 0L
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            freedBytes += deleteSafeRecursively(cacheDir, activeVideoIds, activePaths, userDownloadsDir, isRootDir = true)
        }
        val extCacheDir = context.externalCacheDir
        if (extCacheDir != null && extCacheDir.exists()) {
            freedBytes += deleteSafeRecursively(extCacheDir, activeVideoIds, activePaths, userDownloadsDir, isRootDir = true)
        }

        val sizeAfter = calculateCacheSize()
        val now = System.currentTimeMillis()
        preferencesManager.setLastCacheCleanupTime(now)

        PTLog.d("CacheCleanerManager", "Cache cleanup finished: freedBytes=$freedBytes, sizeBefore=$sizeBefore, sizeAfter=$sizeAfter")

        // Only show notification if meaningful cache was freed (>= 1 MB) or if manual trigger with >0 freed
        if (freedBytes >= 1 * 1024 * 1024L || (isManualTrigger && freedBytes > 0L)) {
            showNotification(freedBytes)
        }

        return CacheCleanupResult(freedBytes = freedBytes, currentCacheSize = sizeAfter)
    }

    private fun deleteSafeRecursively(
        file: File,
        activeVideoIds: Set<String>,
        activePaths: Set<String>,
        userDownloadsDir: String?,
        isRootDir: Boolean = false
    ): Long {
        var freed = 0L
        if (file.isDirectory) {
            val children = file.listFiles() ?: return 0L
            for (child in children) {
                freed += deleteSafeRecursively(child, activeVideoIds, activePaths, userDownloadsDir, isRootDir = false)
            }
            // If subfolder is empty after cleaning, remove empty subfolder (do not delete root cacheDir itself)
            if (!isRootDir && file.listFiles()?.isEmpty() == true) {
                file.delete()
            }
        } else {
            if (isSafeToDelete(file, activeVideoIds, activePaths, userDownloadsDir)) {
                val length = file.length()
                if (file.delete()) {
                    freed += length
                }
            }
        }
        return freed
    }

    private fun isSafeToDelete(
        file: File,
        activeVideoIds: Set<String>,
        activePaths: Set<String>,
        userDownloadsDir: String?
    ): Boolean {
        val canonicalPath = try { file.canonicalPath } catch (_: Exception) { file.absolutePath }

        // RULE 1: NEVER delete completed user-downloaded media in getExternalFilesDir or active paths!
        if (userDownloadsDir != null && canonicalPath.startsWith(userDownloadsDir)) {
            return false
        }
        if (activePaths.contains(canonicalPath)) {
            return false
        }

        // RULE 2: ACTIVE DOWNLOAD PROTECTION
        val fileName = file.name
        for (activeId in activeVideoIds) {
            if (activeId.isNotEmpty() && fileName.contains(activeId)) {
                return false // Belongs to an active/queued download mission
            }
        }

        // RULE 3: PROTECT FILES CURRENTLY BEING WRITTEN (modified within last 3 minutes)
        val isTempOrDownloadFile = fileName.endsWith(".tmp") ||
                fileName.endsWith(".part") ||
                fileName.contains("_video.") ||
                fileName.contains("_audio.") ||
                fileName.contains("_final.")
        val isRecentlyModified = (System.currentTimeMillis() - file.lastModified()) < 3 * 60 * 1000L

        if (isTempOrDownloadFile && isRecentlyModified) {
            return false
        }

        return true
    }

    private fun showNotification(freedBytes: Long) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val freedStr = StorageUtils.formatSize(freedBytes)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle("ZyvoTube Cache Cleaner")
                .setContentText("Freed: $freedStr")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            PTLog.e("CacheCleanerManager", "Failed to show cache cleaner notification", e)
        }
    }
}
