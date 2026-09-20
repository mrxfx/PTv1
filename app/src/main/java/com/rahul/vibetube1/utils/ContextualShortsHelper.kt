/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import com.rahul.vibetube1.domain.model.RelatedFeedItem
import com.rahul.vibetube1.domain.model.VideoItem

object ContextualShortsHelper {

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of",
        "video", "official", "music", "lyrics", "audio", "song", "songs", "hd", "4k", "remastered", "full", "episode",
        "teaser", "trailer", "visualizer", "reaction", "review", "premiere", "youtube", "live", "performance", "clip", "clips",
        "feat", "ft", "prod", "by", "version"
    )

    private val CHANNEL_NOISE_SUFFIXES = Regex("(?i)(\\s*-\\s*topic|\\s+official|\\s+vevo|\\s+records|\\s+music|\\s+tv|\\s+channel|\\s+clips|\\s+media|\\s+studios|\\s+films|\\s+entertainment)")

    fun cleanUploaderName(uploaderName: String): String {
        return uploaderName.replace(CHANNEL_NOISE_SUFFIXES, "").trim()
    }

    fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .replace(Regex("\\([^\\)]*\\)"), " ")
            .replace(Regex("(?i)\\b(official\\s+(music\\s+)?video|lyric\\s+video|full\\s+(video\\s+)?song|audio\\s+song|4k\\s*(60fps)?|1080p|remastered|live\\s+performance|visualizer|teaser|trailer)\\b"), " ")
            .replace(Regex("[#@]\\w+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Builds ranked contextual search queries using the current video's available metadata.
     * Prioritizes topic, artist/channel, song/music, relevant keywords, and related content.
     * Guaranteed not to default to a generic/global feed.
     */
    fun buildContextualQueries(
        title: String,
        uploaderName: String = "",
        uploaderUrl: String? = null,
        description: String? = null
    ): List<String> {
        val queries = mutableListOf<String>()
        val cleanedTitle = cleanTitle(title)
        val cleanedUploader = cleanUploaderName(uploaderName)

        // Split title into natural semantic segments (e.g. "Song Name | Artist | Movie")
        val segments = cleanedTitle
            .split(Regex("[|—–/•:~]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 1 }

        val primarySegment = segments.firstOrNull() ?: cleanedTitle

        // 1. High-priority contextual combination: Artist/Creator + Primary Song/Topic
        if (cleanedUploader.isNotBlank() && primarySegment.isNotBlank()) {
            if (!primarySegment.contains(cleanedUploader, ignoreCase = true)) {
                queries.add("$cleanedUploader $primarySegment")
            } else {
                queries.add(primarySegment)
            }
        }

        // 2. Artist/Channel direct query:
        // When searched with DurationFilter.SHORT, this specifically fetches YouTube Shorts by or about this artist/channel!
        if (cleanedUploader.isNotBlank() && cleanedUploader.length > 2) {
            queries.add(cleanedUploader)
        }

        // 3. Multi-segment title query (e.g. Song + Movie / Topic)
        if (segments.size >= 2) {
            val combinedSegments = segments.take(2).joinToString(" ")
            queries.add(combinedSegments)
            val secondarySegment = segments[1]
            if (secondarySegment.length > 2 && !secondarySegment.equals(cleanedUploader, ignoreCase = true)) {
                queries.add(secondarySegment)
            }
        } else if (cleanedTitle.isNotBlank()) {
            queries.add(cleanedTitle)
        }

        // 4. Extract entities from description (e.g. "Singer: Shreya Ghoshal", "Music: ...", "Starring: ...")
        if (!description.isNullOrBlank()) {
            val descriptionLines = description.lines().take(25)
            for (line in descriptionLines) {
                val match = Regex("(?i)(singer|artist|music|starring|song)\\s*[:\\-]\\s*([^,\\n|]+)").find(line)
                if (match != null) {
                    val entity = match.groupValues[2].trim()
                    if (entity.length in 3..40) {
                        queries.add(entity)
                    }
                }
            }
        }

        // 5. Significant topic keywords from title
        val titleWords = cleanedTitle
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length > 2 && it.lowercase() !in STOP_WORDS }

        if (titleWords.size >= 2) {
            val keywordPhrase = titleWords.take(4).joinToString(" ")
            queries.add(keywordPhrase)
        }

        // 6. Graceful broader contextual fallback (never generic #shorts)
        if (cleanedUploader.isNotBlank()) {
            queries.add("$cleanedUploader shorts")
        }
        if (primarySegment.isNotBlank()) {
            queries.add("$primarySegment shorts")
        }

        return queries
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length > 2 }
            .distinctBy { it.lowercase() }
    }

    /**
     * Weaves normal related videos and horizontal Shorts carousels into a single unified mixed feed.
     * Periodic insertion:
     * - After first 2 normal videos -> First ShortsCarousel
     * - After next 3 normal videos -> Second ShortsCarousel
     * - After every 3-4 normal videos thereafter -> Next ShortsCarousel
     * Each carousel receives its own unique non-overlapping slice of shorts.
     */
    fun buildMixedRelatedFeed(
        normalVideos: List<VideoItem>,
        shorts: List<VideoItem>,
        shortsChunkSize: Int = 5,
        minShortsPerCarousel: Int = 2
    ): List<RelatedFeedItem> {
        if (normalVideos.isEmpty() && shorts.isEmpty()) return emptyList()

        if (normalVideos.isEmpty()) {
            val chunks = shorts.chunked(shortsChunkSize).filter { it.size >= minShortsPerCarousel }
            return chunks.mapIndexed { index, chunk ->
                RelatedFeedItem.ShortsCarousel("carousel_$index", chunk)
            }
        }

        if (shorts.size < minShortsPerCarousel) {
            return normalVideos.map { RelatedFeedItem.NormalVideo(it) }
        }

        val result = mutableListOf<RelatedFeedItem>()
        val shortsChunks = shorts.chunked(shortsChunkSize).filter { it.size >= minShortsPerCarousel }
        var chunkIndex = 0

        // Periodic intervals: 2, 3, 4, 4, 4...
        val intervals = listOf(2, 3, 4, 4, 4, 5)
        var intervalIndex = 0
        var videosSinceLastCarousel = 0

        for (video in normalVideos) {
            result.add(RelatedFeedItem.NormalVideo(video))
            videosSinceLastCarousel++

            val requiredCount = if (intervalIndex < intervals.size) {
                intervals[intervalIndex]
            } else {
                4
            }

            if (videosSinceLastCarousel >= requiredCount && chunkIndex < shortsChunks.size) {
                val chunk = shortsChunks[chunkIndex]
                result.add(RelatedFeedItem.ShortsCarousel("carousel_$chunkIndex", chunk))
                chunkIndex++
                videosSinceLastCarousel = 0
                intervalIndex++
            }
        }

        // If normalVideos was short (e.g. only 1 or 2 items) and chunk 0 hasn't been inserted yet:
        if (chunkIndex == 0 && shortsChunks.isNotEmpty()) {
            result.add(RelatedFeedItem.ShortsCarousel("carousel_0", shortsChunks[0]))
        }

        return result
    }
}
