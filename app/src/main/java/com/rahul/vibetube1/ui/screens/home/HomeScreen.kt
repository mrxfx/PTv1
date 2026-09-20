/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.home
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyRow
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.ui.components.DownloadSelectionSheet
import com.rahul.vibetube1.ui.components.VideoListSkeleton
import com.rahul.vibetube1.ui.components.VideoList
import com.rahul.vibetube1.ui.components.VideoItemRow
import com.rahul.vibetube1.ui.components.SwipeToDismissVideoItem
import com.rahul.vibetube1.ui.components.DownloadDialogState
import com.rahul.vibetube1.ui.components.GlassSurface
import com.rahul.vibetube1.ui.components.VideoPreviewDialog
import com.rahul.vibetube1.ui.components.EmptyState

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import com.rahul.vibetube1.ui.theme.GlassAlpha
import com.rahul.vibetube1.utils.VibeTubeError
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    libraryViewModel: com.rahul.vibetube1.ui.screens.library.LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onNavigateToDownloads: () -> Unit // New parameter
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    
    // Optimized: Using remember(favorites) for ID mapping to avoid O(N) mapping on every recomposition
    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val showWhatsNewDialog by viewModel.showWhatsNewDialog.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkWhatsNew()
    }

    val onRefresh = remember(viewModel) { { viewModel.refresh() } }
    val onLoadMore = remember(viewModel) { { viewModel.loadNextTrendingPage() } }
    val onFavoriteClick = remember(viewModel) { { v: VideoItem -> viewModel.toggleFavorite(v) } }
    val onNotInterestedClick = remember(viewModel) { { v: VideoItem -> viewModel.markNotInterested(v) } }
    val onDownloadClick = remember(viewModel) { { v: VideoItem -> viewModel.prepareDownload(v) } }
    val onDownloadConfirm = remember(viewModel) { { v: VideoItem, b: com.rahul.vibetube1.domain.model.StreamBundle, s1: String?, s2: String?, s3: String?, bool: Boolean, isAudio: Boolean, saveToDevice: Boolean -> viewModel.download(v, b, s1, s2, s3, bool, isAudio, saveToDevice) } }
    val onDismissDownload = remember(viewModel) { { viewModel.dismissDownloadDialog() } }
    val onPersonalizedNotifyShown = remember(viewModel) { { viewModel.onPersonalizedNotifyShown() } }

    HomeContent(
        state = state,
        isRefreshing = isRefreshing,
        downloadState = downloadState,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        savedVideoIds = savedVideoIds,
        snackbarMessage = viewModel.snackbarMessage,
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        onFavoriteClick = onFavoriteClick,
        onNotInterestedClick = onNotInterestedClick,
        onDownloadClick = onDownloadClick,
        onDownloadConfirm = onDownloadConfirm,
        onDismissDownload = onDismissDownload,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onPersonalizedNotifyShown = onPersonalizedNotifyShown,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onNavigateToDownloads = onNavigateToDownloads
    )

    if (showWhatsNewDialog) {
        com.rahul.vibetube1.ui.components.WhatsNewDialog(
            onDismiss = { viewModel.dismissWhatsNew() }
        )
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val itemLayouts = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val coroutineScope = rememberCoroutineScope()
    
    val selectedLayout = itemLayouts[selectedCategory]
    
    val indicatorOffset by animateFloatAsState(
        targetValue = selectedLayout?.first ?: 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorOffset"
    )
    
    val indicatorWidth by animateFloatAsState(
        targetValue = selectedLayout?.second ?: 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "indicatorWidth"
    )

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }

    LaunchedEffect(selectedCategory, selectedLayout) {
        selectedLayout?.let { (x, width) ->
            // Center the selected item in the scroll view roughly
            val targetScroll = (x - (screenWidthPx / 2f) + width / 2).toInt().coerceIn(0, scrollState.maxValue)
            coroutineScope.launch {
                scrollState.animateScrollTo(targetScroll)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            // Background Liquid Indicator
            if (indicatorWidth > 0f) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(indicatorOffset.roundToInt(), 0) }
                        .width(with(LocalDensity.current) { indicatorWidth.toDp() })
                        .height(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(18.dp)
                        )
                )
            }

            // Category Items
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onCategorySelected(category) }
                            )
                            .onGloballyPositioned { coordinates ->
                                itemLayouts[category] = Pair(coordinates.positionInParent().x, coordinates.size.width.toFloat())
                            }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeShortCard(
    video: VideoItem,
    onPreview: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(240.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (onPreview != null) {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreview()
                    }
                } else null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            com.rahul.vibetube1.ui.components.ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                quality = com.rahul.vibetube1.ui.components.ThumbnailQuality.Medium
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .size(20.dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeShortsSection(
    videos: List<VideoItem>,
    onPreview: ((VideoItem) -> Unit)? = null,
    onVideoClick: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Whatshot,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ZyvoTube Shorts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = videos,
                key = { "short_${it.id}" }
            ) { video ->
                HomeShortCard(
                    video = video,
                    onPreview = if (onPreview != null) { { onPreview(video) } } else null,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@Composable
private fun ContinuePlayingSection(
    videos: List<VideoItem>,
    onPreview: ((VideoItem) -> Unit)? = null,
    onVideoClick: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Text(
            text = stringResource(R.string.continue_playing),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = videos,
                key = { it.id }
            ) { video ->
                ContinueWatchingCard(
                    video = video,
                    onPreview = if (onPreview != null) { { onPreview(video) } } else null,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueWatchingCard(
    video: VideoItem,
    onPreview: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = Modifier
            .width(200.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (onPreview != null) {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreview()
                    }
                } else null
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            com.rahul.vibetube1.ui.components.ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                modifier = Modifier.fillMaxSize()
            )
            
            video.watchProgress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = Color.Red,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = video.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    isRefreshing: Boolean,
    downloadState: DownloadDialogState,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    savedVideoIds: Set<String>,
    snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onNotInterestedClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, com.rahul.vibetube1.domain.model.StreamBundle, String?, String?, String?, Boolean, Boolean, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onPersonalizedNotifyShown: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onNavigateToDownloads: () -> Unit // New parameter
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val categories = remember {
        listOf("All", "Music", "Gaming", "News", "Movies", "Technology", "Live")
    }
    var selectedCategory by remember { mutableStateOf("All") }

    val homeShorts = remember(state.homeShorts, state.trendingVideos) {
        if (state.homeShorts.isNotEmpty()) {
            state.homeShorts
        } else {
            state.trendingVideos.filter { com.rahul.vibetube1.utils.VideoUtils.isShort(it) }.take(6)
        }
    }

    val displayVideos = remember(state.trendingVideos, selectedCategory, homeShorts) {
        if (selectedCategory == "All") {
            val shortIds = homeShorts.map { it.id }.toSet()
            state.trendingVideos.filter { it.id !in shortIds }
        } else {
            state.trendingVideos.filter { video ->
                when (selectedCategory) {
                    "Music" -> video.title.contains("music", ignoreCase = true) || video.title.contains("song", ignoreCase = true) || video.title.contains("audio", ignoreCase = true) || video.title.contains("mv", ignoreCase = true) || video.uploaderName.contains("music", ignoreCase = true)
                    "Gaming" -> video.title.contains("gaming", ignoreCase = true) || video.title.contains("game", ignoreCase = true) || video.title.contains("play", ignoreCase = true) || video.title.contains("walkthrough", ignoreCase = true)
                    "News" -> video.title.contains("news", ignoreCase = true) || video.title.contains("politics", ignoreCase = true) || video.title.contains("report", ignoreCase = true) || video.title.contains("update", ignoreCase = true)
                    "Movies" -> video.title.contains("movie", ignoreCase = true) || video.title.contains("trailer", ignoreCase = true) || video.title.contains("film", ignoreCase = true) || video.title.contains("teaser", ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    // Header Scroll State
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val connection = remember(headerHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // Bottom Bar Hiding Logic (Binary Toggle for global Bars)
                if (delta < -15f) onBarsVisibilityChange(false)
                if (delta > 15f) onBarsVisibilityChange(true)

                // Scrolling Down: Collapse Header
                if (delta < 0 && headerOffsetPx > -headerHeightPx) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                
                // Scrolling Up: Expand Header
                // Check if we are at the top of the list or scrolling up significantly
                if (delta > 0 && headerOffsetPx < 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }
        }
    }

    // Ensure Bars are initially visible
    LaunchedEffect(Unit) {
        onBarsVisibilityChange(true)
    }

    LaunchedEffect(Unit) {
        onBarsVisibilityChange(true)
    }

    val pullToRefreshState = rememberPullToRefreshState()

    // Floating Notification State
    var showPersonalizedNotify by remember { mutableStateOf(false) }
    var previewVideo by remember { mutableStateOf<VideoItem?>(null) }
    
    LaunchedEffect(state.isPersonalized) {
        if (state.isPersonalized) {
            showPersonalizedNotify = true
            kotlinx.coroutines.delay(4000)
            showPersonalizedNotify = false
            onPersonalizedNotifyShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = with(density) { (headerHeightPx + headerOffsetPx).coerceAtLeast(0f).toDp() + 8.dp })
                        .testTag("pull_to_refresh_indicator"),
                    color = MaterialTheme.colorScheme.primary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection)
        ) {
            val videos = displayVideos
            val isLoading = state.isTrendingLoading

            AnimatedContent(
                targetState = isLoading && videos.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "HomeContentTransition"
            ) { loading ->
                if (loading) {
                    VideoListSkeleton()
                } else if (state.error != null && videos.isEmpty()) {
                    val isNetworkError = state.error is VibeTubeError.Network
                    
                    EmptyState(
                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                        description = if (isNetworkError) "Your downloads are still available offline." else state.error.getMessage(),
                        actionText = if (isNetworkError) "Checkout Downloads" else stringResource(R.string.retry),
                        onActionClick = { 
                            if (isNetworkError) onNavigateToDownloads() else onRefresh()
                        }
                    )
                } else if (!isLoading && videos.isEmpty() && state.error == null) {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.no_videos_found),
                        description = "Couldn't find any videos right now. Try refreshing later.",
                        actionText = stringResource(R.string.retry),
                        onActionClick = { onRefresh() }
                    )
                } else {
                    val trendingBatch = remember(videos) { videos.take(2) }
                    val moreForYouBatch = remember(videos) { videos.drop(2) }

                    VideoList(
                        videos = if (selectedCategory == "All") moreForYouBatch else videos,
                        downloadedIds = downloadedIds,
                        favoriteIds = favoriteIds,
                        savedVideoIds = savedVideoIds,
                        onVideoClick = onVideoClick,
                        onPreview = { previewVideo = it },
                        onChannelClick = onChannelClick,
                        onFavoriteClick = onFavoriteClick,
                        onNotInterestedClick = onNotInterestedClick,
                        onDownloadClick = onDownloadClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onDismissVideo = onNotInterestedClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        header = {
                            if (selectedCategory == "All") {
                                Column {
                                    ContinuePlayingSection(
                                        videos = state.continuePlayingVideos,
                                        onPreview = { previewVideo = it },
                                        onVideoClick = onVideoClick
                                    )
                                    if (trendingBatch.isNotEmpty()) {
                                        Text(
                                            text = "Trending Feed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                        )
                                        trendingBatch.forEach { video ->
                                            SwipeToDismissVideoItem(
                                                video = video,
                                                onDismiss = onNotInterestedClick
                                            ) {
                                                VideoItemRow(
                                                    video = video,
                                                    isDownloaded = downloadedIds.contains(video.id),
                                                    isFavorite = favoriteIds.contains(video.id),
                                                    isSaved = savedVideoIds.contains(video.id),
                                                    onFavoriteClick = { onFavoriteClick(video) },
                                                    onNotInterestedClick = { onNotInterestedClick(video) },
                                                    onDownloadClick = { onDownloadClick(video) },
                                                    onAddToPlaylistClick = { onAddToPlaylistClick(video) },
                                                    onPreview = { previewVideo = video },
                                                    onChannelClick = if (video.uploaderUrl != null) { { onChannelClick(video.uploaderUrl) } } else null,
                                                    onClick = { onVideoClick(video) }
                                                )
                                            }
                                        }
                                    }
                                    HomeShortsSection(
                                        videos = homeShorts,
                                        onPreview = { previewVideo = it },
                                        onVideoClick = onVideoClick
                                    )
                                    if (moreForYouBatch.isNotEmpty()) {
                                        Text(
                                            text = "More For You",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        contentPadding = PaddingValues(
                            top = with(density) { headerHeightPx.toDp() },
                            bottom = 100.dp
                        )
                    )
                }
            }
        }

        // Horizontal scrolling category chips, sliding with nested scroll offset
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = headerOffsetPx }
                .onGloballyPositioned { layoutCoordinates ->
                    val newHeight = layoutCoordinates.size.height.toFloat()
                    if (headerHeightPx != newHeight) {
                        headerHeightPx = newHeight
                    }
                }
        ) {
            CategoryChipsRow(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
        }

        // Quick Action Dialogs
        when (val downloadDialogState = downloadState) {
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
                    videoStreams = downloadDialogState.bundle.videoStreams,
                    audioStreams = downloadDialogState.bundle.audioStreams,
                    onDismiss = { onDismissDownload() },
                    onDownload = { stream, isAudioOnly, saveToDevice ->
                        onDownloadConfirm(
                            downloadDialogState.video,
                            downloadDialogState.bundle,
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

        state.error?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(error.getMessage())
            }
        }

        // Floating Personalized Notification
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp), // Float above bottom bar
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showPersonalizedNotify,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = GlassAlpha),
                    shape = CircleShape,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.personalized_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.personalized_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )

        // Long-press Video Preview Dialog
        previewVideo?.let { video ->
            VideoPreviewDialog(
                video = video,
                onDismiss = { previewVideo = null },
                onWatchClick = { selectedVideo ->
                    previewVideo = null
                    onVideoClick(selectedVideo)
                }
            )
        }
    }
}
