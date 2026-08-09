package com.finalplayer.app.domain.model

data class RecentVideoItem(
    val video: VideoItem,
    val progress: PlaybackProgress,
    val progressPercent: Float,    // 0f-100f
    val isCompleted: Boolean,      // progress > 95%
    val lastPlayedFormatted: String // "اليوم", "أمس", "منذ 3 أيام"
)
