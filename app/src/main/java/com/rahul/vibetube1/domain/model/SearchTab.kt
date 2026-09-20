/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable

@Keep
@Immutable
enum class SearchTab {
    ALL,
    SHORTS,
    VIDEOS,
    CHANNELS
}
