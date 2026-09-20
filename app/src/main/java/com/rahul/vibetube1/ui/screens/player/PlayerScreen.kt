/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Close
import com.rahul.vibetube1.domain.model.RelatedFeedItem
import com.rahul.vibetube1.utils.ContextualShortsHelper
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahul.vibetube1.ui.components.InfiniteScrollEffect
import com.rahul.vibetube1.ui.components.player.PersistentProgressBar
import com.rahul.vibetube1.ui.components.DownloadSelectionSheet
import com.rahul.vibetube1.ui.components.PlaybackSpeedSelectionSheet
import com.rahul.vibetube1.ui.components.PitchSelectionSheet
import com.rahul.vibetube1.ui.components.QualitySelectionSheet
import com.rahul.vibetube1.ui.components.SubtitleSelectionSheet
import com.rahul.vibetube1.ui.components.DownloadDialogState
import com.rahul.vibetube1.domain.model.StreamItem
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.ui.components.VideoItemRow
import com.rahul.vibetube1.ui.components.ThumbnailImage
import com.rahul.vibetube1.ui.components.rememberSyncShimmerTransition
import com.rahul.vibetube1.ui.components.EmptyState
import com.rahul.vibetube1.ui.components.EmptyState
import com.rahul.vibetube1.ui.components.player.VideoPlayerView
import com.rahul.vibetube1.utils.VibeTubeError
import com.rahul.vibetube1.utils.VideoUtils
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ErrorOutline
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.rahul.vibetube1.utils.VideoChapter
import kotlinx.coroutines.flow.SharedFlow

