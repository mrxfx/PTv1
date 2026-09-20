/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.DownloadEntity
import com.rahul.vibetube1.data.local.FavoriteEntity
import com.rahul.vibetube1.data.local.HistoryEntity
import com.rahul.vibetube1.data.local.SubscriptionEntity
import com.rahul.vibetube1.data.local.DownloadStatus
import com.rahul.vibetube1.data.local.PlaylistFavoriteEntity
import com.rahul.vibetube1.data.local.PlaylistFavoriteDao
import com.rahul.vibetube1.domain.usecase.CancelDownloadUseCase
import com.rahul.vibetube1.domain.usecase.DeleteDownloadUseCase
import com.rahul.vibetube1.domain.usecase.GetDownloadsUseCase
import com.rahul.vibetube1.domain.usecase.GetFavoritesUseCase
import com.rahul.vibetube1.domain.usecase.GetHistoryUseCase
import com.rahul.vibetube1.domain.usecase.GetSubscriptionsUseCase
import com.rahul.vibetube1.domain.usecase.PauseDownloadUseCase
import com.rahul.vibetube1.domain.usecase.ResumeDownloadUseCase
import com.rahul.vibetube1.domain.usecase.SaveToPublicStorageUseCase
import com.rahul.vibetube1.domain.usecase.SyncSubscriptionMetadataUseCase
import com.rahul.vibetube1.domain.usecase.ToggleFavoriteUseCase
import com.rahul.vibetube1.domain.usecase.ToggleSubscriptionUseCase
import com.rahul.vibetube1.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val pauseDownloadUseCase: PauseDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val saveToPublicStorageUseCase: SaveToPublicStorageUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val syncSubscriptionMetadataUseCase: SyncSubscriptionMetadataUseCase,
    private val playlistFavoriteDao: PlaylistFavoriteDao,
    private val libraryRepository: com.rahul.vibetube1.domain.repository.LibraryRepository,
    private val downloadRepository: com.rahul.vibetube1.domain.repository.DownloadRepository
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = getDownloadsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadQueue: StateFlow<List<DownloadEntity>> = downloads
        .map { list -> list.filter { it.status != DownloadStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedDownloads: StateFlow<List<DownloadEntity>> = downloads
        .map { list -> list.filter { it.status == DownloadStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedVideoIds: StateFlow<Set<String>> = downloads
        .map { list -> 
            list.filter { it.status == DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val history: StateFlow<List<HistoryEntity>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = getFavoritesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = getSubscriptionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistFavoriteEntity>> = playlistFavoriteDao.getAllPlaylistFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localPlaylists: StateFlow<List<com.rahul.vibetube1.data.local.LocalPlaylistEntity>> = libraryRepository.getLocalPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedVideoIds: StateFlow<Set<String>> = libraryRepository.getAllSavedVideoIds()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _subscriptionSearchQuery = MutableStateFlow("")
    val subscriptionSearchQuery: StateFlow<String> = _subscriptionSearchQuery.asStateFlow()

    private val _offlineSearchQuery = MutableStateFlow("")
    val offlineSearchQuery: StateFlow<String> = _offlineSearchQuery.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        downloads,
        _offlineSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.uploaderName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredQueue: StateFlow<List<DownloadEntity>> = combine(
        downloadQueue,
        _offlineSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.uploaderName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredCompleted: StateFlow<List<DownloadEntity>> = combine(
        completedDownloads,
        _offlineSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.uploaderName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageUsage: StateFlow<StorageInfo> = downloads
        .map { list ->
            val totalBytes = list.sumOf { it.downloadedSize }
            StorageInfo(
                usedBytes = totalBytes,
                usedText = VideoUtils.formatSize(totalBytes)
            )
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageInfo())

    val filteredSubscriptions: StateFlow<List<SubscriptionEntity>> = combine(
        subscriptions,
        _subscriptionSearchQuery
    ) { subs, query ->
        if (query.isBlank()) subs
        else subs.filter { it.name.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncSubscriptions()
    }

    private fun syncSubscriptions() {
        viewModelScope.launch {
            syncSubscriptionMetadataUseCase()
        }
    }

    fun onSubscriptionSearchQueryChange(query: String) {
        _subscriptionSearchQuery.value = query
    }

    fun onOfflineSearchQueryChange(query: String) {
        _offlineSearchQuery.value = query
    }

    fun clearWatchedDownloads() {
        viewModelScope.launch {
            val watchedIds = history.value
                .filter { it.durationMs > 0 && it.progressMs.toFloat() / it.durationMs > 0.9f }
                .map { it.videoId }
                .toSet()
            
            downloads.value.forEach { download ->
                if (watchedIds.contains(download.videoId) && download.status == DownloadStatus.COMPLETED) {
                    deleteDownload(download.videoId)
                }
            }
        }
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            deleteDownloadUseCase(videoId)
        }
    }

    fun cancelDownload(videoId: String) {
        viewModelScope.launch {
            cancelDownloadUseCase(videoId)
        }
    }

    fun pauseDownload(videoId: String) {
        viewModelScope.launch {
            pauseDownloadUseCase(videoId)
        }
    }

    fun resumeDownload(videoId: String) {
        viewModelScope.launch {
            resumeDownloadUseCase(videoId)
        }
    }

    fun pauseAllDownloads() {
        viewModelScope.launch {
            downloadRepository.pauseAllActiveDownloads()
            _snackbarMessage.emit("Paused all downloads")
        }
    }

    fun resumeAllDownloads() {
        viewModelScope.launch {
            downloadRepository.resumeAllPausedDownloads()
            _snackbarMessage.emit("Resuming downloads")
        }
    }

    fun clearDownloadQueue() {
        viewModelScope.launch {
            val queue = downloadQueue.value
            queue.forEach {
                deleteDownloadUseCase(it.videoId)
            }
            _snackbarMessage.emit("Download queue cleared")
        }
    }

    fun saveToPublicStorage(videoId: String) {
        viewModelScope.launch {
            saveToPublicStorageUseCase(videoId)
                .onSuccess { _snackbarMessage.emit("Saved to gallery") }
                .onFailure { _snackbarMessage.emit("Failed to save: ${it.message}") }
        }
    }

    fun removeFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch {
            toggleFavoriteUseCase(favorite)
        }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch {
            libraryRepository.removeFromHistory(videoId)
        }
    }

    fun toggleSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            toggleSubscriptionUseCase(subscription)
        }
    }

    fun createLocalPlaylist(name: String) {
        viewModelScope.launch {
            libraryRepository.createLocalPlaylist(name)
        }
    }

    fun deleteLocalPlaylist(playlist: com.rahul.vibetube1.data.local.LocalPlaylistEntity) {
        viewModelScope.launch {
            libraryRepository.deleteLocalPlaylist(playlist)
        }
    }
}

data class StorageInfo(
    val usedBytes: Long = 0,
    val usedText: String = "0 B"
)
