/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rahul.vibetube1.domain.repository.UpdateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AppUpdateCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val updateRepository: UpdateRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            updateRepository.checkForUpdates(force = false)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "vibetube_periodic_update_check"
    }
}
