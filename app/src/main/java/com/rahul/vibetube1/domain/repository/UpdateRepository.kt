/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import kotlinx.coroutines.flow.StateFlow
import java.io.File

enum class UpdateStatus {
    CHECKING,
    UP_TO_DATE,
    OPTIONAL_UPDATE,
    MANDATORY_UPDATE,
    FAILED
}

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : UpdateDownloadState
    data class Downloaded(
        val apkFile: File,
        val versionName: String,
        val versionCode: Int
    ) : UpdateDownloadState
    data class Failed(
        val error: String,
        val canRetry: Boolean = true
    ) : UpdateDownloadState
}

data class UpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val latestVersionCode: Int = 0,
    val mandatory: Boolean = false,
    val releaseNotes: String = "",
    val updateUrl: String = "",
    val downloadUrl: String = "",
    val title: String = "",
    val status: UpdateStatus = UpdateStatus.UP_TO_DATE
) {
    val parsedReleaseNotes: com.rahul.vibetube1.data.network.ReleaseNotesInfo?
        get() {
            if (releaseNotes.isBlank()) return null
            return try {
                kotlinx.serialization.json.Json.decodeFromString<com.rahul.vibetube1.data.network.ReleaseNotesInfo>(releaseNotes)
            } catch (e: Exception) {
                com.rahul.vibetube1.data.network.ReleaseNotesInfo(title = "What's New", items = listOf(releaseNotes))
            }
        }
}

interface UpdateRepository {
    val updateInfo: StateFlow<UpdateInfo>
    val downloadState: StateFlow<UpdateDownloadState>

    suspend fun checkForUpdates(force: Boolean = false)
    suspend fun startApkDownload()
    fun retryDownload()
    fun canRequestPackageInstalls(): Boolean
    fun openInstallPermissionSettings()
    fun installDownloadedApk(): Result<Unit>
    fun getValidDownloadedApk(versionName: String, versionCode: Int): File?
    fun checkExistingDownloadedApk()
}
