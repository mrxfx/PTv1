/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.library
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.utils.rememberScrollVisibilityConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val completedDownloads by viewModel.filteredCompleted.collectAsStateWithLifecycle()
    val allCompletedDownloads by viewModel.completedDownloads.collectAsStateWithLifecycle()
    val downloadQueue by viewModel.filteredQueue.collectAsStateWithLifecycle()
    val allQueueItems by viewModel.downloadQueue.collectAsStateWithLifecycle()
    val savedVideoIds by viewModel.savedVideoIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.offlineSearchQuery.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var videoIdToDelete by remember { mutableStateOf<String?>(null) }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCleanupConfirm by remember { mutableStateOf(false) }
    var showClearQueueConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    if (showCleanupConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirm = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.smart_cleanup_title)) },
            text = { Text(stringResource(R.string.smart_cleanup_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearWatchedDownloads()
                        showCleanupConfirm = false
                    }
                ) {
                    Text(stringResource(R.string.clean_now))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearQueueConfirm) {
        AlertDialog(
            onDismissRequest = { showClearQueueConfirm = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear Download Queue?") },
            text = { Text("This will cancel and remove all pending and in-progress downloads from the queue.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDownloadQueue()
                        showClearQueueConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearQueueConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (videoIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoIdToDelete = null },
            title = { Text(stringResource(R.string.delete_download_title)) },
            text = { Text(stringResource(R.string.delete_download_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoIdToDelete?.let { viewModel.deleteDownload(it) }
                        videoIdToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                tonalElevation = 0.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.onOfflineSearchQueryChange(it) },
                                    placeholder = { Text(stringResource(R.string.search_offline)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.downloads), 
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    viewModel.onOfflineSearchQueryChange("")
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(22.dp))
                                }
                                IconButton(onClick = { showCleanupConfirm = true }) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean Watched", modifier = Modifier.size(22.dp))
                                }
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onOfflineSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Compact Storage Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Box(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { 0.4f }, 
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = storageUsage.usedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Queue & Downloaded Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "Downloaded (${allCompletedDownloads.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Queue",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (allQueueItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text(
                                                text = "${allQueueItems.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (selectedTab == 1) {
            // Background Download Queue Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 100.dp
                )
            ) {
                item {
                    DownloadQueueHeader(
                        queue = allQueueItems,
                        onPauseAll = { viewModel.pauseAllDownloads() },
                        onResumeAll = { viewModel.resumeAllDownloads() },
                        onClearQueue = { showClearQueueConfirm = true }
                    )
                }

                if (downloadQueue.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptySectionPlaceholder(
                                if (searchQuery.isNotEmpty()) "No queued downloads match \"$searchQuery\""
                                else "Download queue is empty"
                            )
                        }
                    }
                } else {
                    items(downloadQueue, key = { it.videoId }) { download ->
                        DownloadQueueItemCard(
                            download = download,
                            onPause = { viewModel.pauseDownload(download.videoId) },
                            onResume = { viewModel.resumeDownload(download.videoId) },
                            onDelete = { videoIdToDelete = download.videoId }
                        )
                    }
                }
            }
        } else {
            // Completed Downloads Tab
            if (completedDownloads.isEmpty() && searchQuery.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (allQueueItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .clickable { selectedTab = 1 },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Downloading, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${allQueueItems.size} item(s) in download queue",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Tap to manage background downloads",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptySectionPlaceholder(stringResource(R.string.no_downloads))
                    }
                }
            } else if (completedDownloads.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    EmptySectionPlaceholder("No results found for \"$searchQuery\"")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding(),
                        bottom = 100.dp
                    )
                ) {
                    if (allQueueItems.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { selectedTab = 1 },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Downloading, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${allQueueItems.size} download(s) active in background",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "Tap to view and manage download queue",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    val groupedDownloads = completedDownloads.groupBy { it.playlistId }
                    val singleVideos = groupedDownloads[null] ?: emptyList()
                    val playlistsGroup = groupedDownloads.filterKeys { it != null }

                    playlistsGroup.forEach { (playlistId, playlistVideos) ->
                        item {
                            val title = playlistVideos.firstOrNull()?.playlistTitle ?: "Playlist"
                            val isExpanded = expandedPlaylistId == playlistId
                            
                            PlaylistDownloadRow(
                                title = title,
                                videoCount = playlistVideos.size,
                                thumbnailUrl = playlistVideos.firstOrNull()?.thumbnailUrl ?: "",
                                isExpanded = isExpanded,
                                onClick = {
                                    expandedPlaylistId = if (isExpanded) null else playlistId
                                }
                            )
                        }
                        
                        if (expandedPlaylistId == playlistId) {
                            items(playlistVideos) { download ->
                                DownloadItemRow(
                                    download = download,
                                    isSaved = savedVideoIds.contains(download.videoId),
                                    onClick = { onVideoClick(download.toVideoItem()) },
                                    onDeleteClick = { videoIdToDelete = download.videoId },
                                    onCancelClick = { viewModel.cancelDownload(download.videoId) },
                                    onPauseClick = { viewModel.pauseDownload(download.videoId) },
                                    onResumeClick = { viewModel.resumeDownload(download.videoId) },
                                    onSaveToDeviceClick = { viewModel.saveToPublicStorage(download.videoId) },
                                    onAddToPlaylistClick = { onAddToPlaylistClick(download.toVideoItem()) },
                                    modifier = Modifier.padding(start = 24.dp)
                                )
                            }
                        }
                    }

                    items(singleVideos) { download ->
                        DownloadItemRow(
                            download = download,
                            isSaved = savedVideoIds.contains(download.videoId),
                            onClick = { onVideoClick(download.toVideoItem()) },
                            onDeleteClick = { videoIdToDelete = download.videoId },
                            onCancelClick = { viewModel.cancelDownload(download.videoId) },
                            onPauseClick = { viewModel.pauseDownload(download.videoId) },
                            onResumeClick = { viewModel.resumeDownload(download.videoId) },
                            onSaveToDeviceClick = { viewModel.saveToPublicStorage(download.videoId) },
                            onAddToPlaylistClick = { onAddToPlaylistClick(download.toVideoItem()) }
                        )
                    }
                }
            }
        }
    }
}
