/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateWatchProgressUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(videoId: String, progressMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        if (preferencesManager.isIncognitoMode.first()) return

        val ratio = progressMs.toFloat() / durationMs
        
        // Threshold Logic:
        // 1. If watched less than 10 seconds, don't save progress (don't clutter history with misclicks)
        // 2. If watched more than 95%, mark as fully completed
        val finalProgress = when {
            progressMs < 10000 -> return 
            ratio > 0.95f -> durationMs
            else -> progressMs
        }

        repository.updateWatchProgress(videoId, finalProgress, durationMs)
    }
}
