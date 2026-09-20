/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsMainList(
    appTheme: String = "System",
    onNavigateToSubscreen: (SettingsSubscreen) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // ACCOUNT SECTION (Portion 1)
        SettingsSectionHeader(title = "ACCOUNT")
        SettingsRow(
            title = "Account",
            subtitle = "Manage your account and history",
            icon = Icons.Default.Person,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Account) }
        )
        SettingsDivider()
        SettingsRow(
            title = "General",
            subtitle = "App preferences and updates",
            icon = Icons.Default.Settings,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.General) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PLAYBACK SECTION (Portion 2)
        SettingsSectionHeader(title = "PLAYBACK")
        SettingsRow(
            title = "Playback",
            subtitle = "Quality, autoplay, controls",
            icon = Icons.Default.PlayArrow,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Playback) }
        )
        SettingsDivider()
        SettingsRow(
            title = "Audio & Background",
            subtitle = "Background playback and audio",
            icon = Icons.Default.VolumeUp,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.AudioBackground) }
        )
        SettingsDivider()
        SettingsRow(
            title = "Display",
            subtitle = "Fullscreen, player appearance",
            icon = Icons.Default.Tv,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Display) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // APPEARANCE & THEME SECTION (Portion 3)
        SettingsSectionHeader(title = "APPEARANCE")
        SettingsRow(
            title = "Appearance",
            subtitle = "Theme and interface",
            trailingText = appTheme,
            icon = Icons.Default.Palette,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Appearance) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // DOWNLOADS SECTION (Portion 4)
        SettingsSectionHeader(title = "DOWNLOADS")
        SettingsRow(
            title = "Downloads",
            subtitle = "Location, quality and storage",
            icon = Icons.Default.ArrowDownward,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Downloads) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // PRIVACY & DATA SECTION (Portion 5)
        SettingsSectionHeader(title = "PRIVACY & DATA")
        SettingsRow(
            title = "Privacy",
            subtitle = "History and data controls",
            icon = Icons.Default.Security,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Privacy) }
        )
        SettingsDivider()
        SettingsRow(
            title = "Data usage",
            subtitle = "Network preferences",
            icon = Icons.Default.Language,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.DataUsage) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADVANCED SECTION (Portion 6)
        SettingsSectionHeader(title = "ADVANCED")
        SettingsRow(
            title = "Advanced",
            subtitle = "Proxy and reset controls",
            icon = Icons.Default.Build,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.Advanced) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ABOUT SECTION (Portion 7)
        SettingsSectionHeader(title = "ABOUT")
        SettingsRow(
            title = "About VibeTube",
            subtitle = "Version and open-source information",
            icon = Icons.Default.Info,
            onClick = { onNavigateToSubscreen(SettingsSubscreen.About) }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
