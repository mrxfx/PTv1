/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.network

import com.rahul.vibetube1.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

class YouTubeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()
        val headers = request.headers()
        val data = request.dataToSend()

        val okHttpRequestBuilder = OkHttpRequest.Builder()
            .url(url)
            .method(method, data?.toRequestBody())
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("User-Agent", Constants.DEFAULT_USER_AGENT)
            .addHeader("sec-ch-ua", "\"Chromium\";v=\"122\", \"Not(A:Brand\";v=\"24\", \"Google Chrome\";v=\"122\"")
            .addHeader("sec-ch-ua-mobile", "?1")
            .addHeader("sec-ch-ua-platform", "\"Android\"")

        val mergedHeaders = headers.toMutableMap()

        // Bypass YouTube Consent/GDPR redirection and bot heuristics
        // Merge with existing cookies if present
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            val existingCookies = mergedHeaders["Cookie"] ?: emptyList()
            if (existingCookies.none { it.contains("CONSENT=YES") }) {
                val newCookies = existingCookies.toMutableList().apply { 
                    add(Constants.YouTube.CONSENT_COOKIE) 
                    add("PREF=f6=40000&f7=4100&hl=en&tz=UTC")
                }
                mergedHeaders["Cookie"] = newCookies
            }
        }

        mergedHeaders.forEach { (key, values) ->
            values.forEach { value ->
                okHttpRequestBuilder.addHeader(key, value)
            }
        }

        val maxRetries = 2
        var attempt = 0
        var lastResponse: okhttp3.Response? = null
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val okHttpResponse = client.newCall(okHttpRequestBuilder.build()).execute()
                
                // If it's a 403 or 429, it might be an IP block or temporary throttle.
                if (okHttpResponse.code == 403 || okHttpResponse.code == 429) {
                    okHttpResponse.close()
                    attempt++
                    if (attempt <= maxRetries) {
                        Thread.sleep(1000L * attempt) // Linear backoff
                        continue
                    }
                }
                
                lastResponse = okHttpResponse
                break
            } catch (e: IOException) {
                lastException = e
                attempt++
                if (attempt <= maxRetries) {
                    Thread.sleep(500L * attempt)
                }
            }
        }

        val response = lastResponse ?: throw lastException ?: IOException("Request failed after $maxRetries retries")

        val responseBody = response.body?.string()
        val responseCode = response.code
        val responseMessage = response.message
        val responseHeaders = response.headers.toMultimap()

        return Response(responseCode, responseMessage, responseHeaders, responseBody, url)
    }
}
