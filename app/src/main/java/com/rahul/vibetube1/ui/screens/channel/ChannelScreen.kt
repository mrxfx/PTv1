/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.channel
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rahul.vibetube1.domain.model.ChannelDetails
import com.rahul.vibetube1.domain.model.PlaylistItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.ui.components.ChannelCompactVideoRowSkeleton
import com.rahul.vibetube1.ui.components.ChannelMetadataSkeleton
import com.rahul.vibetube1.ui.components.DownloadDialogState
import com.rahul.vibetube1.ui.components.DownloadSelectionSheet
import com.rahul.vibetube1.ui.components.EmptyState
import com.rahul.vibetube1.ui.components.InfiniteScrollEffect
import com.rahul.vibetube1.ui.components.ThumbnailImage
import com.rahul.vibetube1.ui.components.ThumbnailQuality
import com.rahul.vibetube1.ui.components.rememberSyncShimmerTransition
import com.rahul.vibetube1.ui.screens.library.LibraryViewModel
import com.rahul.vibetube1.ui.theme.VibeTubeRed
import com.rahul.vibetube1.utils.VibeTubeError
import com.rahul.vibetube1.utils.VideoUtils
import com.rahul.vibetube1.utils.rememberScrollVisibilityConnection
import kotlinx.coroutines.flow.SharedFlow

private enum class ChannelTabType(val titleRes: Int) {
    HOME(R.string.channel_tab_home),
    VIDEOS(R.string.channel_tab_videos),
    SHORTS(R.string.channel_tab_shorts),
    PLAYLISTS(R.string.channel_tab_playlists),
    POSTS(R.string.channel_tab_posts)
}

