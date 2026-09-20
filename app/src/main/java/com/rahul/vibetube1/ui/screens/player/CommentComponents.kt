/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rahul.vibetube1.domain.model.CommentItem
import com.rahul.vibetube1.ui.components.InfiniteScrollEffect
import com.rahul.vibetube1.ui.theme.VibeTubeRed
import com.rahul.vibetube1.utils.VideoUtils

@Composable
fun CommentItemRow(
    comment: CommentItem,
    onRepliesClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = comment.authorThumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                comment.publishedTime?.let { time ->
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = comment.commentText,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 17.sp,
                    letterSpacing = 0.15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like count as display-only
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
                if (comment.likeCount > 0) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = VideoUtils.formatNumber(comment.likeCount.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }

                if (comment.isHeartedByUploader) {
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Hearted by uploader",
                        modifier = Modifier.size(11.dp),
                        tint = VibeTubeRed
                    )
                }

                if (comment.replyCount > 0) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "${comment.replyCount} ${if (comment.replyCount == 1) "reply" else "replies"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VibeTubeRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(enabled = onRepliesClick != null) {
                                onRepliesClick?.invoke()
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyItemRow(
    reply: CommentItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Small avatar (22dp)
        AsyncImage(
            model = reply.authorThumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Username and Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.authorName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                reply.publishedTime?.let { time ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Reply text
            Text(
                text = reply.commentText,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp,
                    letterSpacing = 0.15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Like count as display-only
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                if (reply.likeCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = VideoUtils.formatNumber(reply.likeCount.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (reply.isHeartedByUploader) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Hearted by uploader",
                        modifier = Modifier.size(11.dp),
                        tint = VibeTubeRed
                    )
                }
            }
        }
    }
}

@Composable
fun ParentCommentCompactCard(
    parent: CommentItem,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = parent.authorThumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = parent.authorName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    parent.publishedTime?.let { time ->
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = parent.commentText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CommentsPreviewCard(
    comments: List<CommentItem>,
    totalCount: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Comments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (totalCount != null && totalCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = VideoUtils.formatNumber(totalCount.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (comments.isNotEmpty()) {
                val topComment = comments.first()
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = topComment.authorThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = topComment.commentText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No comments yet. Tap to view.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun CommentsSheetContent(
    comments: List<CommentItem>,
    isFetching: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    onRepliesClick: (CommentItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
    ) {
        // Minimal Professional Header: "Comments" with Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Comments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (comments.isEmpty() && isFetching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.5.dp,
                        color = VibeTubeRed
                    )
                }
            } else if (comments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
                ) {
                    items(comments, key = { it.commentId }) { comment ->
                        CommentItemRow(
                            comment = comment,
                            onRepliesClick = { onRepliesClick(comment) }
                        )
                    }

                    if (isFetching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = VibeTubeRed
                                )
                            }
                        }
                    }
                }

                InfiniteScrollEffect(listState = listState, buffer = 5) {
                    onLoadMore()
                }
            }
        }
    }
}

@Composable
fun RepliesSheetContent(
    parentComment: CommentItem,
    replies: List<CommentItem>,
    isFetching: Boolean,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val repliesListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
    ) {
        // Header: "Replies" with Back arrow & Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to comments",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Replies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )

        // Show parent comment compactly
        ParentCommentCompactCard(
            parent = parentComment,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )

        // Clean scrolling list of replies
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (replies.isEmpty() && isFetching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = VibeTubeRed
                    )
                }
            } else if (replies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No replies found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = repliesListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp)
                ) {
                    items(replies, key = { it.commentId }) { reply ->
                        ReplyItemRow(reply = reply)
                    }

                    if (isFetching) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = VibeTubeRed
                                )
                            }
                        }
                    }
                }

                InfiniteScrollEffect(listState = repliesListState, buffer = 4) {
                    onLoadMore()
                }
            }
        }
    }
}

/**
 * Dedicated Material 3 ModalBottomSheet for Replies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepliesSheet(
    parentComment: CommentItem,
    replies: List<CommentItem>,
    isFetching: Boolean,
    onLoadMore: () -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.isDark()
    val sheetBackground = if (isDark) Color(0xFF0F0F0F) else Color.White
    val sheetContentColor = if (isDark) Color.White else Color(0xFF1E293B)

    ModalBottomSheet(
        onDismissRequest = onBack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBackground,
        contentColor = sheetContentColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.35f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier
    ) {
        RepliesSheetContent(
            parentComment = parentComment,
            replies = replies,
            isFetching = isFetching,
            onLoadMore = onLoadMore,
            onBack = onBack,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun CommentsSheet(
    comments: List<CommentItem>,
    isFetching: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    activeReplyParent: CommentItem? = null,
    replies: List<CommentItem> = emptyList(),
    isFetchingReplies: Boolean = false,
    onRepliesClick: (CommentItem) -> Unit = {},
    onLoadMoreReplies: () -> Unit = {},
    onCloseReplies: () -> Unit = {}
) {
    if (activeReplyParent != null) {
        RepliesSheetContent(
            parentComment = activeReplyParent,
            replies = replies,
            isFetching = isFetchingReplies,
            onLoadMore = onLoadMoreReplies,
            onBack = onCloseReplies,
            onDismiss = onDismiss
        )
    } else {
        CommentsSheetContent(
            comments = comments,
            isFetching = isFetching,
            onLoadMore = onLoadMore,
            onDismiss = onDismiss,
            onRepliesClick = onRepliesClick
        )
    }
}

private fun Color.isDark(): Boolean {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return luminance < 0.5f
}
