package com.finalplayer.app.player.core

import android.util.Log
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class SubtitleTrackSelector(
    private val subtitlePreferences: SubtitlesPreferences
) {
    companion object {
        private const val TAG = "SubtitleTrackSelector"
        private val EXCLUDE_KEYWORDS = listOf(
            "signs", "songs", "lyrics", "forced",
            "sdh", "colored", "karaoke"
        )
    }

    suspend fun selectSubtitleTrack(
        hasSavedState: Boolean = false,
        savedSid: String? = null,
        mpvController: MPVController
    ) = withContext(Dispatchers.Main) {
        val view = mpvController.getAttachedView() ?: return@withContext

        // Rule 1: Respect saved state if present (sid > 0 or manually disabled "no")
        if (hasSavedState) {
            if (savedSid != null) {
                if (savedSid == "no" || savedSid == "0") {
                    Log.d(TAG, "Respecting saved subtitle state: disabled (no)")
                    view.setPropertyString("sid", "no")
                    return@withContext
                }
                val savedIdInt = savedSid.toIntOrNull()
                if (savedIdInt != null && savedIdInt > 0) {
                    Log.d(TAG, "Respecting saved subtitle state: sid=$savedIdInt")
                    view.setPropertyString("sid", savedIdInt.toString())
                    return@withContext
                }
            } else {
                val currentSidStr = view.getPropertyString("sid") ?: ""
                if (currentSidStr == "no" || (currentSidStr.toIntOrNull() ?: 0) > 0) {
                    Log.d(TAG, "Respecting current active sid=$currentSidStr from resume state")
                    return@withContext
                }
            }
        }

        // Rule 2: Check if subtitles are enabled in preferences
        val autoEnable = subtitlePreferences.autoEnableSubtitles.get()
        val disableByDefault = subtitlePreferences.disableByDefault.get()
        if (!autoEnable || disableByDefault) {
            Log.d(TAG, "Subtitles disabled by user preferences")
            view.setPropertyString("sid", "no")
            return@withContext
        }

        // Performance rule: Wait for tracks (max 20 attempts x 50ms)
        var attempts = 0
        while (attempts < 20) {
            val count = view.getPropertyInt("track-list/count") ?: 0
            if (count > 0) break
            delay(50)
            attempts++
        }

        val trackCount = view.getPropertyInt("track-list/count") ?: 0
        if (trackCount == 0) return@withContext

        // Read all track properties once into local List
        val allTracks = readAllTracks(view, trackCount)
        val subTracks = allTracks.filter { it.isSubtitle }
        if (subTracks.isEmpty()) {
            Log.d(TAG, "No subtitle tracks found in media")
            return@withContext
        }

        // Preferred languages from settings (default "ar,eng,en")
        val rawLangs = subtitlePreferences.preferredLanguages.get()
        val prefLangs = if (rawLangs.isNotBlank()) {
            rawLangs.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        } else {
            listOf("ar", "ara", "eng", "en")
        }

        fun matchLang(trackLang: String, pref: String): Boolean {
            if (trackLang.isEmpty()) return false
            if (trackLang == pref || trackLang.startsWith(pref)) return true
            if (pref in listOf("ar", "ara") && trackLang in listOf("ar", "ara", "arabic")) return true
            if (pref in listOf("en", "eng") && trackLang in listOf("en", "eng", "english")) return true
            if (pref in listOf("ja", "jp", "jpn") && trackLang in listOf("ja", "jp", "jpn", "japanese")) return true
            return false
        }

        // Pass 00 (Highest Priority): Any external subtitle track
        val externalTracks = subTracks.filter { it.external }
        if (externalTracks.isNotEmpty()) {
            val preferred = externalTracks.firstOrNull { t ->
                prefLangs.any { matchLang(t.lang, it) }
            } ?: externalTracks.first()
            Log.d(TAG, "Pass 00: Selected external subtitle track #${preferred.id} (${preferred.displayName})")
            view.setPropertyString("sid", preferred.id.toString())
            return@withContext
        }

        // Pass A0 (Anime): Single default Japanese track
        if (isAnimeContent(allTracks, view)) {
            val defaultTracks = subTracks.filter { it.isDefault }
            if (defaultTracks.size == 1 && defaultTracks[0].lang in listOf("jpn", "ja", "jp", "japanese")) {
                Log.d(TAG, "Pass A0: Selected single default Japanese anime track #${defaultTracks[0].id}")
                view.setPropertyString("sid", defaultTracks[0].id.toString())
                return@withContext
            }
        }

        // Pass B (Main Match): Preferred languages without exclusion keywords, not forced, not hearing-impaired
        for (pref in prefLangs) {
            val track = subTracks.firstOrNull { t ->
                matchLang(t.lang, pref) &&
                !t.forced &&
                !t.hearingImpaired &&
                EXCLUDE_KEYWORDS.none { kw -> t.title.lowercase().contains(kw) }
            }
            if (track != null) {
                Log.d(TAG, "Pass B: Selected clean subtitle track #${track.id} (${track.lang})")
                view.setPropertyString("sid", track.id.toString())
                return@withContext
            }
        }

        // Pass C (Last Resort): First track matching preferred language without extra filtering
        for (pref in prefLangs) {
            val track = subTracks.firstOrNull { t ->
                matchLang(t.lang, pref)
            }
            if (track != null) {
                Log.d(TAG, "Pass C: Selected fallback subtitle track #${track.id} (${track.lang})")
                view.setPropertyString("sid", track.id.toString())
                return@withContext
            }
        }

        Log.d(TAG, "No subtitle track matched passes 00, A0, B, C")
    }

    private fun readAllTracks(view: MPVView, count: Int): List<TrackNode> {
        val list = mutableListOf<TrackNode>()
        for (i in 0 until count) {
            val id = view.getPropertyInt("track-list/$i/id") ?: (i + 1)
            val type = view.getPropertyString("track-list/$i/type") ?: continue
            val lang = view.getPropertyString("track-list/$i/lang")?.lowercase() ?: ""
            val title = view.getPropertyString("track-list/$i/title") ?: ""
            val isDefault = view.getPropertyString("track-list/$i/default") == "yes" ||
                    view.getPropertyBoolean("track-list/$i/default") == true
            val forced = view.getPropertyString("track-list/$i/forced") == "yes" ||
                    view.getPropertyBoolean("track-list/$i/forced") == true
            val hearingImpaired = view.getPropertyString("track-list/$i/hearing-impaired") == "yes" ||
                    view.getPropertyBoolean("track-list/$i/hearing-impaired") == true
            val external = view.getPropertyString("track-list/$i/external") == "yes" ||
                    view.getPropertyBoolean("track-list/$i/external") == true
            val extFilename = view.getPropertyString("track-list/$i/external-filename")
            val isImage = view.getPropertyString("track-list/$i/image") == "yes" ||
                    view.getPropertyBoolean("track-list/$i/image") == true

            list.add(
                TrackNode(
                    id = id,
                    type = type,
                    lang = lang,
                    title = title,
                    isDefault = isDefault,
                    forced = forced,
                    hearingImpaired = hearingImpaired,
                    external = external,
                    externalFilename = extFilename,
                    isImage = isImage
                )
            )
        }
        return list
    }

    private fun isAnimeContent(tracks: List<TrackNode>, view: MPVView): Boolean {
        val path = view.getPropertyString("path")?.lowercase() ?: ""
        val title = view.getPropertyString("media-title")?.lowercase() ?: ""
        val filename = view.getPropertyString("filename")?.lowercase() ?: ""

        val hasJapaneseAudio = tracks.any { it.isAudio && it.lang in listOf("jpn", "ja", "jp", "japanese") }
        val hasCrcHash = Regex("\\[[0-9a-fA-F]{8}\\]").containsMatchIn(filename)
        val hasFansubBrackets = Regex("\\[.*\\]").containsMatchIn(title)
        val isAnimeFolder = path.contains("/anime/")

        return hasJapaneseAudio || hasCrcHash || hasFansubBrackets || isAnimeFolder
    }
}
