package com.example.data.repository

import com.example.data.local.AudioTrackDao
import com.example.data.local.PlaylistDao
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import com.example.data.model.PlaylistWithTracks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.File

class KnotRepository(
    private val audioTrackDao: AudioTrackDao,
    private val playlistDao: PlaylistDao
) {
    val allTracks: Flow<List<AudioTrack>> = audioTrackDao.getAllTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    suspend fun getTrackById(id: Long): AudioTrack? = audioTrackDao.getTrackById(id)

    suspend fun insertTrack(track: AudioTrack): Long = audioTrackDao.insertTrack(track)

    suspend fun insertTracks(tracks: List<AudioTrack>): List<Long> = audioTrackDao.insertTracks(tracks)

    suspend fun deleteTrack(track: AudioTrack) {
        // Delete audio file from storage
        try {
            val file = File(track.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
        audioTrackDao.deleteTrack(track)
    }

    suspend fun createPlaylist(name: String, description: String = "", colorHex: String = "#38BDF8"): Long {
        val playlist = Playlist(
            name = name.trim().ifEmpty { "Untitled Knot" },
            description = description.trim(),
            colorHex = colorHex
        )
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getPlaylistWithTracks(playlistId: Long): Flow<PlaylistWithTracks?> {
        return combine(
            playlistDao.getPlaylistFlow(playlistId),
            playlistDao.getTracksForPlaylist(playlistId)
        ) { playlist, tracks ->
            playlist?.let {
                PlaylistWithTracks(playlist = it, tracks = tracks)
            }
        }
    }

    suspend fun getTracksForPlaylistSync(playlistId: Long): List<AudioTrack> {
        return playlistDao.getTracksForPlaylistSync(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val maxOrder = playlistDao.getMaxOrderIndex(playlistId) ?: -1
        playlistDao.insertPlaylistItem(
            PlaylistItem(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = maxOrder + 1
            )
        )
        // touch playlist
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun reorderPlaylist(playlistId: Long, trackIds: List<Long>) {
        playlistDao.setPlaylistTracks(playlistId, trackIds)
    }

    suspend fun getTrackCount(): Int = audioTrackDao.getTrackCount()
    suspend fun getPlaylistCount(): Int = playlistDao.getPlaylistCount()
}
