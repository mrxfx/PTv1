/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface DataManagerRepository {
    // YouTube Takeout
    fun importTakeoutHistory(uri: Uri): Flow<ImportProgress>
    fun importTakeoutSubscriptions(uri: Uri): Flow<ImportProgress>

    // Native Backup
    suspend fun createBackup(uri: Uri): Result<Unit>
    suspend fun restoreBackup(uri: Uri): Result<Unit>
}

sealed class ImportProgress {
    data class Loading(val progress: Float, val status: String) : ImportProgress()
    data class Success(val importedCount: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}
