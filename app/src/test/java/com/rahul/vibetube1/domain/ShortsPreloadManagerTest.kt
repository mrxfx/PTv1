/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain

import com.rahul.vibetube1.domain.model.*
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.ui.screens.shorts.ShortsPreloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.extractor.Page

class ShortsPreloadManagerTest {

    private lateinit var fakeVideoRepository: FakeVideoRepository
    private lateinit var preloadManager: ShortsPreloadManager

    private fun createShort(id: String, title: String): VideoItem {
        return VideoItem(
            id = id,
            title = title,
            thumbnailUrl = "https://example.com/$id.jpg",
            uploaderName = "Creator",
            uploaderUrl = null,
            uploaderThumbnailUrl = null,
            viewCount = 5000L,
            uploadDate = "2 days ago",
            duration = 45L,
            isShort = true
        )
    }

    private class FakeVideoRepository : VideoRepository {
        val requestedVideoIds = mutableListOf<String>()
        var shouldThrowForId: String? = null

        override suspend fun getStreamBundle(videoId: String, forceRefresh: Boolean): StreamBundle {
            synchronized(requestedVideoIds) {
                requestedVideoIds.add(videoId)
            }
            if (videoId == shouldThrowForId) {
                throw RuntimeException("Simulated network failure for $videoId")
            }
            return StreamBundle(
                videoStreams = listOf(
                    StreamItem(url = "https://stream/$videoId.mp4", quality = "1080p", format = "mp4", isAdaptive = false)
                ),
                audioStreams = emptyList(),
                title = "Title $videoId",
                uploaderName = "Uploader",
                uploaderUrl = null,
                uploaderThumbnailUrl = null,
                description = "Description",
                viewCount = 1000L,
                uploadDate = "today",
                thumbnailUrl = "https://thumb/$videoId.jpg"
            )
        }

        override suspend fun getCachedStreamBundle(videoId: String): StreamBundle? = null
        override suspend fun preloadStreamBundle(videoId: String) {
            getStreamBundle(videoId)
        }
        override suspend fun fetchNextRelatedPage(videoId: String, page: Page): PaginatedList<VideoItem> = PaginatedList(emptyList(), null)
        override suspend fun getChannelDetails(channelUrl: String): ChannelDetails = throw NotImplementedError()
        override suspend fun getChannelInfo(channelUrl: String): ChannelInfoBasic = throw NotImplementedError()
        override suspend fun fetchNextChannelVideosPage(channelUrl: String, page: Page): PaginatedList<VideoItem> = PaginatedList(emptyList(), null)
        override suspend fun getTrendingVideos(): PaginatedList<VideoItem> = PaginatedList(emptyList(), null)
        override suspend fun fetchNextTrendingPage(page: Page): PaginatedList<VideoItem> = PaginatedList(emptyList(), null)
        override suspend fun getPlaylistDetails(playlistUrl: String): PlaylistDetails = throw NotImplementedError()
        override suspend fun getComments(videoId: String): PaginatedList<CommentItem> = PaginatedList(emptyList(), null)
        override suspend fun fetchNextCommentsPage(videoId: String, page: Page): PaginatedList<CommentItem> = PaginatedList(emptyList(), null)
        override suspend fun getCommentReplies(videoId: String, comment: CommentItem): PaginatedList<CommentItem> = PaginatedList(emptyList(), null)
        override suspend fun fetchNextCommentRepliesPage(videoId: String, commentId: String, page: Page): PaginatedList<CommentItem> = PaginatedList(emptyList(), null)
    }

    @Before
    fun setUp() {
        fakeVideoRepository = FakeVideoRepository()
        preloadManager = ShortsPreloadManager(fakeVideoRepository)
    }

