package com.rahul.vibetube1.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahul.vibetube1.ui.theme.VibeTubeRed

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean = false
)

enum class NotificationType {
    SYSTEM, TRENDING, SUBSCRIPTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("All") }
    var notificationsList by remember {
        mutableStateOf(
            listOf(
                NotificationItem(
                    id = "1",
                    title = "Welcome to VibeTube! 🚀",
                    description = "Watch. Discover. Share. Experience the new era of beautiful media playback, custom made with ❤️ by Mr. Rahul.",
                    timestamp = "Just now",
                    type = NotificationType.SYSTEM
                ),
                NotificationItem(
                    id = "2",
                    title = "Trending on VibeTube 🔥",
                    description = "A wave of custom ambient synthesizer loops is currently trending. Experience the high quality sound track!",
                    timestamp = "10 mins ago",
                    type = NotificationType.TRENDING
                ),
                NotificationItem(
                    id = "3",
                    title = "Mr. Rahul uploaded a video!",
                    description = "VibeTube Design System Implementation walkthrough is now live. Check out the beautiful visual changes.",
                    timestamp = "1 hour ago",
                    type = NotificationType.SUBSCRIPTION
                ),
                NotificationItem(
                    id = "4",
                    title = "Offline Playback Ready 🎵",
                    description = "You can download videos and access them anytime in the Downloads tab without any network connection.",
                    timestamp = "Yesterday",
                    type = NotificationType.SYSTEM
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notificationsList.isNotEmpty()) {
                        TextButton(onClick = { notificationsList = emptyList() }) {
                            Text("Clear all", color = VibeTubeRed)
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Mentions", "Updates").forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VibeTubeRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (notificationsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your inbox is empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Notifications from your subscriptions and activities appear here.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notificationsList, key = { it.id }) { item ->
                        NotificationCard(
                            notification = item,
                            onClick = {
                                if (item.type == NotificationType.TRENDING || item.type == NotificationType.SUBSCRIPTION) {
                                    onNavigateToHome()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (notification.type) {
                            NotificationType.SYSTEM -> VibeTubeRed.copy(alpha = 0.15f)
                            NotificationType.TRENDING -> Color(0xFFFF9800).copy(alpha = 0.15f)
                            NotificationType.SUBSCRIPTION -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.SYSTEM -> Icons.Default.Notifications
                        NotificationType.TRENDING -> Icons.Default.TrendingUp
                        NotificationType.SUBSCRIPTION -> Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        NotificationType.SYSTEM -> VibeTubeRed
                        NotificationType.TRENDING -> Color(0xFFFF9800)
                        NotificationType.SUBSCRIPTION -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = notification.timestamp,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notification.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
