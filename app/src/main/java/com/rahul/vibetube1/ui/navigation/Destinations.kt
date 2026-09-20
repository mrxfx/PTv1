/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    val isTopLevel: Boolean get() = false
    val routeRoot: String get() = when (this) {
        is Home -> "Home"
        is Shorts -> "Shorts"
        is Subscriptions -> "Subscriptions"
        is Notifications -> "Notifications"
        is Library -> "Library"
        is Search -> "Search"
        is Settings -> "Settings"
        is History -> "History"
        is SubscriptionsList -> "SubscriptionsList"
        is Downloads -> "Downloads"
        is Channel -> "Channel"
        is Player -> "Player"
        is Playlist -> "Playlist"
        is Onboarding -> "Onboarding"
        is DataManagement -> "DataManagement"
    }

    @Serializable data object Home : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data class Shorts(
        val initialVideoId: String? = null,
        val title: String? = null,
        val thumbnailUrl: String? = null,
        val uploaderName: String? = null,
        val uploaderThumbnailUrl: String? = null,
        val uploaderUrl: String? = null
    ) : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Subscriptions : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Notifications : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Library : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data class Search(val query: String? = null) : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Settings : Destination
    @Serializable data object History : Destination
    @Serializable data object SubscriptionsList : Destination
    @Serializable data object Downloads : Destination
    @Serializable data class Channel(val channelUrl: String) : Destination
    @Serializable data class Player(
        val videoId: String,
        val title: String? = null,
        val thumbnailUrl: String? = null,
        val playlistId: String? = null,
        val playlistTitle: String? = null
    ) : Destination
    @Serializable data class Playlist(val playlistId: String) : Destination
    @Serializable data object Onboarding : Destination
    @Serializable data object DataManagement : Destination
}

fun String?.toDestination(): Destination? {
    val route = this ?: return null
    return when {
        route.contains("Home") -> Destination.Home
        route.contains("Shorts") -> {
            val videoId = if (route.contains("initialVideoId=")) {
                route.substringAfter("initialVideoId=").substringBefore("&").substringBefore("}")
            } else null
            val title = if (route.contains("title=")) {
                route.substringAfter("title=").substringBefore("&").substringBefore("}")
            } else null
            val thumbnailUrl = if (route.contains("thumbnailUrl=")) {
                route.substringAfter("thumbnailUrl=").substringBefore("&").substringBefore("}")
            } else null
            val uploaderName = if (route.contains("uploaderName=")) {
                route.substringAfter("uploaderName=").substringBefore("&").substringBefore("}")
            } else null
            val uploaderThumbnailUrl = if (route.contains("uploaderThumbnailUrl=")) {
                route.substringAfter("uploaderThumbnailUrl=").substringBefore("&").substringBefore("}")
            } else null
            val uploaderUrl = if (route.contains("uploaderUrl=")) {
                route.substringAfter("uploaderUrl=").substringBefore("&").substringBefore("}")
            } else null
            Destination.Shorts(
                initialVideoId = videoId,
                title = title,
                thumbnailUrl = thumbnailUrl,
                uploaderName = uploaderName,
                uploaderThumbnailUrl = uploaderThumbnailUrl,
                uploaderUrl = uploaderUrl
            )
        }
        route.contains("SubscriptionsList") -> Destination.SubscriptionsList
        route.contains("Subscriptions") -> Destination.Subscriptions
        route.contains("Notifications") -> Destination.Notifications
        route.contains("Library") -> Destination.Library
        route.contains("Search") -> {
            // Extract query if available
            val query = if (route.contains("query=")) {
                route.substringAfter("query=").substringBefore("&").substringBefore("}")
            } else null
            Destination.Search(query)
        }
        route.contains("Settings") -> Destination.Settings
        route.contains("History") -> Destination.History
        route.contains("Downloads") -> Destination.Downloads
        route.contains("Onboarding") -> Destination.Onboarding
        route.contains("DataManagement") -> Destination.DataManagement
        else -> null
    }
}
