/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.compose.runtime.Stable
import androidx.annotation.Keep

@Keep
@Stable
data class VideoItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val uploaderThumbnailUrl: String? = null,
    val viewCount: Long,
    val subscriberCount: Long? = null,
    val uploadDate: String?,
    val rawUploadDate: Long? = null, // Epoch milliseconds for precise sorting
    val duration: Long, // in seconds
    val watchProgress: Float? = null, // 0.0 to 1.0
    val isShort: Boolean = false
)
