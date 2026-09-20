/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.repository

import com.rahul.vibetube1.domain.model.PaginatedList
import com.rahul.vibetube1.domain.model.SearchSort
import com.rahul.vibetube1.domain.model.VideoItem
import com.rahul.vibetube1.domain.model.SearchItem
import com.rahul.vibetube1.domain.model.PlaylistItem
import com.rahul.vibetube1.domain.model.UploadDateFilter
import com.rahul.vibetube1.domain.model.DurationFilter
import com.rahul.vibetube1.domain.repository.SearchRepository
import com.rahul.vibetube1.data.network.NewPipeInitializer
import com.rahul.vibetube1.utils.Constants
import com.rahul.vibetube1.utils.PTLog
import com.rahul.vibetube1.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import com.rahul.vibetube1.ui.screens.search.SearchSuggestionProvider
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val initializer: NewPipeInitializer,
    private val suggestionProvider: SearchSuggestionProvider
) : SearchRepository {
    override suspend fun search(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter,
        duration: DurationFilter
    ): PaginatedList<SearchItem> {
        initializer.ensureInitialized()
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube

                val sortFilter = sort.value
                val contentFilters = mutableListOf<String>()
                
                // Base filter
                if (sort == SearchSort.UPLOAD_DATE) {
                    contentFilters.add("videos")
                } else {
                    contentFilters.add("all")
                }

                // Add upload date filter if not ALL
                if (uploadDate != UploadDateFilter.ALL) {
                    contentFilters.add(uploadDate.value)
                }

                // Add duration filter if not ALL
                if (duration != DurationFilter.ALL) {
                    contentFilters.add(duration.value)
                }
                
                PTLog.d("SearchRepository", "Searching for: $query with sort: $sortFilter, filters: $contentFilters")
                
                val extractor = youtubeService.getSearchExtractor(
                    query,
                    contentFilters,
                    sortFilter
                )
                extractor.fetchPage()

                val page = extractor.initialPage
                val items = page.items.mapNotNull { item ->
                    when (item) {
                        is StreamInfoItem -> SearchItem.Video(mapToVideoItem(item))
                        is ChannelInfoItem -> SearchItem.Channel(
                            id = item.url, // Usually full URL in NewPipe
                            name = item.name ?: "Unknown Channel",
                            thumbnailUrl = item.thumbnails?.maxByOrNull { it.width * it.height }?.url
                                ?: item.thumbnails?.lastOrNull()?.url
                                ?: item.thumbnails?.firstOrNull()?.url,
                            subscriberCount = item.subscriberCount,
                            description = item.description
                        )
                        is PlaylistInfoItem -> SearchItem.Playlist(
                            PlaylistItem(
                                id = VideoUtils.extractPlaylistId(item.url),
                                title = item.name ?: "Unknown Playlist",
                                thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "",
                                uploaderName = item.uploaderName ?: "Unknown Channel",
                                uploaderUrl = item.uploaderUrl,
                                streamCount = item.streamCount
                            )
                        )
                        else -> null
                    }
                }

                PaginatedList(items, if (page.hasNextPage()) page.nextPage else null)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    override suspend fun fetchNextPage(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter,
        duration: DurationFilter,
        page: Page
    ): PaginatedList<SearchItem> {
        initializer.ensureInitialized()
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube

                val sortFilter = sort.value
                val contentFilters = mutableListOf<String>()
                
                if (sort == SearchSort.UPLOAD_DATE) {
                    contentFilters.add("videos")
                } else {
                    contentFilters.add("all")
                }

                if (uploadDate != UploadDateFilter.ALL) {
                    contentFilters.add(uploadDate.value)
                }

                if (duration != DurationFilter.ALL) {
                    contentFilters.add(duration.value)
                }
                
                PTLog.d("SearchRepository", "Fetching next page for: $query with filters: $contentFilters")
                
                val extractor = youtubeService.getSearchExtractor(
                    query,
                    contentFilters,
                    sortFilter
                )
                val nextPage = extractor.getPage(page)
                
                val items = nextPage.items.mapNotNull { item ->
                    when (item) {
                        is StreamInfoItem -> SearchItem.Video(mapToVideoItem(item))
                        is ChannelInfoItem -> SearchItem.Channel(
                            id = item.url,
                            name = item.name ?: "Unknown Channel",
                            thumbnailUrl = item.thumbnails?.maxByOrNull { it.width * it.height }?.url
                                ?: item.thumbnails?.lastOrNull()?.url
                                ?: item.thumbnails?.firstOrNull()?.url,
                            subscriberCount = item.subscriberCount,
                            description = item.description
                        )
                        is PlaylistInfoItem -> SearchItem.Playlist(
                            PlaylistItem(
                                id = VideoUtils.extractPlaylistId(item.url),
                                title = item.name ?: "Unknown Playlist",
                                thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "",
                                uploaderName = item.uploaderName ?: "Unknown Channel",
                                uploaderUrl = item.uploaderUrl,
                                streamCount = item.streamCount
                            )
                        )
                        else -> null
                    }
                }

                PaginatedList(items, if (nextPage.hasNextPage()) nextPage.nextPage else null)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    private fun mapToVideoItem(item: StreamInfoItem): VideoItem {
        val videoId = VideoUtils.extractVideoId(item.url)
        val uploadDate = item.textualUploadDate?.takeIf { it.isNotBlank() }
            ?: item.uploadDate?.offsetDateTime()?.toString()
            ?: item.uploadDate?.instant?.toString()
            ?: item.uploadDate?.offsetDateTime()?.toLocalDate()?.toString()
        val rawUploadDate = item.uploadDate?.instant?.toEpochMilli()
            ?: item.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli()
            ?: VideoUtils.parseTextualUploadDate(item.textualUploadDate)

        val avatarUrl = item.uploaderAvatars?.let { avatars ->
            avatars.filter { !it.url.isNullOrBlank() }
                .maxByOrNull { it.width * it.height }?.url
                ?: avatars.lastOrNull { !it.url.isNullOrBlank() }?.url
                ?: avatars.firstOrNull { !it.url.isNullOrBlank() }?.url
        }
        
        // Multi-signal Real Shorts detection using extracted source metadata:
        // Do NOT classify based on title, description, or search query.
        val isSourceShort = try {
            item.isShortFormContent
        } catch (e: Exception) {
            false
        }
        val isShortUrl = item.url?.contains("/shorts/") == true
        val hasVerticalThumbnail = item.thumbnails?.any { 
            it.height > 0 && it.width > 0 && it.height > it.width 
        } == true
        val hasShortsThumbnailUrl = item.thumbnails?.any {
            it.url?.contains("/shorts/") == true || it.url?.contains("frame0.jpg") == true
        } == true

        // Strict validation: duration must not exceed 180s (standard YouTube Shorts duration limit).
        // Shorts classification requires explicit source metadata or vertical dimension evidence.
        // If classification is uncertain (e.g. landscape video without source flags), do NOT falsely label as a Short.
        val isRealShort = if (item.duration > 180L) {
            false
        } else {
            isSourceShort || isShortUrl || hasVerticalThumbnail || hasShortsThumbnailUrl
        }

        return VideoItem(
            id = videoId,
            title = item.name ?: "Unknown Title",
            thumbnailUrl = VideoUtils.getThumbnailForList(videoId),
            uploaderName = item.uploaderName ?: "Unknown Channel",
            uploaderUrl = item.uploaderUrl ?: "",
            uploaderThumbnailUrl = avatarUrl,
            viewCount = item.viewCount,
            subscriberCount = null,
            duration = item.duration,
            uploadDate = uploadDate,
            rawUploadDate = rawUploadDate,
            isShort = isRealShort
        )
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        return suggestionProvider.getSuggestions(query)
    }
}
