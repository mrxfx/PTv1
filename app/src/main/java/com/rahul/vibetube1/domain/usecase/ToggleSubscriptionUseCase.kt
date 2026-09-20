/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.data.local.SubscriptionEntity
import com.rahul.vibetube1.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleSubscriptionUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(subscription: SubscriptionEntity) {
        val isSubscribed = repository.isSubscribed(subscription.channelId).first()
        if (isSubscribed) {
            // Use fuzzy delete to ensure both ID and legacy URL records are removed
            repository.unsubscribeByIdFuzzy(subscription.channelId)
        } else {
            repository.subscribe(subscription)
        }
    }
}
