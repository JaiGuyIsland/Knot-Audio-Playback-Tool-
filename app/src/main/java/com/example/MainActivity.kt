package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KnotViewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.theme.KnotAccent
import com.example.ui.theme.KnotBorder
import com.example.ui.theme.KnotObsidian
import com.example.ui.theme.KnotSurface
import com.example.ui.theme.KnotSurfaceCard
import com.example.ui.theme.KnotTextPrimary
import com.example.ui.theme.KnotTextSecondary
import com.example.ui.theme.MyApplicationTheme

enum class KnotTab(val title: String, val icon: ImageVector) {
    PLAYLISTS("Playlists", Icons.Rounded.AllInclusive),
    LIBRARY("Library", Icons.Rounded.LibraryMusic)
}

class MainActivity : ComponentActivity() {

    private val viewModel: KnotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KnotApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KnotApp(viewModel: KnotViewModel) {
    val context = LocalContext.current
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    val selectedPlaylistWithTracks by viewModel.selectedPlaylistWithTracks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(KnotTab.PLAYLISTS) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission granted/denied handled gracefully */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = KnotObsidian,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isPlayerExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Floating Mini Player (if track active)
                    MiniPlayerBar(
                        state = playbackState,
                        onExpandPlayer = { isPlayerExpanded = true },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() }
                    )

                    // Minimalist Bottom Nav Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(KnotSurfaceCard)
                            .border(1.dp, KnotBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KnotTab.values().forEach { tab ->
                            val isSelected = currentTab == tab
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) KnotAccent.copy(alpha = 0.15f) else KnotSurfaceCard)
                                    .clickable {
                                        if (currentTab != tab && selectedPlaylistWithTracks != null) {
                                            viewModel.selectPlaylist(null)
                                        }
                                        currentTab = tab
                                    }
                                    .padding(horizontal = 18.dp, vertical = 8.dp)
                                    .testTag("nav_tab_${tab.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) KnotAccent else KnotTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) KnotAccent else KnotTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen content
            when {
                selectedPlaylistWithTracks != null -> {
                    PlaylistDetailScreen(
                        playlistWithTracks = selectedPlaylistWithTracks!!,
                        allLibraryTracks = allTracks,
                        playbackState = playbackState,
                        onBack = { viewModel.selectPlaylist(null) },
                        onPlayPlaylist = { startIndex ->
                            viewModel.playPlaylist(
                                playlist = selectedPlaylistWithTracks!!.playlist,
                                tracks = selectedPlaylistWithTracks!!.tracks,
                                startIndex = startIndex
                            )
                        },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onAddTrackToPlaylist = { trackId ->
                            viewModel.addTrackToPlaylist(selectedPlaylistWithTracks!!.playlist.id, trackId)
                        },
                        onRemoveTrackFromPlaylist = { trackId ->
                            viewModel.removeTrackFromPlaylist(selectedPlaylistWithTracks!!.playlist.id, trackId)
                        },
                        onReorderTracks = { trackIds ->
                            viewModel.reorderPlaylist(selectedPlaylistWithTracks!!.playlist.id, trackIds)
                        },
                        onImportFiles = { uris ->
                            viewModel.importAudioFiles(uris)
                        }
                    )
                }

                currentTab == KnotTab.PLAYLISTS -> {
                    PlaylistsScreen(
                        playlists = allPlaylists,
                        playbackState = playbackState,
                        onSelectPlaylist = { playlistId ->
                            viewModel.selectPlaylist(playlistId)
                        },
                        onQuickPlayPlaylist = { playlist ->
                            // Fetch tracks for playlist and start continuous loop
                            val tracksInPl = selectedPlaylistWithTracks?.tracks ?: allTracks
                            viewModel.playPlaylist(playlist, tracksInPl, 0)
                        },
                        onCreatePlaylist = { name, desc ->
                            viewModel.createPlaylist(name, desc)
                        },
                        onDeletePlaylist = { playlist ->
                            viewModel.deletePlaylist(playlist)
                        }
                    )
                }

                currentTab == KnotTab.LIBRARY -> {
                    LibraryScreen(
                        tracks = filteredTracks,
                        playlists = allPlaylists,
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        isImporting = isImporting,
                        onImportFiles = { uris ->
                            viewModel.importAudioFiles(uris)
                        },
                        onPlayTrack = { track ->
                            viewModel.playTrack(track)
                        },
                        onDeleteTrack = { track ->
                            viewModel.deleteTrack(track)
                        },
                        onAddTrackToPlaylist = { playlistId, trackId ->
                            viewModel.addTrackToPlaylist(playlistId, trackId)
                        },
                        onCreatePlaylistAndAdd = { name, trackId ->
                            viewModel.createPlaylist(name, "", listOf(trackId))
                        },
                        currentPlayingTrackId = playbackState.currentTrack?.id
                    )
                }
            }

            // Expanded Full Player Sheet / Overlay
            AnimatedVisibility(
                visible = isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                BackHandler { isPlayerExpanded = false }
                PlayerScreen(
                    state = playbackState,
                    onDismiss = { isPlayerExpanded = false },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleLoopMode = { viewModel.toggleLoopMode() },
                    onSetSpeed = { viewModel.setSpeed(it) },
                    onStop = {
                        viewModel.stopPlayback()
                        isPlayerExpanded = false
                    },
                    onPlayTrackInQueue = { track, index ->
                        playbackState.queue.let { q ->
                            viewModel.playQueue(
                                queue = q,
                                startIndex = index,
                                name = playbackState.currentPlaylistName ?: "Queue"
                            )
                        }
                    }
                )
            }
        }
    }
}
