package com.example.data.model

data class PlaylistWithTracks(
    val playlist: Playlist,
    val tracks: List<AudioTrack> = emptyList()
) {
    val totalDurationMs: Long
        get() = tracks.sumOf { it.durationMs }
}
