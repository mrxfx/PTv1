/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.shorts

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.data.local.SubscriptionEntity
import com.rahul.vibetube1.domain.model.*
import com.rahul.vibetube1.domain.repository.LibraryRepository
import com.rahul.vibetube1.domain.repository.ShortsRepository
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.domain.usecase.ToggleFavoriteUseCase
import com.rahul.vibetube1.domain.usecase.ToggleSubscriptionUseCase
import com.rahul.vibetube1.domain.usecase.DownloadVideoUseCase
import com.rahul.vibetube1.domain.repository.DownloadRepository
import com.rahul.vibetube1.data.local.DownloadStatus
import com.rahul.vibetube1.utils.PTLog
import com.rahul.vibetube1.utils.DownloadStreamResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ShortsUiState {
    object Loading : ShortsUiState
    data class Refreshing(val currentShorts: List<VideoItem>) : ShortsUiState
    data class Loaded(val shorts: List<VideoItem>, val hasMore: Boolean) : ShortsUiState
    data class LoadingNextPage(val shorts: List<VideoItem>) : ShortsUiState
    data class Error(val message: String, val cachedShorts: List<VideoItem> = emptyList()) : ShortsUiState
    data class EndOfFeed(val shorts: List<VideoItem>) : ShortsUiState
}

val ShortsUiState.shorts: List<VideoItem>
    get() = when (this) {
        is ShortsUiState.Loaded -> shorts
        is ShortsUiState.LoadingNextPage -> shorts
        is ShortsUiState.Refreshing -> currentShorts
        is ShortsUiState.EndOfFeed -> shorts
        is ShortsUiState.Error -> cachedShorts
        is ShortsUiState.Loading -> emptyList()
    }

