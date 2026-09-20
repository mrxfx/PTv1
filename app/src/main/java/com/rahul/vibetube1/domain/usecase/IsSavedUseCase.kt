/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsSavedUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(videoId: String): Flow<Boolean> = repository.isVideoInAnyLocalPlaylist(videoId)
}
