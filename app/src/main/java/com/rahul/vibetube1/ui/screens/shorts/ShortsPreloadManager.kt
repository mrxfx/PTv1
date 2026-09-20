/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.shorts

import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.utils.PTLog
import com.rahul.vibetube1.utils.VideoUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the automatic background preload lifecycle for Shorts playback.
 *
 * Maintains a rolling preload window of the next 3-4 upcoming Shorts in the feed,
 * tracking canonical video IDs for current, queued, already shown, already preloaded,
 * and failed items.
 *
 * All preload operations are strictly single-flight and temporary in memory.
 * No persistent application data or user downloads are touched.
 */
@Singleton
class ShortsPreloadManager @Inject constructor(
    private val videoRepository: VideoRepository
) {
    companion object {
        private const val TAG = "ShortsPreload"
        const val PRELOAD_WINDOW_SIZE = 4
    }

    // Active async preload jobs per canonical ID
    private val preloadJobs = ConcurrentHashMap<String, Job>()

    // Canonical video IDs tracking sets
    private val preloadedIds = ConcurrentHashMap.newKeySet<String>()
    private val shownIds = ConcurrentHashMap.newKeySet<String>()
    private val failedIds = ConcurrentHashMap.newKeySet<String>()
    private val queuedIds = ConcurrentHashMap.newKeySet<String>()
    
    @Volatile
    private var currentVideoId: String? = null

    private fun canonicalize(id: String): String {
        return VideoUtils.extractVideoId(id).ifEmpty { id }
    }

    /**
     * Updates the current Shorts playback position, rolls the preload window forward
     * to prepare the next 3–4 fresh Shorts, and cancels out-of-window preload jobs.
     */
    fun onPageChanged(currentIndex: Int, shorts: List<VideoItem>, scope: CoroutineScope) {
        if (shorts.isEmpty() || currentIndex !in shorts.indices) return

        val currentVideo = shorts[currentIndex]
        val currentCanonicalId = canonicalize(currentVideo.id)
        currentVideoId = currentCanonicalId
        shownIds.add(currentCanonicalId)
        queuedIds.remove(currentCanonicalId)

        PTLog.d(TAG, "[ShortsPreload] Playing canonicalId=$currentCanonicalId at index=$currentIndex")

        // Determine rolling window of next 3–4 items: currentIndex + 1 .. currentIndex + 4
        val windowIndices = (1..PRELOAD_WINDOW_SIZE)
            .map { currentIndex + it }
            .filter { it in shorts.indices }

        val activeWindowCanonicalIds = mutableSetOf<String>()
        activeWindowCanonicalIds.add(currentCanonicalId)

        val windowVideosToPreload = mutableListOf<Pair<Int, VideoItem>>()

        for (idx in windowIndices) {
            val video = shorts[idx]
            val cid = canonicalize(video.id)
            activeWindowCanonicalIds.add(cid)
            windowVideosToPreload.add(Pair(idx - currentIndex, video))
        }

        // Cancel jobs for videos outside the active window
        preloadJobs.keys.toList().forEach { videoId ->
            if (!activeWindowCanonicalIds.contains(videoId)) {
                preloadJobs[videoId]?.cancel()
                preloadJobs.remove(videoId)
                queuedIds.remove(videoId)
                PTLog.d(TAG, "[ShortsPreload] Cancelled out-of-window preload job for id=$videoId")
            }
        }

        // 1. Ensure current video is preloaded/cached if not already done
        if (!preloadedIds.contains(currentCanonicalId) && preloadJobs[currentCanonicalId]?.isActive != true) {
            preloadSingle(currentCanonicalId, currentVideo, priorityOffset = 0, scope = scope)
        }

        // 2. Rolling window background preloads for next 3-4 fresh Shorts
        for ((offset, targetVideo) in windowVideosToPreload) {
            val targetCanonicalId = canonicalize(targetVideo.id)

            // Never preload an ID that is already in the current session's known/preloaded set,
            // already shown, failed, or currently has an active preload job.
            if (preloadedIds.contains(targetCanonicalId)) {
                PTLog.d(TAG, "[ShortsPreload] Skipping already preloaded canonicalId=$targetCanonicalId")
                continue
            }
            if (shownIds.contains(targetCanonicalId)) {
                PTLog.d(TAG, "[ShortsPreload] Skipping already shown canonicalId=$targetCanonicalId")
                continue
            }
            if (failedIds.contains(targetCanonicalId)) {
                PTLog.d(TAG, "[ShortsPreload] Skipping failed canonicalId=$targetCanonicalId")
                continue
            }
            if (preloadJobs[targetCanonicalId]?.isActive == true) {
                PTLog.d(TAG, "[ShortsPreload] Preload job already running for canonicalId=$targetCanonicalId")
                continue
            }

            queuedIds.add(targetCanonicalId)
            preloadSingle(targetCanonicalId, targetVideo, priorityOffset = offset, scope = scope)
        }
    }

    private fun preloadSingle(
        canonicalId: String,
        video: VideoItem,
        priorityOffset: Int,
        scope: CoroutineScope
    ) {
        val job = scope.launch {
            val startTime = System.currentTimeMillis()
            try {
                // Stagger secondary preloads slightly so immediate next gets priority
                if (priorityOffset > 1) {
                    val delayMs = (priorityOffset - 1) * 200L
                    delay(delayMs)
                }

                PTLog.d("SHORTS_STREAM_EXTRACTION", "[ShortsPreload] Preload START: canonicalId=$canonicalId, offset=+$priorityOffset, title='${video.title}'")
                videoRepository.getStreamBundle(canonicalId, forceRefresh = false)

                preloadedIds.add(canonicalId)
                queuedIds.remove(canonicalId)
                failedIds.remove(canonicalId)

                val elapsed = System.currentTimeMillis() - startTime
                PTLog.d("SHORTS_STREAM_EXTRACTION", "[ShortsPreload] Preload SUCCESS in ${elapsed}ms: canonicalId=$canonicalId, offset=+$priorityOffset")
            } catch (e: Exception) {
                queuedIds.remove(canonicalId)
                failedIds.add(canonicalId)
                PTLog.w("SHORTS_STREAM_EXTRACTION", "[ShortsPreload] Preload FAILED for canonicalId=$canonicalId: ${e.message}")
            } finally {
                preloadJobs.remove(canonicalId)
            }
        }
        preloadJobs[canonicalId] = job
    }

    fun markShortAsShown(videoId: String) {
        val cid = canonicalize(videoId)
        shownIds.add(cid)
    }

    fun markShortAsFailed(videoId: String) {
        val cid = canonicalize(videoId)
        failedIds.add(cid)
        queuedIds.remove(cid)
    }

    fun isPreloaded(videoId: String): Boolean {
        return preloadedIds.contains(canonicalize(videoId))
    }

    fun isShown(videoId: String): Boolean {
        return shownIds.contains(canonicalize(videoId))
    }

    fun isQueued(videoId: String): Boolean {
        return queuedIds.contains(canonicalize(videoId))
    }

    fun isFailed(videoId: String): Boolean {
        return failedIds.contains(canonicalize(videoId))
    }

    fun getCurrentVideoId(): String? = currentVideoId

    fun getPreloadedCount(): Int = preloadedIds.size

    /**
     * Clears all in-memory temporary preload state when the Shorts session ends
     * or on application launch.
     */
    fun clear() {
        PTLog.d(TAG, "[ShortsPreload] Clearing Shorts preload session state")
        preloadJobs.values.forEach { it.cancel() }
        preloadJobs.clear()
        preloadedIds.clear()
        shownIds.clear()
        failedIds.clear()
        queuedIds.clear()
        currentVideoId = null
    }
}
