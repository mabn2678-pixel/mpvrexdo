package com.finalplayer.app.ui.player.controls

import android.net.Uri
import androidx.compose.runtime.Composable
import com.finalplayer.app.ui.player.ChapterNode
import com.finalplayer.app.ui.player.Decoder
import com.finalplayer.app.ui.player.Sheets
import com.finalplayer.app.ui.player.controls.components.sheets.AudioTracksSheet
import com.finalplayer.app.ui.player.controls.components.sheets.ChaptersSheet
import com.finalplayer.app.ui.player.controls.components.sheets.DecoderSheet
import com.finalplayer.app.ui.player.controls.components.sheets.MoreSheet
import com.finalplayer.app.ui.player.controls.components.sheets.PlaybackSpeedSheet
import com.finalplayer.app.ui.player.controls.components.sheets.SubtitleSettingsPanel
import com.finalplayer.app.ui.player.controls.components.sheets.SubtitlesSheet
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode

@Composable
fun PlayerSheets(
    sheetShown: Sheets,
    subtitleTracks: List<TrackNode>,
    audioTracks: List<TrackNode>,
    chapters: List<ChapterNode>,
    currentChapterIndex: Int?,
    currentDecoder: Decoder,
    currentAudioId: Int,
    currentSpeed: Float,
    sleepTimerRemaining: Int,
    selectedSubId: Int? = 0,
    selectedSecondarySubId: Int? = 0,
    onToggleSubtitle: (Int) -> Unit = {},
    onDisableSubtitles: () -> Unit = {},
    onAddSubtitle: (Uri) -> Unit = {},
    onRemoveSubtitle: (Int) -> Unit = {},
    onSelectAudio: (Int) -> Unit = {},
    onAddAudio: () -> Unit = {},
    onSeekToChapter: (Int) -> Unit = {},
    onUpdateDecoder: (Decoder) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onOpenSheet: (Sheets) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    when (sheetShown) {
        Sheets.SubtitleTracks -> SubtitlesSheet(
            tracks = subtitleTracks,
            selectedSubId = selectedSubId,
            selectedSecondarySubId = selectedSecondarySubId,
            onSelectSubtitle = onToggleSubtitle,
            onDisableSubtitles = onDisableSubtitles,
            onAddExternalSubtitle = onAddSubtitle,
            onRemoveSubtitle = onRemoveSubtitle,
            onOpenSettings = { onOpenSheet(Sheets.SubtitleSettings) },
            onDismiss = onDismiss
        )
        Sheets.SubtitleSettings -> SubtitleSettingsPanel(
            onDismiss = onDismiss
        )
        Sheets.AudioTracks -> AudioTracksSheet(
            tracks = audioTracks,
            currentAudioId = currentAudioId,
            onSelectAudio = onSelectAudio,
            onAddAudioFile = onAddAudio,
            onDismiss = onDismiss
        )
        Sheets.Chapters -> ChaptersSheet(
            chapters = chapters,
            currentChapterIndex = currentChapterIndex,
            onSeekToChapter = onSeekToChapter,
            onDismiss = onDismiss
        )
        Sheets.Decoders -> DecoderSheet(
            currentDecoder = currentDecoder,
            onSelect = onUpdateDecoder,
            onDismiss = onDismiss
        )
        Sheets.PlaybackSpeed -> PlaybackSpeedSheet(
            currentSpeed = currentSpeed,
            onSpeedChange = onSpeedChange,
            onDismiss = onDismiss
        )
        Sheets.More -> MoreSheet(
            sleepTimerRemaining = sleepTimerRemaining,
            onOpenSheet = onOpenSheet,
            onDismiss = onDismiss
        )
        else -> {}
    }
}
