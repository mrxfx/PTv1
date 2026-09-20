/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahul.vibetube1.domain.model.VideoItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissVideoItem(
    video: VideoItem,
    onDismiss: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDismiss(video)
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipe_dismiss_${video.id}"),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isSwiping = dismissState.targetValue != SwipeToDismissBoxValue.Settled

            val backgroundColor by animateColorAsState(
                targetValue = if (isSwiping) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                },
                animationSpec = tween(200),
                label = "DismissBackground"
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSwiping) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(200),
                label = "DismissContent"
            )

            val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                Alignment.CenterStart
            } else {
                Alignment.CenterEnd
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Not Interested",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Not Interested",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    } else {
                        Text(
                            text = "Hide Video",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hide Video",
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        content = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                content()
            }
        }
    )
}
