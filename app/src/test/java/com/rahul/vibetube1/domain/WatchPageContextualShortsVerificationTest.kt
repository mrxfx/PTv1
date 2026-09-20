/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain

import com.rahul.vibetube1.domain.model.RelatedFeedItem
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.utils.ContextualShortsHelper
import com.rahul.vibetube1.utils.VideoUtils
import org.junit.Assert.*
import org.junit.Test

class WatchPageContextualShortsVerificationTest {

    private fun createTestVideo(
        id: String,
        title: String,
        thumbnailUrl: String = "",
        uploaderName: String = "Channel",
        duration: Long = 300L,
        isShort: Boolean = false
    ): VideoItem {
        return VideoItem(
            id = id,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploaderName = uploaderName,
            uploaderUrl = null,
            uploaderThumbnailUrl = null,
            viewCount = 1000L,
            uploadDate = "1 day ago",
            duration = duration,
            isShort = isShort
        )
    }

    // 1. CONTEXT MATCHING & ACTUAL TEST (Music, Science/Education, Entertainment)
    @Test
    fun `test context matching generates distinct prioritized queries for Music`() {
        val queries = ContextualShortsHelper.buildContextualQueries(
            title = "Cruel Summer [Official Music Video] (4K 60fps)",
            uploaderName = "Taylor Swift - Topic",
            uploaderUrl = "https://www.youtube.com/channel/UCxxxx",
            description = "Provided to YouTube by Universal Music Group\nArtist: Taylor Swift\nMusic: Jack Antonoff"
        )

        assertFalse("Queries should not be empty", queries.isEmpty())
        // Cleans noise: 'Taylor Swift - Topic' -> 'Taylor Swift'
        // 'Cruel Summer [Official Music Video] (4K 60fps)' -> 'Cruel Summer'
        assertTrue("Tier 1 must prioritize combined artist and title", queries.contains("Taylor Swift Cruel Summer"))
        assertTrue("Tier 2 must include clean artist name", queries.contains("Taylor Swift"))
        assertTrue("Must extract entity from description", queries.contains("Jack Antonoff"))
        // Check ordering: artist+title or artist must precede broader fallbacks
        val specificIndex = queries.indexOf("Taylor Swift Cruel Summer")
        val fallbackIndex = queries.indexOf("Cruel Summer shorts")
        assertTrue("Specific contextual query must precede fallback query", specificIndex < fallbackIndex)
    }

    @Test
    fun `test context matching generates distinct prioritized queries for Science Education`() {
        val queries = ContextualShortsHelper.buildContextualQueries(
            title = "How Quantum Computers Work | Quantum Physics Explained",
            uploaderName = "Veritasium",
            uploaderUrl = "https://www.youtube.com/channel/UCxxxx",
            description = "Exploring quantum superposition, qubits, and quantum entanglement."
        )

        assertFalse("Queries should not be empty", queries.isEmpty())
        assertTrue("Tier 1 should include channel and primary segment", queries.contains("Veritasium How Quantum Computers Work"))
        assertTrue("Tier 2 should include creator channel", queries.contains("Veritasium"))
        assertTrue("Should include segment 2", queries.contains("Quantum Physics Explained"))
    }

    @Test
    fun `test context matching generates distinct prioritized queries for Entertainment`() {
        val queries = ContextualShortsHelper.buildContextualQueries(
            title = "Deadpool & Wolverine | Official Teaser Trailer",
            uploaderName = "Marvel Entertainment",
            uploaderUrl = "https://www.youtube.com/channel/UCxxxx",
            description = "In theaters July 26.\nStarring: Ryan Reynolds\nDirector: Shawn Levy"
        )

        assertFalse("Queries should not be empty", queries.isEmpty())
        // 'Marvel Entertainment' -> cleans suffix 'Entertainment' -> 'Marvel'
        assertTrue("Should contain cleaned channel + title", queries.any { it.contains("Deadpool & Wolverine") })
        assertTrue("Should extract actor entity from description", queries.contains("Ryan Reynolds"))
    }

