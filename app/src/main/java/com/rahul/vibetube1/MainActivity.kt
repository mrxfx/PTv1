/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.services.PlaybackService
import com.rahul.vibetube1.ui.components.GlassSurface
import com.rahul.vibetube1.ui.components.main.OfflineBottomBanner
import com.rahul.vibetube1.ui.components.main.VibeTubeTopAppBar
import com.rahul.vibetube1.ui.components.main.RestorationBanner
import com.rahul.vibetube1.ui.navigation.NavGraph
import com.rahul.vibetube1.ui.navigation.Destination
import com.rahul.vibetube1.ui.navigation.toDestination
import com.rahul.vibetube1.ui.screens.player.MiniPlayerManager
import com.rahul.vibetube1.ui.screens.player.PlayerOverlay
import com.rahul.vibetube1.ui.screens.settings.UpdateViewModel
import com.rahul.vibetube1.ui.theme.IncognitoPurple
import com.rahul.vibetube1.ui.theme.VibeTubeTheme
import com.rahul.vibetube1.utils.ConnectivityObserver
import com.rahul.vibetube1.utils.PTLog
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        val BOTTOM_BAR_HEIGHT = 76.dp
    }

    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var miniPlayerManager: MiniPlayerManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Handle permission result if needed
    }

    private val mainViewModel: MainViewModel by viewModels()

    private val playerViewModel: com.rahul.vibetube1.ui.screens.player.PlayerViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private val pendingDeepLink = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Splash screen exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val animSet = AnimatorSet()
            val fadeOut = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)
            fadeOut.interpolator = android.view.animation.LinearInterpolator()
            
            try {
                // Some Android 12+ devices throw NPE when attempting to access the iconView
                val iconView = splashScreenView.iconView
                if (iconView != null) {
                    val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.2f, 0f)
                    val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.2f, 0f)
                    scaleX.interpolator = android.view.animation.AccelerateInterpolator()
                    scaleY.interpolator = android.view.animation.AccelerateInterpolator()
                    animSet.playTogether(scaleX, scaleY, fadeOut)
                } else {
                    animSet.play(fadeOut)
                }
            } catch (e: Exception) {
                // Fallback to just fading out the splash screen view
                animSet.play(fadeOut)
            }
            
            animSet.duration = 400L
            animSet.doOnEnd { splashScreenView.remove() }
            animSet.start()
        }

        pendingDeepLink.value = intent

        // Log app_open telemetry event
        com.rahul.vibetube1.utils.FirebaseTelemetry.logAppOpen()

        // Observe critical events
        lifecycleScope.launch {
            playerViewModel.sleepTimerManager.timerFinishedEvent.collectLatest {
                if (playerViewModel.sleepTimerManager.shouldCloseApp.value) {
                    finishAndRemoveTask()
                }
            }
        }

        setContent {
            val appTheme by mainViewModel.appTheme.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = remember(appTheme, systemDark) {
                when (appTheme) {
                    "Dark" -> true
                    "Light" -> false
                    else -> systemDark
                }
            }
            val isBackgroundPlayEnabled by mainViewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
            val startDestination = Destination.Home

            // Background Play MediaSession Connection
            if (isBackgroundPlayEnabled) {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(this@MainActivity, android.content.ComponentName(this@MainActivity, PlaybackService::class.java))
                    val controllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
                    controllerFuture.addListener({}, MoreExecutors.directExecutor())
                    onDispose {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }
            }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        updateViewModel.checkForUpdates(force = false)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val isAnimationsEnabled by mainViewModel.isAnimationsEnabled.collectAsStateWithLifecycle()

            VibeTubeTheme(
                darkTheme = darkTheme,
                isDynamicColorEnabled = mainViewModel.isDynamicColorEnabled.collectAsStateWithLifecycle().value
            ) {
                CompositionLocalProvider(com.rahul.vibetube1.ui.theme.LocalAnimationsEnabled provides isAnimationsEnabled) {
                    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
                    val downloadState by updateViewModel.downloadState.collectAsStateWithLifecycle()

                    LaunchedEffect(updateInfo.status) {
                        if (updateInfo.status == com.rahul.vibetube1.domain.repository.UpdateStatus.MANDATORY_UPDATE) {
                            playerViewModel.player.pause()
                            miniPlayerManager.close()
                        }
                    }

                    if (updateInfo.status == com.rahul.vibetube1.domain.repository.UpdateStatus.MANDATORY_UPDATE) {
                        com.rahul.vibetube1.ui.screens.update.MandatoryUpdateScreen(
                            updateInfo = updateInfo,
                            downloadState = downloadState,
                            canRequestPackageInstalls = { updateViewModel.canRequestPackageInstalls() },
                            onStartDownload = { updateViewModel.startDownload() },
                            onRetryDownload = { updateViewModel.retryDownload() },
                            onInstallApk = {
                                val result = updateViewModel.installUpdate()
                                result.exceptionOrNull()?.let { e ->
                                    val msg = e.localizedMessage ?: "Update rejected: Downloaded APK is not compatible with this installation."
                                    android.widget.Toast.makeText(this@MainActivity, msg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onOpenInstallSettings = { updateViewModel.openInstallSettings() }
                        )
                    } else {
                        val navController = rememberNavController()
                        val lastDismissedCode by updateViewModel.lastDismissedVersionCode.collectAsStateWithLifecycle()
                        val hasOptionalUpdate = updateInfo.status == com.rahul.vibetube1.domain.repository.UpdateStatus.OPTIONAL_UPDATE
                        val showOptionalDialog = hasOptionalUpdate && updateInfo.latestVersionCode > lastDismissedCode

                        if (showOptionalDialog) {
                            AlertDialog(
                                onDismissRequest = { updateViewModel.dismissUpdate(updateInfo.latestVersionCode) },
                                title = { Text("ZyvoTube Update", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column {
                                        Text("${updateInfo.title} is available.", style = MaterialTheme.typography.bodyLarge)
                                        if (updateInfo.releaseNotes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(updateInfo.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            navController.navigate(Destination.Library) { launchSingleTop = true }
                                            updateViewModel.startDownload()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = com.rahul.vibetube1.ui.theme.VibeTubeRed)
                                    ) {
                                        Text("Update")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { updateViewModel.dismissUpdate(updateInfo.latestVersionCode) }) {
                                        Text("Later")
                                    }
                                }
                            )
                        }

                        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                LaunchedEffect(navController, lifecycleOwner) {
                    lifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                        pendingDeepLink.collectLatest { intent ->
                            if (intent != null) {
                                handleDeepLink(intent, navController)
                                pendingDeepLink.value = null
                            }
                        }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val playerVisibility by miniPlayerManager.visibilityState.collectAsStateWithLifecycle()
                val isExpanded = playerVisibility == com.rahul.vibetube1.ui.screens.player.MiniPlayerVisibility.Expanded

                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                LaunchedEffect(isExpanded) {
                    if (isExpanded) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                }

                val currentVideo by miniPlayerManager.currentVideo.collectAsStateWithLifecycle()
                
                androidx.activity.compose.BackHandler(enabled = isExpanded) {
                    currentVideo?.let { miniPlayerManager.minimize(it) } ?: miniPlayerManager.close()
                }

                val isIncognitoMode by mainViewModel.isIncognitoMode.collectAsStateWithLifecycle()
                val isOffline by mainViewModel.isOffline.collectAsStateWithLifecycle()

                val currentScreen = remember<Destination?>(currentRoute) { currentRoute.toDestination() }
                val isMainRoute = currentScreen?.isTopLevel == true
                val isOnboarding = currentScreen is Destination.Onboarding
                
                LaunchedEffect(currentRoute) {
                    val isPlayer = currentRoute?.contains("Player") == true
                    mainViewModel.setPlayerScreen(isPlayer)
                    
                    if (currentScreen is Destination.Shorts) {
                        mainViewModel.setBarsVisibility(false)
                        playerViewModel.player.pause()
                    } else if (isMainRoute) {
                        mainViewModel.setBarsVisibility(true)
                    } else if (isOnboarding || isPlayer) {
                        mainViewModel.setBarsVisibility(false)
                    }
                }

                val isLandscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

                val showBars by remember(isMainRoute, isOnboarding, uiState.isInPipMode, currentScreen, isLandscape, isExpanded) {
                    derivedStateOf { 
                        isMainRoute && 
                        currentScreen !is Destination.Shorts && 
                        !uiState.isInPipMode && 
                        !isOnboarding &&
                        !isExpanded
                    }
                }

                val showTopBarActual by remember(showBars, currentScreen) {
                    derivedStateOf { showBars && currentScreen !is Destination.Search }
                }
                
                val barsVisibilityProgress by animateFloatAsState(
                    targetValue = if (showBars && uiState.isBarsVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "BarsVisibility"
                )

                DisposableEffect(Unit) {
                    val consumer = Consumer<Configuration> {
                        mainViewModel.setPipMode(isInPictureInPictureMode)
                    }
                    addOnConfigurationChangedListener(consumer)
                    onDispose {
                        removeOnConfigurationChangedListener(consumer)
                    }
                }

                DisposableEffect(playerViewModel.player, isExpanded) {
                    val listener = object : androidx.media3.common.Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying && isExpanded) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }
                    playerViewModel.player.addListener(listener)
                    if (playerViewModel.player.isPlaying && isExpanded) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    
                    onDispose {
                        playerViewModel.player.removeListener(listener)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        if (showTopBarActual) {
                            val barsProgress = barsVisibilityProgress
                            var heightPx by remember { mutableFloatStateOf(0f) }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { heightPx = it.height.toFloat() }
                                    .graphicsLayer {
                                        translationY = -heightPx * (1f - barsProgress)
                                        alpha = barsProgress
                                    }
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 0.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                                        RestorationBanner(isOffline)
                                        if (uiState.isBarsVisible || barsProgress > 0f) {
                                            VibeTubeTopAppBar(
                                                isIncognitoMode = isIncognitoMode,
                                                currentRoute = currentRoute,
                                                navController = navController,
                                                mainViewModel = mainViewModel,
                                                updateViewModel = updateViewModel
                                            )
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val barsProgress = barsVisibilityProgress
                    val topPadding = innerPadding.calculateTopPadding()
                    val density = LocalDensity.current
                    val statusBarHeightPx = WindowInsets.statusBars.getTop(density).toFloat()

                    Box(modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding, bottom = innerPadding.calculateBottomPadding())
                        .graphicsLayer {
                            val topPaddingPx = topPadding.toPx()
                            if (topPaddingPx > 0) {
                                translationY = -(topPaddingPx - statusBarHeightPx) * (1f - barsProgress)
                            }
                        }
                    ) {
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination,
                            onBarsVisibilityChange = { mainViewModel.setBarsVisibility(it) }
                        )

                        if (showBars || barsVisibilityProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        translationY = 100.dp.toPx() * (1f - barsVisibilityProgress)
                                        alpha = barsVisibilityProgress
                                    }
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    shape = RoundedCornerShape(32.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp,
                                    shadowElevation = 8.dp
                                ) {
                                    VibeTubeBottomBar(navController = navController)
                                }
                            }
                        }
                    }
                }

                if (currentScreen !is Destination.Shorts) {
                    PlayerOverlay(
                        isExpanded = isExpanded,
                        currentVideo = currentVideo,
                        bottomBarHeight = if (showBars) BOTTOM_BAR_HEIGHT * barsVisibilityProgress else 0.dp,
                        isIncognito = isIncognitoMode,
                        viewModel = playerViewModel,
                        navController = navController,
                        onClose = { miniPlayerManager.close { playerViewModel.stopPlayback() } },
                        onMaximize = { miniPlayerManager.maximize() },
                        onMinimize = { currentVideo?.let { miniPlayerManager.minimize(it) } },
                        onChannelClick = { url ->
                            currentVideo?.let { miniPlayerManager.minimize(it) }
                            navController.navigate(Destination.Channel(url))
                        },
                        onVideoClick = { video ->
                            val isShort = com.rahul.vibetube1.utils.VideoUtils.isShort(video)
                            android.util.Log.d("VideoNavigation", "[VideoNavigation] id=${video.id}, title='${video.title}', duration=${video.duration}, isShort=$isShort, source=PlayerOverlay, destination=${if (isShort) "ShortsPlayer" else "NormalPlayer"}")
                            if (isShort) {
                                playerViewModel.stopPlayback()
                                miniPlayerManager.close()
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
                        },
                        onAddToPlaylistClick = { mainViewModel.showPlaylistSelection(it) },
                        content = {}
                    )
                }

                // Global Offline Notification
                var showBanner by remember { mutableStateOf(false) }
                LaunchedEffect(isOffline) {
                    if (isOffline) {
                        showBanner = true
                        kotlinx.coroutines.delay(5000L)
                        showBanner = false
                    } else showBanner = false
                }

                val isMiniPlayerActive = playerVisibility == com.rahul.vibetube1.ui.screens.player.MiniPlayerVisibility.Minimized && currentScreen !is Destination.Shorts
                Box(modifier = Modifier.fillMaxSize().zIndex(200f)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (showBars) BOTTOM_BAR_HEIGHT + 24.dp else 16.dp)
                            .then(if (isMiniPlayerActive) Modifier.padding(bottom = 80.dp) else Modifier)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        OfflineBottomBanner(visible = showBanner, onNavigateToDownloads = {
                            navController.navigate(Destination.Downloads) { launchSingleTop = true }
                        })
                    }
                }

                // Global Offline Dialog
                val showOfflineDialog by mainViewModel.showOfflineDialog.collectAsStateWithLifecycle()
                if (showOfflineDialog) {
                    AlertDialog(
                        onDismissRequest = { mainViewModel.dismissOfflineDialog() },
                        icon = { Icon(Icons.Default.WifiOff, null) },
                        title = { Text(stringResource(R.string.no_internet)) },
                        text = { Text(stringResource(R.string.offline_dialog_text)) },
                        confirmButton = {
                            Button(onClick = { 
                                mainViewModel.dismissOfflineDialog()
                                navController.navigate(Destination.Downloads) { launchSingleTop = true }
                            }) { Text(stringResource(R.string.go_to_downloads)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { mainViewModel.dismissOfflineDialog() }) { Text(stringResource(R.string.close)) }
                        }
                    )
                }

                // Global Playlist Selection Sheet
                val playlistSelectionState by mainViewModel.playlistSelectionState.collectAsStateWithLifecycle()
                val localPlaylists by mainViewModel.localPlaylists.collectAsStateWithLifecycle()
                
                if (playlistSelectionState.isVisible) {
                    var showCreateDialog by remember { mutableStateOf(false) }
                    
                    com.rahul.vibetube1.ui.components.AddToPlaylistSheet(
                        playlists = localPlaylists,
                        playlistsWithVideo = playlistSelectionState.playlistsWithVideo,
                        onDismiss = { mainViewModel.hidePlaylistSelection() },
                        onPlaylistSelected = { playlist ->
                            playlistSelectionState.video?.let { video ->
                                mainViewModel.addVideoToPlaylist(playlist.id, video)
                            }
                        },
                        onCreateNewPlaylist = { showCreateDialog = true }
                    )

                    if (showCreateDialog) {
                        var name by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("Create New Playlist") },
                            text = {
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("Playlist name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (name.isNotBlank()) mainViewModel.createLocalPlaylist(name)
                                        showCreateDialog = false
                                    }
                                ) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                    }
                }
            }
        }
    }
    }

    override fun onPause() {
        super.onPause()
        val isInPictureInPictureMode = isInPictureInPictureMode
        val isBackgroundPlayEnabled = mainViewModel.isBackgroundPlayEnabled.value
        if (!isInPictureInPictureMode && !isChangingConfigurations && !isBackgroundPlayEnabled) {
            playerViewModel.player.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            playerViewModel.player.stop()
            playerViewModel.player.clearMediaItems()
            miniPlayerManager.clear()
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainViewModel.setPipMode(isInPictureInPictureMode)
        if (!isInPictureInPictureMode && lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED) {
            playerViewModel.player.pause()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (mainViewModel.isPipEnabled.value && playerViewModel.player.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent
    }

    private fun handleDeepLink(intent: Intent, navController: NavHostController) {
        if (intent.getBooleanExtra("OPEN_PLAYER", false)) {
            miniPlayerManager.maximize()
            intent.removeExtra("OPEN_PLAYER")
        }

        val data: Uri = intent.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        val url = data.toString()
        val videoId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(url)
        val playlistId = com.rahul.vibetube1.utils.VideoUtils.extractPlaylistId(url)
        when {
            url.contains("/playlist?list=") || url.contains("&list=") -> {
                if (playlistId.isNotBlank()) navController.navigate(Destination.Playlist(playlistId)) { launchSingleTop = true }
            }
            url.contains("/channel/") || url.contains("/c/") || url.contains("/user/") || url.contains("/@") -> {
                navController.navigate(Destination.Channel(url)) { launchSingleTop = true }
            }
            url.contains("/results?search_query=") || url.contains("/results?q=") -> {
                val query = data.getQueryParameter("search_query") ?: data.getQueryParameter("q")
                if (!query.isNullOrBlank()) navController.navigate(Destination.Search(query)) { launchSingleTop = true }
            }
            videoId.isNotBlank() -> {
                val isShortLink = url.contains("/shorts/")
                android.util.Log.d("VideoNavigation", "[VideoNavigation] id=$videoId, isShort=$isShortLink, source=DeepLink, destination=${if (isShortLink) "ShortsPlayer" else "NormalPlayer"}")
                if (isShortLink) {
                    navController.navigate(
                        Destination.Shorts(
                            initialVideoId = videoId,
                            title = "Loading Short..."
                        )
                    )
                } else {
                    playerViewModel.loadVideo(VideoItem(id = videoId, title = "Loading...", thumbnailUrl = "", uploaderName = "", uploaderUrl = null, viewCount = 0, uploadDate = null, duration = 0))
                }
            }
        }
        intent.action = null
    }
}

@Composable
fun ZyvoTubeBottomBar(navController: androidx.navigation.NavHostController) {
    val items = remember {
        listOf(
            Triple(Destination.Home, Icons.Default.Home, R.string.tab_home),
            Triple(Destination.Shorts(), Icons.Default.Whatshot, R.string.tab_shorts),
            Triple(Destination.Subscriptions, Icons.Default.Subscriptions, R.string.tab_subscriptions),
            Triple(Destination.Notifications, Icons.Default.Notifications, R.string.tab_notifications),
            Triple(Destination.Library, Icons.Default.AccountCircle, R.string.tab_you)
        )
    }
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val actualIndex = remember(currentDestination) {
        var index = -1
        currentDestination?.hierarchy?.forEach { dest ->
            val route = dest.route ?: ""
            val matchedIndex = when {
                route.contains(".Home") -> 0
                route.contains(".Shorts") -> 1
                route.contains(".Subscriptions") -> 2
                route.contains(".Notifications") -> 3
                route.contains(".Library") || route.contains(".History") || route.contains(".Downloads") || route.contains(".Settings") || route.contains(".DataManagement") -> 4
                else -> -1
            }
            if (matchedIndex != -1 && index == -1) {
                index = matchedIndex
            }
        }
        if (index >= 0) index else 0
    }

    var selectedIndex by remember { mutableIntStateOf(actualIndex) }

    LaunchedEffect(actualIndex) {
        selectedIndex = actualIndex
    }
    
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val itemWidth = maxWidth / items.size
        
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
            label = "indicatorOffset"
        )
        
        // Active liquid pill indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(24.dp)
                )
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (destination, icon, labelRes) ->
                val isSelected = index == selectedIndex
                val label = stringResource(labelRes)
                
                val contentColor = if (isSelected) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                selectedIndex = index
                                val startDestId = navController.graph.findStartDestination().id
                                val isCurrentTab = (actualIndex == index)
                                
                                if (!isCurrentTab) {
                                    navController.navigate(destination) {
                                        popUpTo(startDestId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    val isAtRoot = currentDestination?.id == startDestId
                                    if (!isAtRoot) {
                                        navController.navigate(destination) {
                                            popUpTo(startDestId) {
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            color = contentColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VibeTubeBottomBar(navController: androidx.navigation.NavHostController) {
    ZyvoTubeBottomBar(navController = navController)
}

@Composable
fun PlayTubeBottomBar(navController: androidx.navigation.NavHostController) {
    ZyvoTubeBottomBar(navController = navController)
}
