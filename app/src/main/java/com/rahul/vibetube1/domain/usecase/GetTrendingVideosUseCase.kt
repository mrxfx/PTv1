/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.model.PaginatedList
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

class GetTrendingVideosUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(): Result<PaginatedList<VideoItem>> {
        return try {
            // NewPipe usually fetches trending/kiosk as the initial page.
            // We'll need a way in Repository to fetch the initial trending page with its Page token.
            Result.success(repository.getTrendingVideos())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchNextPage(page: Page): Result<PaginatedList<VideoItem>> {
        return try {
            Result.success(repository.fetchNextTrendingPage(page))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
