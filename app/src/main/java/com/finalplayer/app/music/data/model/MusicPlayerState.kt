package com.finalplayer.app.music.data.model

data class MusicPlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: Int = 0,
    val shuffleEnabled: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = 0
)
