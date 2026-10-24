package com.example.util

import android.content.Context
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object SampleAmbientGenerator {

    suspend fun generateInitialSamplesIfEmpty(context: Context): List<AudioTrack> = withContext(Dispatchers.IO) {
        val audioDir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }
        val tracks = mutableListOf<AudioTrack>()

        val presets = listOf(
            Triple("Zen Harmony 432Hz", "Knot Ambient", 0),
            Triple("Brown Noise Ocean", "Knot Soundscape", 1),
            Triple("Binaural Solfeggio 528Hz", "Knot Focus", 2)
        )

        for ((title, artist, type) in presets) {
            val fileName = "preset_${type}.wav"
            val file = File(audioDir, fileName)
            if (!file.exists()) {
                generateWavFile(file, type)
            }
            if (file.exists()) {
                tracks.add(
                    AudioTrack(
                        title = title,
                        artist = artist,
                        durationMs = 6000L,
                        filePath = file.absolutePath,
                        originalFileName = fileName,
                        dateAdded = System.currentTimeMillis() - (type * 1000),
                        fileSize = file.length(),
                        isPreset = true
                    )
                )
            }
        }
        tracks
    }

    private fun generateWavFile(file: File, type: Int) {
        val sampleRate = 44100
        val durationSeconds = 6.0
        val totalSamples = (sampleRate * durationSeconds).toInt()
        val numChannels = 1
        val bitsPerSample = 16

        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val progress = t / durationSeconds
            // Exact integer cycles for seamless loop wrapping
            val sampleVal: Double = when (type) {
                0 -> {
                    // 432 Hz fundamental (adjusted slightly to fit integer cycles in 6.0s: 432 * 6 = 2592 cycles)
                    val freq1 = 2592.0 / durationSeconds
                    val freq2 = freq1 * 1.5 // fifth
                    val freq3 = freq1 * 2.0 // octave
                    val wave = 0.5 * sin(2.0 * PI * freq1 * t) +
                               0.3 * sin(2.0 * PI * freq2 * t) +
                               0.2 * sin(2.0 * PI * freq3 * t)
                    // Gentle breathing modulation
                    val lfo = 0.8 + 0.2 * sin(2.0 * PI * (3.0 / durationSeconds) * t)
                    wave * lfo * 0.4
                }
                1 -> {
                    // Smooth continuous ocean swell
                    val carrier1 = 200.0 / durationSeconds // deep base
                    val carrier2 = 340.0 / durationSeconds
                    val carrier3 = 600.0 / durationSeconds
                    val swell = 0.7 + 0.3 * sin(2.0 * PI * (2.0 / durationSeconds) * t)
                    val pseudoPink = sin(2.0 * PI * carrier1 * t) * 0.4 +
                                     cos(2.0 * PI * carrier2 * t) * 0.3 +
                                     sin(2.0 * PI * carrier3 * t) * 0.3
                    pseudoPink * swell * 0.35
                }
                else -> {
                    // 528 Hz transformation frequency (528 * 6 = 3168 cycles)
                    val freq1 = 3168.0 / durationSeconds
                    val binauralOffset = 6.0 / durationSeconds // 1Hz binaural beat
                    val freq2 = freq1 + binauralOffset
                    val wave = 0.5 * sin(2.0 * PI * freq1 * t) + 0.5 * sin(2.0 * PI * freq2 * t)
                    wave * 0.4
                }
            }

            val clamped = sampleVal.coerceIn(-1.0, 1.0)
            val pcmSample = (clamped * Short.MAX_VALUE).toInt().toShort()
            buffer.putShort(pcmSample)
        }

        FileOutputStream(file).use { fos ->
            val dataSize = totalSamples * numChannels * (bitsPerSample / 8)
            writeWavHeader(fos, sampleRate, numChannels, bitsPerSample, dataSize)
            fos.write(buffer.array())
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int,
        dataSize: Int
    ) {
        val totalSize = 36 + dataSize
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = channels * (bitsPerSample / 8)

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(totalSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16) // Subchunk1Size for PCM
        header.putShort(1) // AudioFormat 1 = PCM
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign.toShort())
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray())
        header.putInt(dataSize)

        out.write(header.array())
    }
}
