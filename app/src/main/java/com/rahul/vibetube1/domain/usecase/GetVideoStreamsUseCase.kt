/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GetVideoStreamsUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(videoId: String, forceRefresh: Boolean = false): Result<StreamBundle> {
        return try {
            Result.success(repository.getStreamBundle(videoId, forceRefresh))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
