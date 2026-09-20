/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.annotation.Keep

@Keep
data class CommentItem(
    val authorName: String,
    val authorThumbnailUrl: String?,
    val authorUrl: String?,
    val commentText: String,
    val publishedTime: String?,
    val likeCount: Int = 0,
    val isHeartedByUploader: Boolean = false,
    val replyCount: Int = 0,
    val commentId: String
)
