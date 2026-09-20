/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class ErrorHandlingTest {

    @Test
    fun `fromThrowable maps Network errors correctly`() {
        assertTrue(VibeTubeError.fromThrowable(UnknownHostException()) is VibeTubeError.Network)
        assertTrue(VibeTubeError.fromThrowable(IOException()) is VibeTubeError.Network)
    }

    @Test
    fun `fromThrowable maps AuthError correctly`() {
        assertTrue(VibeTubeError.fromThrowable(SecurityException()) is VibeTubeError.AuthError)
    }

    @Test
    fun `fromThrowable maps ApiThrottled correctly`() {
        val throttledException = Exception("HTTP 429 Too Many Requests")
        assertTrue(VibeTubeError.fromThrowable(throttledException) is VibeTubeError.ApiThrottled)
    }

    @Test
    fun `getMessage returns human readable strings`() {
        assertEquals("No internet connection", VibeTubeError.Network.getMessage())
        assertEquals("Authentication required", VibeTubeError.AuthError.getMessage())
        assertEquals("Custom Error", VibeTubeError.Unknown("Custom Error").getMessage())
    }
}
