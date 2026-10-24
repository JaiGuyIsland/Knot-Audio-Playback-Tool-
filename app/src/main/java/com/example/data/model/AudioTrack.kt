package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_tracks")
data class AudioTrack(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Unknown",
    val durationMs: Long = 0,
    val filePath: String,
    val originalFileName: String,
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val isPreset: Boolean = false
)
