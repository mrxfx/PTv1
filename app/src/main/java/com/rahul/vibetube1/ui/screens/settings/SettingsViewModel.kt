/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.repository.DownloadRepository
import com.rahul.vibetube1.domain.repository.LibraryRepository
import com.rahul.vibetube1.utils.LocaleUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val cacheCleanerManager: com.rahul.vibetube1.utils.CacheCleanerManager
) : ViewModel() {

    val isAutoCacheCleanerEnabled: StateFlow<Boolean> = preferencesManager.isAutoCacheCleanerEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val cacheCleanerThresholdMb: StateFlow<Long> = preferencesManager.cacheCleanerThresholdMb
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 500L)

    val lastCacheCleanupTime: StateFlow<Long> = preferencesManager.lastCacheCleanupTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _currentCacheSize = MutableStateFlow(0L)
    val currentCacheSize: StateFlow<Long> = _currentCacheSize.asStateFlow()

    private val _isCacheCleaning = MutableStateFlow(false)
    val isCacheCleaning: StateFlow<Boolean> = _isCacheCleaning.asStateFlow()

    private val _cleanerSnackbarMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val cleanerSnackbarMessage = _cleanerSnackbarMessage.asSharedFlow()

    init {
        refreshCacheSize()
    }

    fun refreshCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            _currentCacheSize.value = cacheCleanerManager.calculateCacheSize()
        }
    }

    fun toggleAutoCacheCleaner(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoCacheCleanerEnabled(enabled)
        }
    }

    fun setCacheCleanerThresholdMb(thresholdMb: Long) {
        viewModelScope.launch {
            preferencesManager.setCacheCleanerThresholdMb(thresholdMb)
        }
    }

    fun runManualCacheCleanup() {
        viewModelScope.launch(Dispatchers.IO) {
            _isCacheCleaning.value = true
            try {
                val result = cacheCleanerManager.performCleanup(isManualTrigger = true)
                _currentCacheSize.value = result.currentCacheSize
                val freedStr = com.rahul.vibetube1.utils.StorageUtils.formatSize(result.freedBytes)
                _cleanerSnackbarMessage.emit("Cleanup completed. Freed: $freedStr")
            } catch (e: Exception) {
                com.rahul.vibetube1.utils.PTLog.e("SettingsViewModel", "Manual cache cleanup failed", e)
                _cleanerSnackbarMessage.emit("Cache cleanup failed")
            } finally {
                _isCacheCleaning.value = false
            }
        }
    }

    val isHistoryEnabled: StateFlow<Boolean> = preferencesManager.isHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSearchHistoryPaused: StateFlow<Boolean> = preferencesManager.isSearchHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPipEnabled: StateFlow<Boolean> = preferencesManager.isPipEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBackgroundPlayEnabled: StateFlow<Boolean> = preferencesManager.isBackgroundPlayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isAutoUpdateEnabled: StateFlow<Boolean> = preferencesManager.isAutoUpdateEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isDynamicColorEnabled: StateFlow<Boolean> = preferencesManager.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPlayerGesturesEnabled: StateFlow<Boolean> = preferencesManager.isPlayerGesturesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAmbientModeEnabled: StateFlow<Boolean> = preferencesManager.isAmbientModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isRecommendationsPaused: StateFlow<Boolean> = preferencesManager.isRecommendationsPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val subtitleFontSize: StateFlow<Float> = preferencesManager.subtitleFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)

    val subtitleBackgroundOpacity: StateFlow<Float> = preferencesManager.subtitleBackgroundOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.65f)

    val appLanguage: StateFlow<String?> = preferencesManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val appTheme: StateFlow<String> = preferencesManager.appTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val isAutoplayEnabled: StateFlow<Boolean> = preferencesManager.isAutoplayEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val preferredQuality: StateFlow<String> = preferencesManager.preferredQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")

    val isAnimationsEnabled: StateFlow<Boolean> = preferencesManager.isAnimationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val playbackSpeed: StateFlow<Float> = preferencesManager.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val allDownloads: StateFlow<List<com.rahul.vibetube1.data.local.DownloadEntity>> = downloadRepository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val proxyEnabled: StateFlow<Boolean> = preferencesManager.isProxyEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val proxyHost: StateFlow<String> = preferencesManager.proxyHost
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val proxyPort: StateFlow<Int> = preferencesManager.proxyPort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8080)

    val isSearchGridView: StateFlow<Boolean> = preferencesManager.isSearchGridView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isIncognitoMode: StateFlow<Boolean> = preferencesManager.isIncognitoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availableLocales = LocaleUtils.getAvailableLocales(context)

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHistoryEnabled(enabled)
        }
    }

    fun setSearchHistoryPaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSearchHistoryPaused(paused)
        }
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPipEnabled(enabled)
        }
    }

    fun setBackgroundPlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBackgroundPlayEnabled(enabled)
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoUpdateEnabled(enabled)
        }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDynamicColorEnabled(enabled)
        }
    }

    fun setPlayerGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPlayerGesturesEnabled(enabled)
        }
    }

    fun setAmbientModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAmbientModeEnabled(enabled)
        }
    }

    fun setRecommendationsPaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesManager.setRecommendationsPaused(paused)
        }
    }

    fun setSubtitleFontSize(size: Float) {
        viewModelScope.launch {
            preferencesManager.setSubtitleFontSize(size)
        }
    }

    fun setSubtitleBackgroundOpacity(opacity: Float) {
        viewModelScope.launch {
            preferencesManager.setSubtitleBackgroundOpacity(opacity)
        }
    }

    fun setAppLanguage(tag: String?) {
        viewModelScope.launch {
            preferencesManager.setAppLanguage(tag)
            val appLocale: LocaleListCompat = if (tag == null) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    fun clearLearnedInterests() {
        viewModelScope.launch {
            libraryRepository.clearAllInterests()
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadRepository.clearAllDownloads()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            libraryRepository.clearHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            libraryRepository.clearSearchHistory()
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setAppTheme(theme)
        }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAutoplayEnabled(enabled)
        }
    }

    fun setPreferredQuality(quality: String) {
        viewModelScope.launch {
            preferencesManager.setPreferredQuality(quality)
        }
    }

    fun clearSeenShorts() {
        viewModelScope.launch {
            preferencesManager.clearSeenShorts()
        }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setAnimationsEnabled(enabled)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.setPlaybackSpeed(speed)
        }
    }

    fun setProxySettings(enabled: Boolean, host: String, port: Int) {
        viewModelScope.launch {
            preferencesManager.setProxySettings(enabled, host, port)
        }
    }

    fun setIncognitoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setIncognitoMode(enabled)
        }
    }

    fun setSearchGridView(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSearchGridView(enabled)
        }
    }

    fun resetPlayerSettings() {
        viewModelScope.launch {
            preferencesManager.setAutoplayEnabled(true)
            preferencesManager.setPreferredQuality("Auto")
            preferencesManager.setPlaybackSpeed(1.0f)
            preferencesManager.setSubtitleFontSize(16f)
            preferencesManager.setSubtitleBackgroundOpacity(0.65f)
            preferencesManager.setBackgroundPlayEnabled(false)
            preferencesManager.setPipEnabled(false)
            preferencesManager.setPlayerGesturesEnabled(true)
        }
    }

    fun resetAppearanceSettings() {
        viewModelScope.launch {
            preferencesManager.setAppTheme("System")
            preferencesManager.setDynamicColorEnabled(false)
            preferencesManager.setAmbientModeEnabled(false)
            preferencesManager.setAnimationsEnabled(false)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            resetPlayerSettings()
            resetAppearanceSettings()
            preferencesManager.setAutoUpdateEnabled(false)
            preferencesManager.setHistoryEnabled(true)
            preferencesManager.setSearchHistoryPaused(false)
            preferencesManager.setRecommendationsPaused(false)
            preferencesManager.setProxySettings(false, "", 8080)
            preferencesManager.setIncognitoMode(false)
            preferencesManager.setSearchGridView(true)
            preferencesManager.setAppLanguage(null)
            
            // Note: Does not clear data/histories, only preferences.
        }
    }
}
