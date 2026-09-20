package com.rahul.vibetube1.ui.screens.player
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.utils.VideoUtils
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

import androidx.compose.ui.res.stringResource

@Composable
fun FullscreenPlayerControlsOverlay(
    title: String,
    channelName: String,
    isPlaying: Boolean,
    currentPosition: () -> Long,
    duration: () -> Long,
    isLive: Boolean,
    isCcEnabled: Boolean,
    hasSubtitles: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleSubtitles: () -> Unit,
    onShowSettings: () -> Unit,
    onBack: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    
    // Bottom actions
    isFavorite: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onSaveClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAskClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    
    // More videos
    relatedVideos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    isMoreVideosOpen: Boolean = false,
    currentVideoId: String = "",
    onOpenMoreVideos: () -> Unit = {},
    onCloseMoreVideos: () -> Unit = {},
    
    // Progress bar passed from parent
    progressBar: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    if (isMoreVideosOpen) {
        MoreVideosFullscreenOverlay(
            title = title,
            channelName = channelName,
            currentVideoId = currentVideoId,
            relatedVideos = relatedVideos,
            onClose = onCloseMoreVideos,
            onVideoClick = { video ->
                // Keep overlay open; update playback of the selected video
                onVideoClick(video)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        // TOP BAR
        // ← Back     Video Title                 ⛶   CC   ⚙
        //            Channel Name
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (channelName.isNotBlank()) {
                    Text(
                        text = channelName,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // TOP ⛶: Aspect Ratio / Screen Presentation Mode Button
            IconButton(
                onClick = onCycleAspectRatio,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AspectRatio,
                    contentDescription = stringResource(R.string.aspect_ratio_mode),
                    tint = Color.White
                )
            }

            // CC: Subtitles / Captions Button
            if (hasSubtitles) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onToggleSubtitles,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClosedCaption,
                        contentDescription = stringResource(R.string.toggle_subtitles),
                        tint = if (isCcEnabled) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ⚙: Settings Button
            IconButton(
                onClick = onShowSettings,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = Color.White
                )
            }
        }

        // CENTER CONTROLS
        // [ Prev ]      [ Play / Pause ]      [ Next ]
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipPrevious()
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipNext()
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // BOTTOM CONTROLS AREA
        // -------------------------------------------------------------------
        // [ Current Time / Duration ]                     [ ⛶ Exit Fullscreen ]
        // ───●───────────────────────────────────────────────────────────────
        // [ Like ] [ Download ] [ Save ] [ Ask ] [ Share ] [ More ]  [More Videos]
        // -------------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            // Time display & Bottom ⛶ (Exit Fullscreen)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${VideoUtils.formatDuration(currentPosition() / 1000)} / ${VideoUtils.formatDuration(duration() / 1000)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )

                // BOTTOM ⛶: Exit Fullscreen Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = stringResource(R.string.exit_fullscreen),
                        tint = Color.White
                    )
                }
            }

            // Progress Bar (Seekbar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                progressBar()
            }

            // Fullscreen Action Row & More Videos Entry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Actions (Like, Download, Save, Ask, Share, More)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FullscreenActionIcon(
                        icon = if (isFavorite) Icons.Default.ThumbUp else Icons.Outlined.ThumbUp,
                        label = stringResource(if (isFavorite) R.string.liked else R.string.like),
                        active = isFavorite,
                        onClick = onToggleFavorite
                    )
                    FullscreenActionIcon(
                        icon = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                        label = stringResource(if (isDownloaded) R.string.downloaded else R.string.download),
                        active = isDownloaded,
                        onClick = onDownloadClick
                    )
                    FullscreenActionIcon(
                        icon = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd,
                        label = stringResource(if (isSaved) R.string.saved else R.string.save),
                        active = isSaved,
                        onClick = onSaveClick
                    )
                    FullscreenActionIcon(
                        icon = Icons.Default.AutoAwesome,
                        label = stringResource(R.string.ask),
                        active = false,
                        onClick = onAskClick
                    )
                    FullscreenActionIcon(
                        icon = Icons.Default.Share,
                        label = stringResource(R.string.share),
                        active = false,
                        onClick = onShareClick
                    )
                    FullscreenActionIcon(
                        icon = Icons.Default.MoreHoriz,
                        label = stringResource(R.string.more),
                        active = false,
                        onClick = onMoreClick
                    )
                }

                // More Videos Entry / Mini Preview
                if (relatedVideos.isNotEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onOpenMoreVideos() }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.more_videos),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        AsyncImage(
                            model = relatedVideos.first().thumbnailUrl,
                            contentDescription = stringResource(R.string.more_videos),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 120.dp, height = 68.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenActionIcon(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.primary else Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun MoreVideosFullscreenOverlay(
    title: String,
    channelName: String,
    currentVideoId: String = "",
    relatedVideos: List<VideoItem>,
    onClose: () -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Intercept clicks on empty space so underlying video player gestures are not triggered
            )
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channelName,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Grid of related videos
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(relatedVideos) { video ->
                    val isPlayingThisVideo = currentVideoId.isNotBlank() && video.id == currentVideoId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVideoClick(video) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (isPlayingThisVideo) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                        ) {
                            AsyncImage(
                                model = video.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isPlayingThisVideo) {
                                Text(
                                    text = "PLAYING",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            if (video.duration > 0) {
                                Text(
                                    text = VideoUtils.formatDuration(video.duration),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = video.title,
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = video.uploaderName,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
