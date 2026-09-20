/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import java.io.IOException
import java.net.UnknownHostException

sealed class VibeTubeError {
    object Network : VibeTubeError()
    data class Extraction(val errorMessage: String) : VibeTubeError()
    data class Unknown(val errorMessage: String) : VibeTubeError()
    object AuthError : VibeTubeError()
    object ApiThrottled : VibeTubeError()
    object StorageFull : VibeTubeError()
    data class UnsupportedFormat(val format: String) : VibeTubeError()

    fun getMessage(): String {
        return when (this) {
            is Network -> "No internet connection"
            is Extraction -> errorMessage
            is Unknown -> errorMessage
            is AuthError -> "Authentication required"
            is ApiThrottled -> "Service busy, try again later"
            is StorageFull -> "Storage is full"
            is UnsupportedFormat -> "Format $format is not supported"
        }
    }

    companion object {
        fun fromThrowable(t: Throwable): VibeTubeError {
            return when (t) {
                is UnknownHostException, is IOException -> Network
                is java.lang.SecurityException -> AuthError
                else -> {
                    val message = t.message ?: "An unexpected error occurred"
                    if (message.contains("429") || message.contains("Too Many Requests")) ApiThrottled
                    else if (t.javaClass.simpleName == "SignInConfirmNotBotException" || message.contains("Sign in to confirm that you're not a bot")) {
                        Extraction("YouTube temporarily blocked this network due to suspected bot activity. Please try again later or switch to a different network/VPN.")
                    }
                    else Extraction(message)
                }
            }
        }
    }
}

typealias PlayTubeError = VibeTubeError
