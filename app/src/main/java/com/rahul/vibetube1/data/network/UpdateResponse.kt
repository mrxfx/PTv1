/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.Json

@Serializable
data class ReleaseNotesInfo(
    val title: String? = null,
    val items: List<String>? = null
)

@Serializable
data class VersionInfo(
    @SerialName("versionName")
    val versionName: String? = null,
    @SerialName("version")
    val version: String? = null,
    @SerialName("tag_name")
    val tagName: String? = null,
    @SerialName("versionCode")
    val versionCode: Int? = null,
    @SerialName("version_code")
    val legacyVersionCode: Int? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("release_notes")
    val releaseNotesLegacy: JsonElement? = null,
    @SerialName("releaseNotes")
    val releaseNotesNew: JsonElement? = null,
    @SerialName("changelog")
    val changelog: String? = null,
    @SerialName("body")
    val body: String? = null,
    @SerialName("downloadUrl")
    val downloadUrl: String? = null,
    @SerialName("download_url")
    val legacyDownloadUrl: String? = null,
    @SerialName("update_url")
    val updateUrl: String? = null,
    @SerialName("url")
    val url: String? = null,
    @SerialName("html_url")
    val htmlUrl: String? = null,
    @SerialName("mandatory")
    val mandatory: Boolean = false
) {
    val resolvedVersionName: String
        get() = (versionName ?: version ?: tagName ?: "").removePrefix("v").trim()

    val resolvedVersionCode: Int?
        get() = versionCode ?: legacyVersionCode

    val resolvedTitle: String
        get() = title ?: (if (resolvedVersionName.isNotEmpty()) "ZyvoTube v$resolvedVersionName" else "ZyvoTube")

    val parsedReleaseNotes: ReleaseNotesInfo?
        get() {
            val element = releaseNotesNew ?: releaseNotesLegacy ?: return null
            return try {
                when (element) {
                    is JsonObject -> {
                        val title = element["title"]?.jsonPrimitive?.contentOrNull
                        val items = element["items"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                        ReleaseNotesInfo(title, items)
                    }
                    is JsonPrimitive -> {
                        val content = element.content
                        if (content.isNotBlank()) {
                            ReleaseNotesInfo(title = "What's New", items = listOf(content))
                        } else null
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

    val resolvedMessage: String
        get() {
            val notesObj = parsedReleaseNotes
            if (notesObj != null) {
                return try {
                    Json.encodeToString(ReleaseNotesInfo.serializer(), notesObj)
                } catch (e: Exception) {
                    message ?: changelog ?: body ?: ""
                }
            }
            return message ?: changelog ?: body ?: ""
        }

    val resolvedDownloadUrl: String
        get() = downloadUrl ?: legacyDownloadUrl ?: updateUrl ?: url ?: htmlUrl ?: "https://github.com/RahulHaldar/ZyvoTube/releases/latest"
}

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String = "",
    @SerialName("body")
    val body: String = "",
    @SerialName("html_url")
    val htmlUrl: String = "",
    @SerialName("assets")
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    @SerialName("name")
    val name: String = "",
    @SerialName("browser_download_url")
    val browserDownloadUrl: String = "",
    @SerialName("size")
    val size: Long = 0L
)

