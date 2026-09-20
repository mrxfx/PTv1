/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.annotation.Keep

@Keep
@Stable
sealed interface SearchItem {
    val uniqueKey: String

    @Keep
    @Immutable
    data class Video(val video: VideoItem) : SearchItem {
        override val uniqueKey: String get() = "video_${video.id}"
    }

    @Keep
    @Immutable
    data class Channel(
        val id: String,
        val name: String,
        val thumbnailUrl: String?,
        val subscriberCount: Long?,
        val description: String?,
        val isSubscribed: Boolean = false
    ) : SearchItem {
        override val uniqueKey: String get() = "channel_$id"
    }

    @Keep
    @Immutable
    data class Playlist(val playlist: PlaylistItem) : SearchItem {
        override val uniqueKey: String get() = "playlist_${playlist.id}"
    }
}
