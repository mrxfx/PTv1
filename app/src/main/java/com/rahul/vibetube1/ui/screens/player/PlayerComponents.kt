/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.rahul.vibetube1.ui.theme.VibeTubeRed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.media3.common.util.UnstableApi
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.ui.components.VideoItemRow
import com.rahul.vibetube1.ui.components.ThumbnailImage
import com.rahul.vibetube1.domain.model.CommentItem
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.rahul.vibetube1.utils.VideoUtils
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.platform.testTag
import com.rahul.vibetube1.domain.model.PlaylistDetails

@UnstableApi
@Composable
fun UnifiedMetadataHub(
    title: String,
    viewCount: Long,
    uploadDate: String?,
    description: String?,
    uploaderName: String,
    uploaderThumbnailUrl: String?,
    uploaderUrl: String?,
    subscriberCount: Long?,
    isSubscribed: Boolean,
    isFavorite: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    comments: List<CommentItem>,
    commentCount: Int?,
    onToggleSubscription: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCommentsClick: () -> Unit,
    onDescriptionClick: () -> Unit = {},
    onAskClick: () -> Unit = {},
    onMoreClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 1. Title & Micro-stats
        VideoHeaderSection(
            title = title,
            viewCount = viewCount,
            uploadDate = uploadDate,
            onDescriptionClick = onDescriptionClick
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Channel Section
        ChannelInfoSection(
            uploaderName = uploaderName,
            uploaderThumbnailUrl = uploaderThumbnailUrl,
            uploaderUrl = uploaderUrl,
            subscriberCount = subscriberCount,
            isSubscribed = isSubscribed,
            onToggleSubscription = onToggleSubscription,
            onChannelClick = onChannelClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Action Row
        PlayerActionRow(
            isFavorite = isFavorite,
            isSaved = isSaved,
            isDownloaded = isDownloaded,
            onToggleFavorite = onToggleFavorite,
            onSaveClick = onSaveClick,
            onDownloadClick = onDownloadClick,
            onShareClick = onShareClick,
            onAskClick = onAskClick,
            onMoreClick = onMoreClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(10.dp))

        // 4. Comments Preview
        CommentsPreviewCard(
            comments = comments,
            totalCount = commentCount,
            onClick = onCommentsClick,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 0.5.dp, 
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
    }
}

@UnstableApi
@Composable
fun VerticalGestureHUD(
    visible: Boolean,
    progress: Float,
    icon: ImageVector,
    isRightSide: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200)),
        modifier = modifier
            .fillMaxHeight(0.32f)
            .width(48.dp)
            .padding(horizontal = 10.dp)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(4.dp)
                        .padding(vertical = 12.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress.coerceIn(0f, 1f))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White,
                                        Color.White.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun VideoHeaderSection(
    title: String,
    viewCount: Long,
    uploadDate: String?,
    onDescriptionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = onDescriptionClick != null,
                onClick = { onDescriptionClick?.invoke() }
            )
            .padding(vertical = 4.dp, horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${VideoUtils.formatViewCount(viewCount)} views",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "•",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = VideoUtils.formatUploadDate(uploadDate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@UnstableApi
@Composable
fun ChannelInfoSection(
    uploaderName: String,
    uploaderThumbnailUrl: String?,
    uploaderUrl: String?,
    subscriberCount: Long?,
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { uploaderUrl?.let { onChannelClick(it) } }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = uploaderThumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uploaderName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subscriberCount != null && subscriberCount > 0) {
                Text(
                    text = "${VideoUtils.formatNumber(subscriberCount)} subscribers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        Button(
            onClick = onToggleSubscription,
            colors = if (isSubscribed) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            },
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@UnstableApi
@Composable
fun PlayerActionRow(
    isFavorite: Boolean,
    isSaved: Boolean = false,
    isDownloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onSaveClick: () -> Unit = {},
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit = {},
    onAskClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Like Pill
        PlayerActionPill(
            icon = if (isFavorite) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            label = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
            active = isFavorite,
            onClick = {
                onToggleFavorite()
            }
        )

        // 2. Download Pill
        PlayerActionPill(
            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
            label = if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download),
            active = isDownloaded,
            onClick = onDownloadClick
        )
        
        // 3. Save Pill
        PlayerActionPill(
            icon = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd,
            label = if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save),
            active = isSaved,
            onClick = onSaveClick
        )

        // 4. Ask Pill (Sparkle icon)
        PlayerActionPill(
            icon = Icons.Default.AutoAwesome,
            label = stringResource(R.string.ask),
            active = false,
            onClick = onAskClick
        )

        // 5. More Pill
        PlayerActionPill(
            icon = Icons.Default.MoreHoriz,
            label = stringResource(R.string.more),
            active = false,
            onClick = onMoreClick
        )
    }
}

