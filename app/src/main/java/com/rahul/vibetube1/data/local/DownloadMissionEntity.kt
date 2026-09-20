/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_missions")
data class DownloadMissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val title: String,
    val quality: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: MissionStatus = MissionStatus.QUEUED,
    val outputFilePath: String? = null,
    val creationTime: Long = System.currentTimeMillis(),
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val format: String? = null,
    val isAudioOnly: Boolean = false
)

enum class MissionStatus {
    QUEUED, DOWNLOADING, MUXING, COMPLETED, FAILED, PAUSED
}
