package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.PlaylistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?

    @Query("SELECT * FROM playlists WHERE id = :id")
    fun getPlaylistFlow(id: Long): Flow<Playlist?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("""
        SELECT a.* FROM audio_tracks a
        INNER JOIN playlist_items pi ON a.id = pi.trackId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    fun getTracksForPlaylist(playlistId: Long): Flow<List<AudioTrack>>

    @Query("""
        SELECT a.* FROM audio_tracks a
        INNER JOIN playlist_items pi ON a.id = pi.trackId
        WHERE pi.playlistId = :playlistId
        ORDER BY pi.orderIndex ASC
    """)
    suspend fun getTracksForPlaylistSync(playlistId: Long): List<AudioTrack>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItem): Long

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    @Query("SELECT MAX(orderIndex) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Transaction
    suspend fun setPlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        clearPlaylistItems(playlistId)
        trackIds.forEachIndexed { index, trackId ->
            insertPlaylistItem(
                PlaylistItem(
                    playlistId = playlistId,
                    trackId = trackId,
                    orderIndex = index
                )
            )
        }
    }

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int
}
