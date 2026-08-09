package com.finalplayer.app.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.finalplayer.app.MainActivity
import com.finalplayer.app.player.PlayerActivity
import com.finalplayer.app.player.core.MPVController
import org.koin.android.ext.android.inject

class MediaPlaybackService : Service() {

    private val binder = LocalBinder()
    private var mediaSession: MediaSessionCompat? = null
    private val mpvController: MPVController by inject()

    private var currentTitle: String = "FinalPlayer"
    private var currentArtist: String = "Media Playback"
    private var currentUri: String? = null

    private var lastNotificationUpdateMs = 0L

    companion object {
        const val CHANNEL_ID = "final_player_media_channel"
        const val NOTIFICATION_ID = 1002

        const val ACTION_PLAY = "com.finalplayer.app.action.PLAY"
        const val ACTION_PAUSE = "com.finalplayer.app.action.PAUSE"
        const val ACTION_PREVIOUS = "com.finalplayer.app.action.PREVIOUS"
        const val ACTION_NEXT = "com.finalplayer.app.action.NEXT"

        fun startService(context: Context, title: String, uri: String) {
            val intent = Intent(context, MediaPlaybackService::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_URI", uri)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MediaPlaybackService::class.java)
            context.stopService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MediaPlaybackService = this@MediaPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "MediaPlaybackServiceSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    mpvController.resume()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                }

                override fun onPause() {
                    mpvController.pause()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                }

                override fun onSeekTo(pos: Long) {
                    mpvController.seekTo(pos)
                }

                override fun onSkipToNext() {
                    mpvController.seekBy(10)
                }

                override fun onSkipToPrevious() {
                    mpvController.seekBy(-10)
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.action
            when (action) {
                ACTION_PLAY -> mpvController.resume()
                ACTION_PAUSE -> mpvController.pause()
                ACTION_PREVIOUS -> mpvController.seekBy(-10)
                ACTION_NEXT -> mpvController.seekBy(10)
                else -> {
                    val title = it.getStringExtra("EXTRA_TITLE") ?: currentTitle
                    val uri = it.getStringExtra("EXTRA_URI")
                    if (uri != null) {
                        setMediaInfo(title, "Local Video", null, uri)
                    }
                }
            }
        }

        val notification = buildNotification(mpvController.playerState.value.isPlaying)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun setMediaInfo(title: String, artist: String, thumbnailUri: String?, mediaUri: String) {
        currentTitle = title
        currentArtist = artist
        currentUri = mediaUri

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .build()
        mediaSession?.setMetadata(metadata)

        updateNotificationThrottled()
    }

    fun updatePlaybackState(state: Int) {
        val position = mpvController.playerState.value.positionMs
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, position, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
        updateNotificationThrottled()
    }

    private fun updateNotificationThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateMs >= 1000) {
            lastNotificationUpdateMs = now
            val isPlaying = mpvController.playerState.value.isPlaying
            val notification = buildNotification(isPlaying)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for current media playback"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, PlayerActivity::class.java).apply {
                currentUri?.let { putExtra("EXTRA_VIDEO_PATH", it) }
                putExtra("EXTRA_VIDEO_TITLE", currentTitle)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevIntent = PendingIntent.getService(
            this, 1, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseActionIntent = PendingIntent.getService(
            this, 2, Intent(this, MediaPlaybackService::class.java).apply {
                action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val nextIntent = PendingIntent.getService(
            this, 3, Intent(this, MediaPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPauseActionIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
