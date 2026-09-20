/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * Main Player Settings Bottom Sheet
 * Displays high-frequency player controls in a compact, Android-native layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsBottomSheet(
    displayQuality: String,
    playbackSpeed: Float,
    isCcEnabled: Boolean,
    selectedSubtitleLanguage: String?,
    sleepTimerRemainingTime: Int?,
    onQualityClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onCaptionsClick: () -> Unit,
    onLockScreenClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAdditionalSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            // Quality row
            PlayerSettingsItemRow(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.quality),
                value = displayQuality,
                testTag = "settings_quality_row",
                onClick = onQualityClick
            )

            // Playback speed row
            val speedLabel = if (playbackSpeed == 1.0f) {
                stringResource(R.string.normal_speed)
            } else {
                "${playbackSpeed}x"
            }
            PlayerSettingsItemRow(
                icon = Icons.Default.Speed,
                title = stringResource(R.string.playback_speed),
                value = speedLabel,
                testTag = "settings_speed_row",
                onClick = onSpeedClick
            )

            // Captions row
            val captionsLabel = if (isCcEnabled) {
                selectedSubtitleLanguage?.let {
                    Locale.forLanguageTag(it).displayLanguage.replaceFirstChar { c -> c.uppercase() }
                } ?: "On"
            } else {
                stringResource(R.string.off)
            }
            PlayerSettingsItemRow(
                icon = Icons.Default.ClosedCaption,
                title = stringResource(R.string.subtitles),
                value = captionsLabel,
                testTag = "settings_captions_row",
                onClick = onCaptionsClick
            )

            // Lock screen row
            PlayerSettingsItemRow(
                icon = Icons.Default.Lock,
                title = "Lock screen",
                value = null,
                showChevron = false,
                testTag = "settings_lock_screen_row",
                onClick = onLockScreenClick
            )

            // Sleep timer row
            val sleepTimerLabel = when (sleepTimerRemainingTime) {
                null -> stringResource(R.string.timer_off)
                -1 -> stringResource(R.string.timer_end_of_video_active)
                else -> stringResource(R.string.timer_minutes_remaining, sleepTimerRemainingTime)
            }
            PlayerSettingsItemRow(
                icon = Icons.Default.Timer,
                title = stringResource(R.string.sleep_timer),
                value = sleepTimerLabel,
                testTag = "settings_sleep_timer_row",
                onClick = onSleepTimerClick
            )

            // Additional settings row
            PlayerSettingsItemRow(
                icon = Icons.Default.Tune,
                title = "Additional settings",
                value = null,
                showChevron = true,
                testTag = "settings_additional_row",
                onClick = onAdditionalSettingsClick
            )
        }
    }
}

/**
 * Additional Settings Bottom Sheet
 * Hosts advanced and secondary player controls (Audio pitch, Ambient mode, Stable volume, Loop, Stats).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdditionalSettingsBottomSheet(
    playbackPitch: Float,
    isAmbientModeEnabled: Boolean,
    isStableVolumeEnabled: Boolean,
    isLooping: Boolean,
    showStatsForNerds: Boolean,
    onPitchClick: () -> Unit,
    onToggleAmbientMode: () -> Unit,
    onToggleStableVolume: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleStatsForNerds: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Additional settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Audio pitch row
            val pitchLabel = if (playbackPitch == 1.0f) "Normal" else "${playbackPitch}x"
            PlayerSettingsItemRow(
                icon = Icons.Default.MusicNote,
                title = "Audio pitch",
                value = pitchLabel,
                testTag = "settings_pitch_row",
                onClick = onPitchClick
            )

            // Ambient mode switch
            PlayerSettingsToggleRow(
                icon = Icons.Default.LightMode,
                title = "Ambient mode",
                subtitle = "Subtle lighting around player",
                checked = isAmbientModeEnabled,
                testTag = "settings_ambient_mode_toggle",
                onCheckedChange = { onToggleAmbientMode() }
            )

            // Stable volume switch
            PlayerSettingsToggleRow(
                icon = Icons.Default.Equalizer,
                title = "Stable volume",
                subtitle = "Balances quiet and loud sounds",
                checked = isStableVolumeEnabled,
                testTag = "settings_stable_volume_toggle",
                onCheckedChange = { onToggleStableVolume() }
            )

            // Loop video switch
            PlayerSettingsToggleRow(
                icon = Icons.Default.Repeat,
                title = "Loop video",
                subtitle = "Repeat current video continuously",
                checked = isLooping,
                testTag = "settings_loop_video_toggle",
                onCheckedChange = { onToggleLoop() }
            )

            // Stats for Nerds switch
            PlayerSettingsToggleRow(
                icon = Icons.Default.Info,
                title = "Stats for Nerds",
                subtitle = "Display technical playback diagnostics",
                checked = showStatsForNerds,
                testTag = "settings_stats_toggle",
                onCheckedChange = { onToggleStatsForNerds() }
            )
        }
    }
}

/**
 * Standard clickable row for player settings
 */
@Composable
fun PlayerSettingsItemRow(
    icon: ImageVector,
    title: String,
    value: String?,
    showChevron: Boolean = true,
    testTag: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Standard toggle switch row for player settings
 */
@Composable
fun PlayerSettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
