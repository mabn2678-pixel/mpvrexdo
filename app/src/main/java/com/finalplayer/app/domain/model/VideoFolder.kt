package com.finalplayer.app.domain.model

data class VideoFolder(
    val path: String,
    val name: String,
    val videoCount: Int,
    val totalDuration: Long,
    val totalSizeBytes: Long,
    val lastModified: Long,
    val coverThumbnail: String? = null
)
