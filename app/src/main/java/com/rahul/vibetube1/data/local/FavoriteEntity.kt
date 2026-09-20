/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

import com.rahul.vibetube1.domain.model.VideoItem

@Entity(
    tableName = "favorites",
    indices = [Index(value = ["timestamp"], name = "index_favorites_timestamp")]
)
data class FavoriteEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val timestamp: Long = System.currentTimeMillis()
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
