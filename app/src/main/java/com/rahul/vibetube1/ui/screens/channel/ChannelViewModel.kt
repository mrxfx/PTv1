/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.channel
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.SubscriptionEntity
import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.domain.model.ChannelDetails
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.domain.repository.LibraryRepository
import com.rahul.vibetube1.domain.usecase.*
import com.rahul.vibetube1.ui.components.DownloadDialogState
import com.rahul.vibetube1.utils.VibeTubeError
import com.rahul.vibetube1.utils.DownloadStreamResolver
import com.rahul.vibetube1.domain.model.StreamItem
import com.rahul.vibetube1.utils.VideoUtils
import com.rahul.vibetube1.utils.HistoryUtils.applyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

enum class ChannelVideoSortMode(val labelRes: Int) {
    LATEST(R.string.sort_latest),
    POPULAR(R.string.sort_popular),
    OLDEST(R.string.sort_oldest)
}

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val libraryRepository: LibraryRepository,
    private val videoRepository: com.rahul.vibetube1.domain.repository.VideoRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    private val _selectedSortMode = MutableStateFlow(ChannelVideoSortMode.LATEST)
    val selectedSortMode: StateFlow<ChannelVideoSortMode> = _selectedSortMode.asStateFlow()
    
    val uiState: StateFlow<ChannelUiState> = combine(
        _internalUiState,
        libraryRepository.getHistory(),
        _selectedSortMode
    ) { state, history, sortMode ->
        if (state is ChannelUiState.Success) {
            ChannelUiState.Success(
                details = state.details.copy(videos = state.details.videos.applyHistory(history)),
                isFetchingNextPage = state.isFetchingNextPage,
                sortMode = sortMode
            )
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, ChannelUiState.Loading)

    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private var currentDetails: ChannelDetails? = null
    private var currentChannelUrl: String? = null
    private var nextPage: Page? = null
    private var isFetchingNextPage = false

    private var subscriptionJob: Job? = null

    fun setSortMode(mode: ChannelVideoSortMode) {
        _selectedSortMode.value = mode
    }

    fun loadChannel(channelUrl: String) {
        if (currentChannelUrl == channelUrl && _internalUiState.value is ChannelUiState.Success) return
        currentChannelUrl = channelUrl
        nextPage = null
        _selectedSortMode.value = ChannelVideoSortMode.LATEST

        viewModelScope.launch {
            _internalUiState.value = ChannelUiState.Loading
            _isSubscribed.value = null // Reset for new channel
            
            // 1. Try immediate ID extraction from URL
            val immediateId = VideoUtils.extractChannelId(channelUrl)
            if (immediateId != null) {
                observeSubscription(immediateId)
            }
            
            getChannelDetailsUseCase(channelUrl)
                .onSuccess { details ->
                    currentDetails = details
                    nextPage = details.nextVideosPage
                    _internalUiState.value = ChannelUiState.Success(details)
                    
                    // 2. Observe using the REAL canonical channel ID (handles cases where URL was a name/@handle)
                    if (details.id != immediateId) {
                        observeSubscription(details.id)
                    }
                }
                .onFailure { exception ->
                    _internalUiState.value = ChannelUiState.Error(VibeTubeError.fromThrowable(exception))
                }
        }
    }

    fun loadNextPage() {
        val url = currentChannelUrl
        val page = nextPage
        val currentState = _internalUiState.value
        
        if (isFetchingNextPage || page == null || url == null || currentState !is ChannelUiState.Success) return

        isFetchingNextPage = true
        _internalUiState.value = currentState.copy(isFetchingNextPage = true)

        viewModelScope.launch {
            try {
                val result = videoRepository.fetchNextChannelVideosPage(url, page)
                val updatedState = _internalUiState.value
                if (updatedState is ChannelUiState.Success) {
                    nextPage = result.nextPage
                    val updatedDetails = updatedState.details.copy(
                        videos = (updatedState.details.videos + result.items).distinctBy { it.id },
                        nextVideosPage = result.nextPage
                    )
                    currentDetails = updatedDetails
                    _internalUiState.value = ChannelUiState.Success(
                        details = updatedDetails,
                        isFetchingNextPage = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val errorState = _internalUiState.value
                if (errorState is ChannelUiState.Success) {
                    _internalUiState.value = errorState.copy(isFetchingNextPage = false)
                }
            } finally {
                isFetchingNextPage = false
            }
        }
    }

    private fun observeSubscription(channelId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            isSubscribedUseCase(channelId).collectLatest {
                _isSubscribed.value = it
            }
        }
    }

    fun toggleSubscription() {
        val details = currentDetails ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = details.id,
                    name = details.name,
                    thumbnailUrl = details.avatarUrl,
                    subscriberCount = details.subscriberCount
                )
            )
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            val isFavorite = libraryRepository.isFavorite(video.id).first()
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName
                )
            )
            _snackbarMessage.emit(if (isFavorite) "Removed from Liked Videos" else "Added to Liked Videos")
        }
    }

    fun prepareDownload(video: VideoItem) {
        viewModelScope.launch {
            // Optimistic Cache Check
            val cachedBundle = videoRepository.getCachedStreamBundle(video.id)
            if (cachedBundle != null && !cachedBundle.videoStreams.isEmpty()) {
                _downloadState.value = DownloadDialogState.ShowDialog(video, cachedBundle)
                return@launch
            }

            _downloadState.value = DownloadDialogState.Loading(video)
            getVideoStreamsUseCase(video.id)
                .onSuccess { bundle ->
                    _downloadState.value = DownloadDialogState.ShowDialog(video, bundle)
                }
                .onFailure {
                    _downloadState.value = DownloadDialogState.Idle
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
            _snackbarMessage.emit("Downloading started")
            _downloadState.value = DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadDialogState.Idle
    }
}

sealed interface ChannelUiState {
    object Loading : ChannelUiState
    data class Success(
        val details: ChannelDetails,
        val isFetchingNextPage: Boolean = false,
        val sortMode: ChannelVideoSortMode = ChannelVideoSortMode.LATEST
    ) : ChannelUiState
    data class Error(val error: VibeTubeError) : ChannelUiState
}
