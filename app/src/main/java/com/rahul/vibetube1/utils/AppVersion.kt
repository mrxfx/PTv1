package com.rahul.vibetube1.utils
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig


/**
 * Single source of truth for the VibeTube version.
 * authoritative values are defined in app/build.gradle.kts
 */
object AppVersion {
    /**
     * Authoritative user-facing version name (e.g., "1.0.1")
     */
    val name: String
        get() = BuildConfig.VERSION_NAME

    /**
     * Authoritative internal version code (e.g., 1)
     */
    val code: Int
        get() = BuildConfig.VERSION_CODE
}