@Composable
fun ChannelScreen(
    channelUrl: String,
    viewModel: ChannelViewModel,
    libraryViewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit = {},
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    ChannelContent(
        channelUrl = channelUrl,
        uiState = uiState,
        isSubscribed = isSubscribed,
        downloadedIds = downloadedIds,
        savedVideoIds = savedVideoIds,
        favoriteIds = favoriteIds,
        downloadState = downloadState,
        snackbarMessage = viewModel.snackbarMessage,
        onLoadChannel = viewModel::loadChannel,
        onLoadMore = viewModel::loadNextPage,
        onToggleSubscription = viewModel::toggleSubscription,
        onFavoriteClick = viewModel::toggleFavorite,
        onDownloadClick = viewModel::prepareDownload,
        onDownloadConfirm = viewModel::download,
        onDismissDownload = viewModel::dismissDownloadDialog,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onNavigateToDownloads = onNavigateToDownloads,
        onBack = onBack,
        onSearchClick = onSearchClick,
        onVideoClick = onVideoClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onPlaylistClick = onPlaylistClick,
        onSortModeSelected = viewModel::setSortMode
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelContent(
    channelUrl: String,
    uiState: ChannelUiState,
    isSubscribed: Boolean?,
    downloadedIds: Set<String>,
    savedVideoIds: Set<String>,
    favoriteIds: Set<String>,
    downloadState: DownloadDialogState,
    snackbarMessage: SharedFlow<String>,
    onLoadChannel: (String) -> Unit,
    onLoadMore: () -> Unit,
    onToggleSubscription: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, StreamBundle, String, String, String, Boolean, Boolean, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSortModeSelected: (ChannelVideoSortMode) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)
    var showAboutSheet by remember { mutableStateOf(false) }

    LaunchedEffect(channelUrl) {
        onLoadChannel(channelUrl)
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val channelName = (uiState as? ChannelUiState.Success)?.details?.name ?: ""
            ChannelTopBar(
                channelName = channelName,
                channelUrl = channelUrl,
                onBack = onBack,
                onSearchClick = onSearchClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is ChannelUiState.Loading -> {
                    ChannelMetadataSkeleton()
                }
                is ChannelUiState.Error -> {
                    EmptyState(
                        icon = if (uiState.error is VibeTubeError.Network) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = stringResource(R.string.error_loading_channel),
                        description = uiState.error.getMessage(),
                        actionText = stringResource(R.string.retry),
                        onActionClick = { onLoadChannel(channelUrl) }
                    )
                }
                is ChannelUiState.Success -> {
                    val details = uiState.details

                    val availableTabs = remember(details) {
                        val tabs = mutableListOf<ChannelTabType>()
                        if (details.videos.isNotEmpty() || details.playlists.isNotEmpty() || details.posts.isNotEmpty()) {
                            tabs.add(ChannelTabType.HOME)
                        }
                        val normalVideos = details.videos.filter { !VideoUtils.isShort(it) }
                        if (normalVideos.isNotEmpty()) {
                            tabs.add(ChannelTabType.VIDEOS)
                        }
                        val shorts = details.videos.filter { VideoUtils.isShort(it) }
                        if (shorts.isNotEmpty()) {
                            tabs.add(ChannelTabType.SHORTS)
                        }
                        if (details.playlists.isNotEmpty()) {
                            tabs.add(ChannelTabType.PLAYLISTS)
                        }
                        if (details.posts.isNotEmpty()) {
                            tabs.add(ChannelTabType.POSTS)
                        }
                        if (tabs.isEmpty()) {
                            tabs.add(ChannelTabType.VIDEOS)
                        }
                        tabs
                    }

                    var selectedTab by remember(availableTabs) {
                        mutableStateOf(availableTabs.firstOrNull() ?: ChannelTabType.VIDEOS)
                    }

                    if (selectedTab !in availableTabs) {
                        selectedTab = availableTabs.firstOrNull() ?: ChannelTabType.VIDEOS
                    }

                    val normalVideos = remember(details.videos) {
                        val filtered = details.videos.filter { !VideoUtils.isShort(it) }.distinctBy { it.id }
                        if (filtered.isEmpty()) details.videos.distinctBy { it.id } else filtered
                    }

                    val currentSortMode = (uiState as? ChannelUiState.Success)?.sortMode ?: ChannelVideoSortMode.LATEST

                    val sortedNormalVideos = remember(normalVideos, currentSortMode) {
                        val distinctList = normalVideos.distinctBy { it.id }
                        when (currentSortMode) {
                            ChannelVideoSortMode.LATEST -> {
                                if (distinctList.any { it.rawUploadDate != null }) {
                                    distinctList.sortedWith(
                                        compareByDescending<VideoItem> { it.rawUploadDate ?: 0L }
                                    )
                                } else {
                                    distinctList
                                }
                            }
                            ChannelVideoSortMode.POPULAR -> {
                                distinctList.sortedByDescending { it.viewCount }
                            }
                            ChannelVideoSortMode.OLDEST -> {
                                if (distinctList.any { it.rawUploadDate != null }) {
                                    distinctList.sortedWith(
                                        compareBy<VideoItem> { it.rawUploadDate ?: Long.MAX_VALUE }
                                    )
                                } else {
                                    distinctList.reversed()
                                }
                            }
                        }
                    }

                    val realShorts = remember(details.videos) {
                        details.videos.filter { VideoUtils.isShort(it) }.distinctBy { it.id }
                    }

                    val shortRows = remember(realShorts) {
                        realShorts.chunked(2)
                    }

                    // Infinite pagination trigger for Videos tab
                    if (selectedTab == ChannelTabType.VIDEOS && details.nextVideosPage != null) {
                        InfiniteScrollEffect(
                            listState = listState,
                            buffer = 4,
                            enabled = !uiState.isFetchingNextPage,
                            onLoadMore = onLoadMore
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Channel Banner (if present)
                        if (!details.bannerUrl.isNullOrBlank()) {
                            item(key = "channel_banner") {
                                ChannelBanner(bannerUrl = details.bannerUrl)
                            }
                        }

                        // 2. Channel Identity (Avatar, Name, Handle, Subscriber & Video count)
                        item(key = "channel_identity") {
                            ChannelIdentity(
                                details = details,
                                isSubscribed = isSubscribed,
                                onToggleSubscription = onToggleSubscription,
                                onOpenDescription = { showAboutSheet = true }
                            )
                        }

                        // 3. Sticky Tab Row
                        stickyHeader(key = "channel_tabs_header") {
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shadowElevation = 0.dp
                            ) {
                                ChannelTabRow(
                                    tabs = availableTabs,
                                    selectedTab = selectedTab,
                                    onTabSelected = { selectedTab = it }
                                )
                            }
                        }

                        // 4. Tab Specific Content
                        when (selectedTab) {
                            ChannelTabType.HOME -> {
                                if (details.videos.isEmpty()) {
                                    item(key = "home_empty") {
                                        ChannelEmptyView(message = stringResource(R.string.no_videos_in_channel))
                                    }
                                } else {
                                    // For You / Featured shelf
                                    if (normalVideos.size >= 3) {
                                        item(key = "home_for_you_section") {
                                            ChannelShelfSection(
                                                title = stringResource(R.string.for_you),
                                                videos = normalVideos.take(6),
                                                onVideoClick = onVideoClick
                                            )
                                        }
                                        item(key = "home_latest_header") {
                                            Text(
                                                text = stringResource(R.string.latest_videos),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                                            )
                                        }
                                        items(
                                            items = normalVideos.drop(6).ifEmpty { normalVideos.take(3) },
                                            key = { "home_vid_${it.id}" }
                                        ) { video ->
                                            CompactChannelVideoRow(
                                                video = video,
                                                isDownloaded = downloadedIds.contains(video.id),
                                                isFavorite = favoriteIds.contains(video.id),
                                                onClick = { onVideoClick(video) },
                                                onDownloadClick = { onDownloadClick(video) },
                                                onFavoriteClick = { onFavoriteClick(video) },
                                                onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                                            )
                                        }
                                    } else {
                                        items(
                                            items = normalVideos,
                                            key = { "home_vid_${it.id}" }
                                        ) { video ->
                                            CompactChannelVideoRow(
                                                video = video,
                                                isDownloaded = downloadedIds.contains(video.id),
                                                isFavorite = favoriteIds.contains(video.id),
                                                onClick = { onVideoClick(video) },
                                                onDownloadClick = { onDownloadClick(video) },
                                                onFavoriteClick = { onFavoriteClick(video) },
                                                onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                                            )
                                        }
                                    }

                                    // Playlists preview in Home if available
                                    if (details.playlists.isNotEmpty()) {
                                        item(key = "home_playlists_header") {
                                            Text(
                                                text = stringResource(R.string.playlists),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                                            )
                                        }
                                        items(
                                            items = details.playlists.take(3),
                                            key = { "home_pl_${it.id}" }
                                        ) { playlist ->
                                            ModernPlaylistItem(
                                                playlist = playlist,
                                                onClick = { onPlaylistClick(playlist.id) }
                                            )
                                        }
                                    }
                                }
                            }

                            ChannelTabType.VIDEOS -> {
                                item(key = "videos_sort_chips") {
                                    ChannelSortChipRow(
                                        selectedSortMode = currentSortMode,
                                        onSortModeSelected = onSortModeSelected
                                    )
                                }

                                if (sortedNormalVideos.isEmpty()) {
                                    item(key = "videos_empty") {
                                        ChannelEmptyView(message = stringResource(R.string.no_videos_in_channel))
                                    }
                                } else {
                                    items(
                                        items = sortedNormalVideos,
                                        key = { "vid_${it.id}" }
                                    ) { video ->
                                        CompactChannelVideoRow(
                                            video = video,
                                            isDownloaded = downloadedIds.contains(video.id),
                                            isFavorite = favoriteIds.contains(video.id),
                                            onClick = { onVideoClick(video) },
                                            onDownloadClick = { onDownloadClick(video) },
                                            onFavoriteClick = { onFavoriteClick(video) },
                                            onAddToPlaylistClick = { onAddToPlaylistClick(video) }
                                        )
                                    }

                                    if (uiState.isFetchingNextPage) {
                                        item(key = "videos_loading_more") {
                                            val syncTransition = rememberSyncShimmerTransition()
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                repeat(2) {
                                                    ChannelCompactVideoRowSkeleton(transition = syncTransition)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            ChannelTabType.SHORTS -> {
                                if (realShorts.isEmpty()) {
                                    item(key = "shorts_empty") {
                                        ChannelEmptyView(message = stringResource(R.string.no_shorts_in_channel))
                                    }
                                } else {
                                    items(
                                        items = shortRows,
                                        key = { row -> "shorts_row_${row.joinToString("_") { it.id }}" }
                                    ) { row ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (short in row) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    ChannelShortCard(
                                                        short = short,
                                                        onClick = { onVideoClick(short) }
                                                    )
                                                }
                                            }
                                            if (row.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            ChannelTabType.PLAYLISTS -> {
                                if (details.playlists.isEmpty()) {
                                    item(key = "playlists_empty") {
                                        ChannelEmptyView(message = stringResource(R.string.no_playlists_in_channel))
                                    }
                                } else {
                                    items(
                                        items = details.playlists,
                                        key = { "pl_${it.id}" }
                                    ) { playlist ->
                                        ModernPlaylistItem(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist.id) }
                                        )
                                    }
                                }
                            }

                            ChannelTabType.POSTS -> {
                                if (details.posts.isEmpty()) {
                                    item(key = "posts_empty") {
                                        ChannelEmptyView(message = stringResource(R.string.no_posts_in_channel))
                                    }
                                } else {
                                    items(
                                        items = details.posts,
                                        key = { "post_${it.id}" }
                                    ) { post ->
                                        ChannelPostCard(
                                            post = post,
                                            channelAvatarUrl = details.avatarUrl,
                                            channelName = details.name
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom spacing for comfortable navigation
                        item(key = "bottom_space") {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // About Channel Bottom Sheet
                    if (showAboutSheet) {
                        ChannelAboutSheet(
                            details = details,
                            channelUrl = channelUrl,
                            onDismiss = { showAboutSheet = false }
                        )
                    }
                }
            }

            // Download Dialogs
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

/**
 * Clean compact Top Bar:
 * ← Back                         Search   ⋮
 */
@Composable
private fun ChannelTopBar(
    channelName: String,
    channelUrl: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val shareChannel = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, if (channelName.isNotBlank()) "$channelName\n$channelUrl" else channelUrl)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share channel"))
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("channel_search_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = { shareChannel() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("channel_share_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.share_channel),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("channel_more_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.channel_more_options),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share_channel)) },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            shareChannel()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_in_browser)) },
                        leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(channelUrl))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                // Graceful fallback
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Responsive channel banner with 16:5.5 aspect ratio and clean rounded corners.
 */
@Composable
private fun ChannelBanner(
    bannerUrl: String?,
    modifier: Modifier = Modifier
) {
    if (bannerUrl.isNullOrBlank()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 5.5f)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(bannerUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Channel Identity: Avatar, Name, Handle, Subscriber Count, Video Count, Description & Subscribe Button.
 */
@Composable
private fun ChannelIdentity(
    details: ChannelDetails,
    isSubscribed: Boolean?,
    onToggleSubscription: () -> Unit,
    onOpenDescription: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar
            Surface(
                modifier = Modifier.size(68.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(details.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = details.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Name
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Handle if available in ID
                val handle = remember(details) {
                    if (details.id.startsWith("@")) details.id else null
                }
                if (handle != null) {
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Metadata: Subscriber count · Video count
                val metadata = buildString {
                    details.subscriberCount?.let { count ->
                        if (count < 0) {
                            append(stringResource(R.string.subscribers_hidden))
                        } else {
                            append(stringResource(R.string.subscribers_count, VideoUtils.formatNumber(count)))
                        }
                    }
                    if (details.videos.isNotEmpty()) {
                        if (isNotEmpty()) append(" · ")
                        append(stringResource(R.string.video_count, details.videos.size))
                    }
                }

                if (metadata.isNotEmpty()) {
                    Text(
                        text = metadata,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Description preview with "...more"
        if (!details.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenDescription() }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "…more",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.about_channel),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Subscribe Button
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onToggleSubscription,
            colors = if (isSubscribed == true) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = VibeTubeRed,
                    contentColor = Color.White
                )
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSubscribed == true) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isSubscribed == true) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Tab Row with VibeTube Red indicator.
 */
@Composable
private fun ChannelTabRow(
    tabs: List<ChannelTabType>,
    selectedTab: ChannelTabType,
    onTabSelected: (ChannelTabType) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        indicator = { tabPositions ->
            if (selectedIndex in tabPositions.indices) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    height = 2.5.dp,
                    color = VibeTubeRed
                )
            }
        },
        divider = {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )
        },
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
}

/**
 * Horizontal shelf for "For You" / Featured videos on the Home tab.
 */
@Composable
private fun ChannelShelfSection(
    title: String,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = videos,
                key = { "shelf_${it.id}" }
            ) { video ->
                ChannelHorizontalVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

/**
 * Horizontal video card for shelves.
 */
@Composable
private fun ChannelHorizontalVideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = video.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        val meta = remember(video) {
            val viewsText = if (video.viewCount >= 0) VideoUtils.formatNumber(video.viewCount) + " views" else null
            val dateText = video.uploadDate?.takeIf { it.isNotBlank() }
            listOfNotNull(viewsText, dateText).joinToString(" · ")
        }
        if (meta.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Compact video row for the Videos list.
 * ┌──────────────┐
 * │              │  Video title...
 * │  Thumbnail   │  Channel / metadata
 * │              │  views · date
 * └──────────────┘
 */
@Composable
private fun CompactChannelVideoRow(
    video: VideoItem,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 16:9 Thumbnail
        Box(
            modifier = Modifier
                .width(135.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (video.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitle = remember(video) {
                val viewsText = if (video.viewCount >= 0) VideoUtils.formatNumber(video.viewCount) + " views" else null
                val dateText = video.uploadDate?.takeIf { it.isNotBlank() }
                listOfNotNull(viewsText, dateText).joinToString(" · ")
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // More Menu (3-dots)
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.channel_more_options),
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.playlist)) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onAddToPlaylistClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download)) },
                    leadingIcon = { Icon(if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onDownloadClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Unlike" else stringResource(R.string.like)) },
                    leadingIcon = { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onFavoriteClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${video.title}\nhttps://youtube.com/watch?v=${video.id}")
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share video"))
                    }
                )
            }
        }
    }
}

/**
 * 9:16 compact Shorts card.
 */
@Composable
private fun ChannelShortCard(
    short: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(9f / 16f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(short.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = short.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = VibeTubeRed
                    )
                    Text(
                        text = "Shorts",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom gradient overlay with Title and View Count
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = short.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )

                    if (short.viewCount >= 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${VideoUtils.formatNumber(short.viewCount)} views",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern playlist row with thumbnail overlay.
 */
@Composable
private fun ModernPlaylistItem(
    playlist: PlaylistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            ThumbnailImage(
                videoId = "",
                thumbnailUrl = playlist.thumbnailUrl,
                quality = ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterEnd),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${playlist.streamCount} videos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * About channel bottom sheet with complete details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelAboutSheet(
    details: ChannelDetails,
    channelUrl: String,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = stringResource(R.string.about_channel),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!details.description.isNullOrBlank()) {
                Text(
                    text = details.description,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Channel Link Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(channelUrl))
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channelUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tap to copy link",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Subscribers & Videos summary
            details.subscriberCount?.let { count ->
                if (count >= 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.subscribers_count, VideoUtils.formatNumber(count)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (details.videos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.video_count, details.videos.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Clean empty state for channel tabs.
 */
@Composable
private fun ChannelEmptyView(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Card composable for channel community posts.
 */
@Composable
private fun ChannelPostCard(
    post: com.rahul.vibetube1.domain.model.ChannelPostItem,
    channelAvatarUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(channelAvatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channelName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    post.timeFormatted?.let { time ->
                        Text(
                            text = time,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            if (!post.attachmentImageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(post.attachmentImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (post.likeCount != null || post.commentCount != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    post.likeCount?.let { likes ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = VideoUtils.formatNumber(likes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    post.commentCount?.let { comments ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = VideoUtils.formatNumber(comments),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter / Sort chip row for Channel Videos tab ([ Latest ] [ Popular ] [ Oldest ]).
 */
@Composable
private fun ChannelSortChipRow(
    selectedSortMode: ChannelVideoSortMode,
    onSortModeSelected: (ChannelVideoSortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            items = ChannelVideoSortMode.entries.toTypedArray(),
            key = { it.name }
        ) { mode ->
            val isSelected = mode == selectedSortMode

            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                onClick = { onSortModeSelected(mode) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("sort_chip_${mode.name.lowercase()}")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

