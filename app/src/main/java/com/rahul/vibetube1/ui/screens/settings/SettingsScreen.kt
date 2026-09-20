/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahul.vibetube1.ui.components.LanguageSelectionSheet
import com.rahul.vibetube1.ui.theme.LocalAnimationsEnabled
import com.rahul.vibetube1.ui.theme.VibeTubeRed

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    updateViewModel: UpdateViewModel = hiltViewModel(),
    onViewHistory: () -> Unit = {},
    onDataManagement: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val isPipSupported = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }

    val isSearchHistoryPaused by viewModel.isSearchHistoryPaused.collectAsStateWithLifecycle()
    val isPipEnabled by viewModel.isPipEnabled.collectAsStateWithLifecycle()
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val isRecommendationsPaused by viewModel.isRecommendationsPaused.collectAsStateWithLifecycle()
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsStateWithLifecycle()
    val isPlayerGesturesEnabled by viewModel.isPlayerGesturesEnabled.collectAsStateWithLifecycle()
    val isAmbientModeEnabled by viewModel.isAmbientModeEnabled.collectAsStateWithLifecycle()
    val subtitleFontSize by viewModel.subtitleFontSize.collectAsStateWithLifecycle()
    val subtitleBackgroundOpacity by viewModel.subtitleBackgroundOpacity.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val availableLocales = viewModel.availableLocales
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isAnimationsEnabled by viewModel.isAnimationsEnabled.collectAsStateWithLifecycle()
    val downloads by viewModel.allDownloads.collectAsStateWithLifecycle()
    
    val isAutoCacheCleanerEnabled by viewModel.isAutoCacheCleanerEnabled.collectAsStateWithLifecycle()
    val cacheCleanerThresholdMb by viewModel.cacheCleanerThresholdMb.collectAsStateWithLifecycle()
    val lastCacheCleanupTime by viewModel.lastCacheCleanupTime.collectAsStateWithLifecycle()
    val currentCacheSize by viewModel.currentCacheSize.collectAsStateWithLifecycle()
    val isCacheCleaning by viewModel.isCacheCleaning.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.cleanerSnackbarMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }
    
    val proxyEnabled by viewModel.proxyEnabled.collectAsStateWithLifecycle()
    val proxyHost by viewModel.proxyHost.collectAsStateWithLifecycle()
    val proxyPort by viewModel.proxyPort.collectAsStateWithLifecycle()
    val isSearchGridView by viewModel.isSearchGridView.collectAsStateWithLifecycle()
    val isIncognitoMode by viewModel.isIncognitoMode.collectAsStateWithLifecycle()

    var currentSubscreen by rememberSaveable { mutableStateOf(SettingsSubscreen.Main) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // Dialog & Sheet States
    var showClearWatchHistoryDialog by remember { mutableStateOf(false) }
    var showClearSearchHistoryDialog by remember { mutableStateOf(false) }
    var showClearInterestsDialog by remember { mutableStateOf(false) }
    var showClearSeenShortsDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showCaptionsSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }

    // Navigation and Back Handling
    BackHandler(enabled = isSearchActive || currentSubscreen != SettingsSubscreen.Main) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else if (currentSubscreen != SettingsSubscreen.Main) {
            currentSubscreen = SettingsSubscreen.Main
        }
    }

    Scaffold(
        topBar = {
            SettingsTopAppBar(
                currentSubscreen = currentSubscreen,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onActivateSearch = { isSearchActive = true },
                onDeactivateSearch = {
                    isSearchActive = false
                    searchQuery = ""
                },
                onNavigateBack = {
                    if (isSearchActive) {
                        isSearchActive = false
                        searchQuery = ""
                    } else if (currentSubscreen != SettingsSubscreen.Main) {
                        currentSubscreen = SettingsSubscreen.Main
                    } else {
                        onBack()
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        val animationsEnabled = LocalAnimationsEnabled.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchActive && searchQuery.isNotBlank()) {
                // Search Results View
                SettingsSearchResults(
                    searchQuery = searchQuery,
                    onNavigateToSubscreen = { subscreen ->
                        isSearchActive = false
                        searchQuery = ""
                        currentSubscreen = subscreen
                    },
                    onOpenQualitySheet = { showQualitySheet = true },
                    onOpenSpeedSheet = { showSpeedSheet = true },
                    onOpenCaptionsSheet = { showCaptionsSheet = true },
                    onOpenLanguageSheet = { showLanguageSheet = true },
                    onOpenLicensesDialog = { showLicensesDialog = true },
                    onClearWatchHistory = { showClearWatchHistoryDialog = true },
                    onClearSearchHistory = { showClearSearchHistoryDialog = true },
                    onClearInterests = { showClearInterestsDialog = true },
                    onClearSeenShorts = { showClearSeenShortsDialog = true },
                    onClearDownloads = { showClearDownloadsDialog = true }
                )
            } else {
                // Content transition between Main and Subscreens
                AnimatedContent(
                    targetState = currentSubscreen,
                    transitionSpec = {
                        if (animationsEnabled) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            EnterTransition.None togetherWith ExitTransition.None
                        }
                    },
                    label = "SettingsSubscreenTransition"
                ) { subscreen ->
                    when (subscreen) {
                        SettingsSubscreen.Main -> {
                            SettingsMainList(
                                appTheme = appTheme,
                                onNavigateToSubscreen = { currentSubscreen = it }
                            )
                        }
                        SettingsSubscreen.Account -> {
                            AccountSubscreen(
                                onViewHistory = onViewHistory,
                                onDataManagement = onDataManagement,
                                onClearWatchHistory = { showClearWatchHistoryDialog = true },
                                onClearSearchHistory = { showClearSearchHistoryDialog = true },
                                onClearInterests = { showClearInterestsDialog = true },
                                onClearSeenShorts = { showClearSeenShortsDialog = true }
                            )
                        }
                        SettingsSubscreen.General -> {
                            GeneralSubscreen(
                                appLanguage = appLanguage,
                                onOpenLanguageSheet = { showLanguageSheet = true },
                                isSearchGridView = isSearchGridView,
                                onSetSearchGridView = viewModel::setSearchGridView,
                                updateInfo = updateInfo
                            )
                        }
                        SettingsSubscreen.Playback -> {
                            PlaybackSubscreen(
                                preferredQuality = preferredQuality,
                                onOpenQualitySheet = { showQualitySheet = true },
                                isAutoplayEnabled = isAutoplayEnabled,
                                onSetAutoplayEnabled = viewModel::setAutoplayEnabled,
                                playbackSpeed = playbackSpeed,
                                onOpenSpeedSheet = { showSpeedSheet = true },
                                subtitleFontSize = subtitleFontSize,
                                onOpenCaptionsSheet = { showCaptionsSheet = true },
                                isBackgroundPlayEnabled = isBackgroundPlayEnabled,
                                onSetBackgroundPlayEnabled = viewModel::setBackgroundPlayEnabled,
                                isPlayerGesturesEnabled = isPlayerGesturesEnabled,
                                onSetPlayerGesturesEnabled = viewModel::setPlayerGesturesEnabled
                            )
                        }
                        SettingsSubscreen.AudioBackground -> {
                            AudioBackgroundSubscreen(
                                isBackgroundPlayEnabled = isBackgroundPlayEnabled,
                                onSetBackgroundPlayEnabled = viewModel::setBackgroundPlayEnabled,
                                isPipSupported = isPipSupported,
                                isPipEnabled = isPipEnabled,
                                onSetPipEnabled = viewModel::setPipEnabled
                            )
                        }
                        SettingsSubscreen.Display -> {
                            DisplaySubscreen(
                                isAmbientModeEnabled = isAmbientModeEnabled,
                                onSetAmbientModeEnabled = viewModel::setAmbientModeEnabled,
                                isPlayerGesturesEnabled = isPlayerGesturesEnabled,
                                onSetPlayerGesturesEnabled = viewModel::setPlayerGesturesEnabled,
                                isAnimationsEnabled = isAnimationsEnabled,
                                onSetAnimationsEnabled = viewModel::setAnimationsEnabled
                            )
                        }
                        SettingsSubscreen.Downloads -> {
                            DownloadsSubscreen(
                                downloads = downloads,
                                preferredQuality = preferredQuality,
                                isAutoCacheCleanerEnabled = isAutoCacheCleanerEnabled,
                                cacheCleanerThresholdMb = cacheCleanerThresholdMb,
                                lastCacheCleanupTime = lastCacheCleanupTime,
                                currentCacheSize = currentCacheSize,
                                isCacheCleaning = isCacheCleaning,
                                onToggleAutoCacheCleaner = viewModel::toggleAutoCacheCleaner,
                                onSetCacheThresholdMb = viewModel::setCacheCleanerThresholdMb,
                                onRunManualCacheCleanup = viewModel::runManualCacheCleanup,
                                onOpenQualitySheet = { showQualitySheet = true },
                                onClearAllDownloads = { showClearDownloadsDialog = true }
                            )
                        }
                        SettingsSubscreen.Privacy -> {
                            PrivacySubscreen(
                                isIncognitoMode = isIncognitoMode,
                                onSetIncognitoMode = viewModel::setIncognitoMode,
                                isSearchHistoryPaused = isSearchHistoryPaused,
                                onSetSearchHistoryPaused = viewModel::setSearchHistoryPaused,
                                isRecommendationsPaused = isRecommendationsPaused,
                                onSetRecommendationsPaused = viewModel::setRecommendationsPaused,
                                onClearSearchHistory = { showClearSearchHistoryDialog = true },
                                onClearWatchHistory = { showClearWatchHistoryDialog = true },
                                onClearInterests = { showClearInterestsDialog = true },
                                onClearSeenShorts = { showClearSeenShortsDialog = true }
                            )
                        }
                        SettingsSubscreen.DataUsage -> {
                            DataUsageSubscreen(
                                preferredQuality = preferredQuality,
                                onOpenQualitySheet = { showQualitySheet = true },
                                isAutoplayEnabled = isAutoplayEnabled,
                                onSetAutoplayEnabled = viewModel::setAutoplayEnabled
                            )
                        }
                        SettingsSubscreen.Appearance -> {
                            AppearanceSubscreen(
                                appTheme = appTheme,
                                onSetAppTheme = viewModel::setAppTheme,
                                isDynamicColorEnabled = isDynamicColorEnabled,
                                onSetDynamicColorEnabled = viewModel::setDynamicColorEnabled,
                                isAmbientModeEnabled = isAmbientModeEnabled,
                                onSetAmbientModeEnabled = viewModel::setAmbientModeEnabled,
                                isAnimationsEnabled = isAnimationsEnabled,
                                onSetAnimationsEnabled = viewModel::setAnimationsEnabled
                            )
                        }
                        SettingsSubscreen.Advanced -> {
                            AdvancedSubscreen(
                                isProxyEnabled = proxyEnabled,
                                proxyHost = proxyHost,
                                proxyPort = proxyPort,
                                onSetProxySettings = viewModel::setProxySettings,
                                onResetPlayerSettings = viewModel::resetPlayerSettings,
                                onResetAppearanceSettings = viewModel::resetAppearanceSettings,
                                onResetAllSettings = viewModel::resetAllSettings
                            )
                        }
                        SettingsSubscreen.About -> {
                            AboutSubscreen(
                                onOpenLicenses = { showLicensesDialog = true },
                                onOpenPrivacy = { currentSubscreen = SettingsSubscreen.Privacy }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showClearWatchHistoryDialog) {
        SettingsConfirmationDialog(
            title = "Clear watch history?",
            message = "This will permanently wipe all watched video logs from this device. This action cannot be undone.",
            confirmText = "Clear",
            isDestructive = true,
            onConfirm = {
                viewModel.clearHistory()
                showClearWatchHistoryDialog = false
                Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearWatchHistoryDialog = false }
        )
    }

    if (showClearSearchHistoryDialog) {
        SettingsConfirmationDialog(
            title = "Clear search history?",
            message = "This will permanently erase all search queries saved on this device.",
            confirmText = "Clear",
            isDestructive = true,
            onConfirm = {
                viewModel.clearSearchHistory()
                showClearSearchHistoryDialog = false
                Toast.makeText(context, "Search history erased", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearSearchHistoryDialog = false }
        )
    }

    if (showClearInterestsDialog) {
        SettingsConfirmationDialog(
            title = "Reset personalized interests?",
            message = "This will reset all learned recommendations back to their initial state.",
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                viewModel.clearLearnedInterests()
                showClearInterestsDialog = false
                Toast.makeText(context, "Personalized interests reset", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearInterestsDialog = false }
        )
    }

    if (showClearSeenShortsDialog) {
        SettingsConfirmationDialog(
            title = "Reset watched Shorts?",
            message = "This will clear the history of short videos you have already watched.",
            confirmText = "Reset",
            isDestructive = true,
            onConfirm = {
                viewModel.clearSeenShorts()
                showClearSeenShortsDialog = false
                Toast.makeText(context, "Shorts history reset", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearSeenShortsDialog = false }
        )
    }

    if (showClearDownloadsDialog) {
        SettingsConfirmationDialog(
            title = "Delete all downloads?",
            message = "This will permanently delete all downloaded offline video files from your device.",
            confirmText = "Delete All",
            isDestructive = true,
            onConfirm = {
                viewModel.clearAllDownloads()
                showClearDownloadsDialog = false
                Toast.makeText(context, "All downloads deleted", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearDownloadsDialog = false }
        )
    }

    if (showQualitySheet) {
        QualitySelectionSheet(
            currentQuality = preferredQuality,
            onQualitySelected = viewModel::setPreferredQuality,
            onDismiss = { showQualitySheet = false }
        )
    }

    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            currentSpeed = playbackSpeed,
            onSpeedSelected = viewModel::setPlaybackSpeed,
            onDismiss = { showSpeedSheet = false }
        )
    }

    if (showCaptionsSheet) {
        CaptionsSheet(
            fontSize = subtitleFontSize,
            opacity = subtitleBackgroundOpacity,
            onFontSizeChange = viewModel::setSubtitleFontSize,
            onOpacityChange = viewModel::setSubtitleBackgroundOpacity,
            onDismiss = { showCaptionsSheet = false }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            availableLocales = availableLocales,
            currentLanguageTag = appLanguage,
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = { selectedTag ->
                viewModel.setAppLanguage(selectedTag)
                showLanguageSheet = false
            }
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopAppBar(
    currentSubscreen: SettingsSubscreen,
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onActivateSearch: () -> Unit,
    onDeactivateSearch: () -> Unit,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            if (isSearchActive) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(VibeTubeRed),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search settings…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = if (currentSubscreen == SettingsSubscreen.Main) "Settings" else currentSubscreen.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (currentSubscreen == SettingsSubscreen.Main) {
                if (isSearchActive) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onActivateSearch) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

private data class SearchableItem(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun SettingsSearchResults(
    searchQuery: String,
    onNavigateToSubscreen: (SettingsSubscreen) -> Unit,
    onOpenQualitySheet: () -> Unit,
    onOpenSpeedSheet: () -> Unit,
    onOpenCaptionsSheet: () -> Unit,
    onOpenLanguageSheet: () -> Unit,
    onOpenLicensesDialog: () -> Unit,
    onClearWatchHistory: () -> Unit,
    onClearSearchHistory: () -> Unit,
    onClearInterests: () -> Unit,
    onClearSeenShorts: () -> Unit,
    onClearDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember {
        listOf(
            SearchableItem("Account", "Manage your account and profile", "ACCOUNT", Icons.Default.Person) {
                onNavigateToSubscreen(SettingsSubscreen.Account)
            },
            SearchableItem("Watch history", "Manage your watched videos", "ACCOUNT", Icons.Default.History) {
                onNavigateToSubscreen(SettingsSubscreen.Account)
            },
            SearchableItem("Backup & restore", "Export and import local playlists", "ACCOUNT", Icons.Default.Storage) {
                onNavigateToSubscreen(SettingsSubscreen.Account)
            },
            SearchableItem("Language", "App display language", "GENERAL", Icons.Default.Language) {
                onOpenLanguageSheet()
            },
            SearchableItem("Check for updates", "Notify when new version is available", "GENERAL", Icons.Default.SystemUpdate) {
                onNavigateToSubscreen(SettingsSubscreen.General)
            },
            SearchableItem("Video quality", "Default video playback resolution", "PLAYBACK", Icons.Default.VideoSettings) {
                onOpenQualitySheet()
            },
            SearchableItem("Autoplay", "Play subsequent videos automatically", "PLAYBACK", Icons.Default.PlayCircleOutline) {
                onNavigateToSubscreen(SettingsSubscreen.Playback)
            },
            SearchableItem("Playback speed", "Default media playback rate", "PLAYBACK", Icons.Default.Speed) {
                onOpenSpeedSheet()
            },
            SearchableItem("Captions & subtitles", "Font size and opacity customizer", "PLAYBACK", Icons.Default.ClosedCaption) {
                onOpenCaptionsSheet()
            },
            SearchableItem("Background playback", "Keep audio playing in background", "AUDIO & BACKGROUND", Icons.Default.Headphones) {
                onNavigateToSubscreen(SettingsSubscreen.AudioBackground)
            },
            SearchableItem("Picture-in-Picture", "Floating video window when leaving app", "AUDIO & BACKGROUND", Icons.Default.PictureInPicture) {
                onNavigateToSubscreen(SettingsSubscreen.AudioBackground)
            },
            SearchableItem("Ambient Mode", "Soft glow effect around player", "DISPLAY", Icons.Default.Lightbulb) {
                onNavigateToSubscreen(SettingsSubscreen.Display)
            },
            SearchableItem("Player gestures", "Swipe controls for brightness and volume", "DISPLAY", Icons.Default.TouchApp) {
                onNavigateToSubscreen(SettingsSubscreen.Display)
            },
            SearchableItem("Animations", "Smooth UI motion transitions", "DISPLAY", Icons.Default.Animation) {
                onNavigateToSubscreen(SettingsSubscreen.Display)
            },
            SearchableItem("Downloads", "Location, quality and storage", "DOWNLOADS", Icons.Default.ArrowDownward) {
                onNavigateToSubscreen(SettingsSubscreen.Downloads)
            },
            SearchableItem("Delete all downloads", "Permanently remove offline files", "DOWNLOADS", Icons.Default.DeleteSweep) {
                onClearDownloads()
            },
            SearchableItem("Pause search history", "Stop recording search terms", "PRIVACY", Icons.Default.Pause) {
                onNavigateToSubscreen(SettingsSubscreen.Privacy)
            },
            SearchableItem("Pause recommendations", "Stop learning your interests", "PRIVACY", Icons.Default.Recommend) {
                onNavigateToSubscreen(SettingsSubscreen.Privacy)
            },
            SearchableItem("Clear search history", "Erase search query entries", "PRIVACY", Icons.Default.Delete) {
                onClearSearchHistory()
            },
            SearchableItem("Clear watch history", "Erase local playback logs", "PRIVACY", Icons.Default.History) {
                onClearWatchHistory()
            },
            SearchableItem("Theme", "System default, light, dark theme", "APPEARANCE", Icons.Default.Palette) {
                onNavigateToSubscreen(SettingsSubscreen.Appearance)
            },
            SearchableItem("Dynamic color", "Material You system colors", "APPEARANCE", Icons.Default.Palette) {
                onNavigateToSubscreen(SettingsSubscreen.Appearance)
            },
            SearchableItem("About VibeTube", "Version and creator information", "ABOUT", Icons.Default.Info) {
                onNavigateToSubscreen(SettingsSubscreen.About)
            },
            SearchableItem("Open source licenses", "GPL-3.0 and library licenses", "ABOUT", Icons.Default.Article) {
                onOpenLicensesDialog()
            }
        )
    }

    val query = searchQuery.trim().lowercase()
    val filtered = remember(query) {
        items.filter {
            it.title.lowercase().contains(query) ||
            it.subtitle.lowercase().contains(query) ||
            it.category.lowercase().contains(query)
        }
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No matching settings",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(vertical = 8.dp)
        ) {
            items(filtered) { item ->
                SettingsRow(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = item.onClick
                )
                SettingsDivider()
            }
        }
    }
}
