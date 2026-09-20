/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.data.local.PlaylistFavoriteEntity
import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TogglePlaylistFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(favorite: PlaylistFavoriteEntity) {
        val isFavorite = repository.isPlaylistFavorite(favorite.playlistId).first()
        if (isFavorite) {
            repository.removeFromPlaylistFavorites(favorite)
        } else {
            repository.addToPlaylistFavorites(favorite)
        }
    }
}
