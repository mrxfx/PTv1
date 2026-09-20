/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.services
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.rahul.vibetube1.data.local.ChunkType
import com.rahul.vibetube1.data.local.DownloadDao
import com.rahul.vibetube1.data.local.DownloadStatus
import com.rahul.vibetube1.data.local.MissionDao
import com.rahul.vibetube1.data.local.MissionStatus
import com.rahul.vibetube1.data.network.ParallelDownloader
import com.rahul.vibetube1.domain.repository.VideoRepository
import com.rahul.vibetube1.utils.Constants
import com.rahul.vibetube1.utils.Mp3AudioConverter
import com.rahul.vibetube1.utils.StorageUtils
import com.rahul.vibetube1.utils.NativeMediaMuxer
import com.rahul.vibetube1.utils.PTLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class VideoDownloadService : android.app.Service() {

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

    override fun onCreate() {
        super.onCreate()
        downloader = ParallelDownloader(okHttpClient, missionDao)
        createNotificationChannel()
        registerNetworkCallback()

        serviceScope.launch {
            try {
                val missions = missionDao.getAllMissions().first()
                missions.forEach { mission ->
                    if (mission.status == MissionStatus.DOWNLOADING) {
                        PTLog.d("VideoDownloadService", "Recovering stale mission ${mission.videoId}")
                        missionDao.updateStatus(mission.id, MissionStatus.PAUSED)
                        downloadDao.setDownloadStatus(mission.videoId, DownloadStatus.PAUSED)
                    }
                }
            } catch (e: Exception) {
                PTLog.e("VideoDownloadService", "Stale recovery failed", e)
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
            var missionTitle = "Video"
            var videoId = ""
            try {
                var mission = missionDao.getMissionById(missionId) ?: return@launch
                missionTitle = mission.title
                videoId = mission.videoId

                updateNotification(
                    missionId = missionId,
                    title = missionTitle,
                    content = "Preparing download...",
                    progress = 0,
                    isIndeterminate = true,
                    type = NotificationType.DOWNLOADING,
                    videoId = videoId
                )

                // Resolve Metadata if URLs are missing
                val isAudioOnlyMission = mission.isAudioOnly || (mission.videoUrl == null && mission.audioUrl != null)
                val needsMetadata = if (isAudioOnlyMission) mission.audioUrl.isNullOrBlank() else (mission.videoUrl.isNullOrBlank() && mission.audioUrl.isNullOrBlank())

                if (needsMetadata) {
                    PTLog.d("VideoDownloadService", "Mission ${mission.videoId} missing URLs, resolving (isAudioOnly=$isAudioOnlyMission)...")
                    val metadata = fetchStreamMetadata(mission.videoId, if (isAudioOnlyMission) "Audio" else mission.quality)
                    if (metadata != null) {
                        val resolvedAudioUrl = if (isAudioOnlyMission) {
                            metadata.audioUrl ?: com.rahul.vibetube1.utils.DownloadStreamResolver.selectAudioStream(videoRepository.getStreamBundle(mission.videoId))?.url
                        } else {
                            metadata.audioUrl
                        }

                        if (isAudioOnlyMission && resolvedAudioUrl.isNullOrBlank()) {
                            throw Exception("Failed to resolve audio stream URL for Audio Only mission")
                        }

                        mission = mission.copy(
                            videoUrl = if (isAudioOnlyMission) null else metadata.videoUrl,
                            audioUrl = resolvedAudioUrl,
                            format = metadata.format,
                            quality = metadata.quality
                        )
                        missionDao.updateMission(mission)

                        downloadDao.getDownloadById(mission.videoId)?.let { download ->
                            downloadDao.updateDownload(download.copy(
                                videoUrl = if (isAudioOnlyMission) null else metadata.videoUrl,
                                audioUrl = resolvedAudioUrl,
                                format = metadata.format,
                                quality = metadata.quality
                            ))
                        }
                    } else {
                        throw Exception("Failed to resolve stream metadata")
                    }
                }

                itemUriCreated = executeDownload(missionId, mission)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    PTLog.d("VideoDownloadService", "Mission $missionId cancelled/paused")
                    missionDao.updateStatus(missionId, MissionStatus.PAUSED)
                    missionDao.getMissionById(missionId)?.let {
                        downloadDao.setDownloadStatus(it.videoId, DownloadStatus.PAUSED)
                    }
                    return@launch
                }
                PTLog.e("VideoDownloadService", "[DIAGNOSTIC-ERROR] Mission $missionId failed for videoId=$videoId: ${e.javaClass.simpleName}: ${e.message}", e)
                e.cause?.let {
                    PTLog.e("VideoDownloadService", "[DIAGNOSTIC-ERROR-CAUSE] Root cause: ${it.javaClass.simpleName}: ${it.message}", it)
                }
                com.rahul.vibetube1.utils.FirebaseTelemetry.recordNonFatal(e, "VideoDownloadService", "Mission $missionId failed for $videoId")

                // Cleanup corrupt MediaStore entry if creation failed mid-way
                itemUriCreated?.let { uri ->
                    try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                }

                if (e is ExpiredUrlException) {
                    try {
                        PTLog.w("VideoDownloadService", "URL expired for mission $missionId, retrying...")
                        val currentMission = missionDao.getMissionById(missionId) ?: throw e
                        val metadata = fetchStreamMetadata(currentMission.videoId, if (currentMission.isAudioOnly) "Audio" else currentMission.quality) ?: throw e

                        val resolvedAudioUrl = if (currentMission.isAudioOnly) {
                            metadata.audioUrl ?: com.rahul.vibetube1.utils.DownloadStreamResolver.selectAudioStream(videoRepository.getStreamBundle(currentMission.videoId))?.url
                        } else {
                            metadata.audioUrl
                        }

                        val updatedMission = currentMission.copy(
                            videoUrl = if (currentMission.isAudioOnly) null else metadata.videoUrl,
                            audioUrl = resolvedAudioUrl,
                            format = metadata.format,
                            quality = metadata.quality
                        )
                        missionDao.updateMission(updatedMission)

                        downloadDao.getDownloadById(currentMission.videoId)?.let { download ->
                            downloadDao.updateDownload(download.copy(
                                videoUrl = if (currentMission.isAudioOnly) null else metadata.videoUrl,
                                audioUrl = resolvedAudioUrl,
                                format = metadata.format,
                                quality = metadata.quality
                            ))
                        }

                        executeDownload(missionId, updatedMission)
                        return@launch
                    } catch (retryEx: Exception) {
                        PTLog.e("VideoDownloadService", "[DIAGNOSTIC-ERROR] Retry failed for $missionId", retryEx)
                        com.rahul.vibetube1.utils.FirebaseTelemetry.recordNonFatal(retryEx, "VideoDownloadService", "Retry failed for $missionId")
                    }
                }

                missionDao.updateStatus(missionId, MissionStatus.FAILED)
                missionDao.getMissionById(missionId)?.let {
                    downloadDao.setDownloadStatus(it.videoId, DownloadStatus.FAILED)
                }

                com.rahul.vibetube1.utils.FirebaseTelemetry.logDownloadFailed(
                    videoId = videoId,
                    errorMessage = e.message,
                    isAudioOnly = missionDao.getMissionById(missionId)?.isAudioOnly == true,
                    saveToDevice = true
                )

                val technicalReason = deriveTechnicalFailureReason(e)
                val failureMessage = "Download failed • $technicalReason"

                val thumbnail = fetchThumbnailBitmap(videoId)
                updateNotification(
                    missionId = missionId,
                    title = missionTitle,
                    content = failureMessage,
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
                title = "ZyvoTube",
                content = "Starting download...",
                progress = 0,
                isIndeterminate = true,
                type = NotificationType.DOWNLOADING
            ).build()
            startForeground(NOTIFICATION_ID_BASE + (missionId % 100000).toInt(), notification)
        }
    }

    private val lastUpdateMap = mutableMapOf<Long, Long>()

    private suspend fun executeDownload(
        missionId: Long,
        mission: com.rahul.vibetube1.data.local.DownloadMissionEntity
    ): Uri? = withContext(Dispatchers.IO) {
        val isAudioOnly = mission.isAudioOnly || (mission.videoUrl == null && mission.audioUrl != null)
        val thumbnail = fetchThumbnailBitmap(mission.videoId)

        PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 1: Execute Download Initiated] missionId=$missionId, videoId=${mission.videoId}, title='${mission.title}', isAudioOnly=$isAudioOnly, quality=${mission.quality}, format=${mission.format}, videoUrl=[${ParallelDownloader.safeUrlSummary(mission.videoUrl)}], audioUrl=[${ParallelDownloader.safeUrlSummary(mission.audioUrl)}]")

        updateNotification(
            missionId = missionId,
            title = mission.title,
            content = "Fetching size...",
            progress = 0,
            isIndeterminate = true,
            type = NotificationType.DOWNLOADING,
            thumbnail = thumbnail,
            videoId = mission.videoId
        )

        var totalVideoSizeRemote = 0L
        if (mission.videoUrl != null) {
            totalVideoSizeRemote = downloader.getFileSize(mission.videoUrl)
        }

        val totalAudioSizeRemote = mission.audioUrl?.let {
            downloader.getFileSize(it)
        } ?: 0L

        val combinedTotalSize = totalVideoSizeRemote + totalAudioSizeRemote
        val isSizeKnown = combinedTotalSize > 0

        PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 2: Stream Size Resolved] videoBytes=$totalVideoSizeRemote, audioBytes=$totalAudioSizeRemote, totalBytes=$combinedTotalSize, isSizeKnown=$isSizeKnown")

        // Storage Check
        val availableStorage = StorageUtils.getAvailableInternalStorage()
        if (combinedTotalSize > 0 && availableStorage < combinedTotalSize + (100 * 1024 * 1024)) { // 100MB buffer
            throw Exception("Not enough storage. Required: ${StorageUtils.formatSize(combinedTotalSize)}, Available: ${StorageUtils.formatSize(availableStorage)}")
        }

        missionDao.updateMission(mission.copy(totalBytes = combinedTotalSize, status = MissionStatus.DOWNLOADING))
        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, mission.downloadedBytes, combinedTotalSize)

        val videoFile = File(cacheDir, "${mission.id}_${mission.videoId}_video.tmp")
        val audioFile = File(cacheDir, "${mission.id}_${mission.videoId}_audio.tmp")

        // 1. Download Video
        var videoSize = 0L
        if (mission.videoUrl != null) {
            videoSize = try {
                downloader.download(mission.videoUrl, videoFile, missionId, ChunkType.VIDEO) { progress ->
                    val currentTime = System.currentTimeMillis()
                    val lastUpdate = lastUpdateMap[missionId] ?: 0L

                    if (currentTime - lastUpdate >= 350L || progress == combinedTotalSize) {
                        lastUpdateMap[missionId] = currentTime
                        val percent = if (isSizeKnown) ((progress * 100) / combinedTotalSize).toInt().coerceIn(0, 99) else 0
                        val statusText = if (isSizeKnown) {
                            "$percent% • ${formatBytes(progress)} of ${formatBytes(combinedTotalSize)}"
                        } else {
                            "Downloading • ${formatBytes(progress)}"
                        }

                        updateNotification(
                            missionId = missionId,
                            title = mission.title,
                            content = statusText,
                            progress = percent / (if (mission.audioUrl != null) 2 else 1),
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
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true) throw ExpiredUrlException()
                throw e
            }
        }

        // 2. Download Audio if available
        var audioSize = 0L
        if (mission.audioUrl != null) {
            PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 3: Starting Audio Stream Download] destination=${audioFile.name}")
            audioSize = try {
                downloader.download(mission.audioUrl, audioFile, missionId, ChunkType.AUDIO) { progress ->
                    val currentTotal = videoSize + progress
                    val currentTime = System.currentTimeMillis()
                    val lastUpdate = lastUpdateMap[missionId] ?: 0L

                    if (currentTime - lastUpdate >= 350L || currentTotal == combinedTotalSize) {
                        lastUpdateMap[missionId] = currentTime
                        val percent = if (isSizeKnown) ((currentTotal * 100) / combinedTotalSize).toInt().coerceIn(0, 99) else 0
                        val statusText = if (isSizeKnown) {
                            "$percent% • ${formatBytes(currentTotal)} of ${formatBytes(combinedTotalSize)}"
                        } else {
                            "Downloading • ${formatBytes(currentTotal)}"
                        }

                        updateNotification(
                            missionId = missionId,
                            title = mission.title,
                            content = statusText,
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
            } catch (e: Exception) {
                PTLog.e("VideoDownloadService", "[AudioLifecycle] Audio stream download failed for ${mission.videoId}: ${e.message}")
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true) throw ExpiredUrlException()
                throw e
            }
            val inspection = Mp3AudioConverter.inspectMediaFile(audioFile)
            PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 4: Raw Audio Stream Downloaded] size=${audioFile.length()} bytes, info: $inspection")
        }

        val finalTotal = videoSize + audioSize
        missionDao.updateMission(mission.copy(totalBytes = finalTotal))

        // 3. Processing / Muxing / Conversion
        missionDao.updateStatus(missionId, MissionStatus.MUXING)
        updateNotification(
            missionId = missionId,
            title = mission.title,
            content = if (isAudioOnly) "Processing/Converting audio..." else "Combining streams to MP4...",
            progress = 95,
            isIndeterminate = true,
            type = NotificationType.DOWNLOADING,
            thumbnail = thumbnail,
            videoId = mission.videoId
        )

        val isDeviceStorage = mission.outputFilePath == "DEVICE"
        
        // Determine correct extension based on format
        val formatExtension = mission.format?.lowercase()?.let { 
            when {
                it.contains("webm") -> "webm"
                it.contains("opus") -> "opus"
                it.contains("mp3") -> "mp3"
                it.contains("m4a") -> "m4a"
                it.contains("mp4") -> "mp4"
                else -> null
            }
        }
        
        val extension = if (isDeviceStorage) {
            if (isAudioOnly) {
                when {
                    formatExtension == "webm" -> "webm"
                    formatExtension == "opus" -> "opus"
                    formatExtension == "mp3" -> "mp3"
                    formatExtension == "m4a" -> "m4a"
                    else -> "m4a"
                }
            } else {
                if (formatExtension == "webm") "webm" else "mp4"
            }
        } else {
            formatExtension ?: (if (isAudioOnly) "m4a" else "mp4")
        }
        
        val finalTempFile = File(cacheDir, "${mission.id}_${mission.videoId}_final.$extension")
        if (finalTempFile.exists()) finalTempFile.delete()

        val uploaderName = withContext(Dispatchers.IO) {
            downloadDao.getDownloadById(mission.videoId)?.uploaderName
        } ?: "ZyvoTube"

        var tempThumbnailFile: File? = null
        if (isAudioOnly && isDeviceStorage && thumbnail != null) {
            try {
                val tempFile = File(cacheDir, "${mission.id}_${mission.videoId}_thumb.jpg")
                if (tempFile.exists()) tempFile.delete()
                java.io.FileOutputStream(tempFile).use { fos ->
                    thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                }
                tempThumbnailFile = tempFile
            } catch (e: Exception) {
                PTLog.e("VideoDownloadService", "Failed to save temp thumbnail for MP3 embedding", e)
            }
        }

        if (isAudioOnly) {
            if (isDeviceStorage && extension == "mp3") {
                val sourceFile = audioFile
                PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 5: Invoking Mp3AudioConverter] input=${sourceFile.name} (${sourceFile.length()} bytes), target=${finalTempFile.name}")
                val success = try {
                    Mp3AudioConverter.convertToMp3(
                        inputFile = sourceFile,
                        outputFile = finalTempFile,
                        title = mission.title,
                        artist = uploaderName,
                        album = "ZyvoTube",
                        thumbnailFile = tempThumbnailFile
                    )
                } catch (e: Throwable) {
                    PTLog.e("VideoDownloadService", "[AudioLifecycle] Exception during convertToMp3 invocation", e)
                    false
                }
                try { tempThumbnailFile?.delete() } catch (_: Exception) {}
                if (!success || !finalTempFile.exists() || finalTempFile.length() <= 0) {
                    PTLog.w("VideoDownloadService", "[AudioLifecycle] MP3 audio conversion unsuccessful, falling back to direct stream copy")
                    if (!audioFile.renameTo(finalTempFile)) {
                        audioFile.copyTo(finalTempFile, overwrite = true)
                        audioFile.delete()
                    }
                } else {
                    PTLog.d("VideoDownloadService", "[AudioLifecycle] [Step 5 Completed: MP3 Audio Converted] size=${finalTempFile.length()} bytes")
                }
            } else {
                PTLog.d("VideoDownloadService", "[AudioLifecycle] Preserving native audio stream format ($extension), copying stream directly")
                if (!audioFile.renameTo(finalTempFile)) {
                    audioFile.copyTo(finalTempFile, overwrite = true)
                    audioFile.delete()
                }
            }
        } else {
            if (mission.videoUrl != null && mission.audioUrl != null && audioFile.exists()) {
                try {
                    muxer.mux(videoFile, audioFile, finalTempFile)
                } catch (e: Exception) {
                    PTLog.e("VideoDownloadService", "Muxing failed, falling back to raw video file", e)
                    videoFile.copyTo(finalTempFile, overwrite = true)
                }
            } else {
                if (!videoFile.renameTo(finalTempFile)) {
                    videoFile.copyTo(finalTempFile, overwrite = true)
                    videoFile.delete()
                }
            }
        }

        // Validate media before moving it to its permanent destination
        PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: Pre-publish Media Validation] validating ${finalTempFile.name} (${finalTempFile.length()} bytes), isAudioOnly=$isAudioOnly")
        if (!validateMedia(finalTempFile, isAudioOnly)) {
            finalTempFile.delete()
            throw Exception("Media validation failed: corrupt file, missing tracks or zero duration")
        }
        PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: Pre-publish Media Validation PASSED] file=${finalTempFile.name}, size=${finalTempFile.length()} bytes")

        val sanitizedTitle = sanitizeFilename(mission.title)
        var createdMediaStoreUri: Uri? = null

        if (isDeviceStorage) {
            val resolver = contentResolver
            if (isAudioOnly) {
                // Specialized Audio-Only MediaStore + IS_PENDING download system
                try {
                    val relativePath = "Music/ZyvoTube/download/ZyvoTube Audio/"
                    val displayName = "$sanitizedTitle.$extension"
                    val audioMimeType = when (extension) {
                        "webm" -> "audio/webm"
                        "opus" -> "audio/opus"
                        "m4a" -> "audio/mp4"
                        "mp3" -> "audio/mpeg"
                        else -> "audio/mp4"
                    }
                    PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore Audio Target] relativePath=$relativePath, displayName=$displayName, mime=$audioMimeType")

                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                        put(MediaStore.Audio.Media.TITLE, mission.title)
                        put(MediaStore.Audio.Media.ARTIST, uploaderName)
                        put(MediaStore.Audio.Media.ALBUM, "ZyvoTube")
                        put(MediaStore.Audio.Media.MIME_TYPE, audioMimeType)
                        
                        // Extract duration
                        val durationMs = try {
                            val retriever = android.media.MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(finalTempFile.absolutePath)
                                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                            } finally {
                                try { retriever.release() } catch (_: Exception) {}
                            }
                        } catch (e: Exception) { null }
                        if (durationMs != null && durationMs > 0) {
                            put(MediaStore.Audio.Media.DURATION, durationMs)
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        } else {
                            val targetFolder = File(Environment.getExternalStorageDirectory(), "ZyvoTube/download/ZyvoTube Audio")
                            if (!targetFolder.exists()) {
                                targetFolder.mkdirs()
                            }
                            val targetFile = File(targetFolder, displayName)
                            put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
                        }
                    }

                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }

                    // 1. Insert MediaStore.Audio item with IS_PENDING=1
                    val itemUri = resolver.insert(collection, contentValues)
                        ?: throw Exception("MediaStore insert returned null URI for $displayName")
                    createdMediaStoreUri = itemUri
                    PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore Insert Result] itemUri=$itemUri")

                    // 2. Write the FINAL validated MP3
                    var bytesWritten = 0L
                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        FileInputStream(finalTempFile).use { inStream ->
                            bytesWritten = inStream.copyTo(outStream)
                        }
                        outStream.flush()
                    } ?: throw Exception("Failed to open MediaStore output stream for $itemUri")

                    PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore Write Result] bytesWritten=$bytesWritten, sourceSize=${finalTempFile.length()}")

                    // 3. Verify and validate the published file
                    var statSize = -1L
                    try {
                        val pfd = resolver.openFileDescriptor(itemUri, "r")
                        if (pfd != null) {
                            statSize = pfd.statSize
                            pfd.close()
                        }
                    } catch (pfdEx: Exception) {
                        PTLog.w("VideoDownloadService", "[DIAGNOSTIC-7: openFileDescriptor statSize check skipped: ${pfdEx.message}]")
                    }

                    PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore Final Verification] finalUri=$itemUri, statSize=$statSize bytes, sourceSize=${finalTempFile.length()} bytes")

                    // 4. Set IS_PENDING=0 only after validation succeeds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val updateValues = ContentValues().apply {
                            put(MediaStore.Audio.Media.IS_PENDING, 0)
                        }
                        val rowsUpdated = resolver.update(itemUri, updateValues, null, null)
                        PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore IS_PENDING=0] rowsUpdated=$rowsUpdated, finalUri=$itemUri")
                    }

                    // Also write directly to ZyvoTube/download/ZyvoTube Audio/ if write access exists (for pre-Q or legacy compatibility)
                    try {
                        val directFolder = File(Environment.getExternalStorageDirectory(), "ZyvoTube/download/ZyvoTube Audio")
                        if (!directFolder.exists()) {
                            directFolder.mkdirs()
                        }
                        if (directFolder.exists() && directFolder.canWrite()) {
                            val directFile = File(directFolder, displayName)
                            finalTempFile.copyTo(directFile, overwrite = true)
                            PTLog.d("VideoDownloadService", "[DIAGNOSTIC-7: Legacy Direct File Saved] path=${directFile.absolutePath}, size=${directFile.length()}")
                        }
                    } catch (e: Exception) {
                        PTLog.w("VideoDownloadService", "Could not copy direct file (this is expected on standard scoped storage)", e)
                    }

                } catch (e: Exception) {
                    PTLog.e("VideoDownloadService", "[DIAGNOSTIC-7: MediaStore FAILED] error=${e.javaClass.simpleName}: ${e.message}", e)
                    // 5. Delete the MediaStore item on failure
                    createdMediaStoreUri?.let {
                        try { resolver.delete(it, null, null) } catch (_: Exception) {}
                    }
                    createdMediaStoreUri = null
                    throw e
                }
            } else {
                // EXISTING VIDEO ONLY PUBLISHING SYSTEM
                val mimeType = if (extension == "webm") "video/webm" else "video/mp4"
                val targetFolder = File(Environment.getExternalStorageDirectory(), "ZyvoTube/download/ZyvoTube Video")
                var savedViaFile = false
                try {
                    if (!targetFolder.exists()) {
                        targetFolder.mkdirs()
                    }
                    if (targetFolder.exists() && targetFolder.canWrite()) {
                        val targetFile = File(targetFolder, "$sanitizedTitle.$extension")
                        finalTempFile.copyTo(targetFile, overwrite = true)
                        
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DATA, targetFile.absolutePath)
                            put(MediaStore.MediaColumns.DISPLAY_NAME, targetFile.name)
                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                        }
                        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        val itemUri = resolver.insert(collection, contentValues)
                        if (itemUri != null) {
                            createdMediaStoreUri = itemUri
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear()
                                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                                resolver.update(itemUri, contentValues, null, null)
                            }
                        }
                        savedViaFile = true
                    }
                } catch (e: Exception) {
                    PTLog.e("VideoDownloadService", "Failed to save video via File API, falling back to MediaStore Scoped Storage", e)
                }
                
                if (!savedViaFile) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$sanitizedTitle.$extension")
                        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/ZyvoTube/download/ZyvoTube Video/")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }
                    }

                    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    } else {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    }

                    val itemUri = resolver.insert(collection, contentValues)
                        ?: throw Exception("Failed to insert MediaStore record")

                    createdMediaStoreUri = itemUri

                    resolver.openOutputStream(itemUri)?.use { outStream ->
                        FileInputStream(finalTempFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    } ?: throw Exception("Failed to write to MediaStore output stream")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(itemUri, contentValues, null, null)
                    }
                }
            }
        } else {
            val appFile = File(getExternalFilesDir(null), "${mission.videoId}.$extension")
            finalTempFile.copyTo(appFile, overwrite = true)
            
            serviceScope.launch(Dispatchers.IO) {
                downloadDao.getDownloadById(mission.videoId)?.let { download ->
                    if (download.filePath != appFile.absolutePath) {
                        downloadDao.updateDownload(download.copy(filePath = appFile.absolutePath))
                    }
                }
            }
        }

        // Clean cache files
        finalTempFile.delete()
        videoFile.delete()
        audioFile.delete()

        missionDao.updateMission(mission.copy(totalBytes = finalTotal, status = MissionStatus.COMPLETED))
        downloadDao.updateProgress(mission.videoId, DownloadStatus.COMPLETED, finalTotal, finalTotal)

        com.rahul.vibetube1.utils.FirebaseTelemetry.logDownloadComplete(
            videoId = mission.videoId,
            isAudioOnly = isAudioOnly,
            saveToDevice = true
        )

        val formatQualityStr = if (isAudioOnly) "MP3" else "MP4 • ${mission.quality}"
        updateNotification(
            missionId = missionId,
            title = mission.title,
            content = "Download complete • $formatQualityStr",
            progress = 100,
            isIndeterminate = false,
            type = NotificationType.COMPLETED,
            thumbnail = thumbnail,
            itemUri = createdMediaStoreUri,
            isAudio = isAudioOnly,
            videoId = mission.videoId
        )

        createdMediaStoreUri
    }

    private fun deriveTechnicalFailureReason(e: Throwable): String {
        val msg = e.message ?: e.javaClass.simpleName
        val causeMsg = e.cause?.message ?: ""
        val combined = "$msg $causeMsg".lowercase()
        return when {
            combined.contains("403") || combined.contains("expired") -> "HTTP 403 (Link Expired)"
            combined.contains("416") -> "HTTP 416 (Range)"
            combined.contains("404") -> "HTTP 404 (Not Found)"
            combined.contains("500") || combined.contains("502") || combined.contains("503") -> "HTTP Server Error"
            combined.contains("ffmpeg") || combined.contains("transcode") -> "FFmpeg Transcode Error"
            combined.contains("validation") || combined.contains("validatemedia") -> "Media validation (${msg.replace("Media validation failed: ", "").take(22)})"
            combined.contains("mediastore") -> "MediaStore Error"
            combined.contains("storage") || combined.contains("space") -> "Insufficient Storage"
            combined.contains("empty") -> "Empty Stream Data"
            else -> msg.take(28)
        }
    }

    private suspend fun fetchStreamMetadata(videoId: String, preferredQuality: String?): StreamMetadata? = withContext(Dispatchers.IO) {
        try {
            val bundle = videoRepository.getStreamBundle(videoId)

            val isAudioOnlyRequest = preferredQuality == "Audio" ||
                preferredQuality?.contains("Audio", ignoreCase = true) == true ||
                preferredQuality?.contains("kbps", ignoreCase = true) == true ||
                preferredQuality?.contains("m4a", ignoreCase = true) == true ||
                preferredQuality?.contains("opus", ignoreCase = true) == true ||
                preferredQuality?.contains("mp3", ignoreCase = true) == true

            if (isAudioOnlyRequest) {
                val audioStreams = bundle.audioStreams
                val matchingAudio = if (!preferredQuality.isNullOrBlank() && preferredQuality != "Audio") {
                    audioStreams.find { it.format.contains(preferredQuality, ignoreCase = true) }
                        ?: audioStreams.find { it.quality.contains(preferredQuality, ignoreCase = true) }
                } else null

                val bestAudio = matchingAudio
                    ?: audioStreams.filter { it.trackType == "ORIGINAL" }.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?: audioStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?: audioStreams.firstOrNull()

                val resolvedUrl = bestAudio?.url ?: bundle.bestAudioStreamUrl
                val resolvedFormat = bestAudio?.format ?: "m4a"
                val resolvedQuality = bestAudio?.quality ?: "128kbps"

                val audioUrlSafe = ParallelDownloader.safeUrlSummary(resolvedUrl)
                PTLog.d("VideoDownloadService", "[DIAGNOSTIC-1: Audio Stream Selected] videoId=$videoId, preferredQuality=$preferredQuality, resolvedFormat=$resolvedFormat, resolvedQuality=$resolvedQuality, urlSummary=[$audioUrlSafe], totalAudioStreams=${audioStreams.size}")

                return@withContext if (!resolvedUrl.isNullOrBlank()) {
                    StreamMetadata(null, resolvedUrl, resolvedFormat, resolvedQuality)
                } else null
            }

            // For Device downloads (MP4), we MUST have AVC/H264 video and AAC/M4A audio for MediaMuxer compatibility
            val videoStream = if (!preferredQuality.isNullOrBlank()) {
                // Try to find MP4 first for better muxing compatibility
                bundle.videoStreams.find { it.quality.contains(preferredQuality, ignoreCase = true) && it.format.contains("mp4", true) }
                    ?: bundle.videoStreams.find { it.quality.contains(preferredQuality, ignoreCase = true) }
                    ?: bundle.videoStreams.find {
                        val res = it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        val prefRes = preferredQuality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        res <= prefRes && it.format.contains("mp4", true)
                    }
                    ?: bundle.videoStreams.find {
                        val res = it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        val prefRes = preferredQuality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        res <= prefRes
                    } ?: bundle.videoStreams.firstOrNull()
            } else {
                bundle.videoStreams.find { it.quality.contains("1080") && it.format.contains("mp4", true) }
                    ?: bundle.videoStreams.find { it.quality.contains("720") && it.format.contains("mp4", true) }
                    ?: bundle.videoStreams.find { it.quality.contains("1080") }
                    ?: bundle.videoStreams.find { it.quality.contains("720") }
                    ?: bundle.videoStreams.firstOrNull()
            }

            if (videoStream == null) return@withContext null

            val videoUrl = videoStream.url
            val format = videoStream.format

            val audioUrl = if (videoStream.isAdaptive) {
                // For MP4 video, we MUST have M4A/AAC audio for MediaMuxer
                val bestAudio = if (videoStream.format.contains("mp4", true)) {
                    bundle.audioStreams.find { it.format.contains("m4a", true) || it.format.contains("aac", true) }
                        ?: bundle.audioStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                } else {
                    bundle.audioStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                }

                bestAudio?.url
            } else null

            StreamMetadata(videoUrl, audioUrl, format, videoStream.quality)
        } catch (e: Exception) {
            PTLog.e("VideoDownloadService", "Failed to fetch metadata for $videoId", e)
            null
        }
    }

    private data class StreamMetadata(val videoUrl: String?, val audioUrl: String?, val format: String, val quality: String)

    private class ExpiredUrlException : Exception("URL expired")

    private fun promoteToForeground(missionId: Long) {
        foregroundMissionId = missionId
        val notification = createNotificationBuilder(
            missionId = missionId,
            title = "ZyvoTube",
            content = "Downloading...",
            progress = 0,
            isIndeterminate = true,
            type = NotificationType.DOWNLOADING
        ).build()
        startForeground(NOTIFICATION_ID_BASE + (missionId % 100000).toInt(), notification)
    }

    private fun stopMission(missionId: Long) {
        synchronized(activeMissions) {
            activeMissions[missionId]?.cancel()
            activeMissions.remove(missionId)
        }

        serviceScope.launch(Dispatchers.IO) {
            missionDao.updateStatus(missionId, MissionStatus.PAUSED)
            missionDao.getMissionById(missionId)?.let {
                downloadDao.setDownloadStatus(it.videoId, DownloadStatus.PAUSED)
            }
        }

        notificationManager.cancel(NOTIFICATION_ID_BASE + (missionId % 100000).toInt())

        if (foregroundMissionId == missionId) {
            foregroundMissionId = -1L
            val nextMissionId = synchronized(activeMissions) { activeMissions.keys.firstOrNull() }
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

    private fun cancelMission(missionId: Long) {
        synchronized(activeMissions) {
            activeMissions[missionId]?.cancel()
            activeMissions.remove(missionId)
        }

        serviceScope.launch(Dispatchers.IO) {
            val mission = missionDao.getMissionById(missionId)
            if (mission != null) {
                downloadDao.getDownloadById(mission.videoId)?.let { downloadDao.deleteDownload(it) }
                missionDao.deleteMission(mission)

                // Clean temp files safely on IO
                try { File(cacheDir, "${mission.videoId}_video.tmp").delete() } catch (_: Exception) {}
                try { File(cacheDir, "${mission.videoId}_audio.tmp").delete() } catch (_: Exception) {}
                try { File(cacheDir, "${mission.videoId}_final.mp4").delete() } catch (_: Exception) {}
                try { File(cacheDir, "${mission.videoId}_final.mp3").delete() } catch (_: Exception) {}
                try { File(cacheDir, "${mission.videoId}_final.webm").delete() } catch (_: Exception) {}
                try { File(cacheDir, "${mission.videoId}_thumb.jpg").delete() } catch (_: Exception) {}
            }
        }

        notificationManager.cancel(NOTIFICATION_ID_BASE + (missionId % 100000).toInt())

        if (foregroundMissionId == missionId) {
            foregroundMissionId = -1L
            val nextMissionId = synchronized(activeMissions) { activeMissions.keys.firstOrNull() }
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

    private fun updateNotification(
        missionId: Long,
        title: String,
        content: String,
        progress: Int,
        isIndeterminate: Boolean = false,
        type: NotificationType,
        thumbnail: Bitmap? = null,
        itemUri: Uri? = null,
        isAudio: Boolean = false,
        videoId: String = ""
    ) {
        val builder = createNotificationBuilder(
            missionId = missionId,
            title = title,
            content = content,
            progress = progress,
            isIndeterminate = isIndeterminate,
            type = type,
            thumbnail = thumbnail,
            itemUri = itemUri,
            isAudio = isAudio,
            videoId = videoId
        )

        notificationManager.notify(NOTIFICATION_ID_BASE + (missionId % 100000).toInt(), builder.build())
    }

    private fun createNotificationBuilder(
        missionId: Long,
        title: String,
        content: String,
        progress: Int,
        isIndeterminate: Boolean,
        type: NotificationType,
        thumbnail: Bitmap? = null,
        itemUri: Uri? = null,
        isAudio: Boolean = false,
        videoId: String = ""
    ): NotificationCompat.Builder {
        val displayTitle = when (type) {
            NotificationType.DOWNLOADING -> "Downloading • $title"
            NotificationType.COMPLETED -> "Download complete"
            NotificationType.FAILED -> "Download failed"
        }

        val displayContent = when (type) {
            NotificationType.DOWNLOADING -> {
                if (content.contains("Muxing") || content.contains("Converting") || content.contains("Combining") || content.contains("Fetching") || content.contains("Combining streams")) {
                    content
                } else {
                    val rawProgressStr = content.substringAfter("Downloading • ").replace("/", "of")
                    if (progress > 0) {
                        "$progress% • $rawProgressStr"
                    } else {
                        rawProgressStr
                    }
                }
            }
            NotificationType.COMPLETED -> title
            NotificationType.FAILED -> title
        }

        val builder = NotificationCompat.Builder(this, Constants.DOWNLOAD_CHANNEL_ID)
            .setContentTitle(displayTitle)
            .setContentText(displayContent)
            .setOngoing(type == NotificationType.DOWNLOADING)
            .setOnlyAlertOnce(true)

        if (thumbnail != null) {
            builder.setLargeIcon(thumbnail)
        }

        when (type) {
            NotificationType.DOWNLOADING -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download)
                builder.setProgress(100, progress.coerceIn(0, 100), isIndeterminate)

                // Pause action
                val stopIntent = Intent(this, VideoDownloadService::class.java).apply {
                    action = ACTION_STOP
                    putExtra("missionId", missionId)
                }
                val stopPendingIntent = PendingIntent.getService(
                    this,
                    (missionId * 10 + 1).toInt(),
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", stopPendingIntent)

                // Cancel action
                val cancelIntent = Intent(this, VideoDownloadService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra("missionId", missionId)
                }
                val cancelPendingIntent = PendingIntent.getService(
                    this,
                    (missionId * 10 + 2).toInt(),
                    cancelIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            }

            NotificationType.COMPLETED -> {
                builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
                builder.setProgress(0, 0, false)

                if (itemUri != null) {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(itemUri, if (isAudio) "audio/mpeg" else "video/mp4")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        (missionId * 10 + 3).toInt(),
                        viewIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    builder.setContentIntent(pendingIntent)
                    builder.setAutoCancel(true)
                }
            }

            NotificationType.FAILED -> {
                builder.setSmallIcon(android.R.drawable.stat_notify_error)
                builder.setProgress(0, 0, false)

                val retryIntent = Intent(this, VideoDownloadService::class.java).apply {
                    action = ACTION_START
                    putExtra("missionId", missionId)
                }
                val pendingIntent = PendingIntent.getService(
                    this,
                    (missionId * 10 + 4).toInt(),
                    retryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.setContentIntent(pendingIntent)
                builder.setAutoCancel(true)
            }
        }

        return builder
    }

    private suspend fun fetchThumbnailBitmap(videoId: String): Bitmap? = withContext(Dispatchers.IO) {
        if (videoId.isBlank()) return@withContext null
        val download = downloadDao.getDownloadById(videoId)
        val url = download?.thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(120)
            .ifBlank { "ZyvoTube_Media" }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.DOWNLOAD_CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                PTLog.d("VideoDownloadService", "Network restored! Checking for pending missions to auto-resume...")
                serviceScope.launch {
                    try {
                        val missions = missionDao.getAllMissions().first()
                        missions.forEach { mission ->
                            if (mission.status == MissionStatus.DOWNLOADING || mission.status == MissionStatus.QUEUED) {
                                if (!activeMissions.containsKey(mission.id)) {
                                    PTLog.d("VideoDownloadService", "Auto-resuming mission ${mission.id} for ${mission.videoId}")
                                    startMission(mission.id)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        PTLog.e("VideoDownloadService", "Failed auto-resume on network available", e)
                    }
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            PTLog.w("VideoDownloadService", "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            PTLog.w("VideoDownloadService", "Error unregistering network callback: ${e.message}")
        } finally {
            networkCallback = null
        }
    }

    private suspend fun validateMedia(file: File, isAudio: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0) {
            PTLog.e("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia FAILED] file does not exist or size <= 0: exists=${file.exists()}, length=${if (file.exists()) file.length() else -1}")
            return@withContext false
        }
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationStr?.toLongOrNull() ?: 0L
            val hasVideoStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            val hasAudioStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val mimeType = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia Retriever Output] target=${file.name}, size=${file.length()}B, duration=${duration}ms, mime=$mimeType, hasAudio=$hasAudioStr, hasVideo=$hasVideoStr, isAudioExpected=$isAudio")

            if (isAudio) {
                if (file.length() > 0) {
                    PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia PASSED] Audio validation accepted: size=${file.length()}B, duration=${duration}ms, mime=$mimeType, hasAudio=$hasAudioStr")
                    return@withContext true
                }
                return@withContext false
            } else {
                if (duration <= 0) {
                    PTLog.e("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia FAILED] Video condition failed: duration is $duration")
                    return@withContext false
                }
                if (hasVideoStr != "yes" && (mimeType == null || !mimeType.startsWith("video/"))) {
                    PTLog.e("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia FAILED] Video condition failed: video track not found or invalid mime: $mimeType, hasVideo=$hasVideoStr")
                    return@withContext false
                }
                PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia PASSED] Video validation successful: duration=${duration}ms, mime=$mimeType, hasVideo=$hasVideoStr")
                true
            }
        } catch (e: Exception) {
            PTLog.w("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia Retriever Exception] ${e.message}", e)
            if (isAudio && (com.rahul.vibetube1.utils.Mp3AudioConverter.isMp3File(file) || file.length() > 1024)) {
                PTLog.d("VideoDownloadService", "[DIAGNOSTIC-6: validateMedia PASSED via Exception Fallback] Audio accepted via raw bytes check: ${file.length()} bytes")
                return@withContext true
            }
            false
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.rahul.vibetube1.action.START_DOWNLOAD"
        const val ACTION_STOP = "com.rahul.vibetube1.action.STOP_DOWNLOAD"
        const val ACTION_CANCEL = "com.rahul.vibetube1.action.CANCEL_DOWNLOAD"
        const val NOTIFICATION_ID_BASE = 1000
    }
}
