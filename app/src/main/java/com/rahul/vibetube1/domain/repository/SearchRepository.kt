/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import com.rahul.vibetube1.domain.model.PaginatedList
import com.rahul.vibetube1.domain.model.SearchSort
import com.rahul.vibetube1.domain.model.SearchItem
import com.rahul.vibetube1.domain.model.UploadDateFilter
import com.rahul.vibetube1.domain.model.DurationFilter
import org.schabi.newpipe.extractor.Page

interface SearchRepository {
    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL
    ): PaginatedList<SearchItem>

    suspend fun fetchNextPage(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL,
        page: Page
    ): PaginatedList<SearchItem>

    suspend fun getSearchSuggestions(query: String): List<String>
}
