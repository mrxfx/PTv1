/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchResultMetadataTest {

    @Test
    fun formatSearchResultViews_formatsCorrectly() {
        assertEquals("1.2M views", formatSearchResultViews(1_200_000L))
        assertEquals("850K views", formatSearchResultViews(850_000L))
        assertEquals("56K views", formatSearchResultViews(56_000L))
        assertEquals("3,295 views", formatSearchResultViews(3_295L))
        assertEquals("1 view", formatSearchResultViews(1L))
        assertEquals("0 views", formatSearchResultViews(0L))
        assertNull(formatSearchResultViews(-1L))
        assertNull(formatSearchResultViews(null))
    }

    @Test
    fun formatSearchResultPublishedDate_formatsCorrectly() {
        assertEquals("2 hours ago", formatSearchResultPublishedDate("2 hours ago"))
        assertEquals("3 days ago", formatSearchResultPublishedDate("3 days ago"))
        assertEquals("18 Aug 2026", formatSearchResultPublishedDate("18 Aug 2026"))
        assertEquals("18 Aug 2026", formatSearchResultPublishedDate("2026-08-18"))
        assertNull(formatSearchResultPublishedDate(null))
        assertNull(formatSearchResultPublishedDate(""))
        assertNull(formatSearchResultPublishedDate("   "))
    }

    @Test
    fun formatSearchResultSecondaryMetadata_combinesViewsAndDateProperly() {
        // Both views and date available
        assertEquals(
            "1.2M views • 18 Aug 2026",
            formatSearchResultSecondaryMetadata(1_200_000L, "18 Aug 2026")
        )

        // Only views available
        assertEquals(
            "850K views",
            formatSearchResultSecondaryMetadata(850_000L, null)
        )

        // Only date available
        assertEquals(
            "3 days ago",
            formatSearchResultSecondaryMetadata(-1L, "3 days ago")
        )

        // Neither available (views missing and date missing)
        assertNull(formatSearchResultSecondaryMetadata(-1L, null))
        assertNull(formatSearchResultSecondaryMetadata(null, ""))
    }
}
