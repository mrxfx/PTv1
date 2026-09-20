/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.search
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbUpOffAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.ui.components.ThumbnailImage
import com.rahul.vibetube1.ui.components.ThumbnailQuality
import com.rahul.vibetube1.ui.components.WatchProgressBar
import com.rahul.vibetube1.utils.VideoUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats a view count for search result presentation.
 * Returns formatted string (e.g. "1.2M views", "850K views", "56K views", "3,295 views")
 * or null if view count is missing or unavailable (< 0).
 */
fun formatSearchResultViews(views: Long?): String? {
    if (views == null || views < 0L) return null
    val formatted = when {
        views >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", views / 1_000_000_000.0).replace(".0", "")
        views >= 1_000_000 -> String.format(Locale.US, "%.1fM", views / 1_000_000.0).replace(".0", "")
        views >= 10_000 -> String.format(Locale.US, "%.1fK", views / 1_000.0).replace(".0", "")
        views >= 1_000 -> NumberFormat.getNumberInstance(Locale.US).format(views)
        else -> views.toString()
    }
    return if (views == 1L) "$formatted view" else "$formatted views"
}

/**
 * Formats the published date for search result presentation.
 * Prefers clean relative format (e.g. "2 hours ago", "3 days ago") or formatted date (e.g. "18 Aug 2026").
 * Returns null if publication date is unavailable.
 */
fun formatSearchResultPublishedDate(uploadDate: String?, rawUploadDate: Long? = null): String? {
    val trimmed = uploadDate?.trim().orEmpty()
    if (trimmed.isBlank() && (rawUploadDate == null || rawUploadDate <= 0L)) return null

    // If source provided an already human-readable relative/clean string (e.g. "2 hours ago", "3 days ago", "18 Aug 2026")
    if (trimmed.isNotEmpty() && !trimmed.contains("T") && !trimmed.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
        return trimmed
    }

    // Use source timestamp if available
    if (rawUploadDate != null && rawUploadDate > 0L) {
        val now = System.currentTimeMillis()
        val diffMillis = now - rawUploadDate
        if (diffMillis in 0..(30L * 24 * 60 * 60 * 1000)) {
            val relative = android.text.format.DateUtils.getRelativeTimeSpanString(
                rawUploadDate,
                now,
                android.text.format.DateUtils.MINUTE_IN_MILLIS
            ).toString()
            if (relative.isNotBlank()) return relative
        } else {
            return try {
                val instant = Instant.ofEpochMilli(rawUploadDate)
                DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault()).format(instant)
            } catch (e: Exception) {
                trimmed.ifBlank { null }
            }
        }
    }

    // Parse ISO date / timestamp if provided
    if (trimmed.isNotEmpty()) {
        return try {
            val date = if (trimmed.contains("T")) {
                OffsetDateTime.parse(trimmed).toLocalDate()
            } else {
                LocalDate.parse(trimmed)
            }
            DateTimeFormatter.ofPattern("d MMM yyyy").format(date)
        } catch (e: Exception) {
            trimmed.ifBlank { null }
        }
    }

    return null
}

/**
 * Combines views and published date into clean secondary metadata string:
 * "1.2M views • 18 Aug 2026", or single item if only one is available, or null if neither.
 */
fun formatSearchResultSecondaryMetadata(views: Long?, uploadDate: String?, rawUploadDate: Long? = null): String? {
    val viewsText = formatSearchResultViews(views)
    val dateText = formatSearchResultPublishedDate(uploadDate, rawUploadDate)
    return when {
        viewsText != null && dateText != null -> "$viewsText • $dateText"
        viewsText != null -> viewsText
        dateText != null -> dateText
        else -> null
    }
}

/**
 * Circular channel avatar for search result items.
 * Uses existing Coil cache architecture, gracefully falls back to a clean VibeTube
 * channel initial placeholder if the URL is unavailable or loading fails.
 */
@Composable
fun SearchChannelAvatar(
    avatarUrl: String?,
    channelName: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    onClick: (() -> Unit)? = null
) {
    var isError by remember(avatarUrl) { mutableStateOf(false) }
    val showPlaceholder = avatarUrl.isNullOrBlank() || isError

    Surface(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank() && !isError) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = channelName.ifBlank { "Channel Avatar" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = { isError = true }
                )
            }

            if (showPlaceholder) {
                val initial = channelName.trim().take(1).uppercase()
                if (initial.isNotEmpty() && initial.first().isLetterOrDigit()) {
                    Text(
                        text = initial,
                        style = if (size <= 20.dp) MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                               else MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = channelName.ifBlank { "Channel Avatar" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxSize(0.8f)
                    )
                }
            }
        }
    }
}

