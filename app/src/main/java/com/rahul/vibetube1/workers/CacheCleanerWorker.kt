package com.rahul.vibetube1.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rahul.vibetube1.utils.CacheCleanerManager
import com.rahul.vibetube1.utils.PTLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CacheCleanerWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val cacheCleanerManager: CacheCleanerManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            PTLog.d("CacheCleanerWorker", "Executing periodic cache cleaner background work...")
            cacheCleanerManager.checkAndAutoClean()
            Result.success()
        } catch (e: Exception) {
            PTLog.e("CacheCleanerWorker", "Error in periodic cache cleaner background work", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "zyvotube_cache_cleaner_periodic"

        fun schedulePeriodicCleanup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<CacheCleanerWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                PTLog.d("CacheCleanerWorker", "Periodic 24h cache cleaner work enqueued successfully")
            } catch (e: Exception) {
                PTLog.w("CacheCleanerWorker", "Failed to schedule periodic cache cleaner work: ${e.message}")
            }
        }
    }
}
