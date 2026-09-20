/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.data.local.HistoryEntity
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(history: HistoryEntity) {
        if (preferencesManager.isIncognitoMode.first()) return
        repository.addToHistory(history)
    }
}
