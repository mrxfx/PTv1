/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.search

import androidx.annotation.VisibleForTesting
import com.rahul.vibetube1.data.network.NewPipeInitializer
import com.rahul.vibetube1.utils.PTLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import com.google.gson.JsonParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.schabi.newpipe.extractor.ServiceList
import java.io.IOException
import java.net.URLEncoder
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SearchSuggestionProvider @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val newPipeInitializer: NewPipeInitializer
) {
    // In-memory cache for fast, instant response when typing/backspacing
    private val cache = object : LinkedHashMap<String, List<String>>(150, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?): Boolean {
            return size > 150
        }
    }

    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        // 1. Check in-memory cache
        synchronized(cache) {
            cache[trimmed.lowercase()]?.let { return@withContext it }
        }

        // 2. Fetch directly from YouTube's official real-time suggestion API
        val directResults = fetchFromYouTubeSuggestApi(trimmed)
        if (directResults.isNotEmpty()) {
            val distinctResults = directResults.distinct()
            synchronized(cache) {
                cache[trimmed.lowercase()] = distinctResults
            }
            return@withContext distinctResults
        }

        // 3. Fallback to NewPipe Extractor's suggestion extractor
        try {
            newPipeInitializer.ensureInitialized()
            val extractorResults = ServiceList.YouTube.suggestionExtractor.suggestionList(trimmed)
            if (!extractorResults.isNullOrEmpty()) {
                val distinctResults = extractorResults.distinct()
                synchronized(cache) {
                    cache[trimmed.lowercase()] = distinctResults
                }
                return@withContext distinctResults
            }
        } catch (e: Exception) {
            PTLog.w("SearchSuggestionProvider", "Extractor suggestions failed for '$trimmed': ${e.message}")
        }

        emptyList()
    }

    private suspend fun fetchFromYouTubeSuggestApi(query: String): List<String> {
        val encodedQuery = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            query
        }

        // Use YouTube's suggestion service with JSON output (client=firefox, ds=yt)
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encodedQuery"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json")
            .build()

        return try {
            suspendCancellableCoroutine { continuation ->
                val call = okHttpClient.newCall(request)
                continuation.invokeOnCancellation {
                    call.cancel()
                }

                call.enqueue(object : Callback {
                    override fun onResponse(call: Call, response: Response) {
                        response.use { resp ->
                            if (!resp.isSuccessful) {
                                continuation.resume(emptyList())
                                return
                            }
                            val body = resp.body?.string()
                            if (body.isNullOrBlank()) {
                                continuation.resume(emptyList())
                                return
                            }
                            val parsed = parseSuggestionsJson(body)
                            continuation.resume(parsed)
                        }
                    }

                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(emptyList())
                    }
                })
            }
        } catch (e: Exception) {
            PTLog.w("SearchSuggestionProvider", "YouTube suggest API request failed: ${e.message}")
            emptyList()
        }
    }

    @VisibleForTesting
    internal fun parseSuggestionsJson(body: String): List<String> {
        val results = mutableListOf<String>()
        try {
            var cleanJson = body.trim()
            if (cleanJson.startsWith("window.google.ac.h(")) {
                cleanJson = cleanJson.removePrefix("window.google.ac.h(")
                if (cleanJson.endsWith(");")) {
                    cleanJson = cleanJson.removeSuffix(");")
                } else if (cleanJson.endsWith(")")) {
                    cleanJson = cleanJson.removeSuffix(")")
                }
            } else if (cleanJson.startsWith("google.ac.h(")) {
                cleanJson = cleanJson.removePrefix("google.ac.h(")
                if (cleanJson.endsWith(");")) {
                    cleanJson = cleanJson.removeSuffix(");")
                } else if (cleanJson.endsWith(")")) {
                    cleanJson = cleanJson.removeSuffix(")")
                }
            }

            val jsonElement = JsonParser.parseString(cleanJson)
            if (jsonElement.isJsonArray) {
                val rootArray = jsonElement.asJsonArray
                // JSON format: [query, [sug1, sug2, ...]]
                if (rootArray.size() >= 2 && rootArray[1].isJsonArray) {
                    val suggestionsArray = rootArray[1].asJsonArray
                    for (item in suggestionsArray) {
                        if (item.isJsonPrimitive) {
                            val text = item.asString
                            if (text.isNotBlank()) results.add(text)
                        } else if (item.isJsonArray) {
                            val innerArr = item.asJsonArray
                            if (innerArr.size() > 0 && innerArr[0].isJsonPrimitive) {
                                val text = innerArr[0].asString
                                if (text.isNotBlank()) results.add(text)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            PTLog.w("SearchSuggestionProvider", "Failed to parse YouTube suggestions: ${e.message}")
        }
        return results
    }
}

