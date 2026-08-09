package com.finalplayer.app.ui.player

sealed class Sheets {
    object None           : Sheets()
    object SubtitleTracks   : Sheets()
    object SubtitleSettings : Sheets()
    object AudioTracks    : Sheets()
    object Chapters       : Sheets()
    object Decoders       : Sheets()
    object PlaybackSpeed  : Sheets()
    object More           : Sheets()
    object SleepTimer     : Sheets()
    object FrameNav       : Sheets()
    object AspectRatios   : Sheets()
    object VideoZoom      : Sheets()
    object Equalizer      : Sheets()
    object Playlist       : Sheets()
}
