/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.domain.model.PlaylistDetails
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.StreamBundle
import com.rahul.vibetube1.data.local.PlaylistFavoriteEntity
import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.domain.repository.DownloadRepository
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.domain.repository.LibraryRepository
import com.rahul.vibetube1.domain.usecase.*
import com.rahul.vibetube1.ui.components.DownloadDialogState
import com.rahul.vibetube1.utils.VibeTubeError
import com.rahul.vibetube1.utils.DownloadStreamResolver
import com.rahul.vibetube1.domain.model.StreamItem
import com.rahul.vibetube1.utils.HistoryUtils.applyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistDetailsUseCase: GetPlaylistDetailsUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val togglePlaylistFavoriteUseCase: TogglePlaylistFavoriteUseCase,
    private val isPlaylistFavoriteUseCase: IsPlaylistFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    val libraryRepository: LibraryRepository,
    private val videoRepository: VideoRepository,
    val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = combine(
        _internalUiState,
        libraryRepository.getHistory()
    ) { state, history ->
        if (state is PlaylistUiState.Success) {
            state.copy(details = state.details.copy(videos = state.details.videos.applyHistory(history)))
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, PlaylistUiState.Loading)

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val downloadedVideoIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { list -> 
            list.filter { it.status == com.rahul.vibetube1.data.local.DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

    fun loadPlaylist(playlistId: String) {
        if (playlistId.startsWith("local:")) {
            val id = playlistId.substringAfter("local:").toIntOrNull()
            if (id != null) {
                loadLocalPlaylist(id)
                return
            }
        }

        val playlistUrl = if (playlistId.startsWith("http")) {
            playlistId
        } else {
            "https://www.youtube.com/playlist?list=$playlistId"
        }
        viewModelScope.launch {
            _internalUiState.value = PlaylistUiState.Loading
            
            // Watch favorite status
            launch {
                isPlaylistFavoriteUseCase(playlistId).collectLatest {
                    _isFavorite.value = it
                }
            }

            getPlaylistDetailsUseCase(playlistUrl)
                .onSuccess { details ->
                    _internalUiState.value = PlaylistUiState.Success(details)
                }
                .onFailure { exception ->
                    _internalUiState.value = PlaylistUiState.Error(VibeTubeError.fromThrowable(exception))
                }
        }
    }

    private fun loadLocalPlaylist(id: Int) {
        viewModelScope.launch {
            _internalUiState.value = PlaylistUiState.Loading
            libraryRepository.getLocalPlaylists().collectLatest { playlists ->
                val playlist = playlists.find { it.id == id }
                if (playlist != null) {
                    libraryRepository.getVideosForLocalPlaylist(id).collectLatest { videos ->
                        val details = PlaylistDetails(
                            id = "local:$id",
                            title = playlist.name,
                            thumbnailUrl = playlist.thumbnailUrl ?: videos.firstOrNull()?.thumbnailUrl ?: "",
                            uploaderName = "Local Playlist",
                            uploaderUrl = null,
                            videos = videos.map { it.toVideoItem() }
                        )
                        _internalUiState.value = PlaylistUiState.Success(details)
                    }
                } else {
                    _internalUiState.value = PlaylistUiState.Error(VibeTubeError.Unknown("Playlist not found"))
                }
            }
        }
    }

    private fun com.rahul.vibetube1.data.local.LocalPlaylistVideoEntity.toVideoItem() = VideoItem(
        id = videoId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = null,
        uploaderThumbnailUrl = null,
        viewCount = 0,
        uploadDate = null,
        rawUploadDate = null,
        duration = duration
    )

    fun downloadPlaylist(quality: String) {
        val state = _internalUiState.value as? PlaylistUiState.Success ?: return
        val details = state.details
        viewModelScope.launch {
            _snackbarMessage.emit("Playlist download started (${quality})")
            details.videos.forEach { video ->
                if (!downloadedVideoIds.value.contains(video.id)) {
                    downloadVideoUseCase(
                        videoId = video.id,
                        url = null,
                        title = video.title,
                        thumbnailUrl = video.thumbnailUrl,
                        uploaderName = video.uploaderName,
                        quality = quality,
                        format = null,
                        audioUrl = null,
                        playlistId = details.id,
                        playlistTitle = details.title,
                        isAudioOnly = false,
                        saveToDevice = false
                    )
                }
            }
        }
    }
    fun toggleVideoFavorite(video: VideoItem) {
        viewModelScope.launch {
            toggleFavoriteUseCase(
                com.rahul.vibetube1.data.local.FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName
                )
            )
            val isFav = libraryRepository.isFavorite(video.id).first()
            _snackbarMessage.emit(if (isFav) "Added to Liked Videos" else "Removed from Liked Videos")
        }
    }

    private val _downloadState = MutableStateFlow<com.rahul.vibetube1.ui.components.DownloadDialogState>(com.rahul.vibetube1.ui.components.DownloadDialogState.Idle)
    val downloadState: StateFlow<com.rahul.vibetube1.ui.components.DownloadDialogState> = _downloadState.asStateFlow()

    fun prepareDownload(video: VideoItem) {
        viewModelScope.launch {
            // Optimistic Cache Check
            val cachedBundle = videoRepository.getCachedStreamBundle(video.id)
            if (cachedBundle != null && !cachedBundle.videoStreams.isEmpty()) {
                _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.ShowDialog(video, cachedBundle)
                return@launch
            }

            _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.Loading(video)
            getVideoStreamsUseCase(video.id)
                .onSuccess { bundle ->
                    _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.ShowDialog(video, bundle)
                }
                .onFailure {
                    _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.Idle
                }
        }
    }

    fun download(video: VideoItem, bundle: com.rahul.vibetube1.domain.model.StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean, isAudioOnly: Boolean = false, saveToDevice: Boolean = false) {
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
            _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = com.rahul.vibetube1.ui.components.DownloadDialogState.Idle
    }

    fun removeFromPlaylist(video: VideoItem) {
        val currentUiState = _internalUiState.value
        if (currentUiState is PlaylistUiState.Success && currentUiState.details.id.startsWith("local:")) {
            val playlistId = currentUiState.details.id.substringAfter("local:").toIntOrNull() ?: return
            viewModelScope.launch {
                libraryRepository.removeVideoFromLocalPlaylist(playlistId, video.id)
                _snackbarMessage.emit("Removed from Playlist")
            }
        }
    }

    private val _showPlaylistDownloadDialog = MutableStateFlow(false)
    val showPlaylistDownloadDialog: StateFlow<Boolean> = _showPlaylistDownloadDialog.asStateFlow()

    fun showPlaylistDownloadDialog() {
        _showPlaylistDownloadDialog.value = true
    }

    fun togglePlaylistFavorite() {
        val currentUiState = _internalUiState.value
        if (currentUiState is PlaylistUiState.Success) {
            viewModelScope.launch {
                togglePlaylistFavoriteUseCase(
                    com.rahul.vibetube1.data.local.PlaylistFavoriteEntity(
                        playlistId = currentUiState.details.id,
                        title = currentUiState.details.title,
                        thumbnailUrl = currentUiState.details.thumbnailUrl,
                        uploaderName = currentUiState.details.uploaderName
                    )
                )
                _snackbarMessage.emit("Playlist favorite status changed")
            }
        }
    }

    fun dismissPlaylistDownloadDialog() {
        _showPlaylistDownloadDialog.value = false
    }
}

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
    data class Success(val details: PlaylistDetails) : PlaylistUiState
    data class Error(val error: VibeTubeError) : PlaylistUiState
}
