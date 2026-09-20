/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import org.schabi.newpipe.extractor.Page
import androidx.annotation.Keep

@Keep
data class ChannelDetails(
    val id: String,
    val name: String,
    val description: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val subscriberCount: Long?,
    val videos: List<VideoItem>,
    val nextVideosPage: Page? = null,
    val playlists: List<PlaylistItem> = emptyList(),
    val posts: List<ChannelPostItem> = emptyList()
)

@Keep
data class ChannelPostItem(
    val id: String,
    val text: String,
    val timeFormatted: String? = null,
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val attachmentImageUrl: String? = null
)

@Keep
data class ChannelInfoBasic(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val subscriberCount: Long?
)
