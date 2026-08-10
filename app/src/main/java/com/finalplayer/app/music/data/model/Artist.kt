package com.finalplayer.app.music.data.model

import android.net.Uri

data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val albumArtUri: Uri? = null
)

