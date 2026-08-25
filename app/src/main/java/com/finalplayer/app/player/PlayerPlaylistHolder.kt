package com.finalplayer.app.player

import com.finalplayer.app.domain.model.VideoItem

/**
 * In-memory holder for passing playlist items to PlayerActivity safely
 * without risking TransactionTooLargeException or Binder IPC limits on large folders.
 */
object PlayerPlaylistHolder {
    @Volatile
    private var playlist: List<VideoItem>? = null
    @Volatile
    private var initialIndex: Int = 0

    fun setPlaylist(items: List<VideoItem>, startIndex: Int) {
        playlist = items
        initialIndex = startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    }

    fun getPlaylist(): Pair<List<VideoItem>, Int>? {
        val currentList = playlist ?: return null
        val currentIndex = initialIndex
        // Keep or clear
        return Pair(currentList, currentIndex)
    }

    fun clear() {
        playlist = null
        initialIndex = 0
    }
}
