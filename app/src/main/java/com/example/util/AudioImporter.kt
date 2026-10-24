package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object AudioImporter {

    suspend fun importAudioUri(context: Context, uri: Uri): AudioTrack? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            var fileName = "imported_audio_${System.currentTimeMillis()}.mp3"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            val audioDir = File(context.filesDir, "audio").apply { if (!exists()) mkdirs() }
            val safeBaseName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = File(audioDir, "${System.currentTimeMillis()}_$safeBaseName")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@withContext null

            if (fileSize <= 0) {
                fileSize = destFile.length()
            }

            // Extract metadata
            var title = destFile.nameWithoutExtension
                .replace(Regex("^\\d+_"), "")
                .replace("_", " ")
            var artist = "Local Upload"
            var durationMs = 0L

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(destFile.absolutePath)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!metaTitle.isNullOrBlank()) {
                    title = metaTitle.trim()
                }
                if (!metaArtist.isNullOrBlank()) {
                    artist = metaArtist.trim()
                }
                if (!metaDuration.isNullOrBlank()) {
                    durationMs = metaDuration.toLongOrNull() ?: 0L
                }
            } catch (_: Exception) {
                // Keep file-based defaults
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }

            AudioTrack(
                title = title,
                artist = artist,
                durationMs = durationMs,
                filePath = destFile.absolutePath,
                originalFileName = fileName,
                dateAdded = System.currentTimeMillis(),
                fileSize = fileSize,
                isPreset = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
