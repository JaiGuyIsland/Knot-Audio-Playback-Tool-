package com.example.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.session.MediaSession
import android.media.session.PlaybackState as SystemPlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class KnotPlaybackService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "knot_playback_channel"
        const val NOTIFICATION_ID = 404

        const val ACTION_PLAY = "com.example.knot.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.knot.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.knot.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.knot.ACTION_NEXT"
        const val ACTION_PREV = "com.example.knot.ACTION_PREV"
        const val ACTION_STOP = "com.example.knot.ACTION_STOP"
        const val ACTION_SEEK = "com.example.knot.ACTION_SEEK"
        const val ACTION_SPEED = "com.example.knot.ACTION_SPEED"
        const val ACTION_LOOP_MODE = "com.example.knot.ACTION_LOOP_MODE"

        const val EXTRA_SEEK_MS = "extra_seek_ms"
        const val EXTRA_SPEED = "extra_speed"

        private val _playbackState = MutableStateFlow(PlaybackState())
        val playbackState = _playbackState.asStateFlow()

        // In-memory queue passed to service
        var pendingQueue: List<AudioTrack> = emptyList()
        var pendingStartIndex: Int = 0
        var pendingPlaylistName: String? = null
        var pendingPlaylistId: Long? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupMediaSession()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                if (pendingQueue.isNotEmpty()) {
                    loadQueueAndPlay(pendingQueue, pendingStartIndex, pendingPlaylistName, pendingPlaylistId)
                } else if (_playbackState.value.isPaused) {
                    resumePlayback()
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_TOGGLE -> {
                if (_playbackState.value.isPlaying) {
                    pausePlayback()
                } else {
                    resumePlayback()
                }
            }
            ACTION_NEXT -> nextTrack(userInitiated = true)
            ACTION_PREV -> previousTrack()
            ACTION_STOP -> stopPlayback()
            ACTION_SEEK -> {
                val seekMs = intent.getLongExtra(EXTRA_SEEK_MS, 0L)
                seekTo(seekMs)
            }
            ACTION_SPEED -> {
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                setSpeed(speed)
            }
            ACTION_LOOP_MODE -> {
                val nextMode = if (_playbackState.value.loopMode == LoopMode.PLAYLIST) {
                    LoopMode.SINGLE_TRACK
                } else {
                    LoopMode.PLAYLIST
                }
                _playbackState.value = _playbackState.value.copy(loopMode = nextMode)
                updateNotification()
            }
            else -> {
                // If started with pending queue
                if (pendingQueue.isNotEmpty()) {
                    loadQueueAndPlay(pendingQueue, pendingStartIndex, pendingPlaylistName, pendingPlaylistId)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun setupMediaSession() {
        mediaSession = MediaSession(this, "KnotMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    nextTrack(userInitiated = true)
                }

                override fun onSkipToPrevious() {
                    previousTrack()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos)
                }
            })
            isActive = true
        }
    }

    private fun loadQueueAndPlay(
        queue: List<AudioTrack>,
        startIndex: Int,
        playlistName: String?,
        playlistId: Long?
    ) {
        if (queue.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, queue.size - 1)
        _playbackState.value = _playbackState.value.copy(
            queue = queue,
            currentIndex = safeIndex,
            currentTrack = queue[safeIndex],
            currentPlaylistName = playlistName,
            currentPlaylistId = playlistId,
            currentPositionMs = 0L,
            durationMs = queue[safeIndex].durationMs
        )
        playTrack(queue[safeIndex])
    }

    private fun playTrack(track: AudioTrack) {
        try {
            mediaPlayer?.release()
            mediaPlayer = null

            val file = File(track.filePath)
            if (!file.exists()) {
                // Track file missing, try next if queue has multiple
                val q = _playbackState.value.queue
                if (q.size > 1) {
                    nextTrack(userInitiated = false)
                }
                return
            }

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()

                // Apply speed if supported
                try {
                    val speed = _playbackState.value.playbackSpeed
                    if (speed != 1.0f) {
                        playbackParams = PlaybackParams().setSpeed(speed)
                    }
                } catch (_: Exception) {}

                setOnCompletionListener {
                    handleTrackCompletion()
                }

                start()
            }

            val actualDuration = mediaPlayer?.duration?.toLong() ?: track.durationMs
            _playbackState.value = _playbackState.value.copy(
                isPlaying = true,
                durationMs = if (actualDuration > 0) actualDuration else track.durationMs,
                currentPositionMs = 0L
            )

            updateSystemPlaybackState(SystemPlaybackState.STATE_PLAYING)
            startForeground(NOTIFICATION_ID, buildNotification())
            startProgressTicker()

        } catch (e: Exception) {
            e.printStackTrace()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
        }
    }

    private fun handleTrackCompletion() {
        val state = _playbackState.value
        when (state.loopMode) {
            LoopMode.SINGLE_TRACK -> {
                // Replay active track
                try {
                    mediaPlayer?.seekTo(0)
                    mediaPlayer?.start()
                    _playbackState.value = _playbackState.value.copy(isPlaying = true, currentPositionMs = 0L)
                } catch (_: Exception) {
                    state.currentTrack?.let { playTrack(it) }
                }
            }
            LoopMode.PLAYLIST -> {
                // Continuous unbroken playlist loop
                nextTrack(userInitiated = false)
            }
        }
    }

    private fun nextTrack(userInitiated: Boolean) {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // CONTINUOUS MODULO LOOP: loops forever back to 0
        val nextIndex = (state.currentIndex + 1) % queue.size
        val next = queue[nextIndex]

        _playbackState.value = state.copy(
            currentIndex = nextIndex,
            currentTrack = next,
            currentPositionMs = 0L,
            durationMs = next.durationMs
        )
        playTrack(next)
    }

    private fun previousTrack() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        val pos = mediaPlayer?.currentPosition ?: 0
        if (pos > 3000) {
            seekTo(0L)
            return
        }

        val prevIndex = if (state.currentIndex - 1 < 0) queue.size - 1 else state.currentIndex - 1
        val prev = queue[prevIndex]

        _playbackState.value = state.copy(
            currentIndex = prevIndex,
            currentTrack = prev,
            currentPositionMs = 0L,
            durationMs = prev.durationMs
        )
        playTrack(prev)
    }

    private fun pausePlayback() {
        try {
            mediaPlayer?.pause()
            _playbackState.value = _playbackState.value.copy(isPlaying = false)
            updateSystemPlaybackState(SystemPlaybackState.STATE_PAUSED)
            updateNotification()
            stopProgressTicker()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun resumePlayback() {
        try {
            if (mediaPlayer == null && _playbackState.value.currentTrack != null) {
                playTrack(_playbackState.value.currentTrack!!)
            } else {
                mediaPlayer?.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
                updateSystemPlaybackState(SystemPlaybackState.STATE_PLAYING)
                startForeground(NOTIFICATION_ID, buildNotification())
                startProgressTicker()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seekTo(posMs: Long) {
        try {
            val clamped = posMs.coerceIn(0L, _playbackState.value.durationMs)
            mediaPlayer?.seekTo(clamped.toInt())
            _playbackState.value = _playbackState.value.copy(currentPositionMs = clamped)
        } catch (_: Exception) {}
    }

    private fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        try {
            mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed)
        } catch (_: Exception) {}
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
        _playbackState.value = _playbackState.value.copy(isPlaying = false, currentPositionMs = 0L)
        stopProgressTicker()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive && _playbackState.value.isPlaying) {
                try {
                    val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    val dur = mediaPlayer?.duration?.toLong() ?: _playbackState.value.durationMs
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = if (dur > 0) dur else _playbackState.value.durationMs
                    )
                } catch (_: Exception) {}
                delay(250)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playbackState.value = _playbackState.value.copy(sleepTimerRemainingMinutes = null)
            return
        }

        _playbackState.value = _playbackState.value.copy(sleepTimerRemainingMinutes = minutes)
        sleepTimerJob = serviceScope.launch {
            var rem = minutes
            while (isActive && rem > 0) {
                delay(60_000)
                rem -= 1
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingMinutes = rem)
            }
            if (isActive) {
                pausePlayback()
                _playbackState.value = _playbackState.value.copy(sleepTimerRemainingMinutes = null)
            }
        }
    }

    private fun updateSystemPlaybackState(state: Int) {
        val actions = SystemPlaybackState.ACTION_PLAY or
                SystemPlaybackState.ACTION_PAUSE or
                SystemPlaybackState.ACTION_SKIP_TO_NEXT or
                SystemPlaybackState.ACTION_SKIP_TO_PREVIOUS or
                SystemPlaybackState.ACTION_SEEK_TO

        mediaSession?.setPlaybackState(
            SystemPlaybackState.Builder()
                .setActions(actions)
                .setState(state, _playbackState.value.currentPositionMs, _playbackState.value.playbackSpeed)
                .build()
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name_playback),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_desc_playback)
                setShowBadge(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val state = _playbackState.value
        val track = state.currentTrack

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, KnotPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, KnotPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, KnotPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, KnotPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val loopModeStr = if (state.loopMode == LoopMode.PLAYLIST) "Playlist Loop" else "Single Loop"
        val subtitle = "${track?.artist ?: "Knot"} • $loopModeStr"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(track?.title ?: "Knot")
            .setContentText(subtitle)
            .setSubText(state.currentPlaylistName ?: "Continuous Loop")
            .setContentIntent(contentIntent)
            .setOngoing(state.isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                getString(R.string.action_previous),
                prevIntent
            )
            .addAction(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (state.isPlaying) getString(R.string.action_pause) else getString(R.string.action_play),
                playPauseIntent
            )
            .addAction(
                android.R.drawable.ic_media_next,
                getString(R.string.action_next),
                nextIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.action_stop),
                stopIntent
            )

        return builder.build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        serviceJob.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        mediaSession = null
        _playbackState.value = PlaybackState()
        super.onDestroy()
    }
}
