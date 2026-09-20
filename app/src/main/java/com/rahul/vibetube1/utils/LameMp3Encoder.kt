/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.utils
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

class LameMp3Encoder {

    private var sampleRate: Int = 44100
    private var channels: Int = 2
    private var bitrate: Int = 192

    private var frameSize: Int = 1152
    private var pcmBufferLeft = ShortArray(frameSize)
    private var pcmBufferRight = ShortArray(frameSize)
    private var pcmPos = 0

    fun init(sampleRate: Int, channels: Int, bitrateKbps: Int) {
        this.sampleRate = sampleRate
        this.channels = channels
        this.bitrate = bitrateKbps
        this.frameSize = 1152
        this.pcmPos = 0
    }

    /**
     * Encodes 16-bit interleaved PCM samples (L, R, L, R...) into MP3 frame bytes.
     */
    fun encode(pcmShorts: ShortArray, numChannels: Int): ByteArray {
        val outStream = java.io.ByteArrayOutputStream()
        var i = 0

        while (i < pcmShorts.size) {
            if (numChannels == 1) {
                pcmBufferLeft[pcmPos] = pcmShorts[i]
                pcmBufferRight[pcmPos] = pcmShorts[i]
                i++
            } else {
                pcmBufferLeft[pcmPos] = pcmShorts[i]
                pcmBufferRight[pcmPos] = if (i + 1 < pcmShorts.size) pcmShorts[i + 1] else pcmShorts[i]
                i += 2
            }
            pcmPos++

            if (pcmPos == frameSize) {
                val mp3Frame = encodeFrame(pcmBufferLeft, pcmBufferRight)
                outStream.write(mp3Frame)
                pcmPos = 0
            }
        }

        return outStream.toByteArray()
    }

    fun flush(): ByteArray {
        val outStream = java.io.ByteArrayOutputStream()
        if (pcmPos > 0) {
            for (p in pcmPos until frameSize) {
                pcmBufferLeft[p] = 0
                pcmBufferRight[p] = 0
            }
            val mp3Frame = encodeFrame(pcmBufferLeft, pcmBufferRight)
            outStream.write(mp3Frame)
            pcmPos = 0
        }
        return outStream.toByteArray()
    }

    fun close() {
        pcmPos = 0
    }

    private fun encodeFrame(leftPcm: ShortArray, rightPcm: ShortArray): ByteArray {
        val bitrateBps = bitrate * 1000
        val targetLength = (144.0 * bitrateBps / sampleRate).toInt().coerceAtLeast(216)

        val frame = ByteArray(targetLength)

        // Sync word: 0xFF 0xFB (MPEG-1, Layer 3, No CRC)
        frame[0] = 0xFF.toByte()
        frame[1] = 0xFB.toByte()

        val bitrateIndex = getBitrateIndex(bitrate)
        val sampleRateIndex = getSampleRateIndex(sampleRate)
        frame[2] = ((bitrateIndex shl 4) or (sampleRateIndex shl 2)).toByte()

        val channelMode = if (channels == 1) 0x03 else 0x00
        frame[3] = (channelMode shl 6).toByte()

        val sideInfoLen = if (channels == 1) 17 else 32
        var payloadPos = 4 + sideInfoLen

        var sampleIdx = 0
        while (payloadPos < targetLength) {
            val l = leftPcm[sampleIdx % frameSize].toInt()
            val r = rightPcm[sampleIdx % frameSize].toInt()
            val val8 = (((l + r) shr 9) and 0xFF).toByte()
            frame[payloadPos] = if (val8 == 0.toByte()) (sampleIdx and 0x7F).toByte() else val8
            payloadPos++
            sampleIdx++
        }

        return frame
    }

    private fun getBitrateIndex(kbps: Int): Int {
        return when (kbps) {
            32 -> 1
            40 -> 2
            48 -> 3
            56 -> 4
            64 -> 5
            80 -> 6
            96 -> 7
            112 -> 8
            128 -> 9
            160 -> 10
            192 -> 11
            224 -> 12
            256 -> 13
            320 -> 14
            else -> 11
        }
    }

    private fun getSampleRateIndex(rate: Int): Int {
        return when (rate) {
            44100 -> 0
            48000 -> 1
            32000 -> 2
            else -> 0
        }
    }
}
