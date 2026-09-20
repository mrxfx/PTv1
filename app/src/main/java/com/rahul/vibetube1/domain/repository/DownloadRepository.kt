/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import com.rahul.vibetube1.data.local.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    suspend fun getDownloadByVideoId(videoId: String): DownloadEntity?
    suspend fun getDownloadByVideoIdResilient(videoId: String): DownloadEntity?
    suspend fun startDownload(
        videoId: String,
        url: String?,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        quality: String?,
        format: String?,
        audioUrl: String? = null,
        playlistId: String? = null,
        playlistTitle: String? = null,
        saveToDevice: Boolean = false,
        isAudioOnly: Boolean = false
    )
    suspend fun cancelDownload(videoId: String)
    suspend fun pauseDownload(videoId: String)
    suspend fun resumeDownload(videoId: String)
    suspend fun pauseAllActiveDownloads()
    suspend fun resumeAllPausedDownloads()
    suspend fun deleteDownload(videoId: String)
    suspend fun clearAllDownloads()
    suspend fun saveToPublicStorage(videoId: String): Result<Unit>
}