/**
 * Search result row in horizontal list mode (VibeTube default search layout):
 * [Thumbnail]   Video Title
 *               [Channel Avatar] Channel Name
 *               1.2M views • 18 Aug 2026
 */
@Composable
fun SearchResultVideoRow(
    video: VideoItem,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val secondaryMetadata = remember(video.viewCount, video.uploadDate, video.rawUploadDate) {
        formatSearchResultSecondaryMetadata(video.viewCount, video.uploadDate, video.rawUploadDate)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(140.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                quality = ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Badge
            if (video.duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Downloaded Tag
            if (isDownloaded) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.downloaded),
                        modifier = Modifier.padding(4.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Watch Progress Bar
            video.watchProgress?.let { progress ->
                if (progress > 0.001f) {
                    WatchProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (onFavoriteClick != null || onDownloadClick != null || onAddToPlaylistClick != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(24.dp)
                                .offset(x = 6.dp, y = (-2).dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        SearchResultOverflowMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            isDownloaded = isDownloaded,
                            isFavorite = isFavorite,
                            isSaved = isSaved,
                            onDownloadClick = onDownloadClick,
                            onFavoriteClick = onFavoriteClick,
                            onAddToPlaylistClick = onAddToPlaylistClick
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Channel Avatar + Channel Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(
                        if (onChannelClick != null) Modifier.clickable(onClick = onChannelClick)
                        else Modifier
                    )
            ) {
                SearchChannelAvatar(
                    avatarUrl = video.uploaderThumbnailUrl,
                    channelName = video.uploaderName,
                    size = 18.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Views • Published Date
            if (secondaryMetadata != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = secondaryMetadata,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Search result card in vertical grid/feed mode:
 * [Thumbnail]
 * Video Title
 * [Channel Avatar] Channel Name
 * Views • Published Date
 */
@Composable
fun SearchResultVideoCard(
    video: VideoItem,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "SearchCardScale")

    val secondaryMetadata = remember(video.viewCount, video.uploadDate, video.rawUploadDate) {
        formatSearchResultSecondaryMetadata(video.viewCount, video.uploadDate, video.rawUploadDate)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                quality = ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )

            // Duration Badge
            if (video.duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Downloaded Tag
            if (isDownloaded) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.downloaded),
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Watch Progress Bar
            video.watchProgress?.let { progress ->
                if (progress > 0.001f) {
                    WatchProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title + More Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onFavoriteClick != null || onDownloadClick != null || onAddToPlaylistClick != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = 8.dp, y = (-2).dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    SearchResultOverflowMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        isDownloaded = isDownloaded,
                        isFavorite = isFavorite,
                        isSaved = isSaved,
                        onDownloadClick = onDownloadClick,
                        onFavoriteClick = onFavoriteClick,
                        onAddToPlaylistClick = onAddToPlaylistClick
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Channel Avatar + Channel Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(
                    if (onChannelClick != null) Modifier.clickable(onClick = onChannelClick)
                    else Modifier
                )
        ) {
            SearchChannelAvatar(
                avatarUrl = video.uploaderThumbnailUrl,
                channelName = video.uploaderName,
                size = 22.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = video.uploaderName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Views • Published Date
        if (secondaryMetadata != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = secondaryMetadata,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchResultOverflowMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isDownloaded: Boolean,
    isFavorite: Boolean,
    isSaved: Boolean,
    onDownloadClick: (() -> Unit)?,
    onFavoriteClick: (() -> Unit)?,
    onAddToPlaylistClick: (() -> Unit)?
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        if (onDownloadClick != null) {
            DropdownMenuItem(
                text = { Text(if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = null,
                        tint = if (isDownloaded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                },
                onClick = {
                    onDismissRequest()
                    if (!isDownloaded) onDownloadClick()
                },
                enabled = !isDownloaded
            )
        }
        if (onFavoriteClick != null) {
            DropdownMenuItem(
                text = { Text(if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                        contentDescription = null,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                },
                onClick = {
                    onDismissRequest()
                    onFavoriteClick()
                }
            )
        }
        if (onAddToPlaylistClick != null) {
            DropdownMenuItem(
                text = { Text(if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save)) },
                leadingIcon = {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    )
                },
                onClick = {
                    onDismissRequest()
                    onAddToPlaylistClick()
                }
            )
        }
    }
}