    @Test
    fun `test 3 contexts produce completely different recommendations`() {
        val musicQueries = ContextualShortsHelper.buildContextualQueries(
            title = "Shape of You (Official Music Video)",
            uploaderName = "Ed Sheeran",
            description = "Album: Divide"
        )
        val scienceQueries = ContextualShortsHelper.buildContextualQueries(
            title = "The James Webb Space Telescope Discoveries",
            uploaderName = "NASA",
            description = "Deep field infrared galaxy survey"
        )
        val gamingQueries = ContextualShortsHelper.buildContextualQueries(
            title = "Elden Ring DLC Shadow of the Erdtree Boss Guide",
            uploaderName = "VaatiVidya",
            description = "Lore and walkthrough"
        )

        // Ensure zero unexpected overlap between completely different domains
        val musicVsScienceIntersection = musicQueries.intersect(scienceQueries.toSet())
        val musicVsGamingIntersection = musicQueries.intersect(gamingQueries.toSet())
        assertTrue("Music and Science queries must be distinct", musicVsScienceIntersection.isEmpty())
        assertTrue("Music and Gaming queries must be distinct", musicVsGamingIntersection.isEmpty())
    }

    // 2. REAL SHORTS VALIDATION
    @Test
    fun `test isShort strictly validates Shorts and rejects normal landscape videos`() {
        // Landscape video with "shorts" in title and 10 minutes duration -> MUST BE REJECTED
        val fakeShort = createTestVideo(
            id = "video1",
            title = "Top 10 Best YouTube Shorts Compilation (Landscape)",
            thumbnailUrl = "https://i.ytimg.com/vi/video1/hqdefault.jpg",
            uploaderName = "Compilations",
            duration = 600, // 10 minutes
            isShort = false
        )
        assertFalse("Long landscape video must never be classified as a Short", VideoUtils.isShort(fakeShort))

        // Standard 5 minute video -> REJECTED
        val normalVideo = createTestVideo(
            id = "video2",
            title = "How to build an Android app",
            thumbnailUrl = "https://i.ytimg.com/vi/video2/mqdefault.jpg",
            uploaderName = "Dev",
            duration = 300,
            isShort = false
        )
        assertFalse("Standard 5 minute video must never be a Short", VideoUtils.isShort(normalVideo))

        // Legitimate Short via metadata flag
        val realShort1 = createTestVideo(
            id = "short1",
            title = "Quick Life Hack",
            thumbnailUrl = "https://i.ytimg.com/vi/short1/hqdefault.jpg",
            uploaderName = "LifeHacks",
            duration = 45,
            isShort = true
        )
        assertTrue("Video with isShort=true and <=180s must be valid", VideoUtils.isShort(realShort1))

        // Legitimate Short via shorts thumbnail pattern
        val realShort2 = createTestVideo(
            id = "short2",
            title = "Insane Trick Shot",
            thumbnailUrl = "https://i.ytimg.com/vi/short2/shorts/default.jpg",
            uploaderName = "Sports",
            duration = 30,
            isShort = false
        )
        assertTrue("Video with /shorts/ thumbnail URL must be recognized", VideoUtils.isShort(realShort2))

        // Video with duration > 180s even if marked isShort=true must be rejected
        val overLengthShort = createTestVideo(
            id = "short3",
            title = "Over length video",
            thumbnailUrl = "https://i.ytimg.com/vi/short3/shorts/default.jpg",
            uploaderName = "Test",
            duration = 240, // 4 mins
            isShort = true
        )
        assertFalse("Duration > 180s must be rejected", VideoUtils.isShort(overLengthShort))
    }

