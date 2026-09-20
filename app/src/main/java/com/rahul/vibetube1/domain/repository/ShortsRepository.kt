/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import com.rahul.vibetube1.domain.model.ShortsContinuation
import com.rahul.vibetube1.domain.model.ShortsFeedResult
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.WatchShortsContinuation
import com.rahul.vibetube1.domain.model.WatchShortsFeedResult

interface ShortsRepository {
    /**
     * Discovers an initial batch of real Shorts for the dedicated Shorts player.
     * Starts from [initialVideoId] if provided, and fetches up to [targetBatchSize] items.
     * Ensures canonical deduplication against [excludedIds].
     */
    suspend fun getInitialShortsFeed(
        initialVideoId: String? = null,
        targetBatchSize: Int = 10,
        excludedIds: Set<String> = emptySet()
    ): ShortsFeedResult

    /**
     * Fetches the next continuous page of real Shorts using [continuation].
     * Never drops pagination on an individual low-yield page; continues multi-page gathering
     * until [targetBatchSize] is collected or the source pipeline is truly exhausted.
     */
    suspend fun getNextShortsPage(
        continuation: ShortsContinuation,
        targetBatchSize: Int = 10,
        excludedIds: Set<String> = emptySet()
    ): ShortsFeedResult

    /**
     * Provides an independent recommendation batch of fresh Shorts for the Home screen.
     * Isolated state: does not mutate or consume the Shorts player queue.
     */
    suspend fun getHomeShorts(
        count: Int = 8,
        excludedIds: Set<String> = emptySet()
    ): List<VideoItem>

    /**
     * Provides an independent recommendation batch of fresh Shorts for the Watch screen.
     * Isolated state: does not mutate or consume the Shorts player queue.
     */
    suspend fun getWatchRelatedShorts(
        videoTitle: String,
        videoId: String,
        count: Int = 6,
        excludedIds: Set<String> = emptySet()
    ): List<VideoItem>

    /**
     * Provides an independent paginated recommendation feed of fresh Shorts for the Watch screen.
     * Isolated state with separate pagination continuation token.
     */
    suspend fun getWatchRelatedShortsFeed(
        videoTitle: String,
        videoId: String,
        uploaderName: String = "",
        uploaderUrl: String? = null,
        description: String? = null,
        targetBatchSize: Int = 8,
        excludedIds: Set<String> = emptySet(),
        seedShorts: List<VideoItem> = emptyList()
    ): WatchShortsFeedResult

    /**
     * Fetches the next page of related Shorts for the Watch screen using [continuation].
     */
    suspend fun getNextWatchRelatedShortsPage(
        continuation: WatchShortsContinuation,
        targetBatchSize: Int = 8,
        excludedIds: Set<String> = emptySet()
    ): WatchShortsFeedResult
}
