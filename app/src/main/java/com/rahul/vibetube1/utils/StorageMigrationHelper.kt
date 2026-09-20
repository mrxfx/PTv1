/*
 * ZyvoTube
 * Copyright (C) 2026 ZyvoTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import android.content.Context
import android.os.Environment
import com.rahul.vibetube1.data.local.DownloadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object StorageMigrationHelper {
    private const val TAG = "StorageMigrationHelper"

    // Primary ZyvoTube directories
    val ZYVOTUBE_ROOT = File(Environment.getExternalStorageDirectory(), "ZyvoTube")
    val ZYVOTUBE_VIDEOS = File(ZYVOTUBE_ROOT, "Videos")
    val ZYVOTUBE_LEGACY_VIDEOS = File(ZYVOTUBE_ROOT, "download/ZyvoTube Video")
    val ZYVOTUBE_MUSIC = File(ZYVOTUBE_ROOT, "Music")
    val ZYVOTUBE_LEGACY_AUDIO = File(ZYVOTUBE_ROOT, "download/ZyvoTube Audio")
    val ZYVOTUBE_THUMBNAILS = File(ZYVOTUBE_ROOT, "Thumbnails")

    // Legacy VibeTube directories (for backward compatibility)
    val VIBETUBE_ROOT = File(Environment.getExternalStorageDirectory(), "VibeTube")
    val VIBETUBE_VIDEOS = File(VIBETUBE_ROOT, "Videos")
    val VIBETUBE_LEGACY_VIDEOS = File(VIBETUBE_ROOT, "download/VibeTube Video")
    val VIBETUBE_MUSIC = File(VIBETUBE_ROOT, "Music")
    val VIBETUBE_LEGACY_AUDIO = File(VIBETUBE_ROOT, "download/VibeTube Audio")
    val VIBETUBE_THUMBNAILS = File(VIBETUBE_ROOT, "Thumbnails")

    // MediaStore Relative Paths
    const val RELATIVE_PATH_AUDIO = "Music/ZyvoTube/download/ZyvoTube Audio/"
    const val RELATIVE_PATH_AUDIO_FALLBACK = "Music/VibeTube/download/VibeTube Audio/"
    const val RELATIVE_PATH_MOVIES = "Movies/ZyvoTube/download/ZyvoTube Video/"
    const val RELATIVE_PATH_MOVIES_FALLBACK = "Movies/VibeTube/download/VibeTube Video/"

    /**
     * Ensures directories exist.
     */
    fun ensureZyvoTubeDirectories() {
        try {
            if (!ZYVOTUBE_ROOT.exists()) ZYVOTUBE_ROOT.mkdirs()
            if (!ZYVOTUBE_VIDEOS.exists()) ZYVOTUBE_VIDEOS.mkdirs()
            if (!ZYVOTUBE_LEGACY_VIDEOS.exists()) ZYVOTUBE_LEGACY_VIDEOS.mkdirs()
            if (!ZYVOTUBE_MUSIC.exists()) ZYVOTUBE_MUSIC.mkdirs()
            if (!ZYVOTUBE_LEGACY_AUDIO.exists()) ZYVOTUBE_LEGACY_AUDIO.mkdirs()
            if (!ZYVOTUBE_THUMBNAILS.exists()) ZYVOTUBE_THUMBNAILS.mkdirs()
            if (!VIBETUBE_ROOT.exists()) VIBETUBE_ROOT.mkdirs()
            if (!VIBETUBE_VIDEOS.exists()) VIBETUBE_VIDEOS.mkdirs()
            if (!VIBETUBE_LEGACY_VIDEOS.exists()) VIBETUBE_LEGACY_VIDEOS.mkdirs()
            if (!VIBETUBE_MUSIC.exists()) VIBETUBE_MUSIC.mkdirs()
            if (!VIBETUBE_LEGACY_AUDIO.exists()) VIBETUBE_LEGACY_AUDIO.mkdirs()
            if (!VIBETUBE_THUMBNAILS.exists()) VIBETUBE_THUMBNAILS.mkdirs()
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Failed to create some directories: ${e.message}")
        }
    }

    /**
     * Resolves a media file with backward-compatible fallback across both ZyvoTube and legacy VibeTube directories.
     */
    fun resolveMediaFile(path: String?): File? {
        if (path.isNullOrBlank()) return null
        val originalFile = File(path)
        if (originalFile.exists() && originalFile.length() > 0) {
            return originalFile
        }

        val filename = originalFile.name

        // Check counterpart path by replacing brand name
        if (path.contains("ZyvoTube", ignoreCase = true)) {
            val legacyPath = path.replace("ZyvoTube", "VibeTube")
                .replace("zyvotube", "vibetube")
            val legacyFile = File(legacyPath)
            if (legacyFile.exists() && legacyFile.length() > 0) return legacyFile
        } else if (path.contains("VibeTube", ignoreCase = true)) {
            val modernPath = path.replace("VibeTube", "ZyvoTube")
                .replace("vibetube", "zyvotube")
            val modernFile = File(modernPath)
            if (modernFile.exists() && modernFile.length() > 0) return modernFile
        }

        // Search known folders for the file by name
        val candidateFolders = listOf(
            ZYVOTUBE_VIDEOS,
            ZYVOTUBE_LEGACY_VIDEOS,
            VIBETUBE_VIDEOS,
            VIBETUBE_LEGACY_VIDEOS,
            ZYVOTUBE_MUSIC,
            ZYVOTUBE_LEGACY_AUDIO,
            VIBETUBE_MUSIC,
            VIBETUBE_LEGACY_AUDIO,
            ZYVOTUBE_THUMBNAILS,
            VIBETUBE_THUMBNAILS
        )

        for (folder in candidateFolders) {
            val candidate = File(folder, filename)
            if (candidate.exists() && candidate.length() > 0) {
                return candidate
            }
        }

        return originalFile
    }

    /**
     * Performs a non-destructive migration check.
     * Preserves existing files in VibeTube while ensuring Room database references
     * are properly updated and accessible.
     */
    suspend fun syncLegacyDownloads(context: Context, downloadDao: DownloadDao) = withContext(Dispatchers.IO) {
        ensureZyvoTubeDirectories()

        try {
            val allDownloads = downloadDao.getAllDownloadsList()
            allDownloads.forEach { download ->
                val currentFile = File(download.filePath)
                if (!currentFile.exists() || currentFile.length() <= 0) {
                    val resolved = resolveMediaFile(download.filePath)
                    if (resolved != null && resolved.exists() && resolved.length() > 0 && resolved.absolutePath != download.filePath) {
                        downloadDao.updateDownload(download.copy(filePath = resolved.absolutePath))
                        ZyvoLog.i(TAG, "Migrated download path for ${download.videoId} to ${resolved.absolutePath}")
                    }
                }
            }
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error syncing legacy download paths: ${e.message}")
        }
    }
}
