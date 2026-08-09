package com.finalplayer.app.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.finalplayer.app.MainActivity

class PlaybackService : Service() {

    private var mediaSession: MediaSessionCompat? = null

    companion object {
        const val CHANNEL_ID = "final_player_media_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_PLAY = "com.finalplayer.app.action.PLAY"
        const val ACTION_PAUSE = "com.finalplayer.app.action.PAUSE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Initialize basic MediaSession
        mediaSession = MediaSessionCompat(this, "FinalPlayerMediaSession").apply {
            isActive = true
        }

        // TODO: connect MPVController to MediaSession in a later phase
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("FinalPlayer Video", "Playback in progress")
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media playback control channel"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, contentText: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }
}
