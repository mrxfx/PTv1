/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.rahul.vibetube1.BuildConfig
import com.rahul.vibetube1.R
import com.rahul.vibetube1.data.local.ChunkType
import com.rahul.vibetube1.data.local.DownloadDao
import com.rahul.vibetube1.data.local.DownloadStatus
import com.rahul.vibetube1.data.local.MissionDao
import com.rahul.vibetube1.data.local.MissionStatus
import com.rahul.vibetube1.data.network.ParallelDownloader
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.utils.DownloadStreamResolver
import com.rahul.vibetube1.utils.Mp3AudioConverter
import com.rahul.vibetube1.utils.NativeMediaMuxer
import com.rahul.vibetube1.utils.PTLog
import com.rahul.vibetube1.utils.StorageUtils
import com.rahul.vibetube1.utils.ZyvoLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class AudioDownloadService : android.app.Service() {

    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var missionDao: MissionDao
    @Inject lateinit var downloadDao: DownloadDao
    @Inject lateinit var videoRepository: VideoRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var downloader: ParallelDownloader
    private val muxer = NativeMediaMuxer()

    private val activeMissions = mutableMapOf<Long, Job>()
    private var foregroundMissionId: Long = -1L
    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    private enum class NotificationType {
        DOWNLOADING, COMPLETED, FAILED
    }

    companion object {
        const val ACTION_START = "com.rahul.vibetube1.services.AudioDownloadService.START"
        const val ACTION_STOP = "com.rahul.vibetube1.services.AudioDownloadService.STOP"
        const val ACTION_CANCEL = "com.rahul.vibetube1.services.AudioDownloadService.CANCEL"

        private const val CHANNEL_ID = "zyvotube_audio_download_channel"
        private const val NOTIFICATION_ID_BASE = 200000
    }

    override fun onCreate() {
        super.onCreate()
        downloader = ParallelDownloader(okHttpClient, missionDao)
        createNotificationChannel()

        serviceScope.launch {
            try {
                val missions = missionDao.getAllMissions().first()
                missions.forEach { mission ->
                    if (mission.isAudioOnly && mission.status == MissionStatus.DOWNLOADING) {
                        PTLog.d("AudioDownloadService", "Recovering stale audio mission ${mission.videoId}")
                        missionDao.updateStatus(mission.id, MissionStatus.PAUSED)
                        downloadDao.setDownloadStatus(mission.videoId, DownloadStatus.PAUSED)
                    }
                }
            } catch (e: Exception) {
                PTLog.e("AudioDownloadService", "Stale audio mission recovery failed", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val missionId = intent?.getLongExtra("missionId", -1L) ?: -1L

        when (action) {
            ACTION_START -> if (missionId != -1L) startMission(missionId)
            ACTION_STOP -> if (missionId != -1L) stopMission(missionId)
            ACTION_CANCEL -> if (missionId != -1L) cancelMission(missionId)
        }

        return START_REDELIVER_INTENT
    }

    private fun startMission(missionId: Long) {
        if (activeMissions.containsKey(missionId)) return

        val job = serviceScope.launch {
            var itemUriCreated: Uri? = null
            var missionTitle = "Audio"
            var videoId = ""
            var tempVideoFileRef: File? = null
            var tempAudioFileRef: File? = null
            var tempMuxedFileRef: File? = null

            try {
                var mission = missionDao.getMissionById(missionId) ?: return@launch
                missionTitle = mission.title
                videoId = mission.videoId

                updateNotification(
                    missionId = missionId,
                    title = missionTitle,
                    content = "Downloading audio… 0%",
                    progress = 0,
                    isIndeterminate = false,
                    type = NotificationType.DOWNLOADING,
                    videoId = videoId
                )

                itemUriCreated = executeAudioDownloadAndConversion(
                    missionId = missionId,
                    mission = mission,
                    onTempFilesCreated = { vFile, aFile, mFile ->
                        tempVideoFileRef = vFile
                        tempAudioFileRef = aFile
                        tempMuxedFileRef = mFile
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) {
                    PTLog.d("AudioDownloadService", "Audio mission $missionId cancelled/paused")
                    missionDao.updateStatus(missionId, MissionStatus.PAUSED)
                    missionDao.getMissionById(missionId)?.let {
                        downloadDao.setDownloadStatus(it.videoId, DownloadStatus.PAUSED)
                    }
                    // Safely clean temp files on cancellation
                    tempVideoFileRef?.let { if (it.exists()) it.delete() }
                    tempAudioFileRef?.let { if (it.exists()) it.delete() }
                    tempMuxedFileRef?.let { if (it.exists()) it.delete() }
                    return@launch
                }

                PTLog.e("AudioDownloadService", "[AUDIO-DOWNLOAD-FAILED] Mission $missionId failed for videoId=$videoId: ${e.message}", e)
                ZyvoLog.e("AudioDownloadService", "Audio download failed for $videoId: ${e.message}")

                itemUriCreated?.let { uri ->
                    try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }

                missionDao.updateStatus(missionId, MissionStatus.FAILED)
                downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)

                val thumbnail = fetchThumbnailBitmap(videoId)
                updateNotification(
                    missionId = missionId,
                    title = missionTitle,
                    content = "Audio download failed • ${e.message ?: "Unknown error"}",
                    progress = 0,
                    isIndeterminate = false,
                    type = NotificationType.FAILED,
                    thumbnail = thumbnail,
                    videoId = videoId
                )
            } finally {
                activeMissions.remove(missionId)
                if (foregroundMissionId == missionId) {
                    foregroundMissionId = -1L
                    val nextMissionId = activeMissions.keys.firstOrNull()
                    if (nextMissionId != null) {
                        promoteToForeground(nextMissionId)
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_DETACH)
                        } else {
                            @Suppress("DEPRECATION")
                            stopForeground(false)
                        }
                        stopSelf()
                    }
                }
            }
        }

        activeMissions[missionId] = job

        if (foregroundMissionId == -1L) {
            foregroundMissionId = missionId
            val notification = createNotificationBuilder(
                missionId = missionId,
                title = "ZyvoTube Audio",
                content = "Downloading audio… 0%",
                progress = 0,
                isIndeterminate = false,
                type = NotificationType.DOWNLOADING
            ).build()
            startForeground(NOTIFICATION_ID_BASE + (missionId % 100000).toInt(), notification)
        }
    }

    private val lastUpdateMap = mutableMapOf<Long, Long>()

    private suspend fun executeAudioDownloadAndConversion(
        missionId: Long,
        mission: com.rahul.vibetube1.data.local.DownloadMissionEntity,
        onTempFilesCreated: (File, File, File) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val thumbnail = fetchThumbnailBitmap(mission.videoId)
        val uploaderName = withContext(Dispatchers.IO) {
            downloadDao.getDownloadById(mission.videoId)?.uploaderName
        } ?: "ZyvoTube"

        PTLog.d("AudioDownloadService", "[AudioPipeline] Initiated Audio->MP3 download pipeline for videoId=${mission.videoId}, title='${mission.title}'")

        // 1. Fetch Stream Bundle to resolve source video streams
        val bundle = videoRepository.getStreamBundle(mission.videoId)
        val selectedVideoStream = bundle.videoStreams.find { it.quality.contains("720") }
            ?: bundle.videoStreams.find { it.quality.contains("360") }
            ?: bundle.videoStreams.firstOrNull()

        val selectedAudioStream = if (selectedVideoStream != null) {
            DownloadStreamResolver.selectAudioStreamForAdaptiveVideo(bundle, selectedVideoStream.format)
        } else {
            DownloadStreamResolver.selectAudioStream(bundle)
        }

        val videoUrl = selectedVideoStream?.url
        val audioUrl = selectedAudioStream?.url ?: bundle.bestAudioStreamUrl

        if (videoUrl.isNullOrBlank() && audioUrl.isNullOrBlank()) {
            throw Exception("Failed to resolve stream URL for videoId=${mission.videoId}")
        }

        var totalVideoBytes = if (!videoUrl.isNullOrBlank()) downloader.getFileSize(videoUrl) else 0L
        var totalAudioBytes = if (!audioUrl.isNullOrBlank()) downloader.getFileSize(audioUrl) else 0L
        val combinedTotalSize = totalVideoBytes + totalAudioBytes
        val isSizeKnown = combinedTotalSize > 0

        // Storage Check
        val availableStorage = StorageUtils.getAvailableInternalStorage()
        if (combinedTotalSize > 0 && availableStorage < combinedTotalSize + (100 * 1024 * 1024)) {
            throw Exception("Not enough storage. Required: ${StorageUtils.formatSize(combinedTotalSize)}, Available: ${StorageUtils.formatSize(availableStorage)}")
        }

        missionDao.updateMission(mission.copy(totalBytes = combinedTotalSize, status = MissionStatus.DOWNLOADING))
        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, 0, combinedTotalSize)

        // Create temporary private files
        val tempVideoFile = File(cacheDir, "${mission.id}_${mission.videoId}_audio_video.tmp")
        val tempAudioFile = File(cacheDir, "${mission.id}_${mission.videoId}_audio_stream.tmp")
        val tempMuxedFile = File(cacheDir, "${mission.id}_${mission.videoId}_audio_muxed.mp4")

        onTempFilesCreated(tempVideoFile, tempAudioFile, tempMuxedFile)

        // Clean stale temp files
        if (tempVideoFile.exists()) tempVideoFile.delete()
        if (tempAudioFile.exists()) tempAudioFile.delete()
        if (tempMuxedFile.exists()) tempMuxedFile.delete()

        // 2. Download source video stream (and audio stream if adaptive)
        var downloadedVideoBytes = 0L
        if (!videoUrl.isNullOrBlank()) {
            downloadedVideoBytes = downloader.download(videoUrl, tempVideoFile, missionId, ChunkType.VIDEO) { progress ->
                val currentTime = System.currentTimeMillis()
                val lastUpdate = lastUpdateMap[missionId] ?: 0L

                if (currentTime - lastUpdate >= 350L || progress == combinedTotalSize) {
                    lastUpdateMap[missionId] = currentTime
                    val percent = if (isSizeKnown) ((progress * 100) / combinedTotalSize).toInt().coerceIn(0, 100) else 0

                    updateNotification(
                        missionId = missionId,
                        title = mission.title,
                        content = "Downloading audio… $percent%",
                        progress = percent,
                        isIndeterminate = !isSizeKnown,
                        type = NotificationType.DOWNLOADING,
                        thumbnail = thumbnail,
                        videoId = mission.videoId
                    )

                    serviceScope.launch(Dispatchers.IO) {
                        missionDao.updateProgress(missionId, progress)
                        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, progress, combinedTotalSize)
                    }
                }
            }
        }

        var downloadedAudioBytes = 0L
        if (!audioUrl.isNullOrBlank() && (videoUrl.isNullOrBlank() || (selectedVideoStream?.isAdaptive == true))) {
            downloadedAudioBytes = downloader.download(audioUrl, tempAudioFile, missionId, ChunkType.AUDIO) { progress ->
                val currentTotal = downloadedVideoBytes + progress
                val currentTime = System.currentTimeMillis()
                val lastUpdate = lastUpdateMap[missionId] ?: 0L

                if (currentTime - lastUpdate >= 350L || currentTotal == combinedTotalSize) {
                    lastUpdateMap[missionId] = currentTime
                    val percent = if (isSizeKnown) ((currentTotal * 100) / combinedTotalSize).toInt().coerceIn(0, 100) else 0

                    updateNotification(
                        missionId = missionId,
                        title = mission.title,
                        content = "Downloading audio… $percent%",
                        progress = percent,
                        isIndeterminate = !isSizeKnown,
                        type = NotificationType.DOWNLOADING,
                        thumbnail = thumbnail,
                        videoId = mission.videoId
                    )

                    serviceScope.launch(Dispatchers.IO) {
                        missionDao.updateProgress(missionId, currentTotal)
                        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, currentTotal, combinedTotalSize)
                    }
                }
            }
        }

        // Final notification for 100% video download
        updateNotification(
            missionId = missionId,
            title = mission.title,
            content = "Downloading audio… 100%",
            progress = 100,
            isIndeterminate = false,
            type = NotificationType.DOWNLOADING,
            thumbnail = thumbnail,
            videoId = mission.videoId
        )

        // 3. Prepare source file for MP3 conversion
        val sourceFileForConversion: File = when {
            tempAudioFile.exists() && tempAudioFile.length() > 0 -> {
                PTLog.d("AudioDownloadService", "[AudioPipeline] Using downloaded audio stream file (${tempAudioFile.name}, ${tempAudioFile.length()} bytes) for MP3 conversion")
                tempAudioFile
            }
            tempVideoFile.exists() && tempVideoFile.length() > 0 -> {
                // Verify if tempVideoFile actually contains an audio stream
                val inspection = Mp3AudioConverter.inspectMediaFile(tempVideoFile)
                if (inspection.contains("audio", ignoreCase = true) || inspection.contains("sound", ignoreCase = true)) {
                    PTLog.d("AudioDownloadService", "[AudioPipeline] Using video file with embedded audio (${tempVideoFile.name}) for MP3 conversion")
                    tempVideoFile
                } else {
                    throw Exception("Downloaded video stream contains no audio track to convert")
                }
            }
            else -> throw Exception("No source audio/video file available after download")
        }

        // 4. MP3 Conversion Phase
        missionDao.updateStatus(missionId, MissionStatus.MUXING)
        updateNotification(
            missionId = missionId,
            title = mission.title,
            content = "Converting to MP3… 0%",
            progress = 0,
            isIndeterminate = false,
            type = NotificationType.DOWNLOADING,
            thumbnail = thumbnail,
            videoId = mission.videoId
        )

        val sanitizedTitle = sanitizeFilename(mission.title)
        val finalMp3FileName = "$sanitizedTitle.mp3"
        val targetMp3File = File(applicationContext.getExternalFilesDir(null), "${mission.videoId}.mp3")

        if (targetMp3File.exists()) targetMp3File.delete()

        // Prepare temp thumbnail if available for ID3 embedding
        var tempThumbnailFile: File? = null
        if (thumbnail != null) {
            try {
                val tFile = File(cacheDir, "${mission.id}_${mission.videoId}_thumb.jpg")
                if (tFile.exists()) tFile.delete()
                java.io.FileOutputStream(tFile).use { fos ->
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                tempThumbnailFile = tFile
            } catch (e: Exception) {
                PTLog.e("AudioDownloadService", "Failed to save temp thumbnail for MP3 embedding", e)
            }
        }

        // Execute MP3 conversion
        PTLog.d("AudioDownloadService", "[AudioPipeline] Invoking Mp3AudioConverter for $finalMp3FileName...")
        val conversionSuccess = try {
            Mp3AudioConverter.convertToMp3(
                inputFile = sourceFileForConversion,
                outputFile = targetMp3File,
                title = mission.title,
                artist = uploaderName,
                album = "ZyvoTube",
                thumbnailFile = tempThumbnailFile,
                onProgress = { convPercent ->
                    updateNotification(
                        missionId = missionId,
                        title = mission.title,
                        content = "Converting to MP3… $convPercent%",
                        progress = convPercent,
                        isIndeterminate = false,
                        type = NotificationType.DOWNLOADING,
                        thumbnail = thumbnail,
                        videoId = mission.videoId
                    )
                }
            )
        } catch (e: Throwable) {
            PTLog.e("AudioDownloadService", "[AudioPipeline] MP3 conversion exception", e)
            false
        } finally {
            tempThumbnailFile?.let { if (it.exists()) it.delete() }
        }

        // 5. Verification
        if (!conversionSuccess || !targetMp3File.exists() || targetMp3File.length() <= 0) {
            targetMp3File.delete()
            throw Exception("MP3 conversion failed or created an invalid 0-byte output file")
        }

        PTLog.d("AudioDownloadService", "[AudioPipeline] MP3 created successfully: ${targetMp3File.name} (${targetMp3File.length()} bytes)")

        // 6. ONLY AFTER SUCCESSFUL MP3 CREATION AND VERIFICATION: DELETE TEMPORARY VIDEO FILES
        PTLog.d("AudioDownloadService", "[AudioPipeline] Deleting temporary video source files...")
        try { if (tempVideoFile.exists()) tempVideoFile.delete() } catch (_: Exception) {}
        try { if (tempAudioFile.exists()) tempAudioFile.delete() } catch (_: Exception) {}
        try { if (tempMuxedFile.exists()) tempMuxedFile.delete() } catch (_: Exception) {}

        // 7. Final Storage (MediaStore or Internal/External App Storage)
        val isDeviceStorage = mission.outputFilePath == "DEVICE"
        var createdMediaStoreUri: Uri? = null
        val finalStoragePath: String

        if (isDeviceStorage) {
            val resolver = contentResolver
            val relativePath = "Music/ZyvoTube/download/ZyvoTube Audio/"
            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, finalMp3FileName)
                put(MediaStore.Audio.Media.TITLE, mission.title)
                put(MediaStore.Audio.Media.ARTIST, uploaderName)
                put(MediaStore.Audio.Media.ALBUM, "ZyvoTube")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(audioCollection, contentValues)
                ?: throw Exception("Failed to create MediaStore entry for MP3")

            createdMediaStoreUri = uri

            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(targetMp3File).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw Exception("Failed to open output stream for MediaStore MP3")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                finalStoragePath = uri.toString()
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        } else {
            finalStoragePath = targetMp3File.absolutePath
        }

        // 8. Update DB & Complete
        missionDao.updateStatus(missionId, MissionStatus.COMPLETED)
        downloadDao.updateDownload(
            downloadDao.getDownloadById(mission.videoId)!!.copy(
                filePath = finalStoragePath,
                status = DownloadStatus.COMPLETED,
                downloadedSize = targetMp3File.length(),
                totalSize = targetMp3File.length(),
                format = "mp3",
                quality = "MP3 192kbps"
            )
        )

        // 9. Final Notification
        updateNotification(
            missionId = missionId,
            title = "Audio download complete",
            content = finalMp3FileName,
            progress = 100,
            isIndeterminate = false,
            type = NotificationType.COMPLETED,
            thumbnail = thumbnail,
            videoId = mission.videoId
        )

        createdMediaStoreUri
    }

    private fun stopMission(missionId: Long) {
        activeMissions[missionId]?.cancel()
        activeMissions.remove(missionId)
    }

    private fun cancelMission(missionId: Long) {
        activeMissions[missionId]?.cancel()
        activeMissions.remove(missionId)
        serviceScope.launch {
            val mission = missionDao.getMissionById(missionId)
            mission?.let {
                downloadDao.deleteDownload(downloadDao.getDownloadById(it.videoId) ?: return@launch)
                missionDao.deleteMission(it)
            }
            notificationManager.cancel(NOTIFICATION_ID_BASE + (missionId % 100000).toInt())
        }
    }

    private fun promoteToForeground(missionId: Long) {
        foregroundMissionId = missionId
        val mission = serviceScope.launch {
            val m = missionDao.getMissionById(missionId) ?: return@launch
            val notification = createNotificationBuilder(
                missionId = missionId,
                title = m.title,
                content = "Downloading audio… 0%",
                progress = 0,
                isIndeterminate = false,
                type = NotificationType.DOWNLOADING
            ).build()
            startForeground(NOTIFICATION_ID_BASE + (missionId % 100000).toInt(), notification)
        }
    }

    private fun updateNotification(
        missionId: Long,
        title: String,
        content: String,
        progress: Int,
        isIndeterminate: Boolean,
        type: NotificationType,
        thumbnail: Bitmap? = null,
        videoId: String = ""
    ) {
        val notificationId = NOTIFICATION_ID_BASE + (missionId % 100000).toInt()
        val builder = createNotificationBuilder(
            missionId = missionId,
            title = title,
            content = content,
            progress = progress,
            isIndeterminate = isIndeterminate,
            type = type,
            thumbnail = thumbnail,
            videoId = videoId
        )
        notificationManager.notify(notificationId, builder.build())
    }

    private fun createNotificationBuilder(
        missionId: Long,
        title: String,
        content: String,
        progress: Int,
        isIndeterminate: Boolean,
        type: NotificationType,
        thumbnail: Bitmap? = null,
        videoId: String = ""
    ): NotificationCompat.Builder {
        val intent = Intent(this, com.rahul.vibetube1.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "downloads")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (missionId % 100000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(if (type == NotificationType.COMPLETED) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        thumbnail?.let {
            builder.setLargeIcon(it)
        }

        when (type) {
            NotificationType.DOWNLOADING -> {
                builder.setProgress(100, progress, isIndeterminate)
                builder.setOngoing(true)

                val cancelIntent = Intent(this, AudioDownloadService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra("missionId", missionId)
                }
                val cancelPendingIntent = PendingIntent.getService(
                    this,
                    (missionId % 100000).toInt() + 10000,
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            }
            NotificationType.COMPLETED -> {
                builder.setProgress(0, 0, false)
                builder.setOngoing(false)
                builder.setAutoCancel(true)
            }
            NotificationType.FAILED -> {
                builder.setProgress(0, 0, false)
                builder.setOngoing(false)
                builder.setAutoCancel(true)
            }
        }

        return builder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ZyvoTube Audio Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for audio downloads and MP3 conversions"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private suspend fun fetchThumbnailBitmap(videoId: String): Bitmap? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        try {
            val url = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
            val request = okhttp3.Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
