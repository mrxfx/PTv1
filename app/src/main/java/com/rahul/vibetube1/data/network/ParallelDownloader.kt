/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.data.network

import android.net.Uri
import com.rahul.vibetube1.data.local.DownloadChunkEntity
import com.rahul.vibetube1.data.local.MissionDao
import com.rahul.vibetube1.utils.Constants
import com.rahul.vibetube1.utils.PTLog
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

class ParallelDownloader(
    private val client: OkHttpClient,
    private val missionDao: MissionDao
) {
    private val semaphore = Semaphore(4) // Max 4 parallel chunks
    private val CHUNK_SIZE = 4L * 1024 * 1024 // 4MB

    class RangeNotSupportedException(message: String) : IOException(message)

    companion object {
        private const val TAG = "ParallelDownloader"

        fun safeUrlSummary(url: String?): String {
            if (url.isNullOrBlank()) return "null"
            return try {
                val uri = Uri.parse(url)
                val host = uri.host ?: "unknown_host"
                val path = uri.path?.take(30) ?: ""
                val queryKeys = uri.queryParameterNames.joinToString(",")
                "host=$host, path=$path, params=[$queryKeys]"
            } catch (_: Exception) {
                "length=${url.length}"
            }
        }
    }

    suspend fun download(
        url: String,
        outputFile: File,
        missionId: Long,
        type: com.rahul.vibetube1.data.local.ChunkType,
        onProgress: (Long) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val safeSummary = safeUrlSummary(url)
        PTLog.d(TAG, "[DIAGNOSTIC-2: Download Initiated] missionId=$missionId, type=$type, urlSummary=[$safeSummary], targetFile=${outputFile.name}, ext=${outputFile.extension}")

        val totalSize = try { getFileSize(url) } catch (_: Exception) { 0L }

        if (totalSize > 0) {
            try {
                PTLog.d(TAG, "[DIAGNOSTIC-2: Download Chunked Mode] totalSize=$totalSize bytes, allocating file and starting parallel chunks...")
                // Pre-allocate file
                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.setLength(totalSize)
                }

                val existingChunks = missionDao.getChunksForMission(missionId).filter { it.type == type }
                val chunks = if (existingChunks.isEmpty()) {
                    createChunks(missionId, totalSize, type)
                } else {
                    existingChunks
                }

                val downloadedBytes = AtomicLong(chunks.sumOf { it.bytesDownloaded })

                val deferreds = chunks.filter { !it.isCompleted }.map { chunk ->
                    async {
                        downloadChunkWithRetry(url, outputFile, chunk, downloadedBytes, onProgress)
                    }
                }

                deferreds.awaitAll()
                if (outputFile.exists() && outputFile.length() > 0) {
                    PTLog.d(TAG, "[DIAGNOSTIC-2: Download Complete (Chunked)] totalSize=$totalSize, finalFileSize=${outputFile.length()} bytes, ext=${outputFile.extension}")
                    return@withContext totalSize
                } else {
                    throw Exception("Chunked download resulted in empty or non-existent file")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) throw e
                PTLog.w(TAG, "[DIAGNOSTIC-2: Chunked Mode Fallback] Chunked download failed: ${e.message}, falling back to sequential download", e)
            }
        } else {
            PTLog.d(TAG, "[DIAGNOSTIC-2: Download Sequential Mode] File size unknown ($totalSize), starting direct streaming download...")
        }

        // Sequential streaming download fallback
        downloadSequential(url, outputFile, onProgress)
    }

    suspend fun downloadSequential(
        url: String,
        outputFile: File,
        onProgress: (Long) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val safeSummary = safeUrlSummary(url)
        PTLog.d(TAG, "[DIAGNOSTIC-2: Sequential Stream Start] urlSummary=[$safeSummary], targetFile=${outputFile.name}, ext=${outputFile.extension}")

        var attempt = 0
        val maxRetries = 4
        while (attempt < maxRetries) {
            try {
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                    .header("Accept-Encoding", "identity")
                    .build()

                client.newCall(request).execute().use { response ->
                    val code = response.code
                    val contentType = response.header("Content-Type") ?: "unknown"
                    val contentLength = response.header("Content-Length") ?: "unknown"
                    val contentRange = response.header("Content-Range") ?: "none"

                    PTLog.d(TAG, "[DIAGNOSTIC-2: Download HTTP Response] code=$code, Content-Type=$contentType, Content-Length=$contentLength, Content-Range=$contentRange, isSuccessful=${response.isSuccessful}")

                    if (code == 403 || code == 410) throw Exception("HTTP $code (Forbidden/Expired)")
                    if (!response.isSuccessful) throw Exception("HTTP $code (${response.message})")

                    val body = response.body ?: throw Exception("Empty HTTP response body")
                    var totalDownloaded = 0L

                    FileOutputStream(outputFile).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        body.byteStream().use { input ->
                            while (input.read(buffer).also { read = it } != -1) {
                                currentCoroutineContext().ensureActive()
                                fos.write(buffer, 0, read)
                                totalDownloaded += read
                                onProgress(totalDownloaded)
                            }
                        }
                        fos.flush()
                    }

                    if (outputFile.exists() && outputFile.length() > 0) {
                        PTLog.d(TAG, "[DIAGNOSTIC-2: Sequential Download Complete] targetFile=${outputFile.name}, writtenBytes=$totalDownloaded, actualFileSize=${outputFile.length()} bytes, ext=${outputFile.extension}")
                        return@withContext totalDownloaded
                    } else {
                        throw Exception("Sequential download resulted in empty file (0 bytes)")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) throw e

                attempt++
                if (attempt >= maxRetries) {
                    PTLog.e(TAG, "[DIAGNOSTIC-2: Sequential Download Failed] after $maxRetries attempts: ${e.message}", e)
                    throw e
                }
                val delayTime = (1000L * attempt).coerceAtMost(6000L)
                PTLog.w(TAG, "[DIAGNOSTIC-2: Retrying Sequential Download] attempt $attempt/$maxRetries after ${delayTime}ms: ${e.message}")
                delay(delayTime)
            }
        }
        outputFile.length()
    }

    private suspend fun createChunks(
        missionId: Long,
        totalSize: Long,
        type: com.rahul.vibetube1.data.local.ChunkType
    ): List<DownloadChunkEntity> {
        val chunks = mutableListOf<DownloadChunkEntity>()
        var start = 0L
        var index = 0
        while (start < totalSize) {
            val end = (start + CHUNK_SIZE - 1).coerceAtMost(totalSize - 1)
            val chunk = DownloadChunkEntity(
                missionId = missionId,
                chunkIndex = index++,
                startByte = start,
                endByte = end,
                type = type
            )
            val id = missionDao.insertChunk(chunk)
            chunks.add(chunk.copy(id = id))
            start = end + 1
        }
        return chunks
    }

    private suspend fun downloadChunkWithRetry(
        url: String,
        outputFile: File,
        chunk: DownloadChunkEntity,
        downloadedBytes: AtomicLong,
        onProgress: (Long) -> Unit
    ) {
        var attempt = 0
        val maxRetries = 4
        while (attempt < maxRetries) {
            try {
                downloadChunk(url, outputFile, chunk, downloadedBytes, onProgress)
                return
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is RangeNotSupportedException) throw e
                if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) throw e
                
                attempt++
                if (attempt >= maxRetries) throw e
                val delayTime = (800L * attempt * attempt).coerceAtMost(8000L)
                PTLog.w(TAG, "Retrying chunk ${chunk.chunkIndex} (attempt $attempt/$maxRetries): ${e.message}")
                delay(delayTime)
            }
        }
    }

    private suspend fun downloadChunk(
        url: String,
        outputFile: File,
        chunk: DownloadChunkEntity,
        downloadedBytes: AtomicLong,
        onProgress: (Long) -> Unit
    ) {
        semaphore.acquire()
        try {
            val start = chunk.startByte + chunk.bytesDownloaded
            if (start > chunk.endByte) {
                missionDao.updateChunkProgress(chunk.id, chunk.bytesDownloaded, true)
                return
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                .header("Accept-Encoding", "identity")
                .header("Range", "bytes=$start-${chunk.endByte}")
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                if (code == 403 || code == 410) throw Exception("HTTP $code Forbidden/Expired")
                if (code == 416) throw Exception("416 Range Not Satisfiable")
                // If server returns 200 OK when a non-zero range was requested, Range is NOT supported
                if (code == 200 && start > 0) {
                    throw RangeNotSupportedException("Server returned 200 OK for byte offset $start; Range not supported")
                }
                if (!response.isSuccessful) throw Exception("Chunk download failed: $code")
                val body = response.body ?: throw Exception("Empty body")
                
                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var currentChunkProgress = chunk.bytesDownloaded
                    
                    body.byteStream().use { input ->
                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            raf.write(buffer, 0, read)
                            currentChunkProgress += read
                            downloadedBytes.addAndGet(read.toLong())
                            onProgress(downloadedBytes.get())
                            
                            // Periodically update DB to avoid excessive writes
                            if (currentChunkProgress % (512 * 1024) == 0L) {
                                missionDao.updateChunkProgress(chunk.id, currentChunkProgress, false)
                            }
                        }
                    }
                    missionDao.updateChunkProgress(chunk.id, currentChunkProgress, true)
                }
            }
        } finally {
            semaphore.release()
        }
    }

    suspend fun getFileSize(url: String): Long = withContext(Dispatchers.IO) {
        val safeSummary = safeUrlSummary(url)
        var attempt = 0
        val maxRetries = 2
        while (attempt < maxRetries) {
            try {
                // 1. Try HEAD request
                val headRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                    .header("Accept-Encoding", "identity")
                    .head()
                    .build()

                try {
                    client.newCall(headRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val len = response.header("Content-Length")?.toLongOrNull() ?: response.body?.contentLength() ?: 0L
                            if (len > 0L) {
                                PTLog.d(TAG, "HEAD probe succeeded: length=$len bytes for urlSummary=[$safeSummary]")
                                return@withContext len
                            }
                        }
                    }
                } catch (headEx: Exception) {
                    PTLog.d(TAG, "HEAD probe skipped/failed (${headEx.message}), proceeding to standard GET probe")
                }

                // 2. Fallback: Standard GET request (without Range header) to probe headers safely
                val getRequest = Request.Builder()
                    .url(url)
                    .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                    .header("Accept-Encoding", "identity")
                    .build()

                client.newCall(getRequest).execute().use { getResponse ->
                    if (getResponse.isSuccessful) {
                        val len = getResponse.header("Content-Length")?.toLongOrNull() ?: getResponse.body?.contentLength() ?: 0L
                        if (len > 0L) {
                            PTLog.d(TAG, "GET probe succeeded: length=$len bytes for urlSummary=[$safeSummary]")
                            return@withContext len
                        }
                    } else {
                        PTLog.d(TAG, "GET probe received non-success HTTP ${getResponse.code} for urlSummary=[$safeSummary]")
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                PTLog.w(TAG, "Failed to probe file size (attempt ${attempt + 1}/$maxRetries): ${e.message}")
            }
            attempt++
            if (attempt < maxRetries) {
                delay(300L * attempt)
            }
        }
        PTLog.d(TAG, "File size could not be pre-determined for urlSummary=[$safeSummary], returning 0 for streaming mode")
        0L
    }
}


