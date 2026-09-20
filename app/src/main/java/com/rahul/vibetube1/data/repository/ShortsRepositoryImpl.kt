/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.repository

import android.util.Log
import com.rahul.vibetube1.domain.model.*
import com.rahul.vibetube1.domain.repository.SearchRepository
import com.rahul.vibetube1.domain.repository.ShortsRepository
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.utils.ContextualShortsHelper
import com.rahul.vibetube1.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShortsRepositoryImpl @Inject constructor(
    private val searchRepository: SearchRepository,
    private val videoRepository: VideoRepository
) : ShortsRepository {

    companion object {
        private const val TAG = "ShortsPipeline"
        private val PIPELINE_QUERIES = listOf(
            "#shorts",
            "shorts trending",
            "viral shorts",
            "youtube shorts",
            "shorts",
            "popular shorts",
            "best shorts",
            "shorts mix",
            "top shorts",
            "new shorts",
            "comedy shorts",
            "gaming shorts",
            "tech shorts",
            "music shorts",
            "shorts daily"
        )
    }

    override suspend fun getInitialShortsFeed(
        initialVideoId: String?,
        targetBatchSize: Int,
        excludedIds: Set<String>
    ): ShortsFeedResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d("SHORTS_PERF", "SHORTS_FIRST_PAGE_START: initialId=$initialVideoId, targetBatch=$targetBatchSize, excludedCount=${excludedIds.size}")

        val collected = mutableListOf<VideoItem>()
        val seenInBatch = excludedIds.map { VideoUtils.extractVideoId(it).ifEmpty { it } }.toMutableSet()

        // 1. If an initial video ID was supplied, place it first in the feed
        if (initialVideoId != null) {
            val canonicalInitialId = VideoUtils.extractVideoId(initialVideoId).ifEmpty { initialVideoId }
            if (!seenInBatch.contains(canonicalInitialId)) {
                try {
                    val bundle = videoRepository.getStreamBundle(canonicalInitialId)
                    val initialItem = VideoItem(
                        id = canonicalInitialId,
                        title = bundle.title,
                        thumbnailUrl = bundle.thumbnailUrl ?: "",
                        uploaderName = bundle.uploaderName,
                        uploaderUrl = bundle.uploaderUrl,
                        uploaderThumbnailUrl = bundle.uploaderThumbnailUrl,
                        viewCount = bundle.viewCount,
                        subscriberCount = bundle.uploaderSubscriberCount,
                        uploadDate = bundle.uploadDate,
                        duration = 0L,
                        isShort = true
                    )
                    collected.add(initialItem)
                    seenInBatch.add(canonicalInitialId)
                    Log.d("SHORTS_METADATA", "Initial video resolved: id=$canonicalInitialId, title='${initialItem.title}'")
                } catch (e: Exception) {
                    Log.w(TAG, "[ShortsPipeline] Failed to resolve initial video bundle for id=$canonicalInitialId: ${e.message}")
                }
            }
        }

        // 2. Fetch fresh candidates using the multi-query pipeline
        // Start from a random query index and random sort index to ensure fresh shorts across app restarts
        val randomStartIndex = PIPELINE_QUERIES.indices.random()
        val randomSortIndex = (0..2).random()
        val initialContinuation = ShortsContinuation(
            queryIndex = randomStartIndex,
            sortIndex = randomSortIndex,
            page = null,
            phase = 0
        )
        val result = fetchShortsUntilTarget(
            continuation = initialContinuation,
            targetCount = (targetBatchSize - collected.size).coerceAtLeast(1),
            seenIds = seenInBatch
        )

        collected.addAll(result.items)
        val durationMs = System.currentTimeMillis() - startTime
        Log.d("SHORTS_PERF", "SHORTS_FIRST_PAGE_END: total=${collected.size} items in ${durationMs}ms, hasMore=${result.hasMore}")

        if (collected.isNotEmpty()) {
            val firstItem = collected.first()
            Log.d("SHORTS_PERF", "SHORTS_FIRST_PLAYABLE_FOUND: id=${firstItem.id}, title='${firstItem.title}'")
            // Async stream bundle pre-extraction for the first item
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                try {
                    videoRepository.getStreamBundle(firstItem.id, forceRefresh = false)
                } catch (e: Exception) {
                    Log.w("SHORTS_PERF", "Pre-extraction for first item failed: ${e.message}")
                }
            }
        }

        ShortsFeedResult(
            items = collected,
            continuation = result.continuation,
            hasMore = result.hasMore
        )
    }

    override suspend fun getNextShortsPage(
        continuation: ShortsContinuation,
        targetBatchSize: Int,
        excludedIds: Set<String>
    ): ShortsFeedResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d("SHORTS_PERF", "SHORTS_NEXT_PAGE_START: phase=${continuation.phase}, queryIdx=${continuation.queryIndex}, hasPage=${continuation.page != null}, targetBatch=$targetBatchSize")

        val canonicalExcluded = excludedIds.map { VideoUtils.extractVideoId(it).ifEmpty { it } }.toMutableSet()
        val result = fetchShortsUntilTarget(
            continuation = continuation,
            targetCount = targetBatchSize,
            seenIds = canonicalExcluded
        )

        val durationMs = System.currentTimeMillis() - startTime
        Log.d("SHORTS_PERF", "SHORTS_NEXT_PAGE_END: ${result.items.size} items in ${durationMs}ms, hasMore=${result.hasMore}")

        result
    }

    override suspend fun getHomeShorts(
        count: Int,
        excludedIds: Set<String>
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        Log.d(TAG, "[ShortsPipeline] getHomeShorts: count=$count, excludedCount=${excludedIds.size}")
        val collected = mutableListOf<VideoItem>()
        val seen = excludedIds.toMutableSet()

        try {
            val result = searchRepository.search(
                query = "#shorts",
                sort = SearchSort.RELEVANCE,
                duration = DurationFilter.SHORT
            )
            val searchShorts = result.items
                .filterIsInstance<SearchItem.Video>()
                .map { it.video }
                .filter { VideoUtils.isShort(it) && !seen.contains(it.id) }

            for (short in searchShorts) {
                if (collected.size >= count) break
                collected.add(short)
                seen.add(short.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "[ShortsPipeline] Error fetching home shorts from search", e)
        }

        // Fill remaining with trending kiosk shorts if needed
        if (collected.size < count) {
            try {
                val trending = videoRepository.getTrendingVideos()
                val trendingShorts = trending.items
                    .filter { VideoUtils.isShort(it) && !seen.contains(it.id) }

                for (short in trendingShorts) {
                    if (collected.size >= count) break
                    collected.add(short)
                    seen.add(short.id)
                }
            } catch (e: Exception) {
                Log.w(TAG, "[ShortsPipeline] Error fetching home shorts from trending", e)
            }
        }

        Log.d(TAG, "[ShortsPipeline] getHomeShorts returned ${collected.size} independent items")
        collected
    }

    override suspend fun getWatchRelatedShorts(
        videoTitle: String,
        videoId: String,
        count: Int,
        excludedIds: Set<String>
    ): List<VideoItem> {
        return getWatchRelatedShortsFeed(
            videoTitle = videoTitle,
            videoId = videoId,
            targetBatchSize = count,
            excludedIds = excludedIds
        ).items
    }

    override suspend fun getWatchRelatedShortsFeed(
        videoTitle: String,
        videoId: String,
        uploaderName: String,
        uploaderUrl: String?,
        description: String?,
        targetBatchSize: Int,
        excludedIds: Set<String>,
        seedShorts: List<VideoItem>
    ): WatchShortsFeedResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "[ShortsPipeline] getWatchRelatedShortsFeed for title='$videoTitle', uploader='$uploaderName', id=$videoId")
        val collected = mutableListOf<VideoItem>()
        val seen = (excludedIds + videoId).toMutableSet()
        val visitedTokens = mutableSetOf<String>()

        // 1. Incorporate any seed shorts directly provided from initial stream info related items
        for (short in seedShorts) {
            val canonId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
            if (VideoUtils.isShort(short) && !seen.contains(canonId) && canonId != videoId) {
                collected.add(short.copy(id = canonId, isShort = true))
                seen.add(canonId)
                if (collected.size >= targetBatchSize) break
            }
        }

        // 2. Build multi-tier ranked contextual queries
        val queries = ContextualShortsHelper.buildContextualQueries(
            title = videoTitle,
            uploaderName = uploaderName,
            uploaderUrl = uploaderUrl,
            description = description
        )

        var currentQueryIndex = 0
        var currentPage: Page? = null

        while (currentQueryIndex < queries.size && collected.size < targetBatchSize) {
            val query = queries[currentQueryIndex]
            try {
                Log.d(TAG, "[ShortsPipeline] Querying contextual shorts tier $currentQueryIndex: '$query'")
                val result = searchRepository.search(
                    query = query,
                    sort = SearchSort.RELEVANCE,
                    duration = DurationFilter.SHORT
                )
                currentPage = result.nextPage
                val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                val valid = rawVideos.filter {
                    val canonId = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
                    VideoUtils.isShort(it) && !seen.contains(canonId) && canonId != videoId
                }
                for (short in valid) {
                    val canonId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                    collected.add(short.copy(id = canonId, isShort = true))
                    seen.add(canonId)
                    if (collected.size >= targetBatchSize) break
                }
            } catch (e: Exception) {
                Log.w(TAG, "[ShortsPipeline] Contextual search failed for query='$query'", e)
            }

            if (collected.size >= targetBatchSize && currentPage != null) {
                break
            }
            if (collected.size >= targetBatchSize) {
                currentQueryIndex++
                currentPage = null
                break
            }
            currentQueryIndex++
            currentPage = null
        }

        val hasMore = (currentQueryIndex < queries.size) || (currentPage != null)
        val continuation = if (hasMore && queries.isNotEmpty()) {
            val activeQuery = if (currentQueryIndex < queries.size) queries[currentQueryIndex] else queries.last()
            WatchShortsContinuation(
                query = activeQuery,
                queries = queries,
                currentQueryIndex = currentQueryIndex.coerceAtMost(queries.lastIndex),
                page = currentPage,
                visitedPageTokens = visitedTokens
            )
        } else null

        WatchShortsFeedResult(
            items = collected,
            continuation = continuation,
            hasMore = hasMore
        )
    }

    override suspend fun getNextWatchRelatedShortsPage(
        continuation: WatchShortsContinuation,
        targetBatchSize: Int,
        excludedIds: Set<String>
    ): WatchShortsFeedResult = withContext(Dispatchers.IO) {
        val collected = mutableListOf<VideoItem>()
        val seen = excludedIds.toMutableSet()
        val visitedTokens = continuation.visitedPageTokens.toMutableSet()

        val queries = continuation.queries.ifEmpty { 
            if (continuation.query.isNotBlank()) listOf(continuation.query) else emptyList() 
        }
        var currentQueryIndex = continuation.currentQueryIndex
        var currentPage = continuation.page

        // 1. Try to fetch next page from active query if available
        if (currentPage != null && currentQueryIndex in queries.indices) {
            val query = queries[currentQueryIndex]
            val tokenKey = currentPage.id ?: currentPage.url ?: "page"
            if (!visitedTokens.contains(tokenKey)) {
                visitedTokens.add(tokenKey)
                try {
                    val result = searchRepository.fetchNextPage(
                        query = query,
                        sort = SearchSort.RELEVANCE,
                        duration = DurationFilter.SHORT,
                        page = currentPage
                    )
                    currentPage = result.nextPage
                    val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                    val valid = rawVideos.filter {
                        val canonId = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
                        VideoUtils.isShort(it) && !seen.contains(canonId)
                    }
                    for (short in valid) {
                        val canonId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                        collected.add(short.copy(id = canonId, isShort = true))
                        seen.add(canonId)
                        if (collected.size >= targetBatchSize) break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[ShortsPipeline] fetchNextPage failed for query='$query'", e)
                    currentPage = null
                }
            } else {
                currentPage = null
            }
        }

        // 2. If we need more shorts, advance to subsequent contextual queries
        if (collected.size < targetBatchSize && currentPage == null) {
            currentQueryIndex++
            while (currentQueryIndex < queries.size && collected.size < targetBatchSize) {
                val query = queries[currentQueryIndex]
                try {
                    Log.d(TAG, "[ShortsPipeline] Advancing to next contextual query [$currentQueryIndex]: '$query'")
                    val result = searchRepository.search(
                        query = query,
                        sort = SearchSort.RELEVANCE,
                        duration = DurationFilter.SHORT
                    )
                    currentPage = result.nextPage
                    val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                    val valid = rawVideos.filter {
                        val canonId = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
                        VideoUtils.isShort(it) && !seen.contains(canonId)
                    }
                    for (short in valid) {
                        val canonId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                        collected.add(short.copy(id = canonId, isShort = true))
                        seen.add(canonId)
                        if (collected.size >= targetBatchSize) break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "[ShortsPipeline] Contextual query failed for query='$query'", e)
                }

                if (collected.size >= targetBatchSize && currentPage != null) {
                    break
                }
                if (collected.size >= targetBatchSize) {
                    currentQueryIndex++
                    currentPage = null
                    break
                }
                currentQueryIndex++
                currentPage = null
            }
        }

        val hasMore = (currentQueryIndex < queries.size) || (currentPage != null)
        val nextContinuation = if (hasMore && queries.isNotEmpty()) {
            val activeQuery = if (currentQueryIndex < queries.size) queries[currentQueryIndex] else queries.last()
            WatchShortsContinuation(
                query = activeQuery,
                queries = queries,
                currentQueryIndex = currentQueryIndex.coerceAtMost(queries.lastIndex),
                page = currentPage,
                visitedPageTokens = visitedTokens
            )
        } else null

        WatchShortsFeedResult(
            items = collected,
            continuation = nextContinuation,
            hasMore = hasMore
        )
    }

    /**
     * Core multi-page loop:
     * Continues pulling from continuation tokens or transitioning to fallback queries
     * until [targetCount] unique, valid real Shorts are collected or all sources terminate.
     * NEVER stops after 4/5/10 Shorts — supports continuous 30+ Shorts loading.
     */
    private suspend fun fetchShortsUntilTarget(
        continuation: ShortsContinuation,
        targetCount: Int,
        seenIds: MutableSet<String>
    ): ShortsFeedResult {
        val collected = mutableListOf<VideoItem>()
        var currentPhase = continuation.phase
        var currentQueryIndex = continuation.queryIndex
        var currentPage: Page? = continuation.page
        val visitedTokens = continuation.visitedPageTokens.toMutableSet()
        var emptyPageStreak = continuation.consecutiveEmptyPages
        var iterations = 0
        val maxIterations = 14 // Generous guard to allow collecting targetCount without premature exit

        while (collected.size < targetCount && iterations < maxIterations && currentPhase <= 3) {
            iterations++

            when (currentPhase) {
                0 -> {
                    // Phase 0: Primary multi-query pipeline
                    if (currentQueryIndex >= PIPELINE_QUERIES.size) {
                        currentPhase = 1
                        currentQueryIndex = 0
                        currentPage = null
                        emptyPageStreak = 0
                        continue
                    }

                    val query = PIPELINE_QUERIES[currentQueryIndex]
                    try {
                        if (currentPage == null) {
                            Log.d("SHORTS_PAGE_FETCH", "Phase 0: Starting query #$currentQueryIndex: '$query'")
                            val result = searchRepository.search(
                                query = query,
                                sort = SearchSort.RELEVANCE,
                                duration = DurationFilter.SHORT
                            )
                            val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                            val validShorts = rawVideos.filter { item ->
                                val canonicalId = VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                                VideoUtils.isShort(item) && !seenIds.contains(canonicalId)
                            }

                            for (short in validShorts) {
                                val canonicalId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                                collected.add(short.copy(id = canonicalId))
                                seenIds.add(canonicalId)
                            }

                            Log.d("SHORTS_METADATA", "Query '$query' first page yielded ${validShorts.size} valid shorts. Collected: ${collected.size}/$targetCount")

                            if (result.nextPage != null) {
                                currentPage = result.nextPage
                                emptyPageStreak = 0
                            } else {
                                currentQueryIndex++
                                currentPage = null
                            }
                        } else {
                            val pageToken = currentPage.id ?: currentPage.url ?: currentPage.toString()
                            val tokenKey = "P0_${currentQueryIndex}_$pageToken"

                            if (visitedTokens.contains(tokenKey)) {
                                Log.w(TAG, "[ShortsPipeline] Token already visited ($tokenKey). Advancing query.")
                                currentQueryIndex++
                                currentPage = null
                                continue
                            }
                            visitedTokens.add(tokenKey)

                            Log.d("SHORTS_PAGE_FETCH", "Phase 0: Fetching continuation page for query '$query'")
                            val result = searchRepository.fetchNextPage(
                                query = query,
                                sort = SearchSort.RELEVANCE,
                                duration = DurationFilter.SHORT,
                                page = currentPage
                            )
                            val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                            val validShorts = rawVideos.filter { item ->
                                val canonicalId = VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                                VideoUtils.isShort(item) && !seenIds.contains(canonicalId)
                            }

                            for (short in validShorts) {
                                val canonicalId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                                collected.add(short.copy(id = canonicalId))
                                seenIds.add(canonicalId)
                            }

                            Log.d("SHORTS_METADATA", "Query '$query' continuation yielded ${validShorts.size} valid shorts. Collected: ${collected.size}/$targetCount")

                            if (validShorts.isEmpty()) {
                                emptyPageStreak++
                            } else {
                                emptyPageStreak = 0
                            }

                            if (result.nextPage != null && emptyPageStreak < 3) {
                                currentPage = result.nextPage
                            } else {
                                currentQueryIndex++
                                currentPage = null
                                emptyPageStreak = 0
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "[ShortsPipeline] Error in query '$query', advancing to next", e)
                        currentQueryIndex++
                        currentPage = null
                        emptyPageStreak = 0
                    }
                }

                1 -> {
                    // Phase 1: Trending Kiosk Feed
                    try {
                        val tokenKey = "P1_${currentPage?.id ?: currentPage?.url ?: "root"}"
                        if (visitedTokens.contains(tokenKey)) {
                            currentPhase = 2
                            currentPage = null
                            continue
                        }
                        visitedTokens.add(tokenKey)

                        Log.d("SHORTS_PAGE_FETCH", "Phase 1: Fetching trending kiosk page (hasPage=${currentPage != null})")
                        val trendingResult = if (currentPage == null) {
                            videoRepository.getTrendingVideos()
                        } else {
                            videoRepository.fetchNextTrendingPage(currentPage)
                        }

                        val validShorts = trendingResult.items.filter { item ->
                            val canonicalId = VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                            VideoUtils.isShort(item) && !seenIds.contains(canonicalId)
                        }

                        for (short in validShorts) {
                            val canonicalId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                            collected.add(short.copy(id = canonicalId))
                            seenIds.add(canonicalId)
                        }

                        Log.d("SHORTS_METADATA", "Trending kiosk yielded ${validShorts.size} valid shorts. Collected: ${collected.size}/$targetCount")

                        if (trendingResult.nextPage != null) {
                            currentPage = trendingResult.nextPage
                        } else {
                            currentPhase = 2
                            currentPage = null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[ShortsPipeline] Trending kiosk phase failed, advancing to Phase 2", e)
                        currentPhase = 2
                        currentPage = null
                    }
                }

                2 -> {
                    // Phase 2: Freshness query by view count
                    try {
                        val tokenKey = "P2_${currentPage?.id ?: currentPage?.url ?: "root"}"
                        if (visitedTokens.contains(tokenKey)) {
                            currentPhase = 3
                            currentPage = null
                            continue
                        }
                        visitedTokens.add(tokenKey)

                        Log.d("SHORTS_PAGE_FETCH", "Phase 2: Fetching view-count sorted #shorts (hasPage=${currentPage != null})")
                        val result = if (currentPage == null) {
                            searchRepository.search("#shorts", sort = SearchSort.VIEW_COUNT, duration = DurationFilter.SHORT)
                        } else {
                            searchRepository.fetchNextPage("#shorts", sort = SearchSort.VIEW_COUNT, duration = DurationFilter.SHORT, page = currentPage)
                        }

                        val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                        val validShorts = rawVideos.filter { item ->
                            val canonicalId = VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                            VideoUtils.isShort(item) && !seenIds.contains(canonicalId)
                        }

                        for (short in validShorts) {
                            val canonicalId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                            collected.add(short.copy(id = canonicalId))
                            seenIds.add(canonicalId)
                        }

                        Log.d("SHORTS_METADATA", "Phase 2 view-count yielded ${validShorts.size} valid shorts. Collected: ${collected.size}/$targetCount")

                        if (result.nextPage != null) {
                            currentPage = result.nextPage
                        } else {
                            currentPhase = 3
                            currentPage = null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[ShortsPipeline] Phase 2 failed, advancing to Phase 3", e)
                        currentPhase = 3
                        currentPage = null
                    }
                }

                3 -> {
                    // Phase 3: Freshness query by upload date
                    try {
                        val tokenKey = "P3_${currentPage?.id ?: currentPage?.url ?: "root"}"
                        if (visitedTokens.contains(tokenKey)) {
                            currentPhase = 4 // All phases complete
                            currentPage = null
                            continue
                        }
                        visitedTokens.add(tokenKey)

                        Log.d("SHORTS_PAGE_FETCH", "Phase 3: Fetching upload-date sorted #shorts (hasPage=${currentPage != null})")
                        val result = if (currentPage == null) {
                            searchRepository.search("#shorts", sort = SearchSort.UPLOAD_DATE, duration = DurationFilter.SHORT)
                        } else {
                            searchRepository.fetchNextPage("#shorts", sort = SearchSort.UPLOAD_DATE, duration = DurationFilter.SHORT, page = currentPage)
                        }

                        val rawVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                        val validShorts = rawVideos.filter { item ->
                            val canonicalId = VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                            VideoUtils.isShort(item) && !seenIds.contains(canonicalId)
                        }

                        for (short in validShorts) {
                            val canonicalId = VideoUtils.extractVideoId(short.id).ifEmpty { short.id }
                            collected.add(short.copy(id = canonicalId))
                            seenIds.add(canonicalId)
                        }

                        Log.d("SHORTS_METADATA", "Phase 3 upload-date yielded ${validShorts.size} valid shorts. Collected: ${collected.size}/$targetCount")

                        if (result.nextPage != null) {
                            currentPage = result.nextPage
                        } else {
                            currentPhase = 4
                            currentPage = null
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[ShortsPipeline] Phase 3 failed", e)
                        currentPhase = 4
                        currentPage = null
                    }
                }
            }
        }

        val hasMore = currentPhase < 4 || currentPage != null
        val updatedContinuation = if (hasMore) {
            ShortsContinuation(
                queryIndex = currentQueryIndex,
                page = currentPage,
                visitedPageTokens = visitedTokens,
                consecutiveEmptyPages = emptyPageStreak,
                phase = currentPhase
            )
        } else {
            null
        }

        return ShortsFeedResult(
            items = collected,
            continuation = updatedContinuation,
            hasMore = hasMore
        )
    }
}
