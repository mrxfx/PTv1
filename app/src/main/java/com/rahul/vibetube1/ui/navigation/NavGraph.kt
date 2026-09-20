/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rahul.vibetube1.MainViewModel
import com.rahul.vibetube1.ui.screens.home.HomeScreen
import com.rahul.vibetube1.ui.screens.home.HomeViewModel
import com.rahul.vibetube1.ui.screens.onboarding.InterestsSelectionScreen
import com.rahul.vibetube1.ui.screens.onboarding.OnboardingViewModel
import com.rahul.vibetube1.ui.screens.library.LibraryScreen
import com.rahul.vibetube1.ui.screens.library.LibraryViewModel
import com.rahul.vibetube1.ui.screens.channel.ChannelScreen
import com.rahul.vibetube1.ui.screens.channel.ChannelViewModel
import com.rahul.vibetube1.ui.screens.history.HistoryScreen
import com.rahul.vibetube1.ui.screens.player.PlayerScreen
import com.rahul.vibetube1.ui.screens.player.PlayerViewModel
import com.rahul.vibetube1.ui.screens.playlist.PlaylistScreen
import com.rahul.vibetube1.ui.screens.playlist.PlaylistViewModel
import com.rahul.vibetube1.ui.screens.playlist.PlaylistUiState
import com.rahul.vibetube1.ui.screens.search.SearchScreen
import com.rahul.vibetube1.ui.screens.search.SearchViewModel
import com.rahul.vibetube1.ui.screens.settings.SettingsScreen
import com.rahul.vibetube1.ui.screens.settings.SettingsViewModel
import com.rahul.vibetube1.ui.screens.settings.UpdateViewModel
import com.rahul.vibetube1.ui.screens.settings.DataManagementScreen
import com.rahul.vibetube1.ui.screens.settings.DataManagementViewModel
import com.rahul.vibetube1.ui.screens.subscriptions.SubscriptionsScreen
import com.rahul.vibetube1.ui.screens.subscriptions.SubscriptionFeedScreen
import com.rahul.vibetube1.ui.screens.subscriptions.SubscriptionsFeedViewModel
import com.rahul.vibetube1.ui.screens.shorts.ShortsScreen
import com.rahul.vibetube1.ui.screens.shorts.ShortsViewModel
import com.rahul.vibetube1.ui.screens.notifications.NotificationsScreen

