@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.media3.common.util.UnstableApi::class)

package com.rahul.vibetube1.ui.screens.shorts

import android.content.Intent
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.CommentItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.ui.theme.VibeTubeRed
import com.rahul.vibetube1.ui.screens.player.CommentsSheet
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import kotlinx.coroutines.coroutineScope

@Composable
fun ShortsScreen(
    viewModel: ShortsViewModel,
    initialVideoId: String? = null,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Hide standard app bars in Shorts; restore cleanly on exit
    DisposableEffect(Unit) {
        onBarsVisibilityChange(false)
        onDispose {
            onBarsVisibilityChange(true)
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val uiState = state) {
            is ShortsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VibeTubeRed)
                }
            }
            is ShortsUiState.Refreshing,
            is ShortsUiState.Loaded,
            is ShortsUiState.LoadingNextPage,
            is ShortsUiState.EndOfFeed -> {
                val currentShorts = uiState.shorts
                if (currentShorts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VideoLibrary, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Shorts found", color = Color.White)
                        }
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ShortsPager(
                            shorts = currentShorts,
                            initialVideoId = initialVideoId,
                            viewModel = viewModel,
                            onChannelClick = onChannelClick,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
            is ShortsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.message, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadShorts() },
                            colors = ButtonDefaults.buttonColors(containerColor = VibeTubeRed)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ShortsPager(
    shorts: List<VideoItem>,
    initialVideoId: String?,
    viewModel: ShortsViewModel,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    val initialPageIndex = remember(shorts, initialVideoId) {
        val index = shorts.indexOfFirst { it.id == initialVideoId }
        if (index != -1) index else 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { shorts.size }
    )

    val coroutineScope = rememberCoroutineScope()
    var waitingForNextPage by remember { mutableStateOf(false) }
    var autoNextHandledPage by remember { mutableIntStateOf(-1) }
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()

    var initialScrollDone by remember { mutableStateOf(false) }
    LaunchedEffect(shorts, initialVideoId) {
        if (!initialScrollDone && initialVideoId != null) {
            val targetIdx = shorts.indexOfFirst { it.id == initialVideoId }
            if (targetIdx > 0 && targetIdx < shorts.size) {
                pagerState.scrollToPage(targetIdx)
                initialScrollDone = true
            }
        }
    }

    LaunchedEffect(shorts.size) {
        if (waitingForNextPage && pagerState.currentPage < shorts.size - 1) {
            waitingForNextPage = false
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    // Reset auto-next guard and notify ViewModel of settled page
    LaunchedEffect(pagerState.settledPage, shorts.size) {
        autoNextHandledPage = -1
        viewModel.onPageSettled(pagerState.settledPage)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = pagerState,
            key = { index -> shorts[index].id },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val video = shorts[page]
            val isPageActive = pagerState.currentPage == page
            val isPreloading = (pagerState.currentPage + 1 == page)

            ShortPlayPage(
                video = video,
                viewModel = viewModel,
                isActive = isPageActive,
                isPreloading = isPreloading,
                onChannelClick = onChannelClick,
                onVideoClick = onVideoClick,
                onVideoEnded = {
                    if (page == pagerState.currentPage && !pagerState.isScrollInProgress && autoNextHandledPage != page) {
                        autoNextHandledPage = page
                        if (page < shorts.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        } else {
                            waitingForNextPage = true
                            viewModel.loadMoreShorts()
                        }
                    }
                }
            )
        }

        if (isLoadingMore && pagerState.currentPage >= shorts.size - 2) {
            LinearProgressIndicator(
                color = VibeTubeRed,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(3.dp)
            )
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShortPlayPage(
    video: VideoItem,
    viewModel: ShortsViewModel,
    isActive: Boolean,
    isPreloading: Boolean,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onVideoEnded: () -> Unit = {},
    onNearEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var streamUrl by remember { mutableStateOf<String?>(null) }
    var audioUrl by remember { mutableStateOf<String?>(null) }
    var isVideoLoading by remember { mutableStateOf(true) }
    var extractionError by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableStateOf(0) }

    var isPlaying by remember { mutableStateOf(true) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var indicatorIsPlay by remember { mutableStateOf(true) }

    // Comments Sheet state
    var showCommentsSheet by remember { mutableStateOf(false) }
    var commentsList by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var isFetchingComments by remember { mutableStateOf(false) }
    var nextCommentsPage by remember { mutableStateOf<Page?>(null) }

    // Metadata 3-dot Menu state
    var showMenuSheet by remember { mutableStateOf(false) }
    var showDownloadSheet by remember { mutableStateOf(false) }
    var streamBundle by remember { mutableStateOf<StreamBundle?>(null) }

    // Bind real states from Database
    val isLiked by viewModel.isFavorite(video.id).collectAsStateWithLifecycle(initialValue = false)
    val isSaved by viewModel.isSaved(video.id).collectAsStateWithLifecycle(initialValue = false)
    val isSubscribed by viewModel.isSubscribed(video.uploaderUrl ?: "").collectAsStateWithLifecycle(initialValue = false)
    val downloadedIds by viewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val isDownloaded = downloadedIds.contains(video.id)

    // Failed streams URLs list for smart failover retry
    var failedUrls by remember { mutableStateOf(setOf<String>()) }
    var areControlsVisible by remember { mutableStateOf(true) }
    
    var isTemporarySpeedBoostActive by remember { mutableStateOf(false) }

    // Track seen Shorts after 3 seconds of active play
    LaunchedEffect(isActive) {
        if (isActive) {
            Log.d("ShortsScreen", "[Shorts] Active page started for videoId=${video.id}")
            com.rahul.vibetube1.utils.FirebaseTelemetry.logShortView(
                shortId = video.id,
                title = video.title,
                channelName = video.uploaderName
            )
            kotlinx.coroutines.delay(3000L)
            viewModel.markShortAsSeen(video.id)
        } else {
            isTemporarySpeedBoostActive = false
        }
    }

    // Reset playing state and controls when active changes
    LaunchedEffect(isActive) {
        if (isActive) {
            isPlaying = true
            areControlsVisible = true
            failedUrls = emptySet()
        }
    }

    // Auto-hide controls after a short inactivity period
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            kotlinx.coroutines.delay(3500L)
            areControlsVisible = false
        }
    }

    // Safe device-compatible stream selector helper for Shorts
    fun selectShortsStreamFiltered(
        bundle: StreamBundle,
        streams: List<com.rahul.vibetube1.domain.model.StreamItem>
    ): Pair<com.rahul.vibetube1.domain.model.StreamItem, String?>? {
        if (streams.isEmpty()) return null

        fun getResVal(quality: String): Int {
            return quality.filter { it.isDigit() }.toIntOrNull() ?: 0
        }

        // Exclude ultra-high resolutions above 1080p
        val safeStreams = streams.filter { getResVal(it.quality) <= 1080 }

        // Progressive MP4 streams (<= 720p) - High priority
        val progressiveMp4 = safeStreams.filter { !it.isAdaptive && it.format.lowercase() == "mp4" && getResVal(it.quality) <= 720 }
        // Adaptive MP4 streams (<= 720p) - Second choice
        val adaptiveMp4 = safeStreams.filter { it.isAdaptive && it.format.lowercase() == "mp4" && getResVal(it.quality) <= 720 }
        // Any progressive streams (<= 720p) - Third choice
        val progressiveAny = safeStreams.filter { !it.isAdaptive && getResVal(it.quality) <= 720 }
        // Adaptive MP4 streams (<= 1080p)
        val adaptiveMp4High = safeStreams.filter { it.isAdaptive && it.format.lowercase() == "mp4" && getResVal(it.quality) <= 1080 }
        // Fallback to any safe stream <= 720p
        val safeAny720 = safeStreams.filter { getResVal(it.quality) <= 720 }
        // Fallback to any safe stream <= 1080p
        val safeAny1080 = safeStreams

        val selectedVideo = progressiveMp4.maxByOrNull { getResVal(it.quality) }
            ?: adaptiveMp4.maxByOrNull { getResVal(it.quality) }
            ?: progressiveAny.maxByOrNull { getResVal(it.quality) }
            ?: adaptiveMp4High.maxByOrNull { getResVal(it.quality) }
            ?: safeAny720.maxByOrNull { getResVal(it.quality) }
            ?: safeAny1080.maxByOrNull { getResVal(it.quality) }
            ?: streams.firstOrNull()

        if (selectedVideo == null) return null
        val selectedAudioUrl = if (selectedVideo.isAdaptive) bundle.bestAudioStreamUrl else null
        return Pair(selectedVideo, selectedAudioUrl)
    }

    // Load stream urls
    LaunchedEffect(isActive, isPreloading, retryTrigger, failedUrls) {
        if (isActive || isPreloading) {
            if (isActive) {
                isVideoLoading = true
            }
            extractionError = null
            Log.d("ShortsScreen", "[Shorts] Extracting streams for videoId=${video.id}")
            try {
                val bundle = viewModel.videoRepository.getStreamBundle(video.id, forceRefresh = false)
                streamBundle = bundle
                val availableStreams = bundle.videoStreams.filter { it.url !in failedUrls }
                
                val selection = selectShortsStreamFiltered(bundle, availableStreams)
                if (selection != null) {
                    val (videoStream, audioUrlResult) = selection
                    streamUrl = videoStream.url
                    audioUrl = audioUrlResult
                    
                    val isWebMVP9 = videoStream.format.lowercase().contains("webm") || videoStream.format.lowercase().contains("vp9")
                    val resVal = videoStream.quality.filter { it.isDigit() }.toIntOrNull() ?: 0
                    
                    Log.d("ShortsScreen", "[Shorts] Stream Selected: id=${video.id}, format=${videoStream.format}, resolution=${videoStream.quality}, isAdaptive=${videoStream.isAdaptive}")
                    
                    if (video.duration > 60) {
                        Log.w("ShortsScreen", "[Shorts] WARNING: Video duration is ${video.duration}s")
                    }
                    if (isWebMVP9) {
                        Log.w("ShortsScreen", "[Shorts] WARNING: Video codec format is ${videoStream.format}")
                    }
                    if (resVal > 1080) {
                        Log.w("ShortsScreen", "[Shorts] WARNING: Video resolution is above 1080p: ${videoStream.quality}")
                    }
                } else {
                    extractionError = "No compatible stream formats available."
                    Log.e("ShortsScreen", "[Shorts] Extraction Error: No compatible streams left for videoId=${video.id}")
                }
            } catch (e: Exception) {
                Log.e("ShortsScreen", "[Shorts] Extraction failed for videoId=${video.id}", e)
                viewModel.preloadManager.markShortAsFailed(video.id)
                val mappedError = com.rahul.vibetube1.utils.VibeTubeError.fromThrowable(e)
                extractionError = mappedError.getMessage()
            } finally {
                isVideoLoading = false
            }
        } else {
            streamUrl = null
            audioUrl = null
            extractionError = null
            streamBundle = null
        }
    }

    // Load comments on-demand
    LaunchedEffect(showCommentsSheet) {
        if (showCommentsSheet && commentsList.isEmpty()) {
            isFetchingComments = true
            try {
                Log.d("ShortsScreen", "[Shorts] Fetching comments for videoId=${video.id}")
                val result = viewModel.videoRepository.getComments(video.id)
                commentsList = result.items
                nextCommentsPage = result.nextPage
                Log.d("ShortsScreen", "[Shorts] Comments fetched: ${commentsList.size} comments found.")
            } catch (e: Exception) {
                Log.e("ShortsScreen", "[Shorts] Comments fetch failed", e)
            } finally {
                isFetchingComments = false
            }
        }
    }

    var playerPlaybackError by remember { mutableStateOf<String?>(null) }
    
    // Auto-skip on failure
    LaunchedEffect(extractionError, playerPlaybackError, isActive) {
        if (isActive && (extractionError != null || playerPlaybackError != null)) {
            Log.w("ShortsScreen", "[Shorts] Unrecoverable error on active video, auto-skipping to next in 400ms")
            kotlinx.coroutines.delay(400L) // Fast auto-skip on failure
            onVideoEnded() // Trigger auto-next
        }
    }

    fun loadMoreComments() {
        val page = nextCommentsPage ?: return
        if (isFetchingComments) return
        scope.launch {
            isFetchingComments = true
            try {
                Log.d("ShortsScreen", "[Shorts] Fetching more comments for videoId=${video.id}")
                val result = viewModel.videoRepository.fetchNextCommentsPage(video.id, page)
                commentsList = commentsList + result.items
                nextCommentsPage = result.nextPage
            } catch (e: Exception) {
                Log.e("ShortsScreen", "[Shorts] Comments pagination failed", e)
            } finally {
                isFetchingComments = false
            }
        }
    }

    val viewConfiguration = androidx.compose.ui.platform.LocalViewConfiguration.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    var longPressJob: kotlinx.coroutines.Job? = null
                    var isLongPressActive = false
                    var startPosition = androidx.compose.ui.geometry.Offset.Zero
                    var tapTime = 0L

                    awaitPointerEventScope {
                        val touchSlop = viewConfiguration.touchSlop
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                            val down = event.changes.find { it.changedToDownIgnoreConsumed() }
                            
                            if (down != null && !down.isConsumed) {
                                startPosition = down.position
                                tapTime = System.currentTimeMillis()
                                longPressJob?.cancel()
                                isLongPressActive = false
                                
                                longPressJob = launch {
                                    kotlinx.coroutines.delay(450L)
                                    isLongPressActive = true
                                    isTemporarySpeedBoostActive = true
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                            }
                            
                            if (event.changes.any { it.isConsumed }) {
                                longPressJob?.cancel()
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    isTemporarySpeedBoostActive = false
                                }
                            }
                            
                            val firstChanged = event.changes.firstOrNull()
                            if (firstChanged != null && firstChanged.pressed) {
                                val distance = (firstChanged.position - startPosition).getDistance()
                                if (distance > touchSlop) {
                                    longPressJob?.cancel()
                                    if (isLongPressActive) {
                                        isLongPressActive = false
                                        isTemporarySpeedBoostActive = false
                                    }
                                }
                            }
                            
                            val up = event.changes.find { it.changedToUpIgnoreConsumed() }
                            if (up != null) {
                                longPressJob?.cancel()
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    isTemporarySpeedBoostActive = false
                                } else {
                                    val distance = (up.position - startPosition).getDistance()
                                    if (distance <= touchSlop && !up.isConsumed && (System.currentTimeMillis() - tapTime < 450L)) {
                                        up.consume()
                                        isPlaying = !isPlaying
                                        indicatorIsPlay = isPlaying
                                        showPlayPauseIndicator = true
                                        areControlsVisible = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        // Thumbnail background while loading/error
        com.rahul.vibetube1.ui.components.ThumbnailImage(
            videoId = video.id,
            thumbnailUrl = video.thumbnailUrl,
            modifier = Modifier.fillMaxSize(),
            quality = com.rahul.vibetube1.ui.components.ThumbnailQuality.High,
            contentScale = ContentScale.Crop
        )

        // Real Inline Player
        val activeUrl = streamUrl

        if ((isActive || isPreloading) && activeUrl != null && extractionError == null) {
            val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
            InlineShortPlayer(
                videoUrl = activeUrl,
                audioUrl = audioUrl,
                isPlaying = isPlaying && isActive,
                isMuted = isMuted,
                playbackSpeed = if (isTemporarySpeedBoostActive && isActive) 2.0f else 1.0f,
                areControlsVisible = areControlsVisible && isActive,
                onError = { errorMsg ->
                    Log.e("ShortsScreen", "[Shorts] Playback error: url=$activeUrl, error=$errorMsg")
                    playerPlaybackError = null
                    failedUrls = failedUrls + activeUrl
                    if (streamBundle?.videoStreams?.all { it.url in (failedUrls + activeUrl) } == true) {
                        viewModel.preloadManager.markShortAsFailed(video.id)
                    }
                },
                onVideoEnded = onVideoEnded,
                onNearEnd = onNearEnd,
                modifier = Modifier.fillMaxSize().let { if (isActive) it else it.graphicsLayer(alpha = 0f) }
            )
        }

        // 2x Speed Boost Indicator Overlay
        if (isTemporarySpeedBoostActive && isActive) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
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

        // Auto-hide controls after 4 seconds of uninterrupted playback
        LaunchedEffect(areControlsVisible, isPlaying, isActive) {
            if (areControlsVisible && isPlaying && isActive) {
                kotlinx.coroutines.delay(4000)
                areControlsVisible = false
            }
        }

        // Overlay layout: dark gradient bottom layer to guarantee readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.45f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f
                    )
                )
        )

        // Top Bar: Back & Mute (smoothly animated with areControlsVisible)
        val context = LocalContext.current
        val activity = remember(context) { context as? androidx.activity.ComponentActivity }
        val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()

        AnimatedVisibility(
            visible = areControlsVisible && isActive,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        Log.d("ShortsScreen", "[Shorts] Back button tapped, exiting Shorts screen")
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom channel & title overlay information
        AnimatedVisibility(
            visible = areControlsVisible && isActive,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 76.dp, bottom = 44.dp)
        ) {
            Column {
                // Creator Row: [Avatar] Channel Name [Subscribe]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { video.uploaderUrl?.let { onChannelClick(it) } }
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        AsyncImage(
                            model = video.uploaderThumbnailUrl ?: "https://www.gstatic.com/images/branding/product/2x/avatar_square_blue_120dp.png",
                            contentDescription = "Channel avatar",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = video.uploaderName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Subscribe button
                    Surface(
                        onClick = { viewModel.toggleSubscription(video) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSubscribed) Color.White.copy(alpha = 0.2f) else VibeTubeRed,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Video Title / Description
                Text(
                    text = video.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }

        // Right corner sidebar buttons (Like, Comments, Share, Download, Save, Watch Full, More)
        AnimatedVisibility(
            visible = areControlsVisible && isActive,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(200)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 44.dp, end = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Like Action
                ShortActionRailItem(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    tint = if (isLiked) VibeTubeRed else Color.White,
                    label = if (isLiked) "Liked" else "Like",
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    onClick = { viewModel.toggleFavorite(video) }
                )

                // 2. Comments Action
                ShortActionRailItem(
                    icon = Icons.Outlined.Comment,
                    label = if (commentsList.isNotEmpty()) formatShortCount(commentsList.size.toLong()) else "Comments",
                    contentDescription = "Comments",
                    onClick = { showCommentsSheet = true }
                )

                // 3. Share Action
                ShortActionRailItem(
                    icon = Icons.Outlined.Share,
                    label = "Share",
                    contentDescription = "Share",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Watch this Short on ZyvoTube: https://youtube.com/watch?v=${video.id}")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                )

                // 4. Download Action
                ShortActionRailItem(
                    icon = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Outlined.FileDownload,
                    tint = if (isDownloaded) VibeTubeRed else Color.White,
                    label = if (isDownloaded) "Saved" else "Download",
                    contentDescription = if (isDownloaded) "Downloaded" else "Download",
                    onClick = { if (!isDownloaded) showDownloadSheet = true }
                )

                // 5. Save Action
                ShortActionRailItem(
                    icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    tint = if (isSaved) VibeTubeRed else Color.White,
                    label = if (isSaved) "Saved" else "Save",
                    contentDescription = if (isSaved) "Remove from saved" else "Save",
                    onClick = { viewModel.toggleSave(video) }
                )

                // 6. Watch Full Action
                ShortActionRailItem(
                    icon = Icons.Outlined.Fullscreen,
                    label = "Watch Full",
                    contentDescription = "Watch Full",
                    onClick = { onVideoClick(video) }
                )

                // 7. More Action
                ShortActionRailItem(
                    icon = Icons.Default.MoreVert,
                    label = "More",
                    contentDescription = "More options",
                    onClick = { showMenuSheet = true }
                )
            }
        }

        // Central loader
        if (isVideoLoading) {
            CircularProgressIndicator(
                color = VibeTubeRed,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Play/Pause brief indicator overlay
        if (showPlayPauseIndicator) {
            LaunchedEffect(showPlayPauseIndicator) {
                kotlinx.coroutines.delay(500)
                showPlayPauseIndicator = false
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (indicatorIsPlay) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Error display
        val currentError = extractionError ?: playerPlaybackError
        if (currentError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { /* prevent bubble click through */ },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Error",
                        tint = VibeTubeRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Couldn't play this video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentError,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            playerPlaybackError = null
                            extractionError = null
                            retryTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VibeTubeRed)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        }
    }

    // Modal Comments bottom sheet (Real data)
    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            CommentsSheet(
                comments = commentsList,
                isFetching = isFetchingComments,
                onLoadMore = { loadMoreComments() },
                onDismiss = { showCommentsSheet = false }
            )
        }
    }

    // Modal Metadata bottom sheet (Real data)
    if (showMenuSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMenuSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
            scrimColor = Color.Black.copy(alpha = 0.4f)
        ) {
            ShortMenuSheet(
                video = video,
                streamBundle = streamBundle,
                isLiked = isLiked,
                isSaved = isSaved,
                isDownloaded = isDownloaded,
                onLikeToggle = { viewModel.toggleFavorite(video) },
                onSaveToggle = { viewModel.toggleSave(video) },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Watch this Short on ZyvoTube: https://youtube.com/watch?v=${video.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                },
                onWatchFullClick = {
                    showMenuSheet = false
                    onVideoClick(video)
                },
                onDownloadClick = {
                    showMenuSheet = false
                    if (!isDownloaded) showDownloadSheet = true
                },
                onDismiss = { showMenuSheet = false }
            )
        }
    }

    if (showDownloadSheet) {
        if (streamBundle != null) {
            com.rahul.vibetube1.ui.components.DownloadSelectionSheet(
                videoStreams = streamBundle!!.videoStreams,
                audioStreams = streamBundle!!.audioStreams,
                onDismiss = { showDownloadSheet = false },
                onDownload = { stream, isAudioOnly, saveToDevice ->
                    viewModel.download(
                        video = video,
                        bundle = streamBundle!!,
                        url = stream.url,
                        quality = stream.quality,
                        format = stream.format,
                        isAdaptive = stream.isAdaptive,
                        isAudioOnly = isAudioOnly,
                        saveToDevice = saveToDevice
                    )
                    showDownloadSheet = false
                }
            )
        } else {
            ModalBottomSheet(
                onDismissRequest = { showDownloadSheet = false },
                scrimColor = Color.Black.copy(alpha = 0.4f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = VibeTubeRed)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Fetching stream formats...", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun ShortActionRailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White,
    contentDescription: String = label
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = androidx.compose.material3.ripple(bounded = false, radius = 24.dp)
            )
            .padding(vertical = 2.dp, horizontal = 2.dp)
            .widthIn(min = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        if (label.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun InlineShortPlayer(
    videoUrl: String,
    audioUrl: String?,
    isPlaying: Boolean,
    isMuted: Boolean = false,
    playbackSpeed: Float = 1.0f,
    areControlsVisible: Boolean,
    onError: (String) -> Unit,
    onVideoEnded: () -> Unit = {},
    onNearEnd: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnVideoEnded by rememberUpdatedState(onVideoEnded)
    val currentOnNearEnd by rememberUpdatedState(onNearEnd)
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            volume = if (isMuted) 0f else 1f
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(playbackSpeed) {
        val currentParams = exoPlayer.playbackParameters
        if (currentParams.speed != playbackSpeed) {
            exoPlayer.playbackParameters = androidx.media3.common.PlaybackParameters(playbackSpeed)
        }
    }

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekProgress by remember { mutableStateOf(0f) }

    // Continuously poll progress of media
    var nearEndTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(exoPlayer, isPlaying, isSeeking) {
        if (!isSeeking) {
            try {
                while (true) {
                    currentPosition = exoPlayer.currentPosition
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                    seekProgress = if (duration > 0) {
                        (currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    
                    if (duration > 0 && isPlaying && !nearEndTriggered && (duration - currentPosition <= 5000)) {
                        nearEndTriggered = true
                        currentOnNearEnd()
                    }
                    if (duration > 0 && currentPosition < 1000) {
                        nearEndTriggered = false
                    }
                    
                    kotlinx.coroutines.delay(250)
                }
            } catch (e: Exception) {
                // Safe ignore on cancel
            }
        }
    }

    // Handle Play/Pause
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Load sources
    LaunchedEffect(videoUrl, audioUrl) {
        try {
            val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context)
            val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
            
            val videoMediaItem = MediaItem.Builder().setUri(videoUrl).build()
            val videoSource = mediaSourceFactory.createMediaSource(videoMediaItem)

            if (audioUrl != null) {
                val audioMediaItem = MediaItem.Builder().setUri(audioUrl).build()
                val audioSource = mediaSourceFactory.createMediaSource(audioMediaItem)
                
                val mergedSource = androidx.media3.exoplayer.source.MergingMediaSource(videoSource, audioSource)
                exoPlayer.setMediaSource(mergedSource)
            } else {
                exoPlayer.setMediaSource(videoSource)
            }

            Log.d("SHORTS_PERF", "SHORTS_PLAYER_PREPARE_START: videoUrl=$videoUrl")
            exoPlayer.prepare()
            exoPlayer.playWhenReady = isPlaying
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to prepare player")
        }
    }

    // Listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onError(error.localizedMessage ?: "Playback failed")
            }
            override fun onRenderedFirstFrame() {
                Log.d("SHORTS_PERF", "SHORTS_FIRST_FRAME: first frame rendered")
            }
            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN"
                }
                Log.d("ShortsScreen", "[Shorts] Playback state changed: $stateName")
                if (state == Player.STATE_READY) {
                    Log.d("SHORTS_PERF", "SHORTS_FIRST_FRAME: player state READY")
                }
                if (state == Player.STATE_ENDED) {
                    currentOnVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show seek timestamp overlay during dragging
        if (isSeeking && duration > 0) {
            val activeMs = (seekProgress * duration).toLong()
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${formatTime(activeMs)} / ${formatTime(duration)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

// Seek Bar
        CustomSeekBar(
            progress = seekProgress,
            areControlsVisible = areControlsVisible,
            onProgressChange = { newValue ->
                isSeeking = true
                seekProgress = newValue
            },
            onProgressChangeFinished = {
                try {
                    val targetPos = (seekProgress * duration).toLong()
                    exoPlayer.seekTo(targetPos)
                    currentPosition = targetPos
                    isSeeking = false
                } catch (e: Exception) {
                    isSeeking = false
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

@Composable
fun ShortMenuSheet(
    video: VideoItem,
    streamBundle: StreamBundle?,
    isLiked: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onShareClick: () -> Unit,
    onWatchFullClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val title = streamBundle?.title ?: video.title
        val uploader = streamBundle?.uploaderName ?: video.uploaderName
        val views = streamBundle?.viewCount ?: video.viewCount
        val uploadDate = streamBundle?.uploadDate ?: video.uploadDate
        val subCount = streamBundle?.uploaderSubscriberCount ?: video.subscriberCount
        val description = streamBundle?.description

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "More options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = uploader,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (subCount != null && subCount > 0) {
                        Text(
                            text = " • ${formatSubs(subCount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatViews(views)} • ${uploadDate ?: "Unknown date"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ListItem(
            headlineContent = { Text(if (isSaved) "Remove from Saved Shorts" else "Save to Saved Shorts") },
            leadingContent = {
                Icon(
                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (isSaved) VibeTubeRed else MaterialTheme.colorScheme.onSurface
                )
            },
            modifier = Modifier.clickable {
                onSaveToggle()
            }
        )

        ListItem(
            headlineContent = { Text(if (isLiked) "Unlike (Remove from Liked)" else "Like Short") },
            leadingContent = {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) VibeTubeRed else MaterialTheme.colorScheme.onSurface
                )
            },
            modifier = Modifier.clickable {
                onLikeToggle()
            }
        )

        ListItem(
            headlineContent = { Text("Share via...") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            modifier = Modifier.clickable {
                onShareClick()
            }
        )

        ListItem(
            headlineContent = { Text("Watch Full Video") },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = "Watch Full",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            modifier = Modifier.clickable {
                onWatchFullClick()
            }
        )

        ListItem(
            headlineContent = { Text(if (isDownloaded) "Downloaded" else "Download Video / Audio") },
            leadingContent = {
                Icon(
                    imageVector = if (isDownloaded) Icons.Filled.CheckCircle else Icons.Outlined.FileDownload,
                    contentDescription = "Download",
                    tint = if (isDownloaded) VibeTubeRed else MaterialTheme.colorScheme.onSurface
                )
            },
            modifier = Modifier.clickable {
                onDownloadClick()
            }
        )
    }
}

private fun formatShortCount(count: Long): String {
    return when {
        count >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.1fB", count / 1_000_000_000.0).replace(".0B", "B")
        count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0).replace(".0M", "M")
        count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
        count > 0 -> count.toString()
        else -> ""
    }
}

private fun formatViews(views: Long): String {
    return when {
        views >= 1_000_000_000 -> String.format("%.1fB views", views / 1_000_000_000.0)
        views >= 1_000_000 -> String.format("%.1fM views", views / 1_000_000.0)
        views >= 1_000 -> String.format("%.1fK views", views / 1_000.0)
        else -> "$views views"
    }
}

private fun formatSubs(subs: Long?): String {
    if (subs == null) return ""
    return when {
        subs >= 1_000_000_000 -> String.format("%.1fB subs", subs / 1_000_000_000.0)
        subs >= 1_000_000 -> String.format("%.1fM subs", subs / 1_000_000.0)
        subs >= 1_000 -> String.format("%.1fK subs", subs / 1_000.0)
        else -> "$subs subs"
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun CustomSeekBar(
    progress: Float,
    areControlsVisible: Boolean,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    val activeProgress = if (isDragging) dragProgress else progress
    
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) 6.dp else if (areControlsVisible) 4.dp else 0.dp,
        label = "ThumbRadius"
    )
    val trackHeight by animateDpAsState(
        targetValue = if (isDragging) 4.dp else if (areControlsVisible) 2.5.dp else 1.5.dp,
        label = "TrackHeight"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isDragging = true
                    dragProgress = (down.position.x / size.width).coerceIn(0f, 1f)
                    onProgressChange(dragProgress)
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            isDragging = false
                            onProgressChangeFinished()
                            break
                        }
                        change.consume()
                        dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        onProgressChange(dragProgress)
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val width = maxWidth
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .background(Color.White.copy(alpha = 0.25f))
        )
        // Active progress track
        Box(
            modifier = Modifier
                .fillMaxWidth(activeProgress.coerceIn(0f, 1f))
                .height(trackHeight)
                .background(VibeTubeRed)
        )
        // Scrubbing Thumb
        if (thumbRadius > 0.dp) {
            Box(
                modifier = Modifier
                    .offset(x = (width * activeProgress.coerceIn(0f, 1f)) - thumbRadius)
                    .size(thumbRadius * 2)
                    .background(VibeTubeRed, CircleShape)
            )
        }
    }
}
