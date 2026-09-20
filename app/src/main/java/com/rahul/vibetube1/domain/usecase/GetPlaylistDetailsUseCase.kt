/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.model.PlaylistDetails
import com.rahul.vibetube1.domain.repository.VideoRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class GetPlaylistDetailsUseCase @Inject constructor(
    private val repository: VideoRepository
) {
    suspend operator fun invoke(playlistUrl: String): Result<PlaylistDetails> {
        return try {
            Result.success(repository.getPlaylistDetails(playlistUrl))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
