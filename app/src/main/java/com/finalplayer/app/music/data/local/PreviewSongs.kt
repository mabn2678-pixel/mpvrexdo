package com.finalplayer.app.music.data.local

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.finalplayer.app.music.data.model.Song

object PreviewSongs {

    fun getPreviewSongs(context: Context): List<Song> {
        val ringtoneManager = RingtoneManager(context)
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
        val cursor = ringtoneManager.cursor

        val songs = mutableListOf<Song>()
        var index = 0

        val previewTitles = listOf("نغمة المعاينة ١", "نغمة المعاينة ٢", "نغمة المعاينة ٣")
        val previewArtists = listOf("FinalPlayer", "FinalPlayer", "FinalPlayer")

        if (cursor != null) {
            while (cursor.moveToNext() && index < 3) {
                try {
                    val uri: Uri = ringtoneManager.getRingtoneUri(index)
                    songs.add(
                        Song(
                            id = -(index + 1L),          // negative ID = preview
                            title = previewTitles[index],
                            artist = previewArtists[index],
                            album = "معاينة",
                            albumId = -1L,
                            duration = 30_000L,           // 30 seconds default
                            path = uri.toString(),
                            uri = uri,
                            albumArtUri = null,
                            trackNumber = index + 1,
                            year = 2024,
                            dateAdded = System.currentTimeMillis(),
                            size = 0L
                        )
                    )
                    index++
                } catch (e: Exception) {
                    index++
                }
            }
            cursor.close()
        }

        // Fallback: if no ringtones found, use notification sounds
        if (songs.isEmpty()) {
            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            listOf(
                Triple(ringUri, "نغمة النداء", 0),
                Triple(notifUri, "نغمة الإشعار", 1),
                Triple(alarmUri, "نغمة المنبّه", 2)
            ).forEachIndexed { i, (uri, title, _) ->
                if (uri != null) {
                    songs.add(
                        Song(
                            id = -(i + 1L),
                            title = title,
                            artist = "FinalPlayer",
                            album = "معاينة",
                            albumId = -1L,
                            duration = 30_000L,
                            path = uri.toString(),
                            uri = uri,
                            albumArtUri = null,
                            trackNumber = i + 1,
                            year = 2024,
                            dateAdded = System.currentTimeMillis(),
                            size = 0L
                        )
                    )
                }
            }
        }

        return songs
    }

    fun isPreviewSong(song: Song): Boolean = song.id < 0
}
