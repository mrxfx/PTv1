/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.repository.DownloadRepository
import javax.inject.Inject

class DownloadVideoUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
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
        isAudioOnly: Boolean = false,
        saveToDevice: Boolean = false
    ) {
        val finalVideoUrl = url
        val finalAudioUrl = audioUrl ?: url

        repository.startDownload(
            videoId = videoId,
            url = finalVideoUrl,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploaderName = uploaderName,
            quality = quality,
            format = format,
            audioUrl = finalAudioUrl,
            playlistId = playlistId,
            playlistTitle = playlistTitle,
            saveToDevice = saveToDevice,
            isAudioOnly = isAudioOnly
        )
    }
}