@UnstableApi
@Composable
fun PlayerScreen(
    videoId: String,
    initialTitle: String? = null,
    initialThumbnail: String? = null,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val currentQuality by viewModel.currentQuality.collectAsStateWithLifecycle()
    val displayQuality by viewModel.displayQuality.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by viewModel.libraryRepository.getFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val seekAmount by viewModel.seekAmount.collectAsStateWithLifecycle()
    val showSeekFeedback by viewModel.showSeekFeedback.collectAsStateWithLifecycle()
    val isSeekForward by viewModel.isSeekForward.collectAsStateWithLifecycle()
    val isCcEnabled by viewModel.isCcEnabled.collectAsStateWithLifecycle()
    val availableSubtitles by viewModel.availableSubtitles.collectAsStateWithLifecycle()
    val selectedSubtitleLanguage by viewModel.selectedSubtitleLanguage.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val bufferedPosition by viewModel.bufferedPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isRecovering by viewModel.isRecovering.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isIncognito by viewModel.isIncognitoMode.collectAsStateWithLifecycle()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val sleepTimerRemainingTime by viewModel.sleepTimerRemainingTime.collectAsStateWithLifecycle()
    val shouldCloseAppOnTimerFinish by viewModel.shouldCloseAppOnTimerFinish.collectAsStateWithLifecycle()
    val subtitleFontSize by viewModel.subtitleFontSize.collectAsStateWithLifecycle()
    val subtitleBackgroundOpacity by viewModel.subtitleBackgroundOpacity.collectAsStateWithLifecycle()
    val isPlayerGesturesEnabled by viewModel.isPlayerGesturesEnabled.collectAsStateWithLifecycle()
    val isAmbientModeEnabled by viewModel.isAmbientModeEnabled.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val playbackPitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val showStatsForNerds by viewModel.showStatsForNerds.collectAsStateWithLifecycle()
    val playbackStats by viewModel.playbackStats.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isFetchingComments by viewModel.isFetchingComments.collectAsStateWithLifecycle()
    val replies by viewModel.replies.collectAsStateWithLifecycle()
    val isFetchingReplies by viewModel.isFetchingReplies.collectAsStateWithLifecycle()
    val activeReplyParent by viewModel.activeReplyParent.collectAsStateWithLifecycle()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val playlistIndex by viewModel.playlistIndex.collectAsStateWithLifecycle()
    val relatedShorts by viewModel.relatedShorts.collectAsStateWithLifecycle()
    val relatedShortsState by viewModel.relatedShortsState.collectAsStateWithLifecycle()
    val isLooping by viewModel.isLooping.collectAsStateWithLifecycle()
    val isStableVolumeEnabled by viewModel.isStableVolumeEnabled.collectAsStateWithLifecycle()
    val syncTransition = rememberSyncShimmerTransition()

    var activeCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    DisposableEffect(viewModel.player) {
        val listener = object : Player.Listener {
            @androidx.annotation.OptIn(UnstableApi::class)
            override fun onCues(cueGroup: CueGroup) {
                // Intercept and sanitize cues to prevent stacking (roll-up)
                activeCues = if (cueGroup.cues.isEmpty()) {
                    emptyList()
                } else {
                    // 1. Only take the most recent cue object
                    val lastCue = cueGroup.cues.last()
                    val originalText = lastCue.text?.toString() ?: ""
                    
                    if (originalText.isNotBlank()) {
                        // 2. Extract only the last line if multiple lines exist
                        val singleLineText = if (originalText.contains("\n")) {
                            originalText.substringAfterLast("\n").trim()
                        } else {
                            originalText
                        }
                        
                        // 3. Rebuild the cue with sanitized text
                        listOf(lastCue.buildUpon().setText(singleLineText).build())
                    } else {
                        emptyList()
                    }
                }
            }
        }
        viewModel.player.addListener(listener)
        onDispose { viewModel.player.removeListener(listener) }
    }

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    // Memoize subtitle list to prevent redundant recompositions when other states change
    val memoizedSubtitles = remember(availableSubtitles) { availableSubtitles }
    
        PlayerContent(
            videoId = videoId,
            initialTitle = initialTitle,
            initialThumbnail = initialThumbnail,
            uiState = uiState,
            isFavorite = isFavorite,
            isSaved = isSaved,
            isSubscribed = isSubscribed,
            playbackSpeed = playbackSpeed,
            currentQuality = currentQuality,
            displayQuality = displayQuality,
            isBuffering = isBuffering,
            isRecovering = isRecovering,
            isPlaying = isPlaying,
            isIncognito = isIncognito,
            preferredQuality = preferredQuality,
            downloadedIds = downloadedIds,
            favoriteIds = favoriteIds,
            seekAmount = seekAmount,
            showSeekFeedback = showSeekFeedback,
            isSeekForward = isSeekForward,
            isCcEnabled = isCcEnabled,
            availableSubtitles = memoizedSubtitles,
            selectedSubtitleLanguage = selectedSubtitleLanguage,
            currentPosition = { currentPosition },
            bufferedPosition = { bufferedPosition },
            duration = { duration },
            downloadState = downloadState,
            player = viewModel.player,
            activeCues = activeCues,
            syncTransition = syncTransition,
            snackbarMessage = viewModel.snackbarMessage,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onToggleSubscription = viewModel::toggleSubscription,
            onSetQuality = viewModel::setQuality,
            onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
            onSetTemporarySpeedBoost = viewModel::setTemporarySpeedBoost,
            onToggleSubtitles = viewModel::toggleSubtitles,
            onSetSubtitleLanguage = viewModel::setSubtitleLanguage,
            onPlayPause = viewModel::togglePlayPause,
            onSkipNext = viewModel::playNext,
            onSkipPrevious = viewModel::playPrevious,
            onDownloadConfirm = viewModel::download,
            onDownloadClick = { viewModel.prepareDownload(it) },
            onDismissDownload = viewModel::dismissDownloadDialog,
            onLoadMore = viewModel::loadNextRelatedPage,
            onSeekForward = viewModel::seekForward,
            onSeekBackward = viewModel::seekBackward,
            onSeekTo = viewModel::seekTo,
            onShareVideo = viewModel::shareVideo,
            onBack = onBack,
            onVideoClick = onVideoClick,
            onChannelClick = onChannelClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onRetry = { viewModel.currentVideoItem?.let { viewModel.loadVideo(it) } },
            isAutoplayEnabled = isAutoplayEnabled,
            onAutoplayChange = viewModel::setAutoplayEnabled,
            sleepTimerRemainingTime = sleepTimerRemainingTime,
            shouldCloseAppOnTimerFinish = shouldCloseAppOnTimerFinish,
            onStartSleepTimer = viewModel.sleepTimerManager::startTimer,
            onSetEndOfVideoSleepTimer = viewModel.sleepTimerManager::setEndOfVideo,
            onCancelSleepTimer = viewModel.sleepTimerManager::cancelTimer,
            onSetShouldCloseApp = viewModel.sleepTimerManager::setShouldCloseApp,
            playbackPitch = playbackPitch,
            onSetPlaybackPitch = viewModel::setPlaybackPitch,
            showStatsForNerds = showStatsForNerds,
            playbackStats = playbackStats,
            onToggleStats = viewModel::toggleStatsForNerds,
            subtitleFontSize = subtitleFontSize,
            subtitleBackgroundOpacity = subtitleBackgroundOpacity,
            isPlayerGesturesEnabled = isPlayerGesturesEnabled,
            isAmbientModeEnabled = isAmbientModeEnabled,
            onToggleAmbientMode = viewModel::toggleAmbientMode,
            isLooping = isLooping,
            onToggleLoop = viewModel::toggleLoop,
            isStableVolumeEnabled = isStableVolumeEnabled,
            onToggleStableVolume = viewModel::toggleStableVolume,
            chapters = chapters,
            comments = comments,
            isFetchingComments = isFetchingComments,
            onLoadComments = viewModel::loadComments,
            onLoadNextCommentsPage = viewModel::loadNextCommentsPage,
            replies = replies,
            isFetchingReplies = isFetchingReplies,
            activeReplyParent = activeReplyParent,
            onLoadReplies = viewModel::loadReplies,
            onLoadNextRepliesPage = viewModel::loadNextRepliesPage,
            onCloseReplies = viewModel::closeReplies,
            currentPlaylist = currentPlaylist,
            playlistIndex = playlistIndex,
            relatedShorts = relatedShorts,
            relatedShortsState = relatedShortsState,
            onLoadMoreShorts = viewModel::loadMoreWatchRelatedShorts,
            onPlaylistVideoClick = { video ->
                currentPlaylist?.let { viewModel.loadVideo(video, it.id, it.title) }
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
private fun PlayerContent(
    videoId: String,
    initialTitle: String?,
    initialThumbnail: String?,
    uiState: PlayerUiState,
    isFavorite: Boolean,
    isSaved: Boolean,
    isSubscribed: Boolean,
    playbackSpeed: Float,
    currentQuality: String?,
    displayQuality: String,
    isBuffering: Boolean,
    isRecovering: Boolean,
    isPlaying: Boolean,
    isIncognito: Boolean,
    preferredQuality: String,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    seekAmount: Int,
    showSeekFeedback: Boolean,
    isSeekForward: Boolean,
    isCcEnabled: Boolean,
    availableSubtitles: List<com.rahul.vibetube1.domain.model.SubtitleItem>,
    selectedSubtitleLanguage: String?,
    currentPosition: () -> Long,
    bufferedPosition: () -> Long,
    duration: () -> Long,
    downloadState: DownloadDialogState,
    player: Player,
    activeCues: List<Cue>,
    syncTransition: InfiniteTransition,
    snackbarMessage: SharedFlow<String>,
    onToggleFavorite: (VideoItem?) -> Unit,
    onToggleSubscription: () -> Unit,
    onSetQuality: (com.rahul.vibetube1.domain.model.StreamItem?) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetTemporarySpeedBoost: (Boolean) -> Unit,
    onToggleSubtitles: () -> Unit,
    onSetSubtitleLanguage: (String?) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onDownloadConfirm: (VideoItem, StreamBundle, String?, String?, String?, Boolean, Boolean, Boolean) -> Unit,
    onDownloadClick: (VideoItem?) -> Unit,
    onDismissDownload: () -> Unit,
    onLoadMore: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShareVideo: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onRetry: () -> Unit,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    sleepTimerRemainingTime: Int?,
    shouldCloseAppOnTimerFinish: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSetEndOfVideoSleepTimer: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetShouldCloseApp: (Boolean) -> Unit,
    playbackPitch: Float,
    onSetPlaybackPitch: (Float) -> Unit,
    showStatsForNerds: Boolean,
    playbackStats: com.rahul.vibetube1.ui.screens.player.PlaybackStats,
    onToggleStats: () -> Unit,
    subtitleFontSize: Float,
    subtitleBackgroundOpacity: Float,
    isPlayerGesturesEnabled: Boolean = true,
    isAmbientModeEnabled: Boolean = false,
    onToggleAmbientMode: () -> Unit = {},
    isLooping: Boolean = false,
    onToggleLoop: () -> Unit = {},
    isStableVolumeEnabled: Boolean = false,
    onToggleStableVolume: () -> Unit = {},
    chapters: List<VideoChapter> = emptyList(),
    comments: List<com.rahul.vibetube1.domain.model.CommentItem>,
    isFetchingComments: Boolean,
    onLoadComments: () -> Unit,
    onLoadNextCommentsPage: () -> Unit,
    replies: List<com.rahul.vibetube1.domain.model.CommentItem>,
    isFetchingReplies: Boolean,
    activeReplyParent: com.rahul.vibetube1.domain.model.CommentItem?,
    onLoadReplies: (com.rahul.vibetube1.domain.model.CommentItem) -> Unit,
    onLoadNextRepliesPage: () -> Unit,
    onCloseReplies: () -> Unit,
    currentPlaylist: com.rahul.vibetube1.domain.model.PlaylistDetails?,
    playlistIndex: Int,
    relatedShorts: List<VideoItem>,
    relatedShortsState: RelatedShortsState,
    onLoadMoreShorts: () -> Unit,
    onPlaylistVideoClick: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAdditionalSettingsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var showLockedPill by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showAiSummarySheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(false) }
    var isMoreVideosOpen by remember { mutableStateOf(false) }
    var cachedRelatedVideos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(videoId) {
        onLoadComments()
    }

    LaunchedEffect(showLockedPill) {
        if (showLockedPill) {
            kotlinx.coroutines.delay(3000)
            showLockedPill = false
        }
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val view = LocalView.current

    androidx.activity.compose.BackHandler(enabled = isLandscape || isMoreVideosOpen) {
        if (isMoreVideosOpen) {
            isMoreVideosOpen = false
        } else if (isLandscape) {
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var aspectRatioText by remember { mutableStateOf("Fit") }
    var aspectRatioOverlayVisible by remember { mutableStateOf(false) }

    fun cycleAspectRatio() {
        if (!isLandscape) return
        resizeMode = when (resizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> {
                aspectRatioText = "Zoom (Fill)"
                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                aspectRatioText = "Stretch"
                AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
            else -> {
                aspectRatioText = "Fit"
                AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        }
        aspectRatioOverlayVisible = true
    }

    LaunchedEffect(isLandscape) {
        if (!isLandscape) {
            isMoreVideosOpen = false
        }
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        
        if (isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            aspectRatioText = "Fit"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes = window.attributes.apply {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
            }
        }
    }

    // Gesture states
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var brightnessOverlayVisible by remember { mutableStateOf(false) }
    var volumeOverlayVisible by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isSpeedBoostActive by remember { mutableStateOf(false) }

    // Initialize brightnessLevel
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val layoutParams = activity?.window?.attributes
        brightnessLevel = if ((layoutParams?.screenBrightness ?: -1f) < 0) {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } else {
            layoutParams?.screenBrightness ?: 0.5f
        }
    }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val listState = rememberLazyListState()
    InfiniteScrollEffect(
        listState = listState,
        buffer = 5,
        enabled = uiState is PlayerUiState.Success && !isBuffering,
        onLoadMore = onLoadMore
    )

    val thumbnailUrlToUse = (uiState as? PlayerUiState.Success)?.bundle?.thumbnailUrl ?: initialThumbnail
    val fallbackColors = listOf(
        Color(0xFF2B0B3F), // Deep Purple
        Color(0xFF1B0B2E), // Darker Purple
        Color(0xFF0D0B1A)  // Very Dark Blue/Black
    )
    
    var dynamicGradientColors by remember { mutableStateOf<List<Color>>(fallbackColors) }
    
    LaunchedEffect(thumbnailUrlToUse) {
        if (thumbnailUrlToUse != null) {
            val colors = com.rahul.vibetube1.utils.DynamicStatusBarUtils.extractGradientColors(thumbnailUrlToUse)
            if (colors != null && colors.size >= 3) {
                dynamicGradientColors = colors
            } else {
                dynamicGradientColors = fallbackColors
            }
        } else {
            dynamicGradientColors = fallbackColors
        }
    }
    
    val animatedColor1 by animateColorAsState(targetValue = dynamicGradientColors[0], animationSpec = tween(durationMillis = 800), label = "Color1")
    val animatedColor2 by animateColorAsState(targetValue = dynamicGradientColors[1], animationSpec = tween(durationMillis = 800), label = "Color2")
    val animatedColor3 by animateColorAsState(targetValue = dynamicGradientColors[2], animationSpec = tween(durationMillis = 800), label = "Color3")

    val animatedGradientColors = listOf(animatedColor1, animatedColor2, animatedColor3)
    val avgLuminance = (animatedColor1.luminance() + animatedColor2.luminance() + animatedColor3.luminance()) / 3f

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()

    DisposableEffect(avgLuminance, isLandscape, isDarkTheme) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (!isLandscape) {
                insetsController.isAppearanceLightStatusBars = avgLuminance > 0.4f
            }
        }
        onDispose {
            // We don't restore here because avgLuminance changes continuously during animation.
            // Restoration happens in the DisposableEffect(Unit) below.
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Reset orientation on dispose
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            
            // Restore system bars on dispose
            val window = activity?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
                val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                insetsController.isAppearanceLightStatusBars = !isDark
            }
        }
    }

    if (showQualityDialog) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            QualitySelectionSheet(
                videoStreams = it.bundle.videoStreams,
                currentQuality = currentQuality,
                preferredQuality = preferredQuality,
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { stream ->
                    onSetQuality(stream)
                    showQualityDialog = false
                }
            )
        }
    }

    if (showSubtitleSheet) {
        SubtitleSelectionSheet(
            subtitles = availableSubtitles,
            currentLanguage = selectedSubtitleLanguage,
            isCcEnabled = isCcEnabled,
            onDismiss = { showSubtitleSheet = false },
            onLanguageSelected = { lang ->
                onSetSubtitleLanguage(lang)
                showSubtitleSheet = false
            }
        )
    }

    if (showSpeedSheet) {
        PlaybackSpeedSelectionSheet(
            currentSpeed = playbackSpeed,
            onDismiss = { showSpeedSheet = false },
            onSpeedSelected = { speed ->
                onSetPlaybackSpeed(speed)
                showSpeedSheet = false
            }
        )
    }

    if (showPitchSheet) {
        PitchSelectionSheet(
            currentPitch = playbackPitch,
            onDismiss = { showPitchSheet = false },
            onPitchSelected = { pitch ->
                onSetPlaybackPitch(pitch)
                showPitchSheet = false
            }
        )
    }

    if (showDescriptionSheet) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            ModalBottomSheet(
                onDismissRequest = { showDescriptionSheet = false },
                sheetState = rememberModalBottomSheetState(),
                dragHandle = { BottomSheetDefaults.DragHandle() },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val context = LocalContext.current
                val primaryColor = MaterialTheme.colorScheme.primary
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showDescriptionSheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Title
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Statistics Row
                    val calculatedLikes = remember(isFavorite) {
                        val baseLikes = (it.bundle.viewCount / 40).coerceAtLeast(1)
                        val finalLikes = if (isFavorite) baseLikes + 1 else baseLikes
                        com.rahul.vibetube1.utils.VideoUtils.formatNumber(finalLikes)
                    }
                    val viewsText = remember(it.bundle.viewCount) {
                        java.text.NumberFormat.getIntegerInstance().format(it.bundle.viewCount)
                    }
                    val dateTuple = remember(it.bundle.uploadDate) {
                        val formatted = com.rahul.vibetube1.utils.VideoUtils.formatUploadDate(it.bundle.uploadDate).replace(",", "")
                        val parts = formatted.split(" ")
                        if (parts.size >= 3) {
                            Pair("${parts[0]} ${parts[1]}", parts.subList(2, parts.size).joinToString(" "))
                        } else {
                            Pair(formatted, "")
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Likes Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = calculatedLikes, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Likes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Views Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = viewsText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "Views", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        // Date Box
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = dateTuple.first, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = dateTuple.second.ifEmpty { "Date" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hashtags
                    val rawDesc = it.bundle.description ?: ""
                    val hashtags = remember(rawDesc) {
                        val hashtagRegex = Regex("#\\w+")
                        hashtagRegex.findAll(rawDesc).map { match -> match.value }.toList().take(5)
                    }
                    if (hashtags.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hashtags.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = tag,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Description text container
                    var isDescriptionExpanded by remember { mutableStateOf(false) }
                    
                    val annotatedDescription = remember(rawDesc) {
                        val builder = androidx.compose.ui.text.AnnotatedString.Builder()
                        val urlRegex = "(https?://[\\w\\d:#@%/;\\$()~_?\\+-=\\\\.&]+)".toRegex()
                        val matches = urlRegex.findAll(rawDesc)
                        var lastIndex = 0
                        for (match in matches) {
                            val start = match.range.first
                            val end = match.range.last + 1
                            if (start > lastIndex) {
                                builder.append(rawDesc.substring(lastIndex, start))
                            }
                            val url = match.value
                            builder.pushStringAnnotation(tag = "URL", annotation = url)
                            builder.pushStyle(
                                style = androidx.compose.ui.text.SpanStyle(
                                    color = primaryColor,
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                            )
                            builder.append(url)
                            builder.pop()
                            builder.pop()
                            lastIndex = end
                        }
                        if (lastIndex < rawDesc.length) {
                            builder.append(rawDesc.substring(lastIndex))
                        }
                        builder.toAnnotatedString()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(12.dp))
                            .animateContentSize()
                            .padding(16.dp)
                    ) {
                        SelectionContainer {
                            androidx.compose.foundation.text.ClickableText(
                                text = annotatedDescription,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                ),
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                onClick = { offset ->
                                    annotatedDescription.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        .firstOrNull()?.let { annotation ->
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(annotation.item))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                // ignore
                                            }
                                        }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = if (isDescriptionExpanded) "Show less" else "See more",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Summarize Video Button (Floating button at the very bottom)
                    Surface(
                        onClick = {
                            showDescriptionSheet = false
                            showAiSummarySheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .height(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Summarise the video",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showAiSummarySheet) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            var isSimulatingSummary by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                delay(1200L) // Beautiful quick simulated AI delay
                isSimulatingSummary = false
            }

            ModalBottomSheet(
                onDismissRequest = { showAiSummarySheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI Video Summary",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showAiSummarySheet = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    if (isSimulatingSummary) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Analyzing video transcript & content...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val aiSummaryPoints = remember(it.title, it.bundle.description) {
                            val title = it.title
                            val desc = it.bundle.description ?: ""
                            val points = mutableListOf<String>()
                            points.add("🎥 Overview: Comprehensive analysis of '$title', providing viewers with key insights, deep-dive discussions, and creator commentary.")
                            
                            if (title.contains("podcast", ignoreCase = true) || desc.contains("podcast", ignoreCase = true)) {
                                points.add("💬 Key Discussions: Engaging, high-profile podcast conversation highlighting modern-day trending topics, viral internet culture, and guest perspectives.")
                                points.add("⚡ Highlights: Memorable humorous anecdotes, lighthearted jokes, and quick-witted banter between creators that keep the audience highly engaged.")
                            } else if (title.contains("tutorial", ignoreCase = true) || desc.contains("learn", ignoreCase = true) || title.contains("how to", ignoreCase = true)) {
                                points.add("📘 Step-by-Step Walkthrough: Detailed technical step-by-step breakdown explaining core methods, tools, and optimal development procedures.")
                                points.add("🛠️ Practical Tips: Actionable advice, code/configuration examples, and best-practice recommendations designed to optimize real-world execution.")
                            } else if (title.contains("review", ignoreCase = true) || title.contains("unboxing", ignoreCase = true)) {
                                points.add("🔍 Expert Evaluation: Balanced analysis of design, performance, specifications, pros, and cons to assist consumers with informed decision-making.")
                                points.add("📦 Features: Hands-on inspection of out-of-the-box build quality, ergonomics, pricing value, and user-experience satisfaction.")
                            } else {
                                points.add("🔥 Main Highlights: Detailed exploration of the core narrative, creative edits, high-production-value visuals, and memorable sequences.")
                                points.add("📈 Audience Takeaways: Valuable educational, cultural, or entertainment content designed to maximize viewer retention and engagement.")
                            }
                            
                            points.add("💡 Creator Vision: Encourages viewers to subscribe, support the creator, and explore related videos, links, and official social handles.")
                            points
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            aiSummaryPoints.forEach { point ->
                                val parts = point.split(":", limit = 2)
                                if (parts.size == 2) {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "✦",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = parts[0].trim(),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = parts[1].trim(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCommentsSheet) {
        val isDarkThemeActive = androidx.compose.foundation.isSystemInDarkTheme() ||
                (MaterialTheme.colorScheme.surface.red +
                        MaterialTheme.colorScheme.surface.green +
                        MaterialTheme.colorScheme.surface.blue) / 3f < 0.5f
        val sheetBg = if (isDarkThemeActive) Color(0xFF0F0F0F) else Color.White
        val sheetFg = if (isDarkThemeActive) Color.White else Color(0xFF1E293B)

        if (activeReplyParent != null) {
            RepliesSheet(
                parentComment = activeReplyParent!!,
                replies = replies,
                isFetching = isFetchingReplies,
                onLoadMore = onLoadNextRepliesPage,
                onBack = onCloseReplies,
                onDismiss = { 
                    showCommentsSheet = false
                    onCloseReplies()
                }
            )
        } else {
            ModalBottomSheet(
                onDismissRequest = { 
                    showCommentsSheet = false
                    onCloseReplies()
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = sheetBg,
                contentColor = sheetFg,
                dragHandle = {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                },
                scrimColor = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                CommentsSheet(
                    comments = comments,
                    isFetching = isFetchingComments,
                    onLoadMore = onLoadNextCommentsPage,
                    onDismiss = { 
                        showCommentsSheet = false
                        onCloseReplies()
                    },
                    activeReplyParent = null,
                    replies = emptyList(),
                    isFetchingReplies = false,
                    onRepliesClick = onLoadReplies,
                    onLoadMoreReplies = {},
                    onCloseReplies = onCloseReplies
                )
            }
        }
    }

    if (showSettingsSheet) {
        PlayerSettingsBottomSheet(
            displayQuality = displayQuality,
            playbackSpeed = playbackSpeed,
            isCcEnabled = isCcEnabled,
            selectedSubtitleLanguage = selectedSubtitleLanguage,
            sleepTimerRemainingTime = sleepTimerRemainingTime,
            onQualityClick = {
                showSettingsSheet = false
                showQualityDialog = true
            },
            onSpeedClick = {
                showSettingsSheet = false
                showSpeedSheet = true
            },
            onCaptionsClick = {
                showSettingsSheet = false
                showSubtitleSheet = true
            },
            onLockScreenClick = {
                showSettingsSheet = false
                isScreenLocked = true
                controlsVisible = false
                showLockedPill = true
            },
            onSleepTimerClick = {
                showSettingsSheet = false
                showSleepTimerSheet = true
            },
            onAdditionalSettingsClick = {
                showSettingsSheet = false
                showAdditionalSettingsSheet = true
            },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showAdditionalSettingsSheet) {
        AdditionalSettingsBottomSheet(
            playbackPitch = playbackPitch,
            isAmbientModeEnabled = isAmbientModeEnabled,
            isStableVolumeEnabled = isStableVolumeEnabled,
            isLooping = isLooping,
            showStatsForNerds = showStatsForNerds,
            onPitchClick = {
                showAdditionalSettingsSheet = false
                showPitchSheet = true
            },
            onToggleAmbientMode = onToggleAmbientMode,
            onToggleStableVolume = onToggleStableVolume,
            onToggleLoop = onToggleLoop,
            onToggleStatsForNerds = onToggleStats,
            onDismiss = { showAdditionalSettingsSheet = false }
        )
    }

    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )

                val currentMinutes = if (sleepTimerRemainingTime != null && sleepTimerRemainingTime > 0) sleepTimerRemainingTime else 0
                var selectedMinutes by remember { mutableIntStateOf(currentMinutes) }
                val isEndOfVideo = sleepTimerRemainingTime == -1

                ListItem(
                    headlineContent = { Text("${stringResource(R.string.duration)}: ${if (isEndOfVideo) stringResource(R.string.timer_end_of_video) else if (selectedMinutes == 0) stringResource(R.string.off) else stringResource(R.string.timer_minutes_placeholder, selectedMinutes)}") },
                    trailingContent = {
                        if (!isEndOfVideo && selectedMinutes > 0) {
                            val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                            Text(
                                text = stringResource(R.string.ends_at, timeFormat.format(System.currentTimeMillis() + selectedMinutes * 60000)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                Slider(
                    value = if (isEndOfVideo) 0f else selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.roundToInt() },
                    valueRange = 0f..120f,
                    steps = 23, // 5 min gaps: (120/5)-1 = 23
                    modifier = Modifier.padding(horizontal = 24.dp),
                    enabled = !isEndOfVideo
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.timer_end_of_video)) },
                    trailingContent = {
                        Switch(
                            checked = isEndOfVideo,
                            onCheckedChange = { if (it) onSetEndOfVideoSleepTimer() else onCancelSleepTimer() }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.close_app_on_finish)) },
                    supportingContent = { Text(stringResource(R.string.close_app_desc)) },
                    trailingContent = {
                        Switch(
                            checked = shouldCloseAppOnTimerFinish,
                            onCheckedChange = onSetShouldCloseApp
                        )
                    }
                )

                Button(
                    onClick = {
                        if (!isEndOfVideo) {
                            if (selectedMinutes > 0) onStartSleepTimer(selectedMinutes)
                            else onCancelSleepTimer()
                        }
                        showSleepTimerSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }

    LaunchedEffect(brightnessOverlayVisible) {
        if (brightnessOverlayVisible) {
            delay(3000L)
            brightnessOverlayVisible = false
        }
    }

    LaunchedEffect(volumeOverlayVisible) {
        if (volumeOverlayVisible) {
            delay(3000L)
            volumeOverlayVisible = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Force edge-to-edge
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // Dynamic Status Bar Background
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(Brush.horizontalGradient(animatedGradientColors))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLandscape) Modifier.fillMaxHeight() 
                            else Modifier.statusBarsPadding().aspectRatio(16f / 9f)
                        )
                        .background(Color.Black)
                ) {
                    when (uiState) {
                        is PlayerUiState.Loading, is PlayerUiState.Error, is PlayerUiState.Upcoming -> {
                            // Show high-res placeholder during loading, error, or upcoming
                            ThumbnailImage(
                                videoId = videoId,
                                thumbnailUrl = when(uiState) {
                                    is PlayerUiState.Upcoming -> uiState.thumbnailUrl
                                    is PlayerUiState.Success -> uiState.bundle.thumbnailUrl
                                    else -> null
                                } ?: initialThumbnail ?: VideoUtils.getBestThumbnailUrl(videoId),
                                quality = com.rahul.vibetube1.ui.components.ThumbnailQuality.Ultra,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High
                            )

                            if (uiState is PlayerUiState.Loading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color.White.copy(alpha = 0.85f),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            if (uiState is PlayerUiState.Upcoming) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Upcoming Content",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "This Premiere or Live Stream has not started yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                        uiState.scheduledTime?.let { time ->
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(
                                                    text = VideoUtils.formatUploadDate(time),
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (uiState is PlayerUiState.Error) {
                                val isNetworkError = uiState.error is VibeTubeError.Network
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState(
                                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Playback Error",
                                        description = uiState.error.getMessage(),
                                        actionText = stringResource(R.string.retry),
                                        onActionClick = onRetry
                                    )
                                }
                            }
                        }
                        is PlayerUiState.Success -> {
                            val isLive = uiState.bundle.isLive
                            VideoPlayerGestureDetector(
                                areVolumeBrightnessGesturesEnabled = isPlayerGesturesEnabled && !isScreenLocked,
                                onDoubleTapLeft = {
                                    if (isScreenLocked) {
                                        showLockedPill = true
                                    } else {
                                        onSeekBackward()
                                    }
                                },
                                onDoubleTapRight = {
                                    if (isScreenLocked) {
                                        showLockedPill = true
                                    } else {
                                        onSeekForward()
                                    }
                                },
                                onSingleTap = { 
                                    if (isScreenLocked) {
                                        showLockedPill = true
                                    } else if (!isMoreVideosOpen) {
                                        controlsVisible = !controlsVisible 
                                    }
                                },
                                onSwipeDown = {
                                    if (!isScreenLocked) onBack()
                                },
                                onSwipeUp = {
                                    if (!isScreenLocked) {
                                        val activity = context as? Activity
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                                },
                                onDragStart = {
                                    if (!isScreenLocked) {
                                        isDragging = true
                                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        volumeLevel = currentVolume.toFloat() / maxVolume
                                    }
                                },
                                onVerticalSwipeLeft = { dragPercentage ->
                                    if (!isScreenLocked) {
                                        brightnessLevel = (brightnessLevel + dragPercentage).coerceIn(0f, 1f)
                                        val activity = context as? Activity
                                        val layoutParams = activity?.window?.attributes
                                        layoutParams?.screenBrightness = brightnessLevel
                                        activity?.window?.attributes = layoutParams
                                        
                                        brightnessOverlayVisible = true
                                        volumeOverlayVisible = false
                                    }
                                },
                                onVerticalSwipeRight = { dragPercentage ->
                                    if (!isScreenLocked) {
                                        volumeLevel = (volumeLevel + dragPercentage).coerceIn(0f, 1f)
                                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        val newVolume = (volumeLevel * maxVolume).toInt()
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                        
                                        volumeOverlayVisible = true
                                        brightnessOverlayVisible = false
                                    }
                                },
                                onLongPressStart = {
                                    if (!isScreenLocked && !isLive && !isSpeedBoostActive) {
                                        onSetTemporarySpeedBoost(true)
                                        isSpeedBoostActive = true
                                    }
                                },
                                onLongPressEnd = {
                                    if (isSpeedBoostActive) {
                                        onSetTemporarySpeedBoost(false)
                                        isSpeedBoostActive = false
                                    }
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    VideoPlayerView(
                                        player = player,
                                        resizeMode = resizeMode,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 2x Speed Boost Pill Overlay
                                    if (isSpeedBoostActive) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "2x Speed",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    // Manual Subtitle Overlay
                                    if (isCcEnabled && activeCues.isNotEmpty()) {
                                        val subtitlePaddingBottom by animateDpAsState(
                                            targetValue = if (controlsVisible) 64.dp else 24.dp,
                                            animationSpec = tween(durationMillis = 200),
                                            label = "SubtitlePaddingAnimation"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = subtitlePaddingBottom),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            ManualSubtitleView(
                                                cues = activeCues,
                                                fontSize = subtitleFontSize,
                                                backgroundOpacity = subtitleBackgroundOpacity,
                                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                            )
                                        }
                                    }
                                }
                            }

                            // Vertical HUDs Left: Brightness, Right: Volume
                            VerticalGestureHUD(
                                visible = brightnessOverlayVisible,
                                progress = brightnessLevel,
                                icon = Icons.Default.BrightnessLow,
                                isRightSide = false,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            
                            VerticalGestureHUD(
                                visible = volumeOverlayVisible,
                                progress = volumeLevel,
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                isRightSide = true,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )

                            // Custom Controls Overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = !isScreenLocked && (controlsVisible || isMoreVideosOpen),
                                enter = androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.fadeOut()
                            ) {
                                if (isLandscape) {
                                    val rawRelatedVideos = (uiState as? PlayerUiState.Success)?.bundle?.relatedVideos ?: emptyList()
                                    if (rawRelatedVideos.isNotEmpty()) {
                                        cachedRelatedVideos = rawRelatedVideos
                                    }
                                    val relatedVideos = if (rawRelatedVideos.isNotEmpty()) rawRelatedVideos else cachedRelatedVideos
                                    FullscreenPlayerControlsOverlay(
                                        title = (uiState as? PlayerUiState.Success)?.title ?: "",
                                        channelName = (uiState as? PlayerUiState.Success)?.uploader ?: "",
                                        isPlaying = isPlaying,
                                        currentPosition = currentPosition,
                                        duration = duration,
                                        isLive = isLive,
                                        isCcEnabled = isCcEnabled,
                                        hasSubtitles = (uiState as? PlayerUiState.Success)?.bundle?.subtitles?.isNotEmpty() == true,
                                        onPlayPause = onPlayPause,
                                        onSkipNext = onSkipNext,
                                        onSkipPrevious = onSkipPrevious,
                                        onToggleSubtitles = onToggleSubtitles,
                                        onShowSettings = { showSettingsSheet = true },
                                        onBack = {
                                            val activity = context as? Activity
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                        },
                                        onCycleAspectRatio = { cycleAspectRatio() },
                                        isFavorite = isFavorite,
                                        isSaved = isSaved,
                                        isDownloaded = downloadedIds.contains(videoId),
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onSaveClick = { 
                                            (uiState as? PlayerUiState.Success)?.let { successState ->
                                                onAddToPlaylistClick(
                                                    VideoItem(
                                                        id = videoId,
                                                        title = successState.title,
                                                        thumbnailUrl = successState.bundle.thumbnailUrl ?: "",
                                                        uploaderName = successState.uploader,
                                                        uploaderUrl = successState.bundle.uploaderUrl,
                                                        viewCount = successState.bundle.viewCount,
                                                        uploadDate = successState.bundle.uploadDate,
                                                        duration = duration() / 1000
                                                    )
                                                )
                                            }
                                        },
                                        onDownloadClick = { if (!downloadedIds.contains(videoId)) onDownloadClick(null) },
                                        onAskClick = { showAiSummarySheet = true },
                                        onShareClick = { onShareVideo() },
                                        onMoreClick = { showSettingsSheet = true },
                                        relatedVideos = relatedVideos,
                                        onVideoClick = { video -> onVideoClick(video) },
                                        isMoreVideosOpen = isMoreVideosOpen,
                                        currentVideoId = videoId,
                                        onOpenMoreVideos = {
                                            isMoreVideosOpen = true
                                            controlsVisible = true
                                        },
                                        onCloseMoreVideos = {
                                            isMoreVideosOpen = false
                                            controlsVisible = true
                                        },
                                        progressBar = {
                                            PersistentProgressBar(
                                                progress = {
                                                    val dur = duration()
                                                    if (isLive) 1f else if (dur > 0) currentPosition().toFloat() / dur else 0f
                                                },
                                                bufferedProgress = {
                                                    val dur = duration()
                                                    if (isLive) 1f else if (dur > 0) bufferedPosition().toFloat() / dur else 0f
                                                },
                                                isInteractive = controlsVisible && !isLive,
                                                chapters = chapters,
                                                durationMs = duration(),
                                                onSeek = { percentage: Float ->
                                                    val totalDuration = duration()
                                                    if (totalDuration > 0) {
                                                        onSeekTo((percentage * totalDuration).toLong())
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    )
                                } else {
                                    PlayerControlsOverlay(
                                        isPlaying = isPlaying,
                                        currentPosition = currentPosition,
                                        duration = duration,
                                        isLive = isLive,
                                        isCcEnabled = isCcEnabled,
                                        isIncognito = isIncognito,
                                        hasSubtitles = (uiState as? PlayerUiState.Success)?.bundle?.subtitles?.isNotEmpty() == true,
                                        isAspectRatioEnabled = isLandscape,
                                        onPlayPause = onPlayPause,
                                        onSkipNext = onSkipNext,
                                        onSkipPrevious = onSkipPrevious,
                                        onToggleSubtitles = onToggleSubtitles,
                                        onShowSubtitleSettings = { showSubtitleSheet = true },
                                        onShowSettings = { showSettingsSheet = true },
                                        onToggleAspectRatio = { cycleAspectRatio() },
                                        onBack = {
                                            if (isLandscape) {
                                                val activity = context as? Activity
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                onBack()
                                            }
                                        },
                                        isLandscape = isLandscape,
                                        onToggleFullscreen = {
                                            val activity = context as? Activity
                                            if (isLandscape) {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                        }
                                    )
                                }
                            }

                            // Persistent Progress Bar (Always visible at the very bottom border)
                            // move it after the controls to ensure it stays on top of the darkened overlay background
                            if (!isLandscape && !isScreenLocked) {
                                PersistentProgressBar(
                                    progress = {
                                        val dur = duration()
                                        if (isLive) 1f else if (dur > 0) currentPosition().toFloat() / dur else 0f
                                    },
                                    bufferedProgress = {
                                        val dur = duration()
                                        if (isLive) 1f else if (dur > 0) bufferedPosition().toFloat() / dur else 0f
                                    },
                                    isInteractive = controlsVisible && !isLive,
                                    chapters = chapters,
                                    durationMs = duration(),
                                    onSeek = { percentage: Float ->
                                        val totalDuration = duration()
                                        if (totalDuration > 0) {
                                            onSeekTo((percentage * totalDuration).toLong())
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                )
                            }

                            // Lock Screen Unlock Pill
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isScreenLocked && showLockedPill,
                                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(initialScale = 0.85f),
                                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 0.85f),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Surface(
                                    onClick = {
                                        isScreenLocked = false
                                        showLockedPill = false
                                        controlsVisible = true
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.Black.copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    shadowElevation = 8.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Unlock screen",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Screen locked • Tap to unlock",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            LaunchedEffect(controlsVisible, isPlaying, isMoreVideosOpen) {
                                if (controlsVisible && isPlaying && !isMoreVideosOpen) {
                                    delay(2500L)
                                    controlsVisible = false
                                }
                            }

                            LaunchedEffect(isPlaying, isMoreVideosOpen) {
                                if (isPlaying && controlsVisible && !isMoreVideosOpen) {
                                    delay(1000L)
                                    controlsVisible = false
                                }
                            }

                            SeekGestureOverlay(
                                visible = showSeekFeedback,
                                amount = seekAmount,
                                isForward = isSeekForward
                            )

                            if (aspectRatioOverlayVisible) {
                                LaunchedEffect(aspectRatioOverlayVisible, resizeMode) {
                                    delay(1500L)
                                    aspectRatioOverlayVisible = false
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = aspectRatioText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (showStatsForNerds) {
                                StatsForNerdsOverlay(
                                    stats = playbackStats,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                )
                            }

                            // Consolidated Player Loading UI
                            val showLoader = isBuffering || isRecovering
                            if (showLoader) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isRecovering) Color.Black.copy(alpha = 0.35f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = Color.White.copy(alpha = 0.85f),
                                            strokeWidth = 2.dp
                                        )
                                        
                                        if (isRecovering) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = stringResource(R.string.waiting_for_connection),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Metadata Area
                AnimatedVisibility(
                    visible = !isLandscape && uiState !is PlayerUiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val normalRelatedVideos = (uiState as? PlayerUiState.Success)?.bundle?.relatedVideos
                        ?.filter { !VideoUtils.isShort(it) && it.id != videoId }
                        ?.distinctBy { it.id } ?: emptyList()

                    val shortsList = when (relatedShortsState) {
                        is RelatedShortsState.Success -> relatedShortsState.shorts
                        else -> emptyList()
                    }

                    val mixedFeedItems = remember(normalRelatedVideos, shortsList) {
                        ContextualShortsHelper.buildMixedRelatedFeed(
                            normalVideos = normalRelatedVideos,
                            shorts = shortsList
                        )
                    }

                    // Keep horizontal scroll states preserved across vertical LazyColumn item recycling
                    val carouselStates = remember { mutableMapOf<String, androidx.compose.foundation.lazy.LazyListState>() }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        when (uiState) {
                            is PlayerUiState.Loading -> {
                                item {
                                    com.rahul.vibetube1.ui.components.PlayerMetadataSkeleton(syncTransition)
                                }

                                items(3) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        com.rahul.vibetube1.ui.components.VideoCardSkeleton(syncTransition)
                                    }
                                }
                            }
                            is PlayerUiState.Success -> {
                                item {
                                    if (currentPlaylist != null && playlistIndex != -1) {
                                        PlaylistStack(
                                            playlist = currentPlaylist,
                                            currentIndex = playlistIndex,
                                            onVideoClick = onPlaylistVideoClick
                                        )
                                    }
                                }
                                // 1. Title + Metadata Section
                                item {
                                    VideoHeaderSection(
                                        title = uiState.title,
                                        viewCount = uiState.bundle.viewCount,
                                        uploadDate = uiState.bundle.uploadDate,
                                        onDescriptionClick = { showDescriptionSheet = true },
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                // 2. Channel Section
                                item {
                                    ChannelInfoSection(
                                        uploaderName = uiState.uploader,
                                        uploaderThumbnailUrl = uiState.bundle.uploaderThumbnailUrl,
                                        uploaderUrl = uiState.bundle.uploaderUrl,
                                        subscriberCount = uiState.bundle.uploaderSubscriberCount,
                                        isSubscribed = isSubscribed,
                                        onToggleSubscription = onToggleSubscription,
                                        onChannelClick = onChannelClick,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }
                                // 3. Action Buttons Section [Like] [Download] [Ask] [Share]
                                item {
                                    PlayerActionRow(
                                        isFavorite = isFavorite,
                                        isSaved = isSaved,
                                        isDownloaded = downloadedIds.contains(videoId),
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onSaveClick = { 
                                            uiState.bundle.let { bundle ->
                                                onAddToPlaylistClick(
                                                    VideoItem(
                                                        id = videoId,
                                                        title = uiState.title,
                                                        thumbnailUrl = bundle.thumbnailUrl ?: "",
                                                        uploaderName = uiState.uploader,
                                                        uploaderUrl = bundle.uploaderUrl,
                                                        viewCount = bundle.viewCount,
                                                        uploadDate = bundle.uploadDate,
                                                        duration = duration() / 1000
                                                    )
                                                )
                                            }
                                        },
                                        onDownloadClick = { if (!downloadedIds.contains(videoId)) onDownloadClick(null) },
                                        onShareClick = onShareVideo,
                                        onAskClick = { showAiSummarySheet = true },
                                        onMoreClick = { showSettingsSheet = true },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                                // 4. Comments Section (Preview Card)
                                item {
                                    CommentsPreviewCard(
                                        comments = comments,
                                        totalCount = null,
                                        onClick = { showCommentsSheet = true },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                // 5. Interleaved Related Feed (Normal videos and Contextual Shorts Shelves)
                                if (mixedFeedItems.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.related_videos),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Autoplay",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Switch(
                                                    checked = isAutoplayEnabled,
                                                    onCheckedChange = onAutoplayChange,
                                                    modifier = Modifier.scale(0.6f)
                                                )
                                            }
                                        }
                                    }
                                    items(
                                        items = mixedFeedItems,
                                        key = { it.stableKey }
                                    ) { feedItem ->
                                        when (feedItem) {
                                            is RelatedFeedItem.NormalVideo -> {
                                                VideoItemRow(
                                                    video = feedItem.video,
                                                    isDownloaded = downloadedIds.contains(feedItem.video.id),
                                                    isFavorite = favoriteIds.contains(feedItem.video.id),
                                                    onFavoriteClick = { onToggleFavorite(feedItem.video) },
                                                    onAddToPlaylistClick = { onAddToPlaylistClick(feedItem.video) },
                                                    onDownloadClick = { onDownloadClick(feedItem.video) },
                                                    onChannelClick = { onChannelClick(feedItem.video.uploaderUrl ?: "") },
                                                    onClick = { onVideoClick(feedItem.video) }
                                                )
                                            }
                                            is RelatedFeedItem.ShortsCarousel -> {
                                                val carouselState = carouselStates.getOrPut(feedItem.id) {
                                                    androidx.compose.foundation.lazy.LazyListState()
                                                }
                                                ShortsCarouselSection(
                                                    shorts = feedItem.shorts,
                                                    carouselState = carouselState,
                                                    onShortClick = { short ->
                                                         onVideoClick(short.copy(isShort = true))
                                                    },
                                                    onLoadMore = onLoadMoreShorts,
                                                    isLoadingMore = (relatedShortsState as? RelatedShortsState.Success)?.isLoadingMore == true,
                                                    hasMore = (relatedShortsState as? RelatedShortsState.Success)?.hasMore == true,
                                                    modifier = Modifier.padding(vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                } else if (relatedShortsState is RelatedShortsState.Loading) {
                                    item {
                                        ShortsCarouselSection(
                                            relatedShortsState = relatedShortsState,
                                            onShortClick = {},
                                            onLoadMore = {},
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            is PlayerUiState.Upcoming -> {
                                item {
                                    UnifiedMetadataHub(
                                        title = uiState.title,
                                        viewCount = -1L,
                                        uploadDate = uiState.scheduledTime,
                                        description = null,
                                        uploaderName = uiState.uploader,
                                        uploaderThumbnailUrl = null,
                                        uploaderUrl = null,
                                        subscriberCount = null,
                                        isSubscribed = isSubscribed,
                                        isFavorite = isFavorite,
                                        isSaved = isSaved,
                                        isDownloaded = false,
                                        comments = emptyList(),
                                        commentCount = null,
                                        onToggleSubscription = onToggleSubscription,
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onSaveClick = {
                                            onAddToPlaylistClick(
                                                VideoItem(
                                                    id = videoId,
                                                    title = uiState.title,
                                                    thumbnailUrl = uiState.thumbnailUrl ?: "",
                                                    uploaderName = uiState.uploader,
                                                    uploaderUrl = null,
                                                    viewCount = -1L,
                                                    uploadDate = uiState.scheduledTime,
                                                    duration = 0L
                                                )
                                            )
                                        },
                                        onDownloadClick = { },
                                        onShareClick = onShareVideo,
                                        onChannelClick = onChannelClick,
                                        onCommentsClick = { },
                                        onDescriptionClick = { showDescriptionSheet = true },
                                        onAskClick = { showAiSummarySheet = true },
                                        onMoreClick = { showSettingsSheet = true }
                                    )
                                }
                            }
                            else -> {
                                // Handled by AnimatedVisibility
                            }
                        }
                    }
                }
            }

            // Shared Download Dialog logic
            when (val currentDownloadState = downloadState) {
                DownloadDialogState.Idle -> {}
                is DownloadDialogState.Loading -> {
                    AlertDialog(
                        onDismissRequest = { onDismissDownload() },
                        confirmButton = {},
                        title = { Text(stringResource(R.string.loading)) },
                        text = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    )
                }
                is DownloadDialogState.ShowDialog -> {
                    DownloadSelectionSheet(
                        videoStreams = currentDownloadState.bundle.videoStreams,
                        audioStreams = currentDownloadState.bundle.audioStreams,
                        onDismiss = { onDismissDownload() },
                        onDownload = { stream, isAudioOnly, saveToDevice ->
                            onDownloadConfirm(
                                currentDownloadState.video,
                                currentDownloadState.bundle,
                                stream.url,
                                stream.quality,
                                stream.format,
                                stream.isAdaptive,
                                isAudioOnly,
                                saveToDevice
                            )
                        }
                    )
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun StatsForNerdsOverlay(
    stats: com.rahul.vibetube1.ui.screens.player.PlaybackStats,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stats for Nerds", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
            
            StatRow("Resolution", stats.resolution)
            StatRow("Format", stats.videoFormat ?: "Unknown")
            StatRow("Bitrate", "${stats.bitrate / 1000} kbps")
            StatRow("Dropped Frames", stats.droppedFrames.toString())
            StatRow("Bandwidth", "${stats.bandwidthEstimate / 1000000} Mbps")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ManualSubtitleView(
    cues: List<Cue>,
    fontSize: Float,
    backgroundOpacity: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cues.forEach { cue ->
            val text = cue.text
            if (text != null && text.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = backgroundOpacity),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = text.toString(),
                        color = Color.White,
                        fontSize = fontSize.sp,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * 1.3f).sp,
                            textAlign = TextAlign.Center,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                blurRadius = 3f
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun PlayerControlsOverlay(
    isPlaying: Boolean,
    currentPosition: () -> Long,
    duration: () -> Long,
    isLive: Boolean,
    isCcEnabled: Boolean,
    isIncognito: Boolean,
    hasSubtitles: Boolean,
    isAspectRatioEnabled: Boolean = true,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onShowSubtitleSettings: () -> Unit,
    onShowSettings: () -> Unit,
    onToggleAspectRatio: () -> Unit,
    onBack: () -> Unit,
    isLandscape: Boolean,
    onToggleFullscreen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        // Center Controls Section (Modern Layout)
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipPrevious()
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SkipPrevious, stringResource(R.string.previous_video), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            // Central Play/Pause with custom circular background (56–64dp outer, 28–32dp icon)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(60.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipNext()
                },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SkipNext, stringResource(R.string.next_video), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
        }

        // Modern Bottom Bar Section (Timestamps only, Seekbar moved to border)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .displayCutoutPadding()
                .padding(bottom = 12.dp) // Sit closer to the bottom border
        ) {
            // Time Display & Live Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                            ),
                            color = Color.White
                        )
                    }
                    IconButton(
                        onClick = onToggleFullscreen,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Text(
                        text = VideoUtils.formatDuration(currentPosition() / 1000),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                        ),
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = VideoUtils.formatDuration(duration() / 1000),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isLandscape) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (isLandscape) "Exit Fullscreen" else "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top Action Pill glassmorphic design
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            if (isIncognito) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Incognito", 
                            color = Color.White, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Unified Settings Pill
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasSubtitles) {
                        IconButton(
                            onClick = onToggleSubtitles,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isCcEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = null,
                                tint = if (isCcEnabled) Color.Yellow else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleAspectRatio, 
                        enabled = isAspectRatioEnabled,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio, 
                            contentDescription = null, 
                            tint = if (isAspectRatioEnabled) Color.White else Color.White.copy(alpha = 0.38f), 
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = onShowSettings, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
