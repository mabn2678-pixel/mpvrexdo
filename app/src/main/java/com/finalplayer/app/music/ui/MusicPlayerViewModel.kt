package com.finalplayer.app.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.music.data.local.LrcParser
import com.finalplayer.app.music.data.model.LrcLine
import com.finalplayer.app.music.data.model.MusicPlayerState
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.music.player.MusicController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerViewModel(
    private val controller: MusicController,
    private val lrcParser: LrcParser
) : ViewModel() {

    val state: StateFlow<MusicPlayerState> = controller.state
    val musicController: MusicController get() = controller

    private val _currentLrc = MutableStateFlow<LrcParser.ParsedLrc?>(null)
    val currentLrc: StateFlow<LrcParser.ParsedLrc?> = _currentLrc.asStateFlow()

    private val _currentLineIndex = MutableStateFlow(-1)
    val currentLineIndex: StateFlow<Int> = _currentLineIndex.asStateFlow()

    private val _isLyricsExpanded = MutableStateFlow(false)
    val isLyricsExpanded: StateFlow<Boolean> = _isLyricsExpanded.asStateFlow()

    init {
        // When currentSong changes -> auto-load LRC
        viewModelScope.launch {
            controller.state.map { it.currentSong }.distinctUntilChanged().collect { song ->
                if (song != null) loadLrc(song)
                else _currentLrc.value = null
            }
        }
        // Update currentLineIndex every 100ms
        viewModelScope.launch {
            while (isActive) {
                delay(100)
                val lines = _currentLrc.value?.lines ?: continue
                val pos = controller.state.value.positionMs
                _currentLineIndex.value = lrcParser.getCurrentLineIndex(lines, pos)
            }
        }
    }

    private fun loadLrc(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentLrc.value = lrcParser.findLrcForSong(song)
        }
    }

    fun togglePlayPause() = controller.togglePlayPause()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun seekToLine(line: LrcLine) = controller.seekTo(line.timeMs)
    fun skipToNext() = controller.skipToNext()
    fun skipToPrevious() = controller.skipToPrevious()
    fun toggleRepeat() {
        val nextMode = (controller.state.value.repeatMode + 1) % 3
        controller.setRepeatMode(nextMode)
    }
    fun toggleShuffle() = controller.toggleShuffle()
    fun toggleLyricsExpanded() {
        _isLyricsExpanded.value = !_isLyricsExpanded.value
    }
}
