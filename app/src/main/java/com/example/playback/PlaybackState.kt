package com.example.playback

import com.example.data.model.AudioTrack

enum class LoopMode {
    PLAYLIST,     // Loops endlessly through playlist tracks (Default for knot)
    SINGLE_TRACK  // Repeats active track continuously
}

data class PlaybackState(
    val currentTrack: AudioTrack? = null,
    val currentPlaylistName: String? = null,
    val currentPlaylistId: Long? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val loopMode: LoopMode = LoopMode.PLAYLIST,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingMinutes: Int? = null,
    val queue: List<AudioTrack> = emptyList(),
    val currentIndex: Int = -1
) {
    val hasActiveTrack: Boolean get() = currentTrack != null
    val isPaused: Boolean get() = !isPlaying && currentTrack != null
    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
