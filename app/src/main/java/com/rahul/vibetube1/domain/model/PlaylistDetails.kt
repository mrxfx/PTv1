/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.annotation.Keep

@Keep
data class PlaylistDetails(
    val id: String,
    val title: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val thumbnailUrl: String,
    val videos: List<VideoItem>
)
