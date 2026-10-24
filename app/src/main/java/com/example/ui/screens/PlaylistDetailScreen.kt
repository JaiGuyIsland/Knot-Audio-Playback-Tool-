package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.data.model.PlaylistWithTracks
import com.example.playback.PlaybackState
import com.example.ui.theme.KnotAccent
import com.example.ui.theme.KnotBorder
import com.example.ui.theme.KnotBorderSubtle
import com.example.ui.theme.KnotCoral
import com.example.ui.theme.KnotSurface
import com.example.ui.theme.KnotSurfaceCard
import com.example.ui.theme.KnotTextPrimary
import com.example.ui.theme.KnotTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistWithTracks: PlaylistWithTracks,
    allLibraryTracks: List<AudioTrack>,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onPlayPlaylist: (startIndex: Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onAddTrackToPlaylist: (Long) -> Unit,
    onRemoveTrackFromPlaylist: (Long) -> Unit,
    onReorderTracks: (List<Long>) -> Unit,
    onImportFiles: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val playlist = playlistWithTracks.playlist
    val tracks = playlistWithTracks.tracks
    val isCurrentlyLooping = playbackState.currentPlaylistId == playlist.id && playbackState.isPlaying

    var showAddTracksSheet by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KnotSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Top Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("playlist_detail_back_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = KnotTextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = KnotTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${tracks.size} tracks • Cycle ${formatDuration(playlistWithTracks.totalDurationMs)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = KnotTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Continuous Loop Banner Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isCurrentlyLooping) KnotAccent.copy(alpha = 0.15f) else KnotSurfaceCard)
                .border(
                    1.dp,
                    if (isCurrentlyLooping) KnotAccent.copy(alpha = 0.6f) else KnotBorder,
                    RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AllInclusive,
                            contentDescription = null,
                            tint = KnotAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCurrentlyLooping) "CONTINUOUSLY LOOPING" else "CONTINUOUS LOOP READY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KnotAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isCurrentlyLooping)
                            "Playing tracks in sequence and cycling forever."
                        else
                            "Start this playlist to loop through all tracks without interruption.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = KnotTextSecondary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Play / Pause Loop Hero Button
                Button(
                    onClick = {
                        if (isCurrentlyLooping) {
                            onTogglePlayPause()
                        } else {
                            onPlayPlaylist(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyLooping) KnotSurfaceCard else KnotAccent,
                        contentColor = if (isCurrentlyLooping) KnotAccent else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("loop_playlist_hero_button")
                ) {
                    Icon(
                        imageVector = if (isCurrentlyLooping) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isCurrentlyLooping) "Pause" else "Start Loop",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCurrentlyLooping) "Pause Loop" else "Loop Playlist",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracks Section Header & Add Tracks Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Track Sequence",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = KnotTextPrimary
            )

            Button(
                onClick = { showAddTracksSheet = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KnotSurfaceCard,
                    contentColor = KnotAccent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .border(1.dp, KnotBorder, RoundedCornerShape(10.dp))
                    .testTag("add_tracks_to_playlist_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Sounds", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tracks List
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = KnotTextSecondary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This playlist is empty",
                        style = MaterialTheme.typography.titleMedium,
                        color = KnotTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Add audio files from your library or upload new loops to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KnotTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    val isTrackPlaying = isCurrentlyLooping && playbackState.currentTrack?.id == track.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTrackPlaying) KnotAccent.copy(alpha = 0.12f) else KnotSurfaceCard)
                            .border(
                                1.dp,
                                if (isTrackPlaying) KnotAccent.copy(alpha = 0.5f) else KnotBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onPlayPlaylist(index) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("playlist_track_${track.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Order number or equalizer
                        Box(
                            modifier = Modifier.width(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTrackPlaying) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = KnotAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = KnotTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (isTrackPlaying) FontWeight.Bold else FontWeight.SemiBold
                                ),
                                color = if (isTrackPlaying) KnotAccent else KnotTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = KnotTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Reorder controls
                        if (index > 0) {
                            IconButton(
                                onClick = {
                                    val mutableList = tracks.map { it.id }.toMutableList()
                                    val item = mutableList.removeAt(index)
                                    mutableList.add(index - 1, item)
                                    onReorderTracks(mutableList)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = KnotTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (index < tracks.size - 1) {
                            IconButton(
                                onClick = {
                                    val mutableList = tracks.map { it.id }.toMutableList()
                                    val item = mutableList.removeAt(index)
                                    mutableList.add(index + 1, item)
                                    onReorderTracks(mutableList)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = KnotTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Remove from playlist
                        IconButton(
                            onClick = { onRemoveTrackFromPlaylist(track.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Remove",
                                tint = KnotCoral.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Add Tracks to Playlist
    if (showAddTracksSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddTracksSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = KnotSurfaceCard
        ) {
            val existingIds = remember(tracks) { tracks.map { it.id }.toSet() }
            val availableTracks = remember(allLibraryTracks, existingIds) {
                allLibraryTracks.filter { it.id !in existingIds }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Sounds to '${playlist.name}'",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = KnotTextPrimary
                    )
                    IconButton(onClick = { showAddTracksSheet = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = KnotTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Upload new audio directly from this sheet
                OutlinedButton(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf("audio/*", "application/ogg", "audio/mpeg", "audio/wav", "audio/mp4", "audio/flac")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.UploadFile, contentDescription = null, tint = KnotAccent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload New Audio File", color = KnotAccent)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (availableTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (allLibraryTracks.isEmpty())
                                "No tracks in library yet. Use the upload button above!"
                            else
                                "All audio files from your library are already in this playlist.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KnotTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    Text(
                        text = "Available Library Sounds",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = KnotTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(availableTracks) { _, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KnotSurface)
                                    .clickable {
                                        onAddTrackToPlaylist(track.id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = KnotAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                        color = KnotTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${formatDuration(track.durationMs)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                        color = KnotTextSecondary
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add",
                                    tint = KnotAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
