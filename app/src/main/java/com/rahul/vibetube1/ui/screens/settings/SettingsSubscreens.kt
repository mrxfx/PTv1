/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.settings
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.rahul.vibetube1.utils.AppVersion
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.rahul.vibetube1.data.local.DownloadEntity
import com.rahul.vibetube1.domain.repository.UpdateInfo
import com.rahul.vibetube1.ui.theme.DarkRed
import com.rahul.vibetube1.ui.theme.VibeTubeRed

@Composable
fun AccountSubscreen(
    onViewHistory: () -> Unit,
    onDataManagement: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearInterests: () -> Unit,
    onClearSeenShorts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Account Profile Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "VibeTube Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Offline-first • Privacy protected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsDivider(startIndent = false)
        Spacer(modifier = Modifier.height(8.dp))

        SettingsSectionHeader(title = "ACTIVITY & BACKUP")
        SettingsRow(
            title = "Watch history",
            subtitle = "View and manage previously played videos",
            icon = Icons.Default.History,
            onClick = onViewHistory
        )
        SettingsDivider()
        SettingsRow(
            title = "Backup & restore",
            subtitle = "Export or import local playlists and history",
            icon = Icons.Default.Storage,
            onClick = onDataManagement
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "CACHE & RECOMMENDATIONS")
        SettingsRow(
            title = "Reset watched Shorts history",
            subtitle = "Clear circular buffer of watched short videos",
            icon = Icons.Default.VideoLibrary,
            onClick = onClearSeenShorts
        )
        SettingsDivider()
        SettingsRow(
            title = "Clear personalized interests",
            subtitle = "Reset machine-learned content affinity model",
            icon = Icons.Default.AutoAwesome,
            onClick = onClearInterests
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "DANGER ZONE")
        SettingsRow(
            title = "Clear watch history",
            subtitle = "Permanently remove all video watch logs",
            icon = Icons.Default.History,
            isDestructive = true,
            onClick = onClearWatchHistory
        )
        SettingsDivider()
        SettingsRow(
            title = "Clear search history",
            subtitle = "Erase all recorded search queries on this device",
            icon = Icons.Default.Delete,
            isDestructive = true,
            onClick = onClearSearchHistory
        )
    }
}

@Composable
fun GeneralSubscreen(
    appLanguage: String?,
    onOpenLanguageSheet: () -> Unit,
    isSearchGridView: Boolean,
    onSetSearchGridView: (Boolean) -> Unit,
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "INTERFACE")
        val currentLanguageDisplay = appLanguage?.let { tag ->
            val locale = java.util.Locale.forLanguageTag(tag)
            locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
        } ?: stringResource(R.string.system_default)

        SettingsRow(
            title = "Language",
            subtitle = "App display language",
            icon = Icons.Default.Language,
            trailingText = currentLanguageDisplay,
            onClick = onOpenLanguageSheet
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Grid layout for search",
            subtitle = "Show search results in a grid instead of a list",
            icon = Icons.Default.GridView,
            checked = isSearchGridView,
            onCheckedChange = onSetSearchGridView
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "UPDATES")
        SettingsRow(
            title = "App updates",
            subtitle = "Updates are checked automatically",
            icon = Icons.Default.SystemUpdate,
            trailingIcon = null,
            onClick = null
        )

        if (updateInfo.hasUpdate) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Update Available: v${updateInfo.latestVersion}",
                        style = MaterialTheme.typography.titleSmall,
                        color = VibeTubeRed,
                        fontWeight = FontWeight.Bold
                    )
                    if (updateInfo.releaseNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, updateInfo.updateUrl.toUri())
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Download from GitHub", color = VibeTubeRed, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = VibeTubeRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaybackSubscreen(
    preferredQuality: String,
    onOpenQualitySheet: () -> Unit,
    isAutoplayEnabled: Boolean,
    onSetAutoplayEnabled: (Boolean) -> Unit,
    playbackSpeed: Float,
    onOpenSpeedSheet: () -> Unit,
    subtitleFontSize: Float,
    onOpenCaptionsSheet: () -> Unit,
    isBackgroundPlayEnabled: Boolean,
    onSetBackgroundPlayEnabled: (Boolean) -> Unit,
    isPlayerGesturesEnabled: Boolean,
    onSetPlayerGesturesEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "VIDEO & CONTROLS")
        SettingsRow(
            title = "Video quality",
            subtitle = "Default playback resolution budget",
            icon = Icons.Default.VideoSettings,
            trailingText = preferredQuality,
            onClick = onOpenQualitySheet
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Autoplay",
            subtitle = "Play subsequent recommended videos automatically",
            icon = Icons.Default.PlayCircleOutline,
            checked = isAutoplayEnabled,
            onCheckedChange = onSetAutoplayEnabled
        )
        SettingsDivider()
        val speedText = if (playbackSpeed == 1.0f) "1.0x" else "${playbackSpeed}x"
        SettingsRow(
            title = "Playback speed",
            subtitle = "Default media playback rate",
            icon = Icons.Default.Speed,
            trailingText = speedText,
            onClick = onOpenSpeedSheet
        )
        SettingsDivider()
        SettingsRow(
            title = "Captions",
            subtitle = "Font size and background styling",
            icon = Icons.Default.ClosedCaption,
            trailingText = "${subtitleFontSize.toInt()}sp",
            onClick = onOpenCaptionsSheet
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "BACKGROUND & GESTURES")
        SettingsSwitchRow(
            title = "Background playback",
            subtitle = "Continue playing audio when app is backgrounded",
            icon = Icons.Default.Headphones,
            checked = isBackgroundPlayEnabled,
            onCheckedChange = onSetBackgroundPlayEnabled
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Player gestures",
            subtitle = "Swipe for volume and brightness, double tap to seek",
            icon = Icons.Default.TouchApp,
            checked = isPlayerGesturesEnabled,
            onCheckedChange = onSetPlayerGesturesEnabled
        )
    }
}

