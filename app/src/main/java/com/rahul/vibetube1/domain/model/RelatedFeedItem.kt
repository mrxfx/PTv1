/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.compose.runtime.Immutable

@Immutable
sealed interface RelatedFeedItem {
    val stableKey: String

    data class NormalVideo(val video: VideoItem) : RelatedFeedItem {
        override val stableKey: String = "normal_${video.id}"
    }

    data class ShortsCarousel(val id: String, val shorts: List<VideoItem>) : RelatedFeedItem {
        override val stableKey: String = "shorts_carousel_$id"
    }
}
