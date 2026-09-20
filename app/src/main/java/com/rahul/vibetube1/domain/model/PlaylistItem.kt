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
data class PlaylistItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val streamCount: Long
)
