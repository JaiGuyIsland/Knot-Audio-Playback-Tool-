package com.example.playback

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.model.AudioTrack
import kotlinx.coroutines.flow.StateFlow

object PlaybackController {

    val state: StateFlow<PlaybackState> = KnotPlaybackService.playbackState

    fun playQueue(
        context: Context,
        queue: List<AudioTrack>,
        startIndex: Int = 0,
        playlistName: String? = null,
        playlistId: Long? = null
    ) {
        if (queue.isEmpty()) return
        KnotPlaybackService.pendingQueue = queue
        KnotPlaybackService.pendingStartIndex = startIndex
        KnotPlaybackService.pendingPlaylistName = playlistName
        KnotPlaybackService.pendingPlaylistId = playlistId

        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_PLAY
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun playTrack(context: Context, track: AudioTrack) {
        playQueue(
            context = context,
            queue = listOf(track),
            startIndex = 0,
            playlistName = "Single Track",
            playlistId = null
        )
    }

    fun togglePlayPause(context: Context) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_TOGGLE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun next(context: Context) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_NEXT
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun previous(context: Context) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_PREV
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun seekTo(context: Context, positionMs: Long) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_SEEK
            putExtra(KnotPlaybackService.EXTRA_SEEK_MS, positionMs)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun setSpeed(context: Context, speed: Float) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_SPEED
            putExtra(KnotPlaybackService.EXTRA_SPEED, speed)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun toggleLoopMode(context: Context) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_LOOP_MODE
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        val intent = Intent(context, KnotPlaybackService::class.java).apply {
            action = KnotPlaybackService.ACTION_STOP
        }
        ContextCompat.startForegroundService(context, intent)
    }
}
