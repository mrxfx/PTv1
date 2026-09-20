/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(favorite: FavoriteEntity) {
        val isFavorite = repository.isFavorite(favorite.videoId).first()
        if (isFavorite) {
            repository.removeFromFavorites(favorite)
        } else {
            repository.addToFavorites(favorite)
        }
    }
}