@UnstableApi
@Composable
fun PlayerActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme() ||
            (MaterialTheme.colorScheme.surface.red +
                    MaterialTheme.colorScheme.surface.green +
                    MaterialTheme.colorScheme.surface.blue) / 3f < 0.5f

    // Light Theme: very subtle neutral background #F5F5F5
    // Dark Theme: slightly lighter surface than page background #1F1F1F
    val neutralBg = if (isDark) Color(0xFF1F1F1F) else Color(0xFFF5F5F5)

    // Active state: subtle VibeTube red tint
    val activeBg = if (isDark) {
        VibeTubeRed.copy(alpha = 0.20f)
    } else {
        VibeTubeRed.copy(alpha = 0.10f)
    }

    // Light Theme: keep icons and labels dark
    // Dark Theme: keep text/icons readable
    val neutralContentColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val activeContentColor = VibeTubeRed

    val backgroundColor by animateColorAsState(
        targetValue = if (active) activeBg else neutralBg,
        animationSpec = tween(durationMillis = 200),
        label = "PillBackground"
    )

    val contentColor by animateColorAsState(
        targetValue = if (active) activeContentColor else neutralContentColor,
        animationSpec = tween(durationMillis = 200),
        label = "PillContent"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@UnstableApi
fun LazyListScope.relatedVideosSection(
    relatedVideos: List<VideoItem>,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.related_videos),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    items(relatedVideos, key = { it.id }) { relatedVideo ->
        VideoItemRow(
            video = relatedVideo,
            isDownloaded = downloadedIds.contains(relatedVideo.id),
            isFavorite = favoriteIds.contains(relatedVideo.id),
            onFavoriteClick = { onFavoriteClick(relatedVideo) },
            onAddToPlaylistClick = { onAddToPlaylistClick(relatedVideo) },
            onDownloadClick = { onDownloadClick(relatedVideo) },
            onChannelClick = { onChannelClick(relatedVideo.uploaderUrl ?: "") },
            onClick = { onVideoClick(relatedVideo) }
        )
    }
}

@UnstableApi
@Composable
fun PlaylistStack(
    playlist: PlaylistDetails,
    currentIndex: Int,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Visual Stack Effect (Overlapping backgrounds)
        Box(modifier = Modifier.fillMaxWidth()) {
            // Background layers for stack effect
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))
            ) {}
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {}

            // Main Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${playlist.uploaderName} • ${currentIndex + 1} / ${playlist.videos.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isExpanded) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            playlist.videos.forEachIndexed { index, video ->
                                PlaylistVideoRow(
                                    video = video,
                                    isPlaying = index == currentIndex,
                                    index = index + 1,
                                    onClick = { onVideoClick(video) }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@UnstableApi
@Composable
fun PlaylistVideoRow(
    video: VideoItem,
    isPlaying: Boolean,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )
            
            com.rahul.vibetube1.ui.components.ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                modifier = Modifier
                    .size(width = 80.dp, height = 45.dp)
                    .clip(RoundedCornerShape(8.dp)),
                quality = com.rahul.vibetube1.ui.components.ThumbnailQuality.Low,
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InlineDescriptionCard(
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description ?: stringResource(R.string.no_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "See more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ShortCard(
    short: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(135.dp)
            .height(240.dp) // 9:16 vertical ratio
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("watch_short_card_${short.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ThumbnailImage(
                videoId = short.id,
                thumbnailUrl = short.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Short",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (short.duration > 0) {
                    Text(
                        text = VideoUtils.formatDuration(short.duration),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = short.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 14.sp,
                        fontSize = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (short.uploaderName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = short.uploaderName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ShortsCarouselSection(
    relatedShortsState: RelatedShortsState,
    onShortClick: (VideoItem) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (relatedShortsState) {
        is RelatedShortsState.Idle -> return
        is RelatedShortsState.Error -> return
        is RelatedShortsState.Loading -> {
            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shorts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .height(240.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        )
                    }
                }
            }
        }
        is RelatedShortsState.Success -> {
            val shorts = relatedShortsState.shorts
            if (shorts.isEmpty()) return

            val listState = rememberLazyListState()

            LaunchedEffect(listState, shorts.size, relatedShortsState.hasMore, relatedShortsState.isLoadingMore) {
                snapshotFlow {
                    val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisibleItemIndex to listState.layoutInfo.totalItemsCount
                }.collect { (lastVisible, total) ->
                    // Only trigger loadMore when user explicitly scrolls near the horizontal end of the carousel
                    if (total > 0 && lastVisible >= total - 1 && relatedShortsState.hasMore && !relatedShortsState.isLoadingMore) {
                        onLoadMore()
                    }
                }
            }

            Column(modifier = modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shorts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                LazyRow(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watch_shorts_carousel"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(
                        items = shorts,
                        key = { it.id }
                    ) { short ->
                        ShortCard(
                            short = short,
                            onClick = { onShortClick(short) }
                        )
                    }

                    if (relatedShortsState.isLoadingMore) {
                        item(key = "watch_shorts_loading_more") {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(240.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsCarouselSection(
    shorts: List<VideoItem>,
    onShortClick: (VideoItem) -> Unit,
    carouselState: LazyListState = rememberLazyListState(),
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (shorts.isEmpty()) return

    LaunchedEffect(carouselState, shorts.size, hasMore, isLoadingMore) {
        snapshotFlow {
            val lastVisibleItemIndex = carouselState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex to carouselState.layoutInfo.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 1 && hasMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Shorts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LazyRow(
            state = carouselState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("watch_shorts_carousel"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(
                items = shorts,
                key = { it.id }
            ) { short ->
                ShortCard(
                    short = short,
                    onClick = { onShortClick(short) }
                )
            }

            if (isLoadingMore) {
                item(key = "watch_shorts_loading_more") {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
