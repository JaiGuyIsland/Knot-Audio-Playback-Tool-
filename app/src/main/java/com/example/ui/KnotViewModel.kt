package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.KnotDatabase
import com.example.data.model.AudioTrack
import com.example.data.model.Playlist
import com.example.data.model.PlaylistWithTracks
import com.example.data.repository.KnotRepository
import com.example.playback.LoopMode
import com.example.playback.PlaybackController
import com.example.playback.PlaybackState
import com.example.util.AudioImporter
import com.example.util.SampleAmbientGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KnotViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KnotDatabase.getInstance(application)
    private val repository = KnotRepository(db.audioTrackDao(), db.playlistDao())

    val playbackState: StateFlow<PlaybackState> = PlaybackController.state

    val allTracks: StateFlow<List<AudioTrack>> = repository.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId = _selectedPlaylistId.asStateFlow()

    private val _selectedPlaylistWithTracks = MutableStateFlow<PlaylistWithTracks?>(null)
    val selectedPlaylistWithTracks = _selectedPlaylistWithTracks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredTracks: StateFlow<List<AudioTrack>> = combine(allTracks, _searchQuery) { tracks, query ->
        if (query.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    init {
        initializeSamplesIfNeeded()
    }

    private fun initializeSamplesIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getTrackCount()
            if (count == 0) {
                val samples = SampleAmbientGenerator.generateInitialSamplesIfEmpty(getApplication())
                if (samples.isNotEmpty()) {
                    val ids = repository.insertTracks(samples)
                    // Create default "Infinite Ambiance" playlist
                    val playlistId = repository.createPlaylist(
                        name = "Infinite Ambiance",
                        description = "Continuous seamless ambient loops",
                        colorHex = "#38BDF8"
                    )
                    ids.forEach { trackId ->
                        repository.addTrackToPlaylist(playlistId, trackId)
                    }
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun selectPlaylist(playlistId: Long?) {
        _selectedPlaylistId.value = playlistId
        if (playlistId != null) {
            viewModelScope.launch {
                repository.getPlaylistWithTracks(playlistId).collect {
                    _selectedPlaylistWithTracks.value = it
                }
            }
        } else {
            _selectedPlaylistWithTracks.value = null
        }
    }

    fun importAudioFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isImporting.value = true
            var importedCount = 0
            for (uri in uris) {
                val track = AudioImporter.importAudioUri(getApplication(), uri)
                if (track != null) {
                    val id = repository.insertTrack(track)
                    if (id > 0) importedCount++

                    // If a playlist is currently open, automatically add imported track to it
                    val currentPlaylistId = _selectedPlaylistId.value
                    if (currentPlaylistId != null && id > 0) {
                        repository.addTrackToPlaylist(currentPlaylistId, id)
                    }
                }
            }
            _isImporting.value = false
            _userMessage.value = if (importedCount > 0) {
                "Imported $importedCount audio ${if (importedCount == 1) "track" else "tracks"}"
            } else {
                "Could not import selected files"
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", trackIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val playlistId = repository.createPlaylist(name, description)
            trackIds.forEach { trackId ->
                repository.addTrackToPlaylist(playlistId, trackId)
            }
            _userMessage.value = "Created playlist '$name'"
        }
    }

    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.updatePlaylist(playlist)
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            if (_selectedPlaylistId.value == playlist.id) {
                _selectedPlaylistId.value = null
                _selectedPlaylistWithTracks.value = null
            }
            _userMessage.value = "Playlist deleted"
        }
    }

    fun deleteTrack(track: AudioTrack) {
        viewModelScope.launch {
            repository.deleteTrack(track)
            _userMessage.value = "Track removed"
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun reorderPlaylist(playlistId: Long, trackIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderPlaylist(playlistId, trackIds)
        }
    }

    // Playback Controls
    fun playPlaylist(playlist: Playlist, tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        PlaybackController.playQueue(
            context = getApplication(),
            queue = tracks,
            startIndex = startIndex,
            playlistName = playlist.name,
            playlistId = playlist.id
        )
    }

    fun playTrack(track: AudioTrack) {
        PlaybackController.playTrack(getApplication(), track)
    }

    fun playQueue(queue: List<AudioTrack>, startIndex: Int = 0, name: String = "Queue") {
        PlaybackController.playQueue(
            context = getApplication(),
            queue = queue,
            startIndex = startIndex,
            playlistName = name,
            playlistId = null
        )
    }

    fun togglePlayPause() {
        PlaybackController.togglePlayPause(getApplication())
    }

    fun next() {
        PlaybackController.next(getApplication())
    }

    fun previous() {
        PlaybackController.previous(getApplication())
    }

    fun seekTo(positionMs: Long) {
        PlaybackController.seekTo(getApplication(), positionMs)
    }

    fun toggleLoopMode() {
        PlaybackController.toggleLoopMode(getApplication())
    }

    fun setSpeed(speed: Float) {
        PlaybackController.setSpeed(getApplication(), speed)
    }

    fun stopPlayback() {
        PlaybackController.stop(getApplication())
    }
}
