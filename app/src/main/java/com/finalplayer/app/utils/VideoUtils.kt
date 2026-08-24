package com.finalplayer.app.utils

import java.io.File

/**
 * Single source of truth for normalizing video paths into unified keys for playback tracking,
 * Room database persistence, and SharedPreferences storage.
 */
fun normalizeVideoKey(rawPath: String): String {
    if (rawPath.isBlank()) return ""
    return when {
        rawPath.startsWith("content://") -> rawPath
        rawPath.startsWith("http://") || rawPath.startsWith("https://") ||
        rawPath.startsWith("smb://") || rawPath.startsWith("ftp://") -> rawPath
        else -> try {
            val clean = if (rawPath.startsWith("file://")) rawPath.substring(7) else rawPath
            File(clean).canonicalPath
        } catch (e: Exception) {
            rawPath
        }
    }
}
