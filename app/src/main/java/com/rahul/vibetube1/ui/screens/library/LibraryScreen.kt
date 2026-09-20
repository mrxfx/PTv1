/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rahul.vibetube1.data.local.DownloadEntity
import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.data.local.HistoryEntity
import com.rahul.vibetube1.data.local.SubscriptionEntity
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.utils.VideoUtils
import com.rahul.vibetube1.utils.rememberScrollVisibilityConnection
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onSeeAllDownloads: () -> Unit,
    onExploreClick: () -> Unit = {}
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.localPlaylists.collectAsStateWithLifecycle()
    val savedVideoIds by viewModel.savedVideoIds.collectAsStateWithLifecycle()

    LibraryDashboard(
        downloads = downloads,
        favorites = favorites,
        history = history,
        subscriptions = subscriptions,
        playlists = playlists,
        localPlaylists = localPlaylists,
        savedVideoIds = savedVideoIds,
        onCreateLocalPlaylist = viewModel::createLocalPlaylist,
        onDeleteLocalPlaylist = viewModel::deleteLocalPlaylist,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onDeleteDownload = viewModel::deleteDownload,
        onCancelDownload = viewModel::cancelDownload,
        onResumeDownload = viewModel::resumeDownload,
        onRemoveFavorite = viewModel::removeFavorite,
        onRemoveHistoryItem = viewModel::removeFromHistory,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onSeeAllHistory = onSeeAllHistory,
        onSeeAllSubscriptions = onSeeAllSubscriptions,
        onSeeAllDownloads = onSeeAllDownloads,
        onChannelClick = onChannelClick,
        onPlaylistClick = onPlaylistClick,
        onExploreClick = onExploreClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryDashboard(
    downloads: List<DownloadEntity>,
    favorites: List<FavoriteEntity>,
    history: List<HistoryEntity>,
    subscriptions: List<SubscriptionEntity>,
    playlists: List<com.rahul.vibetube1.data.local.PlaylistFavoriteEntity>,
    localPlaylists: List<com.rahul.vibetube1.data.local.LocalPlaylistEntity>,
    savedVideoIds: Set<String>,
    onCreateLocalPlaylist: (String) -> Unit,
    onDeleteLocalPlaylist: (com.rahul.vibetube1.data.local.LocalPlaylistEntity) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onSeeAllDownloads: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onExploreClick: () -> Unit
) {
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val continueWatching = remember(history) {
        history.filter { item ->
            item.durationMs > 0 && item.progressMs > 0 && item.progressMs < item.durationMs * 0.95f
        }.take(3)
    }

    val playlistsIndex = remember(continueWatching.isNotEmpty()) {
        var idx = 2
        if (continueWatching.isNotEmpty()) idx++
        idx += 2 // History and Subscriptions are always emitted
        idx
    }

    val downloadsIndex = remember(continueWatching.isNotEmpty()) {
        playlistsIndex + 1
    }
    
    val likedIndex = remember(continueWatching.isNotEmpty()) {
        downloadsIndex + 1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
        ) {
            item {
                CompactProfileHeader(
                    downloadCount = downloads.size,
                    subscriptionCount = subscriptions.size,
                    favoriteCount = favorites.size
                )
            }

            item {
                QuickAccessRow(
                    onHistoryClick = onSeeAllHistory,
                    onDownloadsClick = onSeeAllDownloads,
                    onPlaylistsClick = {
                        coroutineScope.launch { listState.animateScrollToItem(playlistsIndex) }
                    },
                    onLikedClick = {
                        coroutineScope.launch { listState.animateScrollToItem(likedIndex) }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (continueWatching.isNotEmpty()) {
                item {
                    CompactSectionTitle(title = "Continue Watching", onSeeAllClick = onSeeAllHistory)
                    continueWatching.forEach { item ->
                        CompactVideoRow(
                            video = item.toVideoItem(),
                            durationMs = item.durationMs,
                            progressMs = item.progressMs,
                            onClick = { onVideoClick(item.toVideoItem()) },
                            onMoreClick = { onAddToPlaylistClick(item.toVideoItem()) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                CompactSectionTitle(title = "History", onSeeAllClick = onSeeAllHistory)
                if (history.isEmpty()) {
                    CompactEmptyState(icon = Icons.Default.History, message = "No watch history yet.")
                } else {
                    history.take(4).forEach { item ->
                        CompactVideoRow(
                            video = item.toVideoItem(),
                            durationMs = item.durationMs,
                            onClick = { onVideoClick(item.toVideoItem()) },
                            onMoreClick = { onAddToPlaylistClick(item.toVideoItem()) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CompactSectionTitle(title = "Subscriptions", onSeeAllClick = onSeeAllSubscriptions)
                if (subscriptions.isEmpty()) {
                    CompactEmptyState(
                        icon = Icons.Default.PeopleOutline,
                        message = "No subscriptions yet.",
                        actionText = "Explore creators",
                        onActionClick = onExploreClick
                    )
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(subscriptions.take(15)) { sub ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onChannelClick(sub.channelId) }
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = sub.thumbnailUrl,
                                    contentDescription = sub.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                var showCreateDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Playlist", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (showCreateDialog) {
                    var name by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showCreateDialog = false },
                        title = { Text("Create New Playlist") },
                        text = {
                            TextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Playlist name") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (name.isNotBlank()) onCreateLocalPlaylist(name)
                                showCreateDialog = false
                            }) { Text("Create") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (playlists.isEmpty() && localPlaylists.isEmpty()) {
                    CompactEmptyState(
                        icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                        message = "No playlists created yet.",
                        actionText = "Create Playlist",
                        onActionClick = { showCreateDialog = true }
                    )
                } else {
                    localPlaylists.take(3).forEach { playlist ->
                        CompactPlaylistRow(
                            title = playlist.name,
                            videoCount = 0, // Local playlist doesn't eagerly load video count
                            thumbnailUrl = playlist.thumbnailUrl,
                            onClick = { onPlaylistClick("local:${playlist.id}") }
                        )
                    }
                    playlists.take(3).forEach { playlist ->
                        CompactPlaylistRow(
                            title = playlist.title,
                            videoCount = 0, // Remote playlist entity doesn't store count locally
                            thumbnailUrl = playlist.thumbnailUrl,
                            onClick = { onPlaylistClick(playlist.playlistId) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CompactSectionTitle(title = "Downloads", onSeeAllClick = onSeeAllDownloads)
                if (downloads.isEmpty()) {
                    CompactEmptyState(icon = Icons.Default.Download, message = "No offline downloads yet.")
                } else {
                    downloads.take(4).forEach { item ->
                        val subtitle = if (item.status == com.rahul.vibetube1.data.local.DownloadStatus.COMPLETED) "Downloaded" else "Downloading..."
                        CompactVideoRow(
                            video = item.toVideoItem(),
                            durationMs = 0L,
                            subtitleOverride = subtitle,
                            onClick = { onVideoClick(item.toVideoItem()) },
                            onMoreClick = { onDeleteDownload(item.videoId) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                CompactSectionTitle(title = "Liked Videos", onSeeAllClick = null)
                if (favorites.isEmpty()) {
                    CompactEmptyState(icon = Icons.Default.FavoriteBorder, message = "No liked videos yet.")
                } else {
                    favorites.take(4).forEach { item ->
                        CompactVideoRow(
                            video = item.toVideoItem(),
                            durationMs = 0L,
                            onClick = { onVideoClick(item.toVideoItem()) },
                            onMoreClick = { onRemoveFavorite(item) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CompactProfileHeader(
    downloadCount: Int,
    subscriptionCount: Int,
    favoriteCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "ZyvoTube Member",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Personal Hub",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$downloadCount Downloads",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "   |   ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "$subscriptionCount Subscribed",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "   |   ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "$favoriteCount Liked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAccessRow(
    onHistoryClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onLikedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAccessTile(
            icon = Icons.Default.History,
            label = "History",
            onClick = onHistoryClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessTile(
            icon = Icons.Default.Download,
            label = "Downloads",
            onClick = onDownloadsClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessTile(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            label = "Playlists",
            onClick = onPlaylistsClick,
            modifier = Modifier.weight(1f)
        )
        QuickAccessTile(
            icon = Icons.Default.FavoriteBorder,
            label = "Liked",
            onClick = onLikedClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAccessTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "quick_access_scale"
    )

    // Light theme: near-white with a very subtle red tint
    // Dark theme: subtle dark charcoal surface, slightly lighter than page background
    val tileBgColor = when {
        isDark && isPressed -> Color(0xFF29272A)
        isDark -> Color(0xFF1E1D1F)
        !isDark && isPressed -> Color(0xFFF4E4E4)
        else -> Color(0xFFFAF3F3)
    }

    val tileBorderColor = when {
        isDark && isPressed -> Color(0xFF3C383D)
        isDark -> Color(0xFF2C2A2D)
        !isDark && isPressed -> Color(0xFFE4C5C5)
        else -> Color(0xFFF0DFDF)
    }

    val textColor = if (isDark) Color(0xFFEDEDF0) else Color(0xFF1F1D1D)

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(76.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(10.dp),
        color = tileBgColor,
        border = BorderStroke(0.8.dp, tileBorderColor),
        interactionSource = interactionSource,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactSectionTitle(
    title: String,
    onSeeAllClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (onSeeAllClick != null) {
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onSeeAllClick)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun CompactEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onActionClick)
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun CompactVideoRow(
    video: VideoItem,
    durationMs: Long,
    progressMs: Long = 0,
    subtitleOverride: String? = null,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(145.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            if (durationMs > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(durationMs / 1000),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
            
            if (progressMs > 0 && durationMs > 0) {
                val progress = (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (subtitleOverride != null) {
                Text(
                    text = subtitleOverride,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = video.uploaderName ?: "Unknown Channel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val views = if (video.viewCount > 0) "${VideoUtils.formatViewCount(video.viewCount)} views" else null
                val date = video.uploadDate?.takeIf { it.isNotBlank() }
                val viewDateText = listOfNotNull(views, date).joinToString(" • ")
                
                if (viewDateText.isNotEmpty()) {
                    Text(
                        text = viewDateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        
        IconButton(onClick = onMoreClick, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CompactPlaylistRow(
    title: String,
    videoCount: Int,
    thumbnailUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(145.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    val countText = if (videoCount > 0) "$videoCount" else "-"
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            val subtitle = if (videoCount > 0) "Playlist • $videoCount videos" else "Playlist"
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
