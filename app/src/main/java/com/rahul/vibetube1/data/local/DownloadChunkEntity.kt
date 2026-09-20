/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "download_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadMissionEntity::class,
            parentColumns = ["id"],
            childColumns = ["missionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["missionId"])]
)
data class DownloadChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val missionId: Long,
    val chunkIndex: Int,
    val startByte: Long,
    val endByte: Long,
    val bytesDownloaded: Long = 0,
    val isCompleted: Boolean = false,
    val type: ChunkType = ChunkType.VIDEO
)

enum class ChunkType {
    VIDEO, AUDIO
}
