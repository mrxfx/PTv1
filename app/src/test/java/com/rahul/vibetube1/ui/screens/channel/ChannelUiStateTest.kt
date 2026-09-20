/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.channel

import com.rahul.vibetube1.domain.model.ChannelDetails
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelUiStateTest {

    @Test
    fun loadingState_isInstanceOfChannelUiState() {
        val state: ChannelUiState = ChannelUiState.Loading
        assertTrue(state is ChannelUiState.Loading)
    }

    @Test
    fun successState_preservesDetailsAndPaginationFlag() {
        val dummyDetails = ChannelDetails(
            id = "@testchannel",
            name = "Test Channel",
            avatarUrl = "https://example.com/avatar.jpg",
            bannerUrl = "https://example.com/banner.jpg",
            description = "Channel description",
            subscriberCount = 1000L,
            videos = emptyList()
        )

        val state = ChannelUiState.Success(
            details = dummyDetails,
            isFetchingNextPage = true,
            sortMode = ChannelVideoSortMode.POPULAR
        )

        assertEquals(true, state.isFetchingNextPage)
        assertEquals("Test Channel", state.details.name)
        assertEquals("@testchannel", state.details.id)
        assertEquals(ChannelVideoSortMode.POPULAR, state.sortMode)
    }

    @Test
    fun defaultSortMode_isLatest() {
        val dummyDetails = ChannelDetails(
            id = "@testchannel",
            name = "Test Channel",
            avatarUrl = "",
            bannerUrl = null,
            description = "",
            subscriberCount = null,
            videos = emptyList()
        )

        val state = ChannelUiState.Success(details = dummyDetails)
        assertEquals(ChannelVideoSortMode.LATEST, state.sortMode)
    }
}
