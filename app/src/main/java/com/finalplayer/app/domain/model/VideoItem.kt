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
            val isVertical = try {
                if (!resolution.isNullOrBlank()) {
                    val parts = resolution.split("x", "X")
                    if (parts.size == 2) {
                        val width = parts[0].trim().toInt()
                        val height = parts[1].trim().toInt()
                        height > width
                    } else false
                } else false
            } catch (e: Exception) {
                false
            }

            val path = "$folderPath/$uri"
            val isPersonalVideo = path.contains("DCIM/Camera", ignoreCase = true) ||
                    path.contains("DCIM", ignoreCase = true) ||
                    path.contains("Camera", ignoreCase = true) ||
                    path.contains("ScreenRecorder", ignoreCase = true) ||
                    path.contains("Screen_Recording", ignoreCase = true) ||
                    path.contains("ScreenRecordings", ignoreCase = true)

            return isVertical && !isPersonalVideo
        }
}
