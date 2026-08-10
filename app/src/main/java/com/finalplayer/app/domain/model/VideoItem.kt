package com.finalplayer.app.domain.model

data class VideoItem(
    val id: String,
    val uri: String,
    val title: String,
    val duration: Long,
    val sizeBytes: Long,
    val thumbnailPath: String? = null,
    val dateAdded: Long,
    val resolution: String? = null,
    val folderPath: String
) {
    val isShortPlatformVideo: Boolean
        get() {
            val fullPathLower = "$folderPath/$uri $title".lowercase(java.util.Locale.ROOT)
            val isSocialKeyword = fullPathLower.contains("tiktok") ||
                    fullPathLower.contains("shorts") ||
                    fullPathLower.contains("ytshort") ||
                    fullPathLower.contains("reel") ||
                    fullPathLower.contains("reels") ||
                    fullPathLower.contains("instagram") ||
                    fullPathLower.contains("kwai") ||
                    fullPathLower.contains("likee") ||
                    fullPathLower.contains("snapchat") ||
                    fullPathLower.contains("snackvideo") ||
                    fullPathLower.contains("vigo") ||
                    fullPathLower.contains("douyin")

            if (isSocialKeyword) return true

            val isVertical = try {
                if (!resolution.isNullOrBlank()) {
                    val parts = resolution.split("x", "X")
                    if (parts.size == 2) {
                        val width = parts[0].trim().toInt()
                        val height = parts[1].trim().toInt()
                        height >= width && width > 0
                    } else false
                } else false
            } catch (e: Exception) {
                false
            }

            // Short or vertical video (duration <= 10 minutes or duration == 0)
            val isShortDuration = duration <= 600_000L || duration == 0L

            return isVertical && isShortDuration
        }
}
