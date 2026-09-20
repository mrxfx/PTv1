/*
 * ZyvoTube
 * Copyright (C) 2026 ZyvoTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import android.util.Log
import com.rahul.vibetube1.BuildConfig

/**
 * ZyvoTube Centralized Logger.
 * Ensures logs are only emitted in Debug builds for security and performance.
 */
object ZyvoLog {
    fun d(tag: String, message: String) {
        try {
            if (BuildConfig.DEBUG) Log.d(tag, message)
        } catch (_: RuntimeException) {
            // JVM unit test fallback
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (BuildConfig.DEBUG) Log.e(tag, message, throwable)
            if (throwable != null) {
                FirebaseTelemetry.recordNonFatal(throwable, tag, message)
            }
        } catch (_: RuntimeException) {
        }
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (BuildConfig.DEBUG) Log.w(tag, message, throwable)
        } catch (_: RuntimeException) {
        }
    }

    fun i(tag: String, message: String) {
        try {
            if (BuildConfig.DEBUG) Log.i(tag, message)
        } catch (_: RuntimeException) {
        }
    }

    fun v(tag: String, message: String) {
        try {
            if (BuildConfig.DEBUG) Log.v(tag, message)
        } catch (_: RuntimeException) {
        }
    }
}

typealias VTLog = ZyvoLog
typealias PTLog = ZyvoLog
