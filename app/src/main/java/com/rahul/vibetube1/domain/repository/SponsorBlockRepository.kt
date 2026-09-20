/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import com.rahul.vibetube1.domain.model.SponsorSegment

interface SponsorBlockRepository {
    suspend fun getSponsorSegments(videoId: String): Result<List<SponsorSegment>>
}
