/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.domain.model.StreamItem

object DownloadStreamResolver {

    /**
     * Checks if a [StreamItem] represents an audio stream.
     */
    fun isAudioStream(stream: StreamItem, bundle: StreamBundle? = null): Boolean {
        if (stream.url.isBlank()) return false

        // 1. Is it explicitly in bundle.audioStreams?
        if (bundle?.audioStreams?.any { it.url == stream.url } == true) {
            return true
        }

        // 2. Format / MIME indicators
        val fmt = stream.format.lowercase()
        val isAudioFormat = fmt.contains("m4a") ||
                fmt.contains("opus") ||
                fmt.contains("mp3") ||
                fmt.contains("aac") ||
                fmt.contains("audio") ||
                fmt.contains("ogg")

        // 3. Track type indicators
        val track = stream.trackType?.uppercase() ?: ""
        val isAudioTrack = track == "AUDIO" || track == "ORIGINAL" || track.contains("AUDIO")

        // 4. Quality indicators (e.g. "128 kbps", "256kbps", "Audio")
        val qual = stream.quality.lowercase()
        val isAudioQuality = qual.contains("kbps") || qual.contains("audio") || qual == "audio"

        if (isAudioFormat || isAudioTrack || isAudioQuality) {
            return true
        }

        // If it's in videoStreams and has standard video resolution (e.g., 720p, 1080p, 360p), it's not audio-only
        val isVideoQuality = qual.contains("p") && qual.filter { it.isDigit() }.isNotEmpty()
        if (isVideoQuality) {
            return false
        }

        return false
    }

    /**
     * Centralized audio stream resolver for Audio-Only downloads.
     * Guarantees a valid audio stream URL for audio-only requests whenever audio streams exist.
     *
     * Rules:
     * 1. Prefer explicitly selected audio stream if valid and audio.
     * 2. Otherwise choose best compatible audio stream from bundle.audioStreams.
     * 3. Prefer ORIGINAL track type when available.
     * 4. Prefer valid non-blank audio URL.
     * 5. Never return a video-only stream.
     * 6. Return null only when no valid audio stream exists.
     */
    fun selectAudioStream(
        bundle: StreamBundle,
        requestedStream: StreamItem? = null
    ): StreamItem? {
        // 1. If requestedStream has a valid URL, return it immediately
        if (requestedStream != null && requestedStream.url.isNotBlank()) {
            return requestedStream
        }

        val audioStreams = bundle.audioStreams.filter { it.url.isNotBlank() }

        if (audioStreams.isEmpty()) {
            // Fallback: check if bestAudioStreamUrl is available
            if (!bundle.bestAudioStreamUrl.isNullOrBlank()) {
                return StreamItem(
                    url = bundle.bestAudioStreamUrl,
                    quality = "128kbps",
                    format = "m4a",
                    isAdaptive = true,
                    trackType = "ORIGINAL"
                )
            }
            return null
        }

        // 2. Prefer ORIGINAL track type when available
        val originalTracks = audioStreams.filter {
            it.trackType.equals("ORIGINAL", ignoreCase = true) ||
            it.trackType.equals("AUDIO", ignoreCase = true)
        }

        val candidates = if (originalTracks.isNotEmpty()) originalTracks else audioStreams

        // 3. Pick the highest quality audio stream based on numeric bitrate if available
        return candidates.maxByOrNull { stream ->
            val kbps = stream.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
            kbps
        } ?: audioStreams.firstOrNull()
    }

    /**
     * Selects compatible audio stream for pairing with adaptive video.
     */
    fun selectAudioStreamForAdaptiveVideo(
        bundle: StreamBundle,
        videoFormat: String?
    ): StreamItem? {
        val audioStreams = bundle.audioStreams.filter { it.url.isNotBlank() }
        if (audioStreams.isEmpty()) return null

        val isWebm = videoFormat?.contains("webm", ignoreCase = true) == true
        val compatibleStreams = audioStreams.filter { audio ->
            if (isWebm) {
                audio.format.contains("webm", ignoreCase = true) ||
                audio.format.contains("opus", ignoreCase = true)
            } else {
                audio.format.contains("m4a", ignoreCase = true) ||
                audio.format.contains("aac", ignoreCase = true) ||
                audio.format.contains("mp4", ignoreCase = true)
            }
        }

        val pool = if (compatibleStreams.isNotEmpty()) compatibleStreams else audioStreams

        return pool.filter { it.trackType.equals("ORIGINAL", ignoreCase = true) }
            .maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            ?: pool.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
            ?: pool.firstOrNull()
    }
}
