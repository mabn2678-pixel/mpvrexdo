package com.finalplayer.app.ui.home

import com.finalplayer.app.data.preferences.DEFAULT_VISIBLE_FIELDS
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem

data class HomeUiState(
    val folders: List<VideoFolder> = emptyList(),
    val allVideos: List<VideoItem> = emptyList(),
    val playedVideoIds: Set<String> = emptySet(),
    val playbackProgressMap: Map<String, PlaybackProgress> = emptyMap(),
    val isLoading: Boolean = false,
    val selectedTab: HomeTab = HomeTab.HOME,
    val sortBy: String = "title",
    val sortAscending: Boolean = true,
    val viewMode: String = "folder",
    val layoutMode: String = "list",
    val visibleFields: Set<String> = DEFAULT_VISIBLE_FIELDS,
    val onlyForFolderList: Boolean = false,
    val showAudioFiles: Boolean = false
)

enum class HomeTab {
    RECENTS,
    SHORTS,
    HOME
}
