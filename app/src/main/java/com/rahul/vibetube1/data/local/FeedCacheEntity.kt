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

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val feedKey: String, // e.g., "home_trending", "subs_all", "subs_channel_<id>"
    val videos: List<VideoItem>,
    val timestamp: Long = System.currentTimeMillis()
)