    // 3. WATCHED SHORT PREVENTION & CANONICAL IDs
    @Test
    fun `test watched shorts A B C D cannot appear again using canonical IDs`() {
        // Real YouTube IDs are 11 characters
        val idA = "aaaaaaaaaaa"
        val idB = "bbbbbbbbbbb"
        val idC = "ccccccccccc"
        val idD = "ddddddddddd"
        val idE = "eeeeeeeeeee"
        val idF = "fffffffffff"

        val watchedIds = listOf(
            "https://youtu.be/$idA",
            "https://www.youtube.com/shorts/$idB",
            idC,
            "https://www.youtube.com/watch?v=$idD"
        )
        val canonicalWatchedSet = watchedIds.map { VideoUtils.extractVideoId(it).ifEmpty { it } }.toSet()
        assertEquals(setOf(idA, idB, idC, idD), canonicalWatchedSet)

        // Candidate incoming stream
        val incomingCandidates = listOf(
            createTestVideo(idA, "Short A", "", "User", 30, isShort = true),
            createTestVideo("https://youtu.be/$idB", "Short B", "", "User", 25, isShort = true),
            createTestVideo("https://www.youtube.com/shorts/$idC", "Short C", "", "User", 15, isShort = true),
            createTestVideo(idD, "Short D", "", "User", 45, isShort = true),
            createTestVideo(idE, "Short E", "", "User", 50, isShort = true),
            createTestVideo(idF, "Short F", "", "User", 20, isShort = true)
        )

        val filtered = incomingCandidates.filter { candidate ->
            val canonId = VideoUtils.extractVideoId(candidate.id).ifEmpty { candidate.id }
            VideoUtils.isShort(candidate) && !canonicalWatchedSet.contains(canonId)
        }.map {
            val canonId = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
            it.copy(id = canonId)
        }

        assertEquals(2, filtered.size)
        assertEquals(idE, filtered[0].id)
        assertEquals(idF, filtered[1].id)
        assertFalse("A, B, C, D must not be present in filtered", filtered.any { it.id in setOf(idA, idB, idC, idD) })
    }

    // 4. MIXED FEED INTERLEAVING
    @Test
    fun `test mixed feed positions ShortsCarousel periodically between normal videos`() {
        val normalVideos = (1..10).map { i ->
            createTestVideo("norm_$i", "Normal Video $i", "", "Channel", 300, isShort = false)
        }
        val shorts = (1..10).map { i ->
            createTestVideo("short_$i", "Short $i", "", "Channel", 30, isShort = true)
        }

        val feed = ContextualShortsHelper.buildMixedRelatedFeed(
            normalVideos = normalVideos,
            shorts = shorts,
            shortsChunkSize = 5,
            minShortsPerCarousel = 2
        )

        // Expected order:
        // Index 0: NormalVideo 1
        // Index 1: NormalVideo 2
        // Index 2: ShortsCarousel 0 (5 shorts)
        // Index 3: NormalVideo 3
        // Index 4: NormalVideo 4
        // Index 5: NormalVideo 5
        // Index 6: ShortsCarousel 1 (5 shorts)
        // Index 7: NormalVideo 6
        // ...
        assertTrue(feed[0] is RelatedFeedItem.NormalVideo)
        assertEquals("norm_1", (feed[0] as RelatedFeedItem.NormalVideo).video.id)
        assertTrue(feed[1] is RelatedFeedItem.NormalVideo)
        assertEquals("norm_2", (feed[1] as RelatedFeedItem.NormalVideo).video.id)

        // First carousel after 2 normal videos
        assertTrue("Carousel 0 should be at index 2", feed[2] is RelatedFeedItem.ShortsCarousel)
        val carousel0 = feed[2] as RelatedFeedItem.ShortsCarousel
        assertEquals(5, carousel0.shorts.size)
        assertEquals("short_1", carousel0.shorts[0].id)

        // Then next 3 normal videos
        assertTrue(feed[3] is RelatedFeedItem.NormalVideo)
        assertTrue(feed[4] is RelatedFeedItem.NormalVideo)
        assertTrue(feed[5] is RelatedFeedItem.NormalVideo)

        // Second carousel after next 3 normal videos (index 6)
        assertTrue("Carousel 1 should be at index 6", feed[6] is RelatedFeedItem.ShortsCarousel)
        val carousel1 = feed[6] as RelatedFeedItem.ShortsCarousel
        assertEquals(5, carousel1.shorts.size)
        assertEquals("short_6", carousel1.shorts[0].id)

        // Shorts are not placed all at the bottom or all at the top
        assertTrue(feed.last() is RelatedFeedItem.NormalVideo)
    }

