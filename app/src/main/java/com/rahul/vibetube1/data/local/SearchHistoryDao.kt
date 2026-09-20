/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    fun getAllSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(searchQuery: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(queries: List<SearchHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllIgnoreSync(queries: List<SearchHistoryEntity>)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllSearchHistoryStatic(): List<SearchHistoryEntity>

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllSearchHistory()
    @Query("DELETE FROM search_history")
    fun clearAllSearchHistorySync()
}
