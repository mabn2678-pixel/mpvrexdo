package com.finalplayer.app.domain.model

data class PlaybackProgress(
    val videoId: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedTimestamp: Long,
    val isCompleted: Boolean
)
