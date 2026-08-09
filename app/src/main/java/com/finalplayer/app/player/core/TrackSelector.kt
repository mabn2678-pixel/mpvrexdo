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
    companion object {
        private val IGNORE_SUB_KEYWORDS = listOf(
            "signs", "songs", "lyrics", "forced",
            "sdh", "colored", "karaoke"
        )
        private val DIALOGUE_TITLE_KEYWORDS = listOf(
            "subtitle", "subtitles", "full subtitle", "full sub",
            "dialogue", "dialog", "translation", "english",
            "main", "script", "full", "caption"
        )
    }

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

        selectBestAudioTrack(tracks, hasState, mpvController)
        selectBestSubtitleTrack(tracks, hasState, mpvController)
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

    private suspend fun selectBestSubtitleTrack(
        tracks: List<TrackNode>,
        hasState: Boolean,
        mpvController: MPVController
    ) {
        val currentSid = mpvController.getCurrentSid()

        if (!subtitlePreferences.autoEnableSubtitles.get()) {
            if (currentSid > 0) mpvController.setPrimarySubtitle(0)
            return
        }

        if (hasState && currentSid >= 0) return

        val subTracks = tracks.filter { it.isSubtitle }
        if (subTracks.isEmpty()) return

        val prefLangs = subtitlePreferences.preferredLanguages.get()
            .split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("eng", "en") }

        // Pass 00: External subtitle first
        subTracks.filter { it.external }.let { externals ->
            if (externals.isNotEmpty()) {
                val preferred = externals.firstOrNull { t ->
                    prefLangs.any { t.lang == it || t.lang.startsWith(it) }
                } ?: externals.firstOrNull { it.lang in setOf("", "und", "zxx") }
                  ?: externals.first()
                mpvController.setPrimarySubtitle(preferred.id)
                return
            }
        }

        // Pass A0: Anime — single default Japanese track
        if (isAnimeContent(tracks, mpvController)) {
            val defaultTracks = subTracks.filter { it.isDefault }
            if (defaultTracks.size == 1 &&
                defaultTracks[0].lang in listOf("jpn", "ja", "jp")
            ) {
                mpvController.setPrimarySubtitle(defaultTracks[0].id)
                return
            }
        }

        // Pass A: Anime — dialogue track in preferred language
        if (isAnimeContent(tracks, mpvController)) {
            for (lang in prefLangs) {
                val track = subTracks.firstOrNull { t ->
                    (t.lang == lang || t.lang.startsWith(lang)) &&
                    (t.title.contains("dialogue") ||
                     t.title.contains("full") ||
                     t.title.contains("script"))
                }
                if (track != null) {
                    mpvController.setPrimarySubtitle(track.id)
                    return
                }
            }
        }

        // Pass B: Clean match in preferred language
        for (lang in prefLangs) {
            val track = subTracks.firstOrNull { t ->
                (t.lang == lang || t.lang.startsWith(lang)) &&
                IGNORE_SUB_KEYWORDS.none { kw -> t.title.contains(kw) } &&
                !t.forced && !t.hearingImpaired
            }
            if (track != null) {
                mpvController.setPrimarySubtitle(track.id)
                return
            }
        }

        // Pass C: Any track in preferred language
        for (lang in prefLangs) {
            val track = subTracks.firstOrNull { t ->
                t.lang == lang || t.lang.startsWith(lang)
            }
            if (track != null) {
                mpvController.setPrimarySubtitle(track.id)
                return
            }
        }

        // Pass D: Title match (lang=und/empty)
        val unknownLangs = setOf("", "und", "zxx")
        val byTitle = subTracks.firstOrNull { t ->
            t.lang in unknownLangs &&
            DIALOGUE_TITLE_KEYWORDS.any { kw -> t.title.contains(kw) } &&
            IGNORE_SUB_KEYWORDS.none { kw -> t.title.contains(kw) } &&
            !t.forced && !t.hearingImpaired
        }
        if (byTitle != null) {
            mpvController.setPrimarySubtitle(byTitle.id)
            return
        }

        // Pass E: Single clean track
        val cleanTracks = subTracks.filter { t ->
            IGNORE_SUB_KEYWORDS.none { kw -> t.title.contains(kw) } &&
            !t.forced && !t.hearingImpaired
        }
        if (cleanTracks.size == 1) {
            mpvController.setPrimarySubtitle(cleanTracks[0].id)
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

