package com.finalplayer.app.ui.player.controls.components.sheets

data class TrackNode(
    val id: Int,
    val type: String,           // "sub" | "audio" | "video"
    val lang: String = "",       // "eng", "ara", "jpn", ""
    val title: String = "",      // اسم المسار
    val isDefault: Boolean = false,
    val forced: Boolean = false,
    val hearingImpaired: Boolean = false,
    val external: Boolean = false,
    val externalFilename: String? = null,
    val isImage: Boolean = false
) {
    val isSubtitle: Boolean get() = type == "sub"
    val isAudio: Boolean get() = type == "audio"
    val isVideo: Boolean get() = type == "video" && !isImage
    val isAlbumArtwork: Boolean get() = type == "video" && isImage

    val displayName: String get() = when {
        title.isNotBlank() -> title
        lang.isNotBlank() -> lang.uppercase()
        external -> externalFilename
            ?.substringAfterLast("/")
            ?.substringBeforeLast(".") ?: "External"
        else -> "Track $id"
    }
}