@HiltViewModel
class ShortsViewModel @Inject constructor(
    val shortsRepository: ShortsRepository,
    val videoRepository: VideoRepository,
    val libraryRepository: LibraryRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val downloadRepository: DownloadRepository,
    val preferencesManager: PreferencesManager,
    val preloadManager: ShortsPreloadManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val TAG = "ShortsPlayer"
        private const val TARGET_BATCH_SIZE = 10
    }

    private val initialVideoId: String? = savedStateHandle["initialVideoId"]
    private val initialTitle: String? = savedStateHandle["title"]
    private val initialThumbnailUrl: String? = savedStateHandle["thumbnailUrl"]
    private val initialUploaderName: String? = savedStateHandle["uploaderName"]
    private val initialUploaderThumbnailUrl: String? = savedStateHandle["uploaderThumbnailUrl"]
    private val initialUploaderUrl: String? = savedStateHandle["uploaderUrl"]

    private val _uiState = MutableStateFlow<ShortsUiState>(ShortsUiState.Loading)
    val uiState: StateFlow<ShortsUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    val downloadedVideoIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { list -> 
            list.filter { it.status == DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Isolated Shorts Player Feed State
    private val allShorts = mutableListOf<VideoItem>()
    private val loadedVideoIds = mutableSetOf<String>()
    private val sessionSeenVideoIds = mutableSetOf<String>()
    private var currentContinuation: ShortsContinuation? = null
    private var hasMoreContent: Boolean = true
    private var isFetchInProgress = false

    init {
        loadShorts()
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun isFavorite(videoId: String): Flow<Boolean> = libraryRepository.isFavorite(videoId)

    fun isSaved(videoId: String): Flow<Boolean> = libraryRepository.isVideoInAnyLocalPlaylist(videoId)

    fun isSubscribed(channelUrl: String): Flow<Boolean> {
        val uploaderId = channelUrl.substringAfterLast("/")
        return libraryRepository.isSubscribed(uploaderId)
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            PTLog.d(TAG, "[ShortsPlayer] Toggling favorite for video: ${video.id}")
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName
                )
            )
        }
    }

    fun toggleSubscription(video: VideoItem) {
        val uploaderId = video.uploaderUrl?.substringAfterLast("/") ?: return
        viewModelScope.launch {
            PTLog.d(TAG, "[ShortsPlayer] Toggling subscription for channel: $uploaderId")
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = uploaderId,
                    name = video.uploaderName,
                    thumbnailUrl = video.uploaderThumbnailUrl ?: "",
                    subscriberCount = null
                )
            )
        }
    }

    fun toggleSave(video: VideoItem) {
        viewModelScope.launch {
            val savedPlaylists = libraryRepository.getPlaylistsContainingVideo(video.id).first()
            if (savedPlaylists.isNotEmpty()) {
                PTLog.d(TAG, "[ShortsPlayer] Removing video ${video.id} from local playlists")
                savedPlaylists.forEach { playlistId ->
                    libraryRepository.removeVideoFromLocalPlaylist(playlistId, video.id)
                }
            } else {
                PTLog.d(TAG, "[ShortsPlayer] Saving video ${video.id} to default playlist")
                val playlists = libraryRepository.getLocalPlaylists().first()
                val targetPlaylistId = if (playlists.isEmpty()) {
                    libraryRepository.createLocalPlaylist("Saved Shorts", "Videos saved from Shorts")
                } else {
                    playlists.first().id.toLong()
                }
                libraryRepository.addVideoToLocalPlaylist(targetPlaylistId.toInt(), video)
            }
        }
    }

    fun markShortAsSeen(videoId: String) {
        val canonicalId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(videoId).ifEmpty { videoId }
        sessionSeenVideoIds.add(canonicalId)
        preloadManager.markShortAsShown(canonicalId)
    }

    fun onPageSettled(pageIndex: Int) {
        if (pageIndex in allShorts.indices) {
            val currentVideo = allShorts[pageIndex]
            Log.d(TAG, "[ShortsPlayer] Page settled at index=$pageIndex (id=${currentVideo.id}, title='${currentVideo.title}')")
            markShortAsSeen(currentVideo.id)
            
            // Trigger preloading for upcoming video
            preloadManager.onPageChanged(pageIndex, allShorts, viewModelScope)

            // Low queue detection: fetch more before end is reached (remaining <= 3)
            val remaining = allShorts.size - pageIndex
            if (remaining <= 3) {
                Log.d("SHORTS_NEXT_PAGE", "Remaining queue is low ($remaining <= 3). Triggering background pagination.")
                loadMoreShorts()
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _uiState.value = ShortsUiState.Refreshing(allShorts.toList())
            Log.d(TAG, "[ShortsPlayer] Pull-to-refresh started")
            allShorts.clear()
            loadedVideoIds.clear()
            currentContinuation = null
            hasMoreContent = true
            preloadManager.clear()
            loadShortsInternal(isRefresh = true)
            _isRefreshing.value = false
        }
    }

    fun loadShorts() {
        viewModelScope.launch {
            Log.d("SHORTS_PERF", "SHORTS_SESSION_START: initialVideoId=$initialVideoId")
            _uiState.value = ShortsUiState.Loading
            loadShortsInternal(isRefresh = false)
        }
    }

    private suspend fun loadShortsInternal(isRefresh: Boolean) {
        val startTime = System.currentTimeMillis()
        Log.d("SHORTS_PAGE_FETCH", "Feed discovery started. isRefresh=$isRefresh, initialVideoId=$initialVideoId")
        isFetchInProgress = true

        try {
            val seedId = if (!isRefresh) initialVideoId else null
            val excluded = (loadedVideoIds + sessionSeenVideoIds).toSet()
            // Request a smaller initial batch for faster first startup
            val result = shortsRepository.getInitialShortsFeed(
                initialVideoId = seedId,
                targetBatchSize = 3,
                excludedIds = excluded
            )

            currentContinuation = result.continuation
            hasMoreContent = result.hasMore

            val newItems = result.items.filter { item ->
                val canonicalId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                !loadedVideoIds.contains(canonicalId)
            }
            newItems.forEach { item ->
                val canonicalId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                loadedVideoIds.add(canonicalId)
            }
            allShorts.addAll(newItems)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d("SHORTS_PAGE_FETCH", "Discovery completed in ${elapsed}ms: loaded=${allShorts.size} items, hasMore=$hasMoreContent")

            if (allShorts.isNotEmpty()) {
                _uiState.value = ShortsUiState.Loaded(allShorts.toList(), hasMoreContent)
                
                // Immediately trigger preloading for upcoming video
                preloadManager.onPageChanged(0, allShorts, viewModelScope)
                
                // If we loaded a small initial batch, immediately trigger background pagination
                if (allShorts.size <= 3 && hasMoreContent) {
                    loadMoreShorts()
                }
            } else {
                Log.e(TAG, "[ShortsPlayer] Discovery failure: 0 real shorts discoverable.")
                _uiState.value = ShortsUiState.Error("No short videos could be loaded. Please pull down to retry.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "[ShortsPlayer] Error loading initial shorts feed", e)
            _uiState.value = ShortsUiState.Error(e.localizedMessage ?: "Failed to load Shorts")
        } finally {
            isFetchInProgress = false
        }
    }

    fun loadMoreShorts() {
        if (isFetchInProgress || !hasMoreContent || currentContinuation == null) {
            Log.d("SHORTS_NEXT_PAGE", "Skipping loadMoreShorts: inProgress=$isFetchInProgress, hasMore=$hasMoreContent, hasContinuation=${currentContinuation != null}")
            return
        }

        val continuation = currentContinuation ?: return
        isFetchInProgress = true
        _isLoadingMore.value = true
        _uiState.value = ShortsUiState.LoadingNextPage(allShorts.toList())
        Log.d("SHORTS_NEXT_PAGE", "Continuous pagination started. Current feed size=${allShorts.size}")

        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            try {
                val excluded = (loadedVideoIds + sessionSeenVideoIds).toSet()
                val result = shortsRepository.getNextShortsPage(
                    continuation = continuation,
                    targetBatchSize = TARGET_BATCH_SIZE,
                    excludedIds = excluded
                )

                currentContinuation = result.continuation
                hasMoreContent = result.hasMore

                val freshItems = result.items.filter { item ->
                    val canonicalId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                    !loadedVideoIds.contains(canonicalId)
                }
                if (freshItems.isNotEmpty()) {
                    freshItems.forEach { item ->
                        val canonicalId = com.rahul.vibetube1.utils.VideoUtils.extractVideoId(item.id).ifEmpty { item.id }
                        loadedVideoIds.add(canonicalId)
                    }
                    allShorts.addAll(freshItems)
                    val elapsed = System.currentTimeMillis() - startTime
                    Log.d("SHORTS_PAGE_FETCH", "Pagination appended ${freshItems.size} items in ${elapsed}ms. Total feed size=${allShorts.size}, hasMore=$hasMoreContent")
                    if (hasMoreContent) {
                        _uiState.value = ShortsUiState.Loaded(allShorts.toList(), true)
                    } else {
                        _uiState.value = ShortsUiState.EndOfFeed(allShorts.toList())
                    }
                } else {
                    Log.d("SHORTS_PAGE_FETCH", "Pagination returned 0 new items. hasMore=$hasMoreContent")
                    if (hasMoreContent) {
                        // Immediately try fetching again so we don't stall the feed
                        _uiState.value = ShortsUiState.Loaded(allShorts.toList(), true)
                        isFetchInProgress = false
                        _isLoadingMore.value = false
                        loadMoreShorts()
                        return@launch
                    } else {
                        _uiState.value = ShortsUiState.EndOfFeed(allShorts.toList())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[ShortsPlayer] Error during pagination", e)
                // Never clear the existing queue on failure!
                _uiState.value = ShortsUiState.Loaded(allShorts.toList(), hasMoreContent)
            } finally {
                isFetchInProgress = false
                _isLoadingMore.value = false
            }
        }
    }

    fun download(video: VideoItem, bundle: StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean, isAudioOnly: Boolean = false, saveToDevice: Boolean = false) {
        viewModelScope.launch {
            if (isAudioOnly) {
                val requestedStream = url?.let { StreamItem(url = it, quality = quality ?: "Audio", format = format ?: "m4a", isAdaptive = isAdaptive) }
                val selectedAudioStream = DownloadStreamResolver.selectAudioStream(bundle, requestedStream)
                val selectedAudioUrl = selectedAudioStream?.url ?: url ?: bundle.bestAudioStreamUrl

                downloadVideoUseCase(
                    videoId = video.id,
                    url = null,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName,
                    quality = quality ?: selectedAudioStream?.quality,
                    format = format ?: selectedAudioStream?.format,
                    audioUrl = selectedAudioUrl,
                    isAudioOnly = true,
                    saveToDevice = saveToDevice
                )
            } else {
                val audioStream = if (isAdaptive) DownloadStreamResolver.selectAudioStreamForAdaptiveVideo(bundle, format) else null
                downloadVideoUseCase(
                    videoId = video.id,
                    url = url,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName,
                    quality = quality,
                    format = format,
                    audioUrl = audioStream?.url,
                    isAudioOnly = false,
                    saveToDevice = saveToDevice
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        preloadManager.clear()
    }
}
