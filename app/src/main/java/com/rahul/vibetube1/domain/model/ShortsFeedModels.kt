/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.compose.runtime.Immutable
import org.schabi.newpipe.extractor.Page

@Immutable
data class ShortsContinuation(
    val queryIndex: Int = 0,
    val sortIndex: Int = 0,
    val page: Page? = null,
    val visitedPageTokens: Set<String> = emptySet(),
    val consecutiveEmptyPages: Int = 0,
    val phase: Int = 0
)

@Immutable
data class ShortsFeedResult(
    val items: List<VideoItem>,
    val continuation: ShortsContinuation?,
    val hasMore: Boolean
)

@Immutable
data class WatchShortsContinuation(
    val query: String = "",
    val page: Page? = null,
    val isFallback: Boolean = false,
    val fallbackPage: Page? = null,
    val visitedPageTokens: Set<String> = emptySet(),
    val queries: List<String> = emptyList(),
    val currentQueryIndex: Int = 0
)

@Immutable
data class WatchShortsFeedResult(
    val items: List<VideoItem>,
    val continuation: WatchShortsContinuation?,
    val hasMore: Boolean
)
