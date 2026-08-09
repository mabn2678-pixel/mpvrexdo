package com.finalplayer.app.domain.model

data class SearchResults(
    val folders: List<VideoFolder>,
    val videos: List<VideoItem>,
    val isEmpty: Boolean
) {
    companion object {
        val Empty = SearchResults(emptyList(), emptyList(), true)
    }
}
