/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.usecase

import com.rahul.vibetube1.domain.model.PaginatedList
import com.rahul.vibetube1.domain.model.SearchSort
import com.rahul.vibetube1.domain.model.SearchItem
import com.rahul.vibetube1.domain.model.UploadDateFilter
import com.rahul.vibetube1.domain.model.DurationFilter
import com.rahul.vibetube1.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL
    ): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.search(query, sort, uploadDate, duration))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchNextPage(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL,
        page: Page
    ): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.fetchNextPage(query, sort, uploadDate, duration, page))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