    // 9. STABLE FEED ITEMS & SCROLL STABILITY TEST
    @Test
    fun `test stable keys and stability across scroll up and down`() {
        val normalVideos = (1..10).map { createTestVideo("norm_$it", "Normal Video $it", duration = 300) }
        val shorts = (1..10).map { createTestVideo("short_$it", "Short $it", duration = 30, isShort = true) }

        // Initial build
        val feed = ContextualShortsHelper.buildMixedRelatedFeed(normalVideos, shorts)

        // Verify keys are stable
        val keys = feed.map { it.stableKey }
        assertEquals("normal_norm_1", keys[0])
        assertEquals("normal_norm_2", keys[1])
        assertEquals("shorts_carousel_carousel_0", keys[2])
        assertEquals("normal_norm_3", keys[3])
        assertEquals("normal_norm_4", keys[4])
        assertEquals("normal_norm_5", keys[5])
        assertEquals("shorts_carousel_carousel_1", keys[6])

        // Verify no duplicate keys
        assertEquals("All keys must be strictly unique", keys.size, keys.distinct().size)

        // Simulate scrolling down and then back up (re-evaluating feed with same source lists)
        val feedAfterScroll = ContextualShortsHelper.buildMixedRelatedFeed(normalVideos, shorts)
        assertEquals("Feed item count must not change on scroll", feed.size, feedAfterScroll.size)
        assertEquals("Feed keys must remain identical on scroll", keys, feedAfterScroll.map { it.stableKey })

        // Check that Shorts content inside carousel_0 is unchanged
        val carousel0Before = feed[2] as RelatedFeedItem.ShortsCarousel
        val carousel0After = feedAfterScroll[2] as RelatedFeedItem.ShortsCarousel
        assertEquals(carousel0Before.id, carousel0After.id)
        assertEquals(carousel0Before.shorts.map { it.id }, carousel0After.shorts.map { it.id })

        // Check that no duplicate shorts exist across carousels
        val allShortsInFeed = feed.filterIsInstance<RelatedFeedItem.ShortsCarousel>().flatMap { it.shorts.map { s -> s.id } }
        assertEquals("Shorts must not be duplicated across carousels", allShortsInFeed.size, allShortsInFeed.distinct().size)
    }

    // 8. CURRENT VIDEO PRECLUSION
    @Test
    fun `test current playing video cannot appear in recommendations or carousel`() {
        val currentPlayingId = "currently_playing_123"

        val normalVideos = listOf(
            createTestVideo(currentPlayingId, "Current Video", "", "Creator", 200, isShort = false),
            createTestVideo("other_1", "Other Video 1", "", "Creator", 300, isShort = false)
        )
        val candidateShorts = listOf(
            createTestVideo(currentPlayingId, "Current Video as Short", "", "Creator", 30, isShort = true),
            createTestVideo("short_1", "Other Short 1", "", "Creator", 30, isShort = true)
        )

        val cleanNormalVideos = normalVideos.filter {
            val canon = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
            !VideoUtils.isShort(it) && canon != currentPlayingId
        }
        val cleanShorts = candidateShorts.filter {
            val canon = VideoUtils.extractVideoId(it.id).ifEmpty { it.id }
            VideoUtils.isShort(it) && canon != currentPlayingId
        }

        assertEquals(1, cleanNormalVideos.size)
        assertEquals("other_1", cleanNormalVideos[0].id)
        assertEquals(1, cleanShorts.size)
        assertEquals("short_1", cleanShorts[0].id)
        assertFalse(cleanNormalVideos.any { it.id == currentPlayingId })
        assertFalse(cleanShorts.any { it.id == currentPlayingId })
    }
}
