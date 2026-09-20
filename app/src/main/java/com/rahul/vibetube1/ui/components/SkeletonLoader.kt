/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Shared infinite transition for all shimmer effects to ensure synchronization
 * and reduce CPU overhead from multiple independent animation timers.
 */
@Composable
fun rememberSyncShimmerTransition(): InfiniteTransition {
    return rememberInfiniteTransition(label = "shimmerSync")
}

fun Modifier.shimmerEffect(
    transition: InfiniteTransition? = null,
    baseColor: Color? = null,
    highlightColor: Color? = null
): Modifier = composed {
    // Use provided transition or fallback to a local one
    val actualTransition = transition ?: rememberInfiniteTransition(label = "shimmerLocal")
    
    val progress by actualTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = LinearEasing)
        ),
        label = "shimmerTranslate"
    )

    val actualBaseColor = baseColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val actualHighlightColor = highlightColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    val shimmerColors = listOf(
        Color.Transparent,
        actualHighlightColor,
        Color.Transparent,
    )

    this.drawBehind {
        val width = this.size.width
        val height = this.size.height
        
        // Draw the theme-aware base container background first so the skeleton element is crisp and defined
        drawRect(color = actualBaseColor)

        // Calculate offset based on progress: from -2*width to 2*width
        val startOffsetX = (progress * 4 * width) - (2 * width)
        
        drawRect(
            brush = Brush.linearGradient(
                colors = shimmerColors,
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + width, height)
            )
        )
    }
}

@Composable
fun VideoCardSkeleton(transition: InfiniteTransition? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .shimmerEffect(transition)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .shimmerEffect(transition)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(transition)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Metadata placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(transition)
                )
            }
        }
    }
}

@Composable
fun VideoListSkeleton() {
    val transition = rememberSyncShimmerTransition()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(5) {
            VideoCardSkeleton(transition)
        }
    }
}

@Composable
fun SubscriptionFeedSkeleton() {
    val transition = rememberSyncShimmerTransition()
    Column(modifier = Modifier.fillMaxSize()) {
        // Redesigned Channel bubbles skeleton (More fluid, no divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(6) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .shimmerEffect(transition)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(transition)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Video list skeleton
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(3) {
                VideoCardSkeleton(transition)
            }
        }
    }
}

@Composable
fun PlayerMetadataSkeleton(transition: InfiniteTransition? = null) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Title
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(actualTransition)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(actualTransition)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Micro-stats row
        Row {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Channel Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .shimmerEffect(actualTransition)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
            }
            
            // Subscribe Button Placeholder
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(36.dp)
                    .clip(CircleShape)
                    .shimmerEffect(actualTransition)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Action Row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .shimmerEffect(actualTransition)
                )
            }
        }
    }
}

/**
 * Channel banner skeleton matching the 16:5.5 aspect ratio of the real channel banner.
 */
@Composable
fun ChannelBannerSkeleton(
    transition: InfiniteTransition? = null,
    modifier: Modifier = Modifier
) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 5.5f)
            .shimmerEffect(actualTransition)
            .testTag("channel_banner_skeleton")
    )
}

/**
 * Channel identity skeleton matching the 68dp avatar, channel name, handle,
 * subscriber & video count, description lines, and full-width pill subscribe button.
 */
@Composable
fun ChannelIdentitySkeleton(
    transition: InfiniteTransition? = null,
    modifier: Modifier = Modifier
) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("channel_identity_skeleton")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar (68dp)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .shimmerEffect(actualTransition)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Channel Name (Large title)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .shimmerEffect(actualTransition)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Handle (@channel)
                Box(
                    modifier = Modifier
                        .width(95.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Metadata (Subscribers · Videos)
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Description preview (2 lines with "...more" preview feel)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Full-width pill Subscribe Button (40dp height, 20dp corners)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .shimmerEffect(actualTransition)
        )
    }
}

/**
 * Tab row skeleton placeholder for Home, Videos, Shorts, Playlists tabs.
 */
@Composable
fun ChannelTabsSkeleton(
    transition: InfiniteTransition? = null,
    modifier: Modifier = Modifier
) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("channel_tabs_skeleton")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
        }
        // Subtle divider below tabs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .shimmerEffect(actualTransition)
        )
    }
}

/**
 * Compact channel video row skeleton matching CompactChannelVideoRow.
 */
@Composable
fun ChannelCompactVideoRowSkeleton(
    transition: InfiniteTransition? = null,
    modifier: Modifier = Modifier
) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("channel_video_skeleton"),
        verticalAlignment = Alignment.Top
    ) {
        // 16:9 Thumbnail (135dp)
        Box(
            modifier = Modifier
                .width(135.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect(actualTransition)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Title Line 1
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            // Title Line 2
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Metadata (views · upload date)
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
        }
    }
}

/**
 * Channel video list skeleton displaying a series of compact video row skeletons.
 */
@Composable
fun ChannelVideoListSkeleton(
    transition: InfiniteTransition? = null,
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("channel_video_list_skeleton"),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(itemCount) {
            ChannelCompactVideoRowSkeleton(transition = actualTransition)
        }
    }
}

/**
 * Complete Channel Page Skeleton during data fetching, orchestrating the banner,
 * identity section, tabs, and compact video list with synchronized theme-aware shimmer.
 */
@Composable
fun ChannelMetadataSkeleton(transition: InfiniteTransition? = null) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("channel_metadata_skeleton")
    ) {
        // 1. Channel Banner Skeleton (16:5.5 responsive ratio)
        ChannelBannerSkeleton(transition = actualTransition)

        // 2. Channel Identity Skeleton (Avatar 68dp, Titles, Stats, Description, Pill Button)
        ChannelIdentitySkeleton(transition = actualTransition)

        // 3. Channel Tabs Row Skeleton
        ChannelTabsSkeleton(transition = actualTransition)

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Channel Videos List Skeleton (Compact channel video rows)
        repeat(4) {
            ChannelCompactVideoRowSkeleton(transition = actualTransition)
        }
    }
}

@Composable
fun PlaylistMetadataSkeleton(transition: InfiniteTransition? = null) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Column(modifier = Modifier.fillMaxWidth()) {
        // Banner Placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shimmerEffect(actualTransition)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .shimmerEffect(actualTransition)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Uploader Placeholder
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect(actualTransition)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons Row Placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect(actualTransition)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect(actualTransition)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Content Placeholders (Videos)
        repeat(3) {
            VideoCardSkeleton(actualTransition)
        }
    }
}

@Composable
fun LibraryDashboardSkeleton() {
    val transition = rememberSyncShimmerTransition()
    Column(modifier = Modifier.fillMaxSize()) {
        // Fluid Profile Header Placeholder
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .shimmerEffect(transition)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(3) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect(transition)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .shimmerEffect(transition)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section Carousels Placeholder
        repeat(3) {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(transition)
                    )
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(transition)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Carousel
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(2) {
                        Column(modifier = Modifier.width(180.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .shimmerEffect(transition)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect(transition)
                            )
                        }
                    }
                }
            }
        }
    }
}