@Composable
fun AudioBackgroundSubscreen(
    isBackgroundPlayEnabled: Boolean,
    onSetBackgroundPlayEnabled: (Boolean) -> Unit,
    isPipSupported: Boolean,
    isPipEnabled: Boolean,
    onSetPipEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "BACKGROUND PLAYBACK")
        SettingsSwitchRow(
            title = "Background playback",
            subtitle = "Keep audio playing when screen is turned off or when switching apps",
            icon = Icons.Default.PlayArrow,
            checked = isBackgroundPlayEnabled,
            onCheckedChange = onSetBackgroundPlayEnabled
        )

        if (isPipSupported) {
            SettingsDivider()
            SettingsSwitchRow(
                title = "Picture-in-Picture",
                subtitle = "Automatically shrink player to floating window upon home navigation",
                icon = Icons.Default.PictureInPicture,
                checked = isPipEnabled,
                onCheckedChange = onSetPipEnabled
            )
        }
    }
}

@Composable
fun DisplaySubscreen(
    isAmbientModeEnabled: Boolean,
    onSetAmbientModeEnabled: (Boolean) -> Unit,
    isPlayerGesturesEnabled: Boolean,
    onSetPlayerGesturesEnabled: (Boolean) -> Unit,
    isAnimationsEnabled: Boolean,
    onSetAnimationsEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "PLAYER APPEARANCE")
        SettingsSwitchRow(
            title = "Ambient Mode",
            subtitle = "Soft glow effect around the video matching video colors",
            icon = Icons.Default.Lightbulb,
            checked = isAmbientModeEnabled,
            onCheckedChange = onSetAmbientModeEnabled
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Player gestures",
            subtitle = "Vertical swipe for volume & brightness, double-tap seek",
            icon = Icons.Default.TouchApp,
            checked = isPlayerGesturesEnabled,
            onCheckedChange = onSetPlayerGesturesEnabled
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Animations",
            subtitle = "Smooth motion and transition effects across the app",
            icon = Icons.Default.Animation,
            checked = isAnimationsEnabled,
            onCheckedChange = onSetAnimationsEnabled
        )
    }
}

