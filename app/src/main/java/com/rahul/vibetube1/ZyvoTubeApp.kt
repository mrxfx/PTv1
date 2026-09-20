/*
 * ZyvoTube
 * Copyright (C) 2026 ZyvoTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.StrictMode
import androidx.work.Configuration
import com.rahul.vibetube1.data.network.YouTubeDownloader
import com.rahul.vibetube1.utils.AppVersion
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.data.local.VibeTubeDatabase
import androidx.hilt.work.HiltWorkerFactory
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.rahul.vibetube1.domain.repository.DownloadRepository
import com.rahul.vibetube1.utils.ConnectivityObserver
import com.rahul.vibetube1.utils.ZyvoLog
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import java.io.File

@HiltAndroidApp
class ZyvoTubeApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var downloadRepository: DownloadRepository
    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var database: VibeTubeDatabase
    @Inject lateinit var shortsPreloadManager: com.rahul.vibetube1.ui.screens.shorts.ShortsPreloadManager
    @Inject lateinit var updateManager: com.rahul.vibetube1.utils.UpdateManager
    @Inject lateinit var cacheCleanerManager: com.rahul.vibetube1.utils.CacheCleanerManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: Context): ImageLoader = imageLoader

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase Telemetry (Analytics & Crashlytics) safely
        com.rahul.vibetube1.utils.FirebaseTelemetry.init(this)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        try {
            if (BuildConfig.DEBUG) {
                StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                        .detectDiskReads()
                        .detectDiskWrites()
                        .detectNetwork()
                        .penaltyLog()
                        .build()
                )
                StrictMode.setVmPolicy(
                    StrictMode.VmPolicy.Builder()
                        .detectLeakedSqlLiteObjects()
                        .detectLeakedClosableObjects()
                        .penaltyLog()
                        .build()
                )
            }

            createNotificationChannel()
            observeConnectivity()
            checkVersionAndCleanup()
            checkActivityAndCleanup()
            shortsPreloadManager.clear()
            prewarmNetwork()
            schedulePeriodicUpdateCheck()
            com.rahul.vibetube1.workers.CacheCleanerWorker.schedulePeriodicCleanup(this)
            applicationScope.launch(Dispatchers.IO) {
                com.rahul.vibetube1.utils.StorageMigrationHelper.syncLegacyDownloads(applicationContext, database.downloadDao())
            }
        } catch (e: Exception) {
            ZyvoLog.e("ZyvoTubeApp", "Critical error during Application initialization", e)
        }
    }

    private fun checkVersionAndCleanup() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val lastVersion = preferencesManager.lastAppVersion.first()
                val currentVersion = AppVersion.code
                
                if (lastVersion != currentVersion) {
                    ZyvoLog.i("ZyvoTubeApp", "Detected update from $lastVersion to $currentVersion. Performing cache cleanup.")
                    performUpdateCleanup()
                    preferencesManager.setLastAppVersion(currentVersion)
                }
            } catch (e: Exception) {
                ZyvoLog.e("ZyvoTubeApp", "Version check or cleanup failed", e)
            }
        }
    }

    private suspend fun performUpdateCleanup() {
        try {
            // 1. Clear Feed Cache (Crucial for avoiding VideoItem serialization crashes)
            database.feedCacheDao().clearAll()
            
            // 2. Clear technical library caches (OkHttp, Coil, ExoPlayer)
            val cacheDir = applicationContext.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                deleteRecursively(file)
            }

            // 3. Clear pending update state and APKs
            preferencesManager.clearUpdateState()
            
            ZyvoLog.i("ZyvoTubeApp", "Update cleanup completed successfully")
        } catch (e: Exception) {
            ZyvoLog.e("ZyvoTubeApp", "Error during update cleanup", e)
        }
    }

    private fun schedulePeriodicUpdateCheck() {
        try {
            updateManager.schedulePeriodicUpdateCheck(24)
        } catch (e: Exception) {
            ZyvoLog.w("ZyvoTubeApp", "Could not enqueue update check work: ${e.message}")
        }
    }

    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        file.delete()
    }

    private fun prewarmNetwork() {
        val youtubeRequest = Request.Builder().url("https://www.youtube.com").head().build()
        val gVideoRequest = Request.Builder().url("https://www.googlevideo.com").head().build()

        val callback = object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                ZyvoLog.w("ZyvoTubeApp", "Network pre-warming failed for ${call.request().url}: ${e.message}")
            }
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
                ZyvoLog.d("ZyvoTubeApp", "Network pre-warmed for ${call.request().url}")
            }
        }

        okHttpClient.newCall(youtubeRequest).enqueue(callback)
        okHttpClient.newCall(gVideoRequest).enqueue(callback)
    }

    private fun checkActivityAndCleanup() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                preferencesManager.setLastActiveAt(now)
                cacheCleanerManager.checkAndAutoClean()
            } catch (e: Exception) {
                ZyvoLog.e("ZyvoTubeApp", "Activity-based cleanup failed", e)
            }
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun showCleanupNotification(freedBytes: Long) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val freedStr = com.rahul.vibetube1.utils.StorageUtils.formatSize(freedBytes)
        
        val notification = androidx.core.app.NotificationCompat.Builder(this, "update_channel")
            .setSmallIcon(R.drawable.ic_splash_logo)
            .setContentTitle("Temporary cache cleaned")
            .setContentText("Freed $freedStr of storage. Your downloads are safe.")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
            
        manager.notify(2001, notification)
    }

    private fun observeConnectivity() {
        connectivityObserver.observe()
            .onEach { status ->
                when (status) {
                    ConnectivityObserver.Status.Available -> {
                        downloadRepository.resumeAllPausedDownloads()
                    }
                    ConnectivityObserver.Status.Lost, ConnectivityObserver.Status.Unavailable -> {
                        downloadRepository.pauseAllActiveDownloads()
                    }
                    else -> {}
                }
            }
            .launchIn(applicationScope)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val downloadChannel = NotificationChannel(
                "download_channel",
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress"
            }
            notificationManager.createNotificationChannel(downloadChannel)

            val updateChannel = NotificationChannel(
                "update_channel",
                "App Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows app update status and download progress"
            }
            notificationManager.createNotificationChannel(updateChannel)
        }
    }
}

typealias VibeTubeApp = ZyvoTubeApp

