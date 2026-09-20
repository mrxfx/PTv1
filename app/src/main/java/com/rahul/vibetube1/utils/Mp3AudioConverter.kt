/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils

import android.media.MediaMetadataRetriever
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

object Mp3AudioConverter {
    private const val TAG = "Mp3AudioConverter"

    /**
     * Checks if the file is already a valid MP3 file based on magic bytes or metadata.
     */
    fun isMp3File(file: File): Boolean {
        if (!file.exists() || file.length() < 128) return false
        try {
            java.io.FileInputStream(file).use { fis ->
                val header = ByteArray(10)
                val read = fis.read(header)
                if (read >= 3 && header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) {
                    return true // ID3 header found
                }
                if (read >= 2 && (header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0) {
                    return true // MPEG sync word found
                }
            }
        } catch (_: Exception) {}
        return false
    }

    /**
     * Inspects media file using MediaMetadataRetriever and returns diagnostic info string.
     */
    fun inspectMediaFile(file: File): String {
        if (!file.exists()) return "File does not exist"
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "unknown"
            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "unknown"
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: "unknown"
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) ?: "unknown"
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) ?: "unknown"
            "size=${file.length()}B, mime=$mime, duration=${duration}ms, bitrate=${bitrate}bps, hasAudio=$hasAudio, hasVideo=$hasVideo"
        } catch (e: Exception) {
            "size=${file.length()}B, retriever_error=${e.message}"
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    fun convertToMp3(
        inputFile: File,
        outputFile: File,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        thumbnailFile: File? = null,
        onProgress: ((Int) -> Unit)? = null
    ): Boolean {
        PTLog.d(TAG, "[DIAGNOSTIC-3: Media Inspection (Input)] input=${inputFile.name}, path=${inputFile.absolutePath}, bytes=${inputFile.length()}")

        if (!inputFile.exists() || inputFile.length() <= 0) {
            PTLog.e(TAG, "[DIAGNOSTIC-3: Media Inspection FAILED] Input file does not exist or is 0 bytes: ${inputFile.absolutePath}")
            return false
        }

        // Inspect input stream properties
        val inputInspection = inspectMediaFile(inputFile)
        val isPreMp3 = isMp3File(inputFile)
        PTLog.d(TAG, "[DIAGNOSTIC-3: Media Inspection (Retriever)] $inputInspection, isPreExistingMp3Header=$isPreMp3")

        if (outputFile.exists()) {
            outputFile.delete()
        }

        // 1. If input is already an MP3 stream, copy directly
        if (isPreMp3) {
            PTLog.d(TAG, "[DIAGNOSTIC-4: Stream Check] Input stream already contains MP3 header/magic bytes, attempting direct copy...")
            try {
                inputFile.copyTo(outputFile, overwrite = true)
                if (outputFile.exists() && outputFile.length() > 0) {
                    val outInspect = inspectMediaFile(outputFile)
                    PTLog.d(TAG, "[DIAGNOSTIC-5: MP3 Output (Direct Copy)] exists=true, size=${outputFile.length()} bytes, info: $outInspect")
                    return true
                }
            } catch (e: Exception) {
                PTLog.w(TAG, "[DIAGNOSTIC-4: Direct Copy Failed] ${e.message}, proceeding with FFmpeg transcode", e)
            }
        }

        // 2. Primary conversion: FFmpeg with libmp3lame and -vn (pure audio, no video codecs needed)
        val args = mutableListOf<String>()
        args.add("-y")
        args.add("-i")
        args.add(inputFile.absolutePath)
        args.add("-vn") // Explicitly strip video stream
        args.add("-c:a")
        args.add("libmp3lame")
        args.add("-b:a")
        args.add("192k")
        args.add("-ar")
        args.add("44100")

        // Add ID3 tags
        if (!title.isNullOrBlank()) {
            args.add("-metadata")
            args.add("title=$title")
        }
        if (!artist.isNullOrBlank()) {
            args.add("-metadata")
            args.add("artist=$artist")
        }
        args.add("-metadata")
        args.add("album=${album ?: "ZyvoTube"}")
        args.add("-id3v2_version")
        args.add("3")
        args.add("-write_id3v1")
        args.add("1")
        args.add(outputFile.absolutePath)

        PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg Primary Command (libmp3lame)] ${args.joinToString(" ")}")

        // Extract duration for progress calculation
        val inputDurationMs = try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(inputFile.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) { 0L }

        if (onProgress != null) {
            onProgress(0)
            if (inputDurationMs > 0) {
                FFmpegKitConfig.enableStatisticsCallback { statistics ->
                    if (statistics != null && statistics.time > 0) {
                        val pct = ((statistics.time * 100) / inputDurationMs).toInt().coerceIn(0, 99)
                        onProgress(pct)
                    }
                }
            }
        }

        try {
            val session = FFmpegKit.executeWithArguments(args.toTypedArray())
            val returnCode = session.getReturnCode()
            val logs = session.getAllLogsAsString() ?: session.getOutput() ?: ""
            val failStackTrace = session.getFailStackTrace()
            PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg libmp3lame Result] returnCode=$returnCode (isSuccess=${ReturnCode.isSuccess(returnCode)}), duration=${session.getDuration()}ms, failStackTrace=$failStackTrace")
            if (!ReturnCode.isSuccess(returnCode)) {
                PTLog.w(TAG, "[DIAGNOSTIC-4: FFmpeg libmp3lame Error Output] logs=[$logs]")
            }

            if (ReturnCode.isSuccess(returnCode) && outputFile.exists() && outputFile.length() > 0) {
                val outInspect = inspectMediaFile(outputFile)
                PTLog.d(TAG, "[DIAGNOSTIC-5: MP3 Output (libmp3lame)] exists=true, size=${outputFile.length()} bytes, info: $outInspect")
                if (onProgress != null) {
                    FFmpegKitConfig.enableStatisticsCallback(null)
                    onProgress(100)
                }
                return true
            }

            PTLog.w(TAG, "[DIAGNOSTIC-4: Primary FFmpeg Failed] returnCode=$returnCode, attempting Fallback 1 (native mp3 encoder)...")

            // 3. Fallback 1: FFmpeg with standard mp3 encoder
            val fallbackArgs = mutableListOf<String>()
            fallbackArgs.add("-y")
            fallbackArgs.add("-i")
            fallbackArgs.add(inputFile.absolutePath)
            fallbackArgs.add("-vn")
            fallbackArgs.add("-c:a")
            fallbackArgs.add("mp3")
            fallbackArgs.add("-b:a")
            fallbackArgs.add("192k")
            fallbackArgs.add("-ar")
            fallbackArgs.add("44100")

            if (!title.isNullOrBlank()) {
                fallbackArgs.add("-metadata")
                fallbackArgs.add("title=$title")
            }
            if (!artist.isNullOrBlank()) {
                fallbackArgs.add("-metadata")
                fallbackArgs.add("artist=$artist")
            }
            fallbackArgs.add("-metadata")
            fallbackArgs.add("album=${album ?: "ZyvoTube"}")
            fallbackArgs.add("-id3v2_version")
            fallbackArgs.add("3")
            fallbackArgs.add(outputFile.absolutePath)

            PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 1 Command (native mp3)] ${fallbackArgs.joinToString(" ")}")

            val fallbackSession = FFmpegKit.executeWithArguments(fallbackArgs.toTypedArray())
            val fallbackCode = fallbackSession.getReturnCode()
            val fallbackLogs = fallbackSession.getAllLogsAsString() ?: fallbackSession.getOutput() ?: ""
            val fallbackFailStack = fallbackSession.getFailStackTrace()

            PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 1 Result] returnCode=$fallbackCode (isSuccess=${ReturnCode.isSuccess(fallbackCode)}), duration=${fallbackSession.getDuration()}ms, failStackTrace=$fallbackFailStack")
            if (!ReturnCode.isSuccess(fallbackCode)) {
                PTLog.w(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 1 Error Output] logs=[$fallbackLogs]")
            }

            if (ReturnCode.isSuccess(fallbackCode) && outputFile.exists() && outputFile.length() > 0) {
                val outInspect = inspectMediaFile(outputFile)
                PTLog.d(TAG, "[DIAGNOSTIC-5: MP3 Output (native mp3)] exists=true, size=${outputFile.length()} bytes, info: $outInspect")
                if (onProgress != null) {
                    FFmpegKitConfig.enableStatisticsCallback(null)
                    onProgress(100)
                }
                return true
            }

            PTLog.w(TAG, "[DIAGNOSTIC-4: Fallback 1 Failed] returnCode=$fallbackCode, attempting Fallback 2 (auto transcode)...")

            // 4. Fallback 2: Auto audio encoder
            val autoArgs = mutableListOf<String>()
            autoArgs.add("-y")
            autoArgs.add("-i")
            autoArgs.add(inputFile.absolutePath)
            autoArgs.add("-vn")
            autoArgs.add("-b:a")
            autoArgs.add("192k")
            autoArgs.add("-ar")
            autoArgs.add("44100")
            autoArgs.add(outputFile.absolutePath)

            PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 2 Command (auto transcode)] ${autoArgs.joinToString(" ")}")

            val autoSession = FFmpegKit.executeWithArguments(autoArgs.toTypedArray())
            val autoCode = autoSession.getReturnCode()
            val autoLogs = autoSession.getAllLogsAsString() ?: autoSession.getOutput() ?: ""
            val autoFailStack = autoSession.getFailStackTrace()

            PTLog.d(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 2 Result] returnCode=$autoCode (isSuccess=${ReturnCode.isSuccess(autoCode)}), duration=${autoSession.getDuration()}ms, failStackTrace=$autoFailStack")
            if (!ReturnCode.isSuccess(autoCode)) {
                PTLog.w(TAG, "[DIAGNOSTIC-4: FFmpeg Fallback 2 Error Output] logs=[$autoLogs]")
            }

            if (ReturnCode.isSuccess(autoCode) && outputFile.exists() && outputFile.length() > 0) {
                val outInspect = inspectMediaFile(outputFile)
                PTLog.d(TAG, "[DIAGNOSTIC-5: MP3 Output (auto transcode)] exists=true, size=${outputFile.length()} bytes, info: $outInspect")
                if (onProgress != null) {
                    FFmpegKitConfig.enableStatisticsCallback(null)
                    onProgress(100)
                }
                return true
            }

            PTLog.e(TAG, "[DIAGNOSTIC-4: ALL FFmpeg Transcodes FAILED] input=${inputFile.name} (${inputFile.length()}B), lastReturnCode=$autoCode, lastLogs=[$autoLogs], failStackTrace=$autoFailStack")
            FirebaseTelemetry.recordNonFatal(
                Exception("FFmpeg transcode failed for ${inputFile.name}: autoCode=$autoCode, logs=${autoLogs.take(200)}"),
                TAG,
                "MP3 Transcode Failure"
            )

            // Cleanup invalid file
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return false
        } catch (t: Throwable) {
            PTLog.e(TAG, "[DIAGNOSTIC-4: FFmpeg Exception] Fatal error converting to MP3: ${t.message}", t)
            FirebaseTelemetry.recordNonFatal(t, TAG, "MP3 Transcode Exception")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            return false
        } finally {
            if (onProgress != null) {
                try { FFmpegKitConfig.enableStatisticsCallback(null) } catch (_: Exception) {}
            }
        }
    }
}


