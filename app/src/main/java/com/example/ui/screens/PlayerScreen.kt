package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioTrack
import com.example.playback.LoopMode
import com.example.playback.PlaybackState
import com.example.ui.components.KnotVisualizer
import com.example.ui.components.LoopBadge
import com.example.ui.theme.KnotAccent
import com.example.ui.theme.KnotBorder
import com.example.ui.theme.KnotObsidian
import com.example.ui.theme.KnotSurfaceCard
import com.example.ui.theme.KnotTextPrimary
import com.example.ui.theme.KnotTextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlaybackState,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleLoopMode: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStop: () -> Unit,
    onPlayTrackInQueue: (AudioTrack, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = state.currentTrack ?: return
    var showQueueSheet by remember { mutableStateOf(false) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var userSliderPos by remember { mutableStateOf(0f) }

    val displayPosMs = if (isUserDraggingSlider) {
        (userSliderPos * state.durationMs).toLong()
    } else {
        state.currentPositionMs
    }

    val displayProgress = if (state.durationMs > 0) {
        if (isUserDraggingSlider) userSliderPos else state.progress
    } else 0f

    val speeds = listOf(0.5f, 0.8f, 1.0f, 1.2f, 1.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KnotObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("player_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse Player",
                    tint = KnotTextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = state.currentPlaylistName ?: "Knot Loop",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = KnotTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.loopMode == LoopMode.PLAYLIST) "Unbroken Playlist Cycle" else "Infinite Track Loop",
                    style = MaterialTheme.typography.labelSmall,
                    color = KnotAccent
                )
            }

            IconButton(
                onClick = { showQueueSheet = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("player_queue_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.QueueMusic,
                    contentDescription = "View Queue",
                    tint = KnotTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Animated Knot Visualizer
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            KnotVisualizer(
                isPlaying = state.isPlaying,
                sizeDp = 270.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title & Artist
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = KnotTextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = KnotTextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Loop Mode Badge
            LoopBadge(
                loopMode = state.loopMode,
                onToggle = onToggleLoopMode
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress Slider & Timestamps
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = displayProgress.coerceIn(0f, 1f),
                onValueChange = {
                    isUserDraggingSlider = true
                    userSliderPos = it
                },
                onValueChangeFinished = {
                    isUserDraggingSlider = false
                    val targetMs = (userSliderPos * state.durationMs).toLong()
                    onSeekTo(targetMs)
                },
                colors = SliderDefaults.colors(
                    thumbColor = KnotAccent,
                    activeTrackColor = KnotAccent,
                    inactiveTrackColor = KnotBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(displayPosMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KnotTextSecondary
                )
                Text(
                    text = formatMs(state.durationMs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = KnotTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Playback Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("player_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = KnotTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Big Play/Pause
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(KnotTextPrimary)
                    .testTag("player_play_pause_button")
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                    tint = KnotObsidian,
                    modifier = Modifier.size(38.dp)
                )
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Track",
                    tint = KnotTextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Speed Selector & Stop
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = "Speed",
                    tint = KnotTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                speeds.forEach { speed ->
                    val isSelected = state.playbackSpeed == speed
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) KnotAccent.copy(alpha = 0.2f) else KnotSurfaceCard)
                            .border(
                                1.dp,
                                if (isSelected) KnotAccent else KnotBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSetSpeed(speed) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("speed_chip_${speed}")
                    ) {
                        Text(
                            text = "${speed}x",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) KnotAccent else KnotTextSecondary
                        )
                    }
                }
            }

            // Stop Button
            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KnotSurfaceCard)
                    .testTag("player_stop_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop",
                    tint = KnotTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Modal Queue Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = KnotSurfaceCard
        ) {
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
                        text = "Loop Queue (${state.queue.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = KnotTextPrimary
                    )
                    IconButton(onClick = { showQueueSheet = false }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = KnotTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    itemsIndexed(state.queue) { index, item ->
                        val isCurrent = index == state.currentIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isCurrent) KnotAccent.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isCurrent) KnotAccent.copy(alpha = 0.4f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onPlayTrackInQueue(item, index)
                                    showQueueSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrent) KnotAccent else KnotTextSecondary,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) KnotAccent else KnotTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.artist,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                    color = KnotTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = formatMs(item.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = KnotTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
