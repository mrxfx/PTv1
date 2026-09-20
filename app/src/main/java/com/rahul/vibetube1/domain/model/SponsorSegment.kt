/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import kotlinx.serialization.Serializable
import androidx.annotation.Keep

@Keep
@Serializable
data class SponsorSegment(
    val category: String,
    val segment: List<Float>, // [start, end]
    val UUID: String
) {
    val startMs: Long get() = (segment.getOrNull(0) ?: 0f).times(1000).toLong()
    val endMs: Long get() = (segment.getOrNull(1) ?: 0f).times(1000).toLong()
}
