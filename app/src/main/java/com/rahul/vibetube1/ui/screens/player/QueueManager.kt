/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor() {
    private val _skipToNextEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val skipToNextEvent = _skipToNextEvent.asSharedFlow()

    private val _skipToPreviousEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val skipToPreviousEvent = _skipToPreviousEvent.asSharedFlow()

    fun skipToNext() {
        _skipToNextEvent.tryEmit(Unit)
    }

    fun skipToPrevious() {
        _skipToPreviousEvent.tryEmit(Unit)
    }
}
