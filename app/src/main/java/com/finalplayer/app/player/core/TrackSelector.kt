package com.finalplayer.app.player.core

import com.finalplayer.app.data.preferences.AudioPreferences
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TrackSelector(
    private val subtitlePreferences: SubtitlesPreferences,
    private val audioPreferences: AudioPreferences
) {
    suspend fun onFileLoaded(
        hasState: Boolean = false,
        mpvController: MPVController
    ) = withContext(Dispatchers.Main) {
        var attempts = 0
        while (attempts < 20) {
            val count = mpvController.getAttachedView()?.getPropertyInt("track-list/count") ?: 0
            if (count > 0) break
            delay(50)
            attempts++
        }

        val trackCount = mpvController.getAttachedView()?.getPropertyInt("track-list/count") ?: 0
        if (trackCount == 0) return@withContext

        val tracks = readAllTracks(mpvController, trackCount)

        if (tracks.none { it.isVideo }) return@withContext

        val subtitleSelector = SubtitleTrackSelector(subtitlePreferences)
        subtitleSelector.selectSubtitleTrack(hasSavedState = hasState, mpvController = mpvController)
        selectBestAudioTrack(tracks, hasState, mpvController)
    }

    private fun readAllTracks(mpvController: MPVController, count: Int): List<TrackNode> {
        val view = mpvController.getAttachedView() ?: return emptyList()
        return (0 until count).mapNotNull { i ->
            val id = view.getPropertyInt("track-list/$i/id") ?: return@mapNotNull null
            val type = view.getPropertyString("track-list/$i/type") ?: return@mapNotNull null
            TrackNode(
                id = id,
                type = type,
                lang = view.getPropertyString("track-list/$i/lang")?.lowercase() ?: "",
                title = view.getPropertyString("track-list/$i/title")?.lowercase() ?: "",
                isDefault = view.getPropertyString("track-list/$i/default") == "yes" ||
                        (view.getTrackList().firstOrNull { it.id == id }?.isDefault == true),
                forced = view.getPropertyString("track-list/$i/forced") == "yes",
                hearingImpaired = view.getPropertyString("track-list/$i/hearing-impaired") == "yes",
                external = view.getPropertyString("track-list/$i/external") == "yes",
                externalFilename = view.getPropertyString("track-list/$i/external-filename"),
                isImage = view.getPropertyString("track-list/$i/image") == "yes"
            )
        }
    }

    private suspend fun selectBestAudioTrack(
        tracks: List<TrackNode>,
        hasState: Boolean,
        mpvController: MPVController
    ) {
        val currentAid = mpvController.getCurrentAid()
        if (hasState && currentAid > 0) return

        val prefLangs = audioPreferences.preferredLanguages.get()
            .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }

        val audioTracks = tracks.filter { it.isAudio }
        val ignoreKeywords = listOf("commentary", "description", "adh", "comment")

        if (prefLangs.isNotEmpty()) {
            for (lang in prefLangs) {
                val track = audioTracks.firstOrNull { t ->
                    (t.lang == lang || t.lang.startsWith(lang)) &&
                    ignoreKeywords.none { kw -> t.title.contains(kw) }
                }
                if (track != null) {
                    if (currentAid != track.id) mpvController.selectAudioTrack(track.id)
                    return
                }
            }
        }

        if (currentAid <= 0) {
            val fallback = audioTracks.firstOrNull { t ->
                ignoreKeywords.none { kw -> t.title.contains(kw) }
            }
            fallback?.let { mpvController.selectAudioTrack(it.id) }
        }
    }

    private fun isAnimeContent(tracks: List<TrackNode>, mpvController: MPVController): Boolean {
        val view = mpvController.getAttachedView()
        val path = view?.getPropertyString("path") ?: ""
        val title = view?.getPropertyString("media-title") ?: ""
        val filename = view?.getPropertyString("filename") ?: ""

        val hasJapaneseAudio = tracks.any { it.isAudio && it.lang in listOf("jpn", "ja") }
        val hasCrcHash = Regex("\\[[0-9a-fA-F]{8}\\]").containsMatchIn(filename)
        val hasFansubBrackets = Regex("\\[.*\\]").containsMatchIn(title)
        val isAnimeFolder = path.lowercase().contains("/anime/")

        return hasJapaneseAudio || hasCrcHash || hasFansubBrackets || isAnimeFolder
    }
}

