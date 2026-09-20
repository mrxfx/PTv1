/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
open class PreferencesManager(
    dataStoreProvider: (() -> DataStore<Preferences>)? = null
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({ context.dataStore })

    private val dataStore: DataStore<Preferences> by lazy {
        dataStoreProvider?.invoke() ?: object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = emptyPreferences()
        }
    }

    open val isHistoryEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[HISTORY_ENABLED] ?: true
        }

    open val isSearchHistoryPaused: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SEARCH_HISTORY_PAUSED] ?: false
        }

    open val isPipEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PIP_ENABLED] ?: false
        }

    open val isBackgroundPlayEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] ?: true
        }

    open val isAnimationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[ANIMATIONS_ENABLED] ?: false
        }

    open val isSubtitlesEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SUBTITLES_ENABLED] ?: false
        }

    open val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[ONBOARDING_COMPLETED] ?: false
        }

    open val isSearchGridView: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[SEARCH_GRID_VIEW] ?: false
        }

    open val isAutoUpdateEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[AUTO_UPDATE_ENABLED] ?: false
        }

    open val isDynamicColorEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[DYNAMIC_COLOR_ENABLED] ?: false
        }

    open val isRecommendationsPaused: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[RECOMMENDATIONS_PAUSED] ?: false
        }

    open val isAutoplayEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[AUTOPLAY_ENABLED] ?: true
        }

    open val isIncognitoMode: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[INCOGNITO_MODE] ?: false
        }

    open val isAutoCacheCleanerEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[AUTO_CACHE_CLEANER_ENABLED] ?: true
        }

    open val cacheCleanerThresholdMb: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[CACHE_CLEANER_THRESHOLD_MB] ?: 500L
        }

    open val lastCacheCleanupTime: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[LAST_CACHE_CLEANUP_TIME] ?: 0L
        }

    open val isPlayerGesturesEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PLAYER_GESTURES_ENABLED] ?: true
        }

    open val preferredSubtitleLanguage: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PREFERRED_SUBTITLE_LANGUAGE]
        }

    open val preferredQuality: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[PREFERRED_QUALITY] ?: "Auto"
        }

    open val subtitleFontSize: Flow<Float> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[SUBTITLE_FONT_SIZE] ?: 16f
        }

    open val subtitleBackgroundOpacity: Flow<Float> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[SUBTITLE_BACKGROUND_OPACITY] ?: 0.65f
        }

    open val appLanguage: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[APP_LANGUAGE]
        }

    open val lastAppVersion: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[LAST_APP_VERSION] ?: 0
        }

    open val lastWhatsNewVersion: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[LAST_WHATS_NEW_VERSION]
        }

    open val lastPlayedVideoId: Flow<String?> = dataStore.data.map { it[LAST_PLAYED_VIDEO_ID] }
    open val lastPlayedPosition: Flow<Long> = dataStore.data.map { it[LAST_PLAYED_POSITION] ?: 0L }
    open val lastPlayedIsLocal: Flow<Boolean> = dataStore.data.map { it[LAST_PLAYED_IS_LOCAL] ?: false }

    open val seenShorts: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[SEEN_SHORTS] ?: emptySet()
        }

    open val appTheme: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { preferences ->
            preferences[APP_THEME] ?: "Light"
        }

    open val lastActiveAt: Flow<Long> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }.map { it[LAST_ACTIVE_AT] ?: 0L }

    suspend fun setLastActiveAt(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_ACTIVE_AT] = timestamp
        }
    }

    suspend fun markShortAsSeen(videoId: String) {
        dataStore.edit { preferences ->
            val current = preferences[SEEN_SHORTS]?.toMutableSet() ?: mutableSetOf()
            current.add(videoId)
            // keep last 500
            if (current.size > 500) {
                val toRemove = current.size - 500
                preferences[SEEN_SHORTS] = current.drop(toRemove).toSet()
            } else {
                preferences[SEEN_SHORTS] = current
            }
        }
    }

    suspend fun clearSeenShorts() {
        dataStore.edit { preferences ->
            preferences[SEEN_SHORTS] = emptySet()
        }
    }

    suspend fun setAppTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    open val playbackSpeed: Flow<Float> = dataStore.data.map { it[PLAYBACK_SPEED] ?: 1.0f }
    open val playbackPitch: Flow<Float> = dataStore.data.map { it[PLAYBACK_PITCH] ?: 1.0f }

    open val isProxyEnabled: Flow<Boolean> = dataStore.data.map { it[PROXY_ENABLED] ?: false }
    open val proxyHost: Flow<String> = dataStore.data.map { it[PROXY_HOST] ?: "" }
    open val proxyPort: Flow<Int> = dataStore.data.map { it[PROXY_PORT] ?: 8080 }

    open val isAmbientModeEnabled: Flow<Boolean> = dataStore.data.map { it[AMBIENT_MODE_ENABLED] ?: false }

    suspend fun setAmbientModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AMBIENT_MODE_ENABLED] = enabled
        }
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HISTORY_ENABLED] = enabled
        }
    }

    suspend fun setSearchHistoryPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEARCH_HISTORY_PAUSED] = paused
        }
    }

    suspend fun setPipEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PIP_ENABLED] = enabled
        }
    }

    suspend fun setBackgroundPlayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BACKGROUND_PLAY_ENABLED] = enabled
        }
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ANIMATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSubtitlesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SUBTITLES_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setSearchGridView(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEARCH_GRID_VIEW] = enabled
        }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_UPDATE_ENABLED] = enabled
        }
    }

    open val updateLatestVersionCode: Flow<Int> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_LATEST_VERSION_CODE] ?: 0 }

    open val updateLatestVersionName: Flow<String> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_LATEST_VERSION_NAME] ?: "" }

    open val isUpdateMandatory: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_MANDATORY] ?: false }

    open val updateReleaseNotes: Flow<String> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_RELEASE_NOTES] ?: "" }

    open val updateDownloadUrl: Flow<String> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_DOWNLOAD_URL] ?: "" }

    open val updateDownloadedApkPath: Flow<String?> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_DOWNLOADED_APK_PATH] }

    open val isUpdateDownloadCompleted: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_DOWNLOAD_COMPLETED] ?: false }

    open val lastUpdateCheckTime: Flow<Long> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[UPDATE_LAST_CHECK_TIME] ?: 0L }

    open val lastDismissedUpdateVersionCode: Flow<Int> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[LAST_DISMISSED_UPDATE_VERSION] ?: 0 }

    suspend fun setLastDismissedUpdateVersionCode(versionCode: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_DISMISSED_UPDATE_VERSION] = versionCode
        }
    }

    suspend fun saveUpdateState(
        versionCode: Int,
        versionName: String,
        mandatory: Boolean,
        notes: String,
        downloadUrl: String
    ) {
        dataStore.edit { preferences ->
            preferences[UPDATE_LATEST_VERSION_CODE] = versionCode
            preferences[UPDATE_LATEST_VERSION_NAME] = versionName
            preferences[UPDATE_MANDATORY] = mandatory
            preferences[UPDATE_RELEASE_NOTES] = notes
            preferences[UPDATE_DOWNLOAD_URL] = downloadUrl
            preferences[UPDATE_LAST_CHECK_TIME] = System.currentTimeMillis()
        }
    }

    suspend fun setDownloadedApk(path: String?, completed: Boolean) {
        dataStore.edit { preferences ->
            if (path != null) {
                preferences[UPDATE_DOWNLOADED_APK_PATH] = path
            } else {
                preferences.remove(UPDATE_DOWNLOADED_APK_PATH)
            }
            preferences[UPDATE_DOWNLOAD_COMPLETED] = completed
        }
    }

    suspend fun clearUpdateState() {
        dataStore.edit { preferences ->
            preferences.remove(UPDATE_LATEST_VERSION_CODE)
            preferences.remove(UPDATE_LATEST_VERSION_NAME)
            preferences.remove(UPDATE_MANDATORY)
            preferences.remove(UPDATE_RELEASE_NOTES)
            preferences.remove(UPDATE_DOWNLOAD_URL)
            preferences.remove(UPDATE_DOWNLOADED_APK_PATH)
            preferences.remove(UPDATE_DOWNLOAD_COMPLETED)
        }
    }

    suspend fun setLastUpdateCheckTime(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[UPDATE_LAST_CHECK_TIME] = timestamp
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    suspend fun setRecommendationsPaused(paused: Boolean) {
        dataStore.edit { preferences ->
            preferences[RECOMMENDATIONS_PAUSED] = paused
        }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTOPLAY_ENABLED] = enabled
        }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[INCOGNITO_MODE] = enabled
        }
    }

    suspend fun setPlayerGesturesEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PLAYER_GESTURES_ENABLED] = enabled
        }
    }

    suspend fun setPreferredSubtitleLanguage(language: String?) {
        dataStore.edit { preferences ->
            if (language == null) preferences.remove(PREFERRED_SUBTITLE_LANGUAGE)
            else preferences[PREFERRED_SUBTITLE_LANGUAGE] = language
        }
    }

    suspend fun setPreferredQuality(quality: String) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_QUALITY] = quality
        }
    }

    suspend fun setSubtitleFontSize(size: Float) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_FONT_SIZE] = size
        }
    }

    suspend fun setSubtitleBackgroundOpacity(opacity: Float) {
        dataStore.edit { preferences ->
            preferences[SUBTITLE_BACKGROUND_OPACITY] = opacity
        }
    }

    suspend fun setAppLanguage(tag: String?) {
        dataStore.edit { preferences ->
            if (tag == null) preferences.remove(APP_LANGUAGE)
            else preferences[APP_LANGUAGE] = tag
        }
    }

    suspend fun setLastAppVersion(version: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_APP_VERSION] = version
        }
    }

    suspend fun setLastWhatsNewVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[LAST_WHATS_NEW_VERSION] = version
        }
    }

    suspend fun setLastPlayedSession(videoId: String, position: Long, isLocal: Boolean) {
        dataStore.edit { preferences ->
            preferences[LAST_PLAYED_VIDEO_ID] = videoId
            preferences[LAST_PLAYED_POSITION] = position
            preferences[LAST_PLAYED_IS_LOCAL] = isLocal
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        dataStore.edit { preferences ->
            preferences[PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setPlaybackPitch(pitch: Float) {
        dataStore.edit { preferences ->
            preferences[PLAYBACK_PITCH] = pitch
        }
    }

    suspend fun setProxySettings(enabled: Boolean, host: String, port: Int) {
        dataStore.edit { preferences ->
            preferences[PROXY_ENABLED] = enabled
            preferences[PROXY_HOST] = host
            preferences[PROXY_PORT] = port
        }
    }

    suspend fun setAutoCacheCleanerEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AUTO_CACHE_CLEANER_ENABLED] = enabled
        }
    }

    suspend fun setCacheCleanerThresholdMb(thresholdMb: Long) {
        dataStore.edit { preferences ->
            preferences[CACHE_CLEANER_THRESHOLD_MB] = thresholdMb
        }
    }

    suspend fun setLastCacheCleanupTime(time: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_CACHE_CLEANUP_TIME] = time
        }
    }

    companion object {
        val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val SEARCH_HISTORY_PAUSED = booleanPreferencesKey("search_history_paused")
        val PIP_ENABLED = booleanPreferencesKey("pip_enabled")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SEARCH_GRID_VIEW = booleanPreferencesKey("search_grid_view")
        val AUTO_UPDATE_ENABLED = booleanPreferencesKey("auto_update_enabled")
        val UPDATE_LATEST_VERSION_CODE = intPreferencesKey("update_latest_version_code")
        val UPDATE_LATEST_VERSION_NAME = stringPreferencesKey("update_latest_version_name")
        val UPDATE_MANDATORY = booleanPreferencesKey("update_mandatory")
        val UPDATE_RELEASE_NOTES = stringPreferencesKey("update_release_notes")
        val UPDATE_DOWNLOAD_URL = stringPreferencesKey("update_download_url")
        val UPDATE_DOWNLOADED_APK_PATH = stringPreferencesKey("update_downloaded_apk_path")
        val UPDATE_DOWNLOAD_COMPLETED = booleanPreferencesKey("update_download_completed")
        val UPDATE_LAST_CHECK_TIME = longPreferencesKey("update_last_check_time")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val RECOMMENDATIONS_PAUSED = booleanPreferencesKey("recommendations_paused")
        val AUTOPLAY_ENABLED = booleanPreferencesKey("autoplay_enabled")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val PLAYER_GESTURES_ENABLED = booleanPreferencesKey("player_gestures_enabled")
        val PREFERRED_SUBTITLE_LANGUAGE = stringPreferencesKey("preferred_subtitle_language")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val SUBTITLE_FONT_SIZE = floatPreferencesKey("subtitle_font_size")
        val SUBTITLE_BACKGROUND_OPACITY = floatPreferencesKey("subtitle_background_opacity")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APP_THEME = stringPreferencesKey("app_theme")
        val LAST_APP_VERSION = intPreferencesKey("last_app_version")
        val LAST_WHATS_NEW_VERSION = stringPreferencesKey("last_whats_new_version")
        val LAST_ACTIVE_AT = longPreferencesKey("last_active_at")
        val LAST_DISMISSED_UPDATE_VERSION = intPreferencesKey("last_dismissed_update_version")
        
        val LAST_PLAYED_VIDEO_ID = stringPreferencesKey("last_played_video_id")
        val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
        val LAST_PLAYED_IS_LOCAL = booleanPreferencesKey("last_played_is_local")

        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")

        val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val AMBIENT_MODE_ENABLED = booleanPreferencesKey("ambient_mode_enabled")

        val AUTO_CACHE_CLEANER_ENABLED = booleanPreferencesKey("auto_cache_cleaner_enabled")
        val CACHE_CLEANER_THRESHOLD_MB = longPreferencesKey("cache_cleaner_threshold_mb")
        val LAST_CACHE_CLEANUP_TIME = longPreferencesKey("last_cache_cleanup_time")
        
        val SEEN_SHORTS = stringSetPreferencesKey("seen_shorts")
    }
}
