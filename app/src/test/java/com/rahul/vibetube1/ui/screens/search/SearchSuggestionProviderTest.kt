/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.search

import com.rahul.vibetube1.data.network.NewPipeInitializer
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchSuggestionProviderTest {

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var newPipeInitializer: NewPipeInitializer
    private lateinit var provider: SearchSuggestionProvider

    @Before
    fun setup() {
        okHttpClient = OkHttpClient()
        newPipeInitializer = NewPipeInitializer(okHttpClient)
        provider = SearchSuggestionProvider(okHttpClient, newPipeInitializer)
    }

    @Test
    fun parseSuggestionsJson_standardFirefoxFormat_parsesCorrectly() {
        val json = """["lofi", ["lofi hip hop", "lofi beats", "lofi study"]]"""
        val result = provider.parseSuggestionsJson(json)
        assertEquals(listOf("lofi hip hop", "lofi beats", "lofi study"), result)
    }

    @Test
    fun parseSuggestionsJson_nestedArrayFormat_parsesCorrectly() {
        val json = """["kotlin", [["kotlin android", 0], ["kotlin coroutines", 0], ["kotlin tutorial", 0]]]"""
        val result = provider.parseSuggestionsJson(json)
        assertEquals(listOf("kotlin android", "kotlin coroutines", "kotlin tutorial"), result)
    }

    @Test
    fun parseSuggestionsJson_jsonpWrapped_parsesCorrectly() {
        val json = """window.google.ac.h(["chill", ["chill vibes", "chill music"]]);"""
        val result = provider.parseSuggestionsJson(json)
        assertEquals(listOf("chill vibes", "chill music"), result)
    }

    @Test
    fun parseSuggestionsJson_googleAcFormat_parsesCorrectly() {
        val json = """google.ac.h(["music", ["music 2026", "music relax"]])"""
        val result = provider.parseSuggestionsJson(json)
        assertEquals(listOf("music 2026", "music relax"), result)
    }

    @Test
    fun parseSuggestionsJson_emptyOrInvalid_returnsEmptyList() {
        assertTrue(provider.parseSuggestionsJson("").isEmpty())
        assertTrue(provider.parseSuggestionsJson("invalid json").isEmpty())
        assertTrue(provider.parseSuggestionsJson("[]").isEmpty())
    }

    @Test
    fun getSuggestions_emptyQuery_returnsEmptyList() = runBlocking {
        val result = provider.getSuggestions("   ")
        assertTrue(result.isEmpty())
    }
}
