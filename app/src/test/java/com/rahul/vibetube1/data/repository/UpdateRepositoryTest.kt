/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.repository

import com.rahul.vibetube1.data.network.VersionInfo
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val cleanCurrent = current.removePrefix("v").trim()
        val cleanLatest = latest.removePrefix("v").trim()
        if (cleanLatest.isBlank() || cleanCurrent == cleanLatest) return false

        val currentParts = cleanCurrent.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = cleanLatest.split(".").mapNotNull { it.toIntOrNull() }

        if (latestParts.isEmpty()) return false

        val size = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until size) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val latestPart = latestParts.getOrElse(i) { 0 }
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }

    @Test
    fun testVersionComparison() {
        // Newer versions
        assertTrue(isVersionNewer("1.3.3", "1.3.4"))
        assertTrue(isVersionNewer("1.3.3", "1.4.0"))
        assertTrue(isVersionNewer("1.3.3", "2.0.0"))
        assertTrue(isVersionNewer("1.3.3", "1.3.3.1"))
        assertTrue(isVersionNewer("v1.0.0", "v1.0.2"))
        assertTrue(isVersionNewer("1.0.0", "v1.0.2"))

        // Same versions
        assertFalse(isVersionNewer("1.3.3", "1.3.3"))
        assertFalse(isVersionNewer("v1.0.2", "1.0.2"))

        // Older versions
        assertFalse(isVersionNewer("1.3.3", "1.3.2"))
        assertFalse(isVersionNewer("1.3.3", "1.2.9"))
        assertFalse(isVersionNewer("1.3.3", "0.9.9"))
        assertFalse(isVersionNewer("v2.0.0", "1.0.2"))
    }

    @Test
    fun testVersionJsonDeserialization_NewMandatoryFormat() {
        val jsonString = """
            {
              "versionName": "1.0.2",
              "versionCode": 2,
              "title": "VibeTube v1.0.2",
              "message": "Important improvements and bug fixes.",
              "downloadUrl": "https://github.com/Rahulhaldar/VibeTube/releases/latest",
              "mandatory": true
            }
        """.trimIndent()

        val parsed = json.decodeFromString<VersionInfo>(jsonString)

        assertEquals("1.0.2", parsed.resolvedVersionName)
        assertEquals(2, parsed.resolvedVersionCode)
        assertEquals("VibeTube v1.0.2", parsed.resolvedTitle)
        assertEquals("Important improvements and bug fixes.", parsed.resolvedMessage)
        assertEquals("https://github.com/Rahulhaldar/VibeTube/releases/latest", parsed.resolvedDownloadUrl)
        assertTrue(parsed.mandatory)
    }

    @Test
    fun testVersionJsonDeserialization_BackwardCompatibility() {
        val legacyJsonString = """
            {
              "version": "1.0.1",
              "changelog": "Initial release with basic features",
              "url": "https://github.com/Rahulhaldar/VibeTube/releases/tag/v1.0.1"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<VersionInfo>(legacyJsonString)

        assertEquals("1.0.1", parsed.resolvedVersionName)
        assertEquals("Initial release with basic features", parsed.resolvedMessage)
        assertEquals("https://github.com/Rahulhaldar/VibeTube/releases/tag/v1.0.1", parsed.resolvedDownloadUrl)
        assertFalse(parsed.mandatory)
    }

    private fun runVerificationTest(
        installedPackageName: String,
        downloadedPackageName: String,
        installedVersionCode: Int,
        downloadedVersionCode: Int,
        signaturesMatch: Boolean
    ): Boolean {
        val result = performIdentityVerificationRules(
            downloadedPackageName = downloadedPackageName,
            installedPackageName = installedPackageName,
            downloadedVersionCode = downloadedVersionCode,
            installedVersionCode = installedVersionCode,
            signaturesMatch = signaturesMatch
        )
        return result is VerificationResult.Success
    }

    @Test
    fun testExactProductionPackageMatchAcceptance() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertTrue(result)
    }

    @Test
    fun testExactTestingPackageMatchAcceptance() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube1",
            downloadedPackageName = "com.rahul.vibetube1",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertTrue(result)
    }

    @Test
    fun testExactTestPackageMatchAcceptance() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube.test",
            downloadedPackageName = "com.rahul.vibetube.test",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertTrue(result)
    }

    @Test
    fun testCrossPackageRejectionProductionToTesting() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube1",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testCrossPackageRejectionTestingToProduction() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube1",
            downloadedPackageName = "com.rahul.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testRandomPackageRejection() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.example.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testApkEditorStylePackageModificationRejection() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube2",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testSamePackageButWrongSigningCertificateRejection() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 20,
            signaturesMatch = false
        )
        assertFalse(result)
    }

    @Test
    fun testSamePackageWithLowerVersionCodeRejection() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 5,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testSamePackageWithEqualVersionCodeRejection() {
        val result = runVerificationTest(
            installedPackageName = "com.rahul.vibetube",
            downloadedPackageName = "com.rahul.vibetube",
            installedVersionCode = 10,
            downloadedVersionCode = 10,
            signaturesMatch = true
        )
        assertFalse(result)
    }

    @Test
    fun testStructuredReleaseNotesDeserialization() {
        val jsonString = """
            {
              "versionName": "1.0.1",
              "versionCode": 2,
              "mandatory": true,
              "releaseNotes": {
                "title": "What's New in VibeTube",
                "items": [
                  "Improved performance",
                  "Fixed bugs"
                ]
              }
            }
        """.trimIndent()

        val parsed = json.decodeFromString<VersionInfo>(jsonString)
        val releaseNotesInfo = parsed.parsedReleaseNotes

        assertTrue(releaseNotesInfo != null)
        assertEquals("What's New in VibeTube", releaseNotesInfo?.title)
        assertEquals(listOf("Improved performance", "Fixed bugs"), releaseNotesInfo?.items)
    }

    @Test
    fun testLegacyStringReleaseNotesDeserializationGracefulFallback() {
        val jsonString = """
            {
              "versionName": "1.0.1",
              "versionCode": 2,
              "mandatory": true,
              "release_notes": "Simple legacy plain text release notes"
            }
        """.trimIndent()

        val parsed = json.decodeFromString<VersionInfo>(jsonString)
        val releaseNotesInfo = parsed.parsedReleaseNotes

        assertTrue(releaseNotesInfo != null)
        assertEquals("What's New", releaseNotesInfo?.title)
        assertEquals(listOf("Simple legacy plain text release notes"), releaseNotesInfo?.items)
    }
}
