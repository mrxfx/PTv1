/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.rahul.vibetube1.domain.model.VideoItem

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val filePath: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val status: DownloadStatus,
    val quality: String?,
    val format: String?,
    val videoUrl: String?,
    val audioUrl: String? = null,
    val playlistId: String? = null,
    val playlistTitle: String? = null,
    val isAudioOnly: Boolean = false
) {
    fun toVideoItem() = VideoItem(
        id = videoId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = null,
        viewCount = 0,
        uploadDate = null,
        rawUploadDate = null,
        duration = 0
    )
}

enum class DownloadStatus {
    WAITING, DOWNLOADING, COMPLETED, FAILED, PAUSED, PENDING
}