import androidx.navigation.toRoute
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Destination = Destination.Home,
    onBarsVisibilityChange: (Boolean) -> Unit
) {
    val activity = LocalActivity.current as ComponentActivity
    val playerViewModel: PlayerViewModel = hiltViewModel(activity)
    val libraryViewModel: LibraryViewModel = hiltViewModel(activity)
    val mainViewModel: MainViewModel = hiltViewModel(activity)

    val handleVideoClick: (com.rahul.vibetube1.domain.model.VideoItem) -> Unit = { video ->
        val isShort = com.rahul.vibetube1.utils.VideoUtils.isShort(video)
        android.util.Log.d("VideoNavigation", "[VideoNavigation] id=${video.id}, title='${video.title}', duration=${video.duration}, isShort=$isShort, source=NavGraph, destination=${if (isShort) "ShortsPlayer" else "NormalPlayer"}")
        if (isShort) {
            playerViewModel.stopPlayback()
            navController.navigate(
                Destination.Shorts(
                    initialVideoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName,
                    uploaderThumbnailUrl = video.uploaderThumbnailUrl,
                    uploaderUrl = video.uploaderUrl
                )
            )
        } else {
            playerViewModel.loadVideo(video)
        }
    }

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val slideSpring = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val animationsEnabled = com.rahul.vibetube1.ui.theme.LocalAnimationsEnabled.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            onBarsVisibilityChange(true)
            if (!animationsEnabled) return@NavHost EnterTransition.None
            val initial = initialState.destination.route?.toDestination()
            val target = targetState.destination.route?.toDestination()
            val isBottomTab = initial?.isTopLevel == true && target?.isTopLevel == true
            
            if (isBottomTab) {
                fadeIn(animationSpec = tween(120, easing = LinearEasing))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        exitTransition = {
            if (!animationsEnabled) return@NavHost ExitTransition.None
            val initial = initialState.destination.route?.toDestination()
            val target = targetState.destination.route?.toDestination()
            val isBottomTab = initial?.isTopLevel == true && target?.isTopLevel == true
            
            if (isBottomTab) {
                fadeOut(animationSpec = tween(120, easing = LinearEasing))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeOut(animationSpec = tween(200))
            }
        },
        popEnterTransition = {
            onBarsVisibilityChange(true)
            if (!animationsEnabled) return@NavHost EnterTransition.None
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = slideSpring
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            if (!animationsEnabled) return@NavHost ExitTransition.None
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = slideSpring
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable<Destination.Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = handleVideoClick,
                onChannelClick = { channelUrl: String ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Destination.Shorts> { backStackEntry ->
            val shorts: Destination.Shorts = backStackEntry.toRoute()
            val viewModel: ShortsViewModel = hiltViewModel()
            ShortsScreen(
                viewModel = viewModel,
                initialVideoId = shorts.initialVideoId,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                }
            )
        }
        composable<Destination.Notifications> {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Home) { inclusive = true }
                    }
                }
            )
        }
        composable<Destination.Subscriptions> {
            val viewModel: SubscriptionsFeedViewModel = hiltViewModel()
            SubscriptionFeedScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = handleVideoClick,
                onChannelClick = { channelUrl: String ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSubscriptionsList = {
                    navController.navigate(Destination.SubscriptionsList) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Destination.SubscriptionsList> {
            SubscriptionsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                showTopAppBar = true
            )
        }
        composable<Destination.Library> {
            LibraryScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = handleVideoClick,
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                },
                onSeeAllHistory = { navController.navigate(Destination.History) { launchSingleTop = true } },
                onSeeAllSubscriptions = { navController.navigate(Destination.SubscriptionsList) { launchSingleTop = true } },
                onSeeAllDownloads = { navController.navigate(Destination.Downloads) { launchSingleTop = true } },
                onExploreClick = {
                    navController.navigate(Destination.Home) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable<Destination.Downloads> {
            com.rahul.vibetube1.ui.screens.library.DownloadsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = handleVideoClick,
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                }
            )
        }
        composable<Destination.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val updateViewModel: UpdateViewModel = hiltViewModel(activity)
            SettingsScreen(
                viewModel = viewModel,
                updateViewModel = updateViewModel,
                onViewHistory = { navController.navigate(Destination.History) { launchSingleTop = true } },
                onDataManagement = { navController.navigate(Destination.DataManagement) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.DataManagement> {
            val viewModel: DataManagementViewModel = hiltViewModel()
            DataManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.History> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            HistoryScreen(
                settingsViewModel = settingsViewModel,
                historyViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = handleVideoClick,
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onDiscoverVideos = {
                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Home) { inclusive = true }
                    }
                }
            )
        }
        composable<Destination.Channel> { backStackEntry ->
            val channel: Destination.Channel = backStackEntry.toRoute()
            val viewModel: ChannelViewModel = hiltViewModel()
            ChannelScreen(
                channelUrl = channel.channelUrl,
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onSearchClick = { navController.navigate(Destination.Search()) },
                onVideoClick = handleVideoClick,
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                }
            )
        }
        composable<Destination.Playlist> { backStackEntry ->
            val playlist: Destination.Playlist = backStackEntry.toRoute()
            val viewModel: PlaylistViewModel = hiltViewModel()
            PlaylistScreen(
                playlistId = playlist.playlistId,
                viewModel = viewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    val isShort = com.rahul.vibetube1.utils.VideoUtils.isShort(video)
                    android.util.Log.d("VideoNavigation", "[VideoNavigation] id=${video.id}, title='${video.title}', duration=${video.duration}, isShort=$isShort, source=Playlist, destination=${if (isShort) "ShortsPlayer" else "NormalPlayer"}")
                    if (isShort) {
                        playerViewModel.stopPlayback()
                        navController.navigate(
                            Destination.Shorts(
                                initialVideoId = video.id,
                                title = video.title,
                                thumbnailUrl = video.thumbnailUrl,
                                uploaderName = video.uploaderName,
                                uploaderThumbnailUrl = video.uploaderThumbnailUrl,
                                uploaderUrl = video.uploaderUrl
                            )
                        )
                    } else {
                        playerViewModel.loadVideo(
                            video = video,
                            playlistId = playlist.playlistId,
                            playlistTitle = (viewModel.uiState.value as? PlaylistUiState.Success)?.details?.title
                        )
                    }
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                }
            )
        }
        composable<Destination.Search> { backStackEntry ->
            val search: Destination.Search = backStackEntry.toRoute()
            val viewModel: SearchViewModel = hiltViewModel()
            val mainViewModel: MainViewModel = hiltViewModel(activity)
            val updateViewModel: UpdateViewModel = hiltViewModel(activity)

            androidx.compose.runtime.LaunchedEffect(search.query) {
                if (!search.query.isNullOrBlank()) {
                    viewModel.onQueryChange(search.query)
                    viewModel.search(search.query)
                }
            }

            SearchScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                updateViewModel = updateViewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = handleVideoClick,
                onShortClick = { shortVideo ->
                    playerViewModel.stopPlayback()
                    navController.navigate(
                        Destination.Shorts(
                            initialVideoId = shortVideo.id,
                            title = shortVideo.title,
                            thumbnailUrl = shortVideo.thumbnailUrl,
                            uploaderName = shortVideo.uploaderName,
                            uploaderThumbnailUrl = shortVideo.uploaderThumbnailUrl,
                            uploaderUrl = shortVideo.uploaderUrl
                        )
                    )
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Destination.Settings) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.Onboarding> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            InterestsSelectionScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Onboarding) { inclusive = true }
                    }
                }
            )
        }
    }
}
