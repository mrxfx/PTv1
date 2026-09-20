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
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    fun isFavorite(videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(favorites: List<FavoriteEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllIgnoreSync(favorites: List<FavoriteEntity>)

    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    suspend fun getAllFavoritesStatic(): List<FavoriteEntity>

    @Query("DELETE FROM favorites")
    fun clearFavorites()

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)
}
