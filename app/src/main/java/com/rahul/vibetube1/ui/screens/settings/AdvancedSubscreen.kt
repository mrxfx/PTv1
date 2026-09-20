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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AdvancedSubscreen(
    isProxyEnabled: Boolean,
    proxyHost: String,
    proxyPort: Int,
    onSetProxySettings: (Boolean, String, Int) -> Unit,
    onResetPlayerSettings: () -> Unit,
    onResetAppearanceSettings: () -> Unit,
    onResetAllSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showProxyDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }
    var showResetPlayerDialog by remember { mutableStateOf(false) }
    var showResetAppearanceDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "NETWORK")
        SettingsSwitchRow(
            title = "HTTP Proxy",
            subtitle = if (isProxyEnabled) "$proxyHost:$proxyPort" else "Disabled",
            icon = Icons.Default.VpnKey,
            checked = isProxyEnabled,
            onCheckedChange = { enabled ->
                if (enabled) {
                    showProxyDialog = true
                } else {
                    onSetProxySettings(false, proxyHost, proxyPort)
                }
            }
        )
        if (isProxyEnabled) {
            SettingsRow(
                title = "Configure proxy",
                subtitle = "Set proxy host and port",
                onClick = { showProxyDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        SettingsSectionHeader(title = "RESET")
        SettingsRow(
            title = "Reset player settings",
            subtitle = "Restore playback defaults",
            icon = Icons.Default.Restore,
            onClick = { showResetPlayerDialog = true }
        )
        SettingsDivider()
        SettingsRow(
            title = "Reset appearance settings",
            subtitle = "Restore theme and visual defaults",
            icon = Icons.Default.Restore,
            onClick = { showResetAppearanceDialog = true }
        )
        SettingsDivider()
        SettingsRow(
            title = "Reset all settings",
            subtitle = "Restore all preferences to default",
            icon = Icons.Default.Warning,
            isDestructive = true,
            onClick = { showResetAllDialog = true }
        )
    }

    if (showProxyDialog) {
        var hostInput by remember { mutableStateOf(proxyHost) }
        var portInput by remember { mutableStateOf(proxyPort.toString()) }

        AlertDialog(
            onDismissRequest = { showProxyDialog = false },
            title = { Text("Configure Proxy") },
            text = {
                Column {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val port = portInput.toIntOrNull() ?: 8080
                        onSetProxySettings(true, hostInput, port)
                        showProxyDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProxyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetPlayerDialog) {
        SettingsConfirmationDialog(
            title = "Reset player settings?",
            message = "This will restore default video quality, autoplay, background play, and gesture settings.",
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                onResetPlayerSettings()
                showResetPlayerDialog = false
            },
            onDismiss = { showResetPlayerDialog = false }
        )
    }

    if (showResetAppearanceDialog) {
        SettingsConfirmationDialog(
            title = "Reset appearance?",
            message = "This will restore the system default theme and animation settings.",
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                onResetAppearanceSettings()
                showResetAppearanceDialog = false
            },
            onDismiss = { showResetAppearanceDialog = false }
        )
    }

    if (showResetAllDialog) {
        SettingsConfirmationDialog(
            title = "Reset all settings?",
            message = "This will restore all application preferences to their defaults. Your watch history, downloads, and interests will NOT be deleted.",
            confirmText = "Reset All",
            isDestructive = true,
            onConfirm = {
                onResetAllSettings()
                showResetAllDialog = false
            },
            onDismiss = { showResetAllDialog = false }
        )
    }
}
