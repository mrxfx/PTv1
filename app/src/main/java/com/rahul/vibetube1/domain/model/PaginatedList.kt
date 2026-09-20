/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.model

import org.schabi.newpipe.extractor.Page
import androidx.annotation.Keep

@Keep
data class PaginatedList<T>(
    val items: List<T>,
    val nextPage: Page?
)