    @Test
    fun testRollingPreloadWindowMovesForward() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Default)
        val shorts = listOf(
            createShort("A", "Short A"),
            createShort("B", "Short B"),
            createShort("C", "Short C"),
            createShort("D", "Short D"),
            createShort("E", "Short E"),
            createShort("F", "Short F"),
            createShort("G", "Short G")
        )

        // 1. Start Playing A (index 0)
        preloadManager.onPageChanged(0, shorts, testScope)
        delay(1000)

        // Playing A -> A is current/ready, B, C, D, E are preloaded (window of 4 items)
        assertTrue("A must be preloaded / ready", preloadManager.isPreloaded("A"))
        assertTrue("B must be preloaded", preloadManager.isPreloaded("B"))
        assertTrue("C must be preloaded", preloadManager.isPreloaded("C"))
        assertTrue("D must be preloaded", preloadManager.isPreloaded("D"))
        assertTrue("E must be preloaded", preloadManager.isPreloaded("E"))
        assertFalse("F must not yet be preloaded while playing A", preloadManager.isPreloaded("F"))

        // 2. Advance to Playing B (index 1)
        preloadManager.onPageChanged(1, shorts, testScope)
        delay(1000)

        // Playing B -> keep C, D, E; fetch/preload F
        assertTrue("C must remain preloaded", preloadManager.isPreloaded("C"))
        assertTrue("D must remain preloaded", preloadManager.isPreloaded("D"))
        assertTrue("E must remain preloaded", preloadManager.isPreloaded("E"))
        assertTrue("F must now be preloaded", preloadManager.isPreloaded("F"))
        assertFalse("G must not yet be preloaded while playing B", preloadManager.isPreloaded("G"))

        // 3. Advance to Playing C (index 2)
        preloadManager.onPageChanged(2, shorts, testScope)
        delay(1000)

        // Playing C -> keep D, E, F; fetch/preload G
        assertTrue("D must remain preloaded", preloadManager.isPreloaded("D"))
        assertTrue("E must remain preloaded", preloadManager.isPreloaded("E"))
        assertTrue("F must remain preloaded", preloadManager.isPreloaded("F"))
        assertTrue("G must now be preloaded", preloadManager.isPreloaded("G"))

        // Verify single-flight / deduplication: each item requested once
        val aRequests = fakeVideoRepository.requestedVideoIds.count { it == "A" }
        val bRequests = fakeVideoRepository.requestedVideoIds.count { it == "B" }
        val cRequests = fakeVideoRepository.requestedVideoIds.count { it == "C" }
        val dRequests = fakeVideoRepository.requestedVideoIds.count { it == "D" }
        val eRequests = fakeVideoRepository.requestedVideoIds.count { it == "E" }
        val fRequests = fakeVideoRepository.requestedVideoIds.count { it == "F" }
        val gRequests = fakeVideoRepository.requestedVideoIds.count { it == "G" }

        assertEquals("A must be fetched once", 1, aRequests)
        assertEquals("B must be fetched once", 1, bRequests)
        assertEquals("C must be fetched once", 1, cRequests)
        assertEquals("D must be fetched once", 1, dRequests)
        assertEquals("E must be fetched once", 1, eRequests)
        assertEquals("F must be fetched once", 1, fRequests)
        assertEquals("G must be fetched once", 1, gRequests)
    }

    @Test
    fun testCanonicalIdDeduplication() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Default)
        val shorts = listOf(
            createShort("https://www.youtube.com/watch?v=VIDEO_1", "Short 1"),
            createShort("VIDEO_1", "Duplicate Short 1 Canonical"),
            createShort("VIDEO_2", "Short 2"),
            createShort("VIDEO_3", "Short 3")
        )

        preloadManager.onPageChanged(0, shorts, testScope)
        delay(800)

        assertTrue("VIDEO_1 must be marked preloaded", preloadManager.isPreloaded("VIDEO_1"))
        assertTrue("VIDEO_1 URL must also resolve to preloaded", preloadManager.isPreloaded("https://www.youtube.com/watch?v=VIDEO_1"))
        val video1Requests = fakeVideoRepository.requestedVideoIds.count { it == "VIDEO_1" }
        assertEquals("VIDEO_1 should only be requested once across different URL formats", 1, video1Requests)
    }

    @Test
    fun testTemporaryCacheSessionClear() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Default)
        val shorts = listOf(
            createShort("A", "Short A"),
            createShort("B", "Short B"),
            createShort("C", "Short C")
        )

        preloadManager.onPageChanged(0, shorts, testScope)
        delay(800)

        assertTrue(preloadManager.isPreloaded("A"))
        assertTrue(preloadManager.isPreloaded("B"))
        assertTrue(preloadManager.isPreloaded("C"))

        // Clear session on exit or app launch
        preloadManager.clear()

        assertFalse("Session clear must remove preloaded state for A", preloadManager.isPreloaded("A"))
        assertFalse("Session clear must remove preloaded state for B", preloadManager.isPreloaded("B"))
        assertFalse("Session clear must remove preloaded state for C", preloadManager.isPreloaded("C"))
        assertNull("Current video ID must be cleared", preloadManager.getCurrentVideoId())
        assertEquals("Preloaded count must be 0", 0, preloadManager.getPreloadedCount())
    }

    @Test
    fun testFailedIdTrackingDoesNotRetry() = runBlocking {
        val testScope = CoroutineScope(Dispatchers.Default)
        val shorts = listOf(
            createShort("A", "Short A"),
            createShort("FAIL_1", "Failing Short"),
            createShort("C", "Short C")
        )

        fakeVideoRepository.shouldThrowForId = "FAIL_1"

        preloadManager.onPageChanged(0, shorts, testScope)
        delay(800)

        assertTrue(preloadManager.isFailed("FAIL_1"))
        assertFalse(preloadManager.isPreloaded("FAIL_1"))

        val initialAttempts = fakeVideoRepository.requestedVideoIds.count { it == "FAIL_1" }
        assertEquals("FAIL_1 should have attempted once and recorded failure", 1, initialAttempts)

        // Trigger onPageChanged again on the same page
        preloadManager.onPageChanged(0, shorts, testScope)
        delay(500)

        val subsequentAttempts = fakeVideoRepository.requestedVideoIds.count { it == "FAIL_1" }
        assertEquals("FAIL_1 must not be retried", 1, subsequentAttempts)
    }
}
