/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.repository.UpdateDownloadState
import com.rahul.vibetube1.domain.repository.UpdateInfo
import com.rahul.vibetube1.domain.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val updateInfo: StateFlow<UpdateInfo> = updateRepository.updateInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpdateInfo())

    val downloadState: StateFlow<UpdateDownloadState> = updateRepository.downloadState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpdateDownloadState.Idle)

    val isAutoUpdateEnabled: StateFlow<Boolean> = preferencesManager.isAutoUpdateEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val lastDismissedVersionCode: StateFlow<Int> = preferencesManager.lastDismissedUpdateVersionCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            // Check for updates on startup - always run (force = true) to ensure fresh check
            updateRepository.checkForUpdates(force = true)
        }
    }

    fun dismissUpdate(versionCode: Int) {
        viewModelScope.launch {
            preferencesManager.setLastDismissedUpdateVersionCode(versionCode)
        }
    }

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch {
            updateRepository.checkForUpdates(force = force)
        }
    }

    fun startDownload() {
        viewModelScope.launch {
            updateRepository.startApkDownload()
        }
    }

    fun retryDownload() {
        updateRepository.retryDownload()
    }

    fun installUpdate(): Result<Unit> {
        return updateRepository.installDownloadedApk()
    }

    fun canRequestPackageInstalls(): Boolean {
        return updateRepository.canRequestPackageInstalls()
    }

    fun openInstallSettings() {
        updateRepository.openInstallPermissionSettings()
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoUpdateEnabled(enabled)
            if (enabled) {
                updateRepository.checkForUpdates(force = true)
            }
        }
    }
}
