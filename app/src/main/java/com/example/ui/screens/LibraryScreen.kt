package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Playlist
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
fun LibraryScreen(
    tracks: List<AudioTrack>,
    playlists: List<Playlist>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isImporting: Boolean,
    onImportFiles: (List<Uri>) -> Unit,
    onPlayTrack: (AudioTrack) -> Unit,
    onDeleteTrack: (AudioTrack) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylistAndAdd: (String, Long) -> Unit,
    currentPlayingTrackId: Long?,
    modifier: Modifier = Modifier
) {
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onImportFiles(uris)
        }
    }

    var trackToAddToPlaylist by remember { mutableStateOf<AudioTrack?>(null) }
    var trackToDelete by remember { mutableStateOf<AudioTrack?>(null) }
    var showCreatePlaylistInDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KnotSurface)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header & Upload Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Audio Library",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = KnotTextPrimary
                )
                Text(
                    text = "${tracks.size} ${if (tracks.size == 1) "sound loop" else "sound loops"} ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KnotTextSecondary
                )
            }

            // Upload Button
            Button(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "audio/*",
                            "application/ogg",
                            "audio/mpeg",
                            "audio/wav",
                            "audio/mp4",
                            "audio/flac"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KnotAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("upload_audio_button")
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Importing...", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.UploadFile,
                        contentDescription = "Upload",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Audio", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search sound loops...", color = KnotTextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = KnotTextSecondary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = KnotTextSecondary
                        )
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = KnotSurfaceCard,
                unfocusedContainerColor = KnotSurfaceCard,
                focusedBorderColor = KnotAccent,
                unfocusedBorderColor = KnotBorder,
                focusedTextColor = KnotTextPrimary,
                unfocusedTextColor = KnotTextPrimary
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("library_search_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tracks List or Empty State
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
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(KnotSurfaceCard)
                            .border(1.dp, KnotBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "No audio",
                            tint = KnotAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No matching loops found" else "No audio files uploaded yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KnotTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Upload any MP3, WAV, AAC, or FLAC files to build custom looping playlists.",
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
                items(tracks, key = { it.id }) { track ->
                    val isPlaying = currentPlayingTrackId == track.id
                    var menuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPlaying) KnotAccent.copy(alpha = 0.12f) else KnotSurfaceCard)
                            .border(
                                1.dp,
                                if (isPlaying) KnotAccent.copy(alpha = 0.4f) else KnotBorderSubtle,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onPlayTrack(track) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("track_item_${track.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Icon indicator
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) KnotAccent else KnotSurface)
                                .border(1.dp, if (isPlaying) KnotAccent else KnotBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isPlaying) Color.Black else KnotTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Info
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isPlaying) KnotAccent else KnotTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = KnotTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = " • ",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = KnotTextSecondary
                                )
                                Text(
                                    text = formatDuration(track.durationMs),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = KnotTextSecondary
                                )
                                if (track.isPreset) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PRESET",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = KnotAccent,
                                        modifier = Modifier
                                            .background(KnotAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // More Actions
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.testTag("track_menu_${track.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Options",
                                    tint = KnotTextSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(KnotSurfaceCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Playlist", color = KnotTextPrimary) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.PlaylistAdd,
                                            contentDescription = null,
                                            tint = KnotAccent
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        trackToAddToPlaylist = track
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Sound", color = KnotCoral) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = KnotCoral
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        trackToDelete = track
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add to Playlist Dialog
    trackToAddToPlaylist?.let { targetTrack ->
        AlertDialog(
            onDismissRequest = {
                trackToAddToPlaylist = null
                showCreatePlaylistInDialog = false
            },
            containerColor = KnotSurfaceCard,
            title = {
                Text(
                    text = "Add to Playlist",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = KnotTextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select a playlist for '${targetTrack.title}':",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KnotTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (playlists.isEmpty() || showCreatePlaylistInDialog) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text("Playlist name (e.g. Deep Focus)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = KnotTextPrimary,
                                unfocusedTextColor = KnotTextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(playlists) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onAddTrackToPlaylist(pl.id, targetTrack.id)
                                            trackToAddToPlaylist = null
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlaylistAdd,
                                        contentDescription = null,
                                        tint = KnotAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = pl.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = KnotTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showCreatePlaylistInDialog = true }
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = KnotAccent)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create new playlist", color = KnotAccent)
                        }
                    }
                }
            },
            confirmButton = {
                if (showCreatePlaylistInDialog || playlists.isEmpty()) {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylistAndAdd(newPlaylistName.trim(), targetTrack.id)
                                newPlaylistName = ""
                                showCreatePlaylistInDialog = false
                                trackToAddToPlaylist = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KnotAccent, contentColor = Color.Black)
                    ) {
                        Text("Create & Add")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    trackToAddToPlaylist = null
                    showCreatePlaylistInDialog = false
                }) {
                    Text("Cancel", color = KnotTextSecondary)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    trackToDelete?.let { targetTrack ->
        AlertDialog(
            onDismissRequest = { trackToDelete = null },
            containerColor = KnotSurfaceCard,
            title = {
                Text("Delete Audio Loop", color = KnotTextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete '${targetTrack.title}'? This will remove it from all playlists and delete the local audio file.",
                    color = KnotTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTrack(targetTrack)
                        trackToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KnotCoral)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { trackToDelete = null }) {
                    Text("Cancel", color = KnotTextSecondary)
                }
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "--:--"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
