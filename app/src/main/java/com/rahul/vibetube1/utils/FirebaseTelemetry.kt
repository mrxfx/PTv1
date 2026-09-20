/*
 * ZyvoTube
 * Copyright (C) 2026 ZyvoTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized telemetry and diagnostics coordinator for ZyvoTube.
 * Encapsulates Firebase Analytics event tracking and Firebase Crashlytics error reporting.
 * Safe for debug and release environments. Sensitive PII (passwords, auth tokens,
 * private messages, emails) is strictly excluded.
 */
object FirebaseTelemetry {
    private const val TAG = "FirebaseTelemetry"
    private var analytics: FirebaseAnalytics? = null
    private var crashlytics: FirebaseCrashlytics? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val appAnalytics = FirebaseAnalytics.getInstance(context)
                val appCrashlytics = FirebaseCrashlytics.getInstance()

                // Register global application version metadata
                appAnalytics.setUserProperty("app_version", AppVersion.name)
                appAnalytics.setUserProperty("app_version_code", AppVersion.code.toString())

                appCrashlytics.setCustomKey("app_version", AppVersion.name)
                appCrashlytics.setCustomKey("app_version_code", AppVersion.code)

                analytics = appAnalytics
                crashlytics = appCrashlytics
                isInitialized = true
                ZyvoLog.d(TAG, "Firebase Telemetry successfully initialized.")
            } else {
                ZyvoLog.w(TAG, "FirebaseApp is not initialized (google-services.json may not be loaded).")
            }
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Failed to initialize Firebase Telemetry: ${e.message}")
        }
    }

    /**
     * Tracks the standard app_open event with version metadata.
     */
    fun logAppOpen() {
        try {
            val bundle = Bundle().apply {
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, bundle)
            crashlytics?.log("Event: app_open (v${AppVersion.name})")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging app_open: ${e.message}")
        }
    }

    /**
     * Tracks video playback initiation.
     */
    fun logVideoPlay(videoId: String, title: String?, channelName: String?, isPlaylist: Boolean = false) {
        try {
            val bundle = Bundle().apply {
                putString("video_id", videoId)
                putString("video_title", title?.take(100))
                putString("channel_name", channelName?.take(100))
                putBoolean("is_playlist", isPlaylist)
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("video_play", bundle)
            crashlytics?.log("Event: video_play id=$videoId isPlaylist=$isPlaylist")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging video_play: ${e.message}")
        }
    }

    /**
     * Tracks Shorts impression / playback view.
     */
    fun logShortView(shortId: String, title: String?, channelName: String?) {
        try {
            val bundle = Bundle().apply {
                putString("short_id", shortId)
                putString("short_title", title?.take(100))
                putString("channel_name", channelName?.take(100))
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("short_view", bundle)
            crashlytics?.log("Event: short_view id=$shortId")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging short_view: ${e.message}")
        }
    }

    /**
     * Tracks download initiation.
     */
    fun logDownloadStart(
        videoId: String,
        title: String?,
        isAudioOnly: Boolean,
        saveToDevice: Boolean,
        quality: String?
    ) {
        try {
            val bundle = Bundle().apply {
                putString("video_id", videoId)
                putString("title", title?.take(100))
                putBoolean("is_audio_only", isAudioOnly)
                putBoolean("save_to_device", saveToDevice)
                putString("quality", quality ?: "unknown")
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("download_start", bundle)
            crashlytics?.log("Event: download_start id=$videoId isAudio=$isAudioOnly saveToDevice=$saveToDevice")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging download_start: ${e.message}")
        }
    }

    /**
     * Tracks successful download completion.
     */
    fun logDownloadComplete(
        videoId: String,
        isAudioOnly: Boolean,
        saveToDevice: Boolean,
        durationMs: Long? = null
    ) {
        try {
            val bundle = Bundle().apply {
                putString("video_id", videoId)
                putBoolean("is_audio_only", isAudioOnly)
                putBoolean("save_to_device", saveToDevice)
                if (durationMs != null && durationMs > 0) {
                    putLong("duration_ms", durationMs)
                }
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("download_complete", bundle)
            crashlytics?.log("Event: download_complete id=$videoId isAudio=$isAudioOnly")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging download_complete: ${e.message}")
        }
    }

    /**
     * Tracks download failure.
     */
    fun logDownloadFailed(
        videoId: String,
        errorMessage: String?,
        isAudioOnly: Boolean,
        saveToDevice: Boolean
    ) {
        try {
            val sanitizedError = errorMessage?.take(150) ?: "Unknown error"
            val bundle = Bundle().apply {
                putString("video_id", videoId)
                putString("error_message", sanitizedError)
                putBoolean("is_audio_only", isAudioOnly)
                putBoolean("save_to_device", saveToDevice)
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("download_failed", bundle)
            crashlytics?.log("Event: download_failed id=$videoId error=$sanitizedError")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging download_failed: ${e.message}")
        }
    }

    /**
     * Tracks background audio/video playback service engagement.
     */
    fun logBackgroundPlay(videoId: String?, title: String?, isAudioOnly: Boolean = false) {
        try {
            val bundle = Bundle().apply {
                if (videoId != null) putString("video_id", videoId)
                if (title != null) putString("title", title.take(100))
                putBoolean("is_audio_only", isAudioOnly)
                putString("app_version", AppVersion.name)
                putInt("app_version_code", AppVersion.code)
            }
            analytics?.logEvent("background_play", bundle)
            crashlytics?.log("Event: background_play id=$videoId")
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging background_play: ${e.message}")
        }
    }

    /**
     * Records non-fatal exceptions to Crashlytics with context details.
     */
    fun recordNonFatal(throwable: Throwable, tag: String? = null, message: String? = null) {
        try {
            if (tag != null || message != null) {
                crashlytics?.log("[$tag] ${message ?: ""}")
            }
            crashlytics?.recordException(throwable)
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error recording non-fatal exception: ${e.message}")
        }
    }

    /**
     * Logs custom diagnostics breadcrumbs to Crashlytics.
     */
    fun log(message: String) {
        try {
            crashlytics?.log(message)
        } catch (e: Exception) {
            ZyvoLog.w(TAG, "Error logging breadcrumb: ${e.message}")
        }
    }
}