@Composable
fun DownloadsSubscreen(
    downloads: List<DownloadEntity>,
    preferredQuality: String,
    isAutoCacheCleanerEnabled: Boolean = true,
    cacheCleanerThresholdMb: Long = 500L,
    lastCacheCleanupTime: Long = 0L,
    currentCacheSize: Long = 0L,
    isCacheCleaning: Boolean = false,
    onToggleAutoCacheCleaner: (Boolean) -> Unit = {},
    onSetCacheThresholdMb: (Long) -> Unit = {},
    onRunManualCacheCleanup: () -> Unit = {},
    onOpenQualitySheet: () -> Unit,
    onClearAllDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val totalBytes = remember(downloads) { downloads.sumOf { it.downloadedSize } }
    val count = downloads.size
    var showThresholdDialog by remember { mutableStateOf(false) }

    val formattedLastCleanup = remember(lastCacheCleanupTime) {
        if (lastCacheCleanupTime <= 0L) {
            "Never"
        } else {
            try {
                java.text.SimpleDateFormat("MMM d, yyyy, h:mm a", java.util.Locale.getDefault())
                    .format(java.util.Date(lastCacheCleanupTime))
            } catch (_: Exception) {
                "Recently"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "DOWNLOAD SETTINGS")
        SettingsRow(
            title = "Download location",
            subtitle = "Storage directory for offline media",
            icon = Icons.Default.Folder,
            trailingText = "Internal / VibeTube",
            onClick = {
                try {
                    val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Downloads are stored locally inside VibeTube", Toast.LENGTH_SHORT).show()
                }
            }
        )
        SettingsDivider()
        SettingsRow(
            title = "Download quality",
            subtitle = "Default resolution for offline media saves",
            icon = Icons.Default.HighQuality,
            trailingText = preferredQuality,
            onClick = onOpenQualitySheet
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "AUTO CACHE CLEANER")

        SettingsSwitchRow(
            title = "Auto Cache Cleaner",
            subtitle = "Automatically remove temporary cache & junk files",
            icon = Icons.Default.CleaningServices,
            checked = isAutoCacheCleanerEnabled,
            onCheckedChange = onToggleAutoCacheCleaner
        )
        SettingsDivider()
        SettingsRow(
            title = "Clean when cache >",
            subtitle = "Threshold to trigger automatic cache cleaning",
            icon = Icons.Default.Speed,
            trailingText = "$cacheCleanerThresholdMb MB",
            onClick = { showThresholdDialog = true }
        )
        SettingsDivider()
        SettingsRow(
            title = "Last cleanup",
            subtitle = "Time of last temporary cache maintenance",
            icon = Icons.Default.Schedule,
            trailingText = formattedLastCleanup
        )
        SettingsDivider()
        SettingsRow(
            title = "Clean Now",
            subtitle = "Safe temporary cache: ${formatStorageBytes(currentCacheSize)}",
            icon = Icons.Default.CleaningServices,
            trailingText = if (isCacheCleaning) "Cleaning..." else "Clean",
            onClick = { if (!isCacheCleaning) onRunManualCacheCleanup() }
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "STORAGE")

        // Compact Storage Stats Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatStorageBytes(totalBytes)} used for downloads",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count file${if (count == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val progress = remember(totalBytes) {
                val budget = 5L * 1024 * 1024 * 1024 // 5GB reference
                (totalBytes.toFloat() / budget).coerceIn(0.01f, 1f)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = VibeTubeRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (count > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow(
                title = "Delete all downloads",
                subtitle = "Permanently remove all offline video files",
                icon = Icons.Default.DeleteSweep,
                isDestructive = true,
                onClick = onClearAllDownloads
            )
        }
    }

    if (showThresholdDialog) {
        val options = listOf(250L, 500L, 1000L, 2000L)
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Clean when cache exceeds") },
            text = {
                Column {
                    options.forEach { threshold ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetCacheThresholdMb(threshold)
                                    showThresholdDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (cacheCleanerThresholdMb == threshold),
                                onClick = {
                                    onSetCacheThresholdMb(threshold)
                                    showThresholdDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (threshold >= 1000L) "${threshold / 1000} GB (${threshold} MB)" else "$threshold MB",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivacySubscreen(
    isIncognitoMode: Boolean,
    onSetIncognitoMode: (Boolean) -> Unit,
    isSearchHistoryPaused: Boolean,
    onSetSearchHistoryPaused: (Boolean) -> Unit,
    isRecommendationsPaused: Boolean,
    onSetRecommendationsPaused: (Boolean) -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearInterests: () -> Unit,
    onClearSeenShorts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "INCOGNITO")
        SettingsSwitchRow(
            title = "Incognito Mode",
            subtitle = "Do not save history or use personalized data for sessions",
            icon = Icons.Default.VisibilityOff,
            checked = isIncognitoMode,
            onCheckedChange = onSetIncognitoMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsSectionHeader(title = "TRACKING & DATA")
        SettingsSwitchRow(
            title = "Pause search history",
            subtitle = "Don't save search queries or provide history suggestions",
            icon = Icons.Default.Pause,
            checked = isSearchHistoryPaused,
            onCheckedChange = onSetSearchHistoryPaused
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Pause recommendations",
            subtitle = "Stop learning your viewing interests temporarily",
            icon = Icons.Default.Recommend,
            checked = isRecommendationsPaused,
            onCheckedChange = onSetRecommendationsPaused
        )

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "HISTORY MANAGEMENT")
        SettingsRow(
            title = "Clear search history",
            subtitle = "Reset all saved search entries",
            icon = Icons.Default.Delete,
            isDestructive = true,
            onClick = onClearSearchHistory
        )
        SettingsDivider()
        SettingsRow(
            title = "Clear watch history",
            subtitle = "Wipe local playback log permanently",
            icon = Icons.Default.History,
            isDestructive = true,
            onClick = onClearWatchHistory
        )
        SettingsDivider()
        SettingsRow(
            title = "Reset learned recommendations",
            subtitle = "Wipe personalization model cache",
            icon = Icons.Default.AutoAwesome,
            isDestructive = true,
            onClick = onClearInterests
        )
        SettingsDivider()
        SettingsRow(
            title = "Reset watched Shorts history",
            subtitle = "Erase sliding cache of viewed Shorts",
            icon = Icons.Default.VideoLibrary,
            isDestructive = true,
            onClick = onClearSeenShorts
        )
    }
}

@Composable
fun DataUsageSubscreen(
    preferredQuality: String,
    onOpenQualitySheet: () -> Unit,
    isAutoplayEnabled: Boolean,
    onSetAutoplayEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "NETWORK PREFERENCES")
        SettingsRow(
            title = "Preferred video quality",
            subtitle = "Maximum default playback resolution",
            icon = Icons.Default.NetworkCheck,
            trailingText = preferredQuality,
            onClick = onOpenQualitySheet
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Autoplay next video",
            subtitle = "Stream upcoming video on mobile and Wi-Fi networks",
            icon = Icons.Default.PlayCircleOutline,
            checked = isAutoplayEnabled,
            onCheckedChange = onSetAutoplayEnabled
        )
    }
}

@Composable
fun AppearanceSubscreen(
    appTheme: String,
    onSetAppTheme: (String) -> Unit,
    isDynamicColorEnabled: Boolean,
    onSetDynamicColorEnabled: (Boolean) -> Unit,
    isAmbientModeEnabled: Boolean,
    onSetAmbientModeEnabled: (Boolean) -> Unit,
    isAnimationsEnabled: Boolean,
    onSetAnimationsEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        SettingsSectionHeader(title = "THEME")
        SettingsRadioRow(
            title = "System default",
            subtitle = "Match device display settings",
            selected = appTheme == "System",
            onClick = { onSetAppTheme("System") }
        )
        SettingsRadioRow(
            title = "Light",
            subtitle = "White / clean bright surfaces",
            selected = appTheme == "Light",
            onClick = { onSetAppTheme("Light") }
        )
        SettingsRadioRow(
            title = "Dark",
            subtitle = "Near-black deep dark surfaces",
            selected = appTheme == "Dark",
            onClick = { onSetAppTheme("Dark") }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(modifier = Modifier.height(16.dp))
            SettingsSectionHeader(title = "MATERIAL YOU")
            SettingsSwitchRow(
                title = "Dynamic color",
                subtitle = "Harmonize app colors with device wallpaper theme",
                icon = Icons.Default.Palette,
                checked = isDynamicColorEnabled,
                onCheckedChange = onSetDynamicColorEnabled
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SettingsSectionHeader(title = "PLAYER APPEARANCE")
        SettingsSwitchRow(
            title = "Ambient Mode",
            subtitle = "Soft glow effect around video matching content",
            icon = Icons.Default.Lightbulb,
            checked = isAmbientModeEnabled,
            onCheckedChange = onSetAmbientModeEnabled
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = "Animations",
            subtitle = "Smooth motion and transition effects across the app",
            icon = Icons.Default.Animation,
            checked = isAnimationsEnabled,
            onCheckedChange = onSetAnimationsEnabled
        )
    }
}

@Composable
fun AboutSubscreen(
    onOpenLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. App Identity Section (Logo + Name + Version)
        Surface(
            modifier = Modifier.size(80.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_splash_logo),
                    contentDescription = "ZyvoTube Logo",
                    tint = VibeTubeRed,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ZyvoTube",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Version ${AppVersion.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2. DEVELOPER PROFILE SECTION
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "DEVELOPER",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            // Enhanced Developer Profile Hero
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Image Placeholder with Gradient Border
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(VibeTubeRed, DarkRed)
                                )
                            )
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RH",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = VibeTubeRed
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Rahul Haldar",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Lead Developer & Designer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = VibeTubeRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Independent Android Dev",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = VibeTubeRed,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Developer Links - Modern Grid/List
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeveloperSocialButton(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_github,
                    label = "GitHub",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Rahulhaldar".toUri())
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    }
                )
                DeveloperSocialButton(
                    modifier = Modifier.weight(1f),
                    icon = R.drawable.ic_instagram,
                    label = "Instagram",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://www.instagram.com/mr.haldar__/".toUri())
                        try { context.startActivity(intent) } catch (e: Exception) { }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Legal & Source Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "LEGAL & POLICY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Column {
                    AboutActionRow(
                        icon = Icons.Default.Language,
                        title = "Official website",
                        subtitle = "https://www.zyvotube.in/",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://www.zyvotube.in/".toUri())
                            try { context.startActivity(intent) } catch (e: Exception) { }
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    AboutActionRow(
                        icon = Icons.Default.Gavel,
                        title = "Open source licenses",
                        subtitle = "GPL-3.0 and third-party notices",
                        onClick = onOpenLicenses
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )

                    AboutActionRow(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "Your data remains local and private",
                        onClick = onOpenPrivacy
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Footer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "ZyvoTube",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Crafted with ♥ in India",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DeveloperSocialButton(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
