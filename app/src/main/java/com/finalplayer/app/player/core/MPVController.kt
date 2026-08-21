package com.finalplayer.app.player.core

import android.content.Context
import android.util.Log
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode
import com.finalplayer.app.ui.player.controls.components.sheets.ChapterNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MPVController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mpvView: MPVView? = null
    private var pollingJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun attachView(view: MPVView) {
        this.mpvView = view
        MPVLib.activeView = view
        view.initialize(context, context.filesDir)
        startPolling()
    }

    fun detachView() {
        pollingJob?.cancel()
        if (MPVLib.activeView == this.mpvView) {
            MPVLib.activeView = null
        }
        this.mpvView = null
    }

    fun stopAndDestroy() {
        pollingJob?.cancel()
        try {
            mpvView?.stop()
            mpvView?.destroy()
        } catch (_: Throwable) {}
        if (MPVLib.activeView == this.mpvView) {
            MPVLib.activeView = null
        }
        this.mpvView = null
        _playerState.update {
            it.copy(
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                currentFilePath = null,
                isBuffering = false
            )
        }
    }

    fun getAttachedView(): MPVView? = mpvView

    fun refreshVideoSurface() {
        val view = mpvView ?: return
        if (view.isSurfaceReady && view.holder.surface?.isValid == true) {
            view.refreshVideoSurface()
        }
    }

    fun isIdle(): Boolean = mpvView?.isIdle() ?: true

    fun play(path: String) {
        mpvView?.playFile(path)
        _playerState.update {
            it.copy(
                isPlaying = true,
                currentFilePath = path,
                isBuffering = false
            )
        }
        startPolling()
    }

    fun togglePlayPause() {
        mpvView?.togglePause()
        updateStateFromView()
    }

    fun pause() {
        mpvView?.pause()
        _playerState.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        mpvView?.unpause()
        _playerState.update { it.copy(isPlaying = true) }
    }

    fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        mpvView?.seekTo(seconds)
        _playerState.update { it.copy(positionMs = positionMs) }
    }

    fun seekBy(offsetSeconds: Int) {
        mpvView?.seekBy(offsetSeconds)
        updateStateFromView()
    }

    fun stop() {
        mpvView?.stop()
        _playerState.update {
            it.copy(
                isPlaying = false,
                positionMs = 0L,
                durationMs = 0L,
                currentFilePath = null,
                isBuffering = false
            )
        }
    }

    private fun updateStateFromView() {
        mpvView?.let { view ->
            view.updatePlaybackState()
            _playerState.update {
                it.copy(
                    positionMs = view.positionMs,
                    durationMs = view.durationMs,
                    isPlaying = !view.isPaused,
                    isBuffering = view.isPausedForCache
                )
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive) {
                updateStateFromView()
                delay(250)
            }
        }
    }

    fun release() {
        detachView()
    }

    // Subtitle methods
    fun addSubtitle(path: String, select: Boolean) {
        mpvView?.command(arrayOf("sub-add", path, if (select) "select" else "auto"))
    }

    fun setPrimarySubtitle(id: Int) {
        val sidVal = if (id <= 0) "no" else id.toString()
        mpvView?.setPropertyString("sid", sidVal)
    }

    fun setSecondarySubtitle(id: Int) {
        val sidVal = if (id <= 0) "no" else id.toString()
        mpvView?.setPropertyString("secondary-sid", sidVal)
    }

    fun getTracks(): List<TrackNode> {
        return mpvView?.getTrackList() ?: emptyList()
    }

    fun getSubtitleText(): String? {
        return mpvView?.getSubtitleText()
    }

    fun getCurrentSid(): Int {
        val str = mpvView?.getPropertyString("sid")
        return str?.toIntOrNull() ?: 0
    }

    fun getCurrentSecondarySid(): Int {
        val str = mpvView?.getPropertyString("secondary-sid")
        return str?.toIntOrNull() ?: 0
    }

    // Audio methods
    fun selectAudioTrack(id: Int) {
        val aidVal = if (id <= 0) "no" else id.toString()
        mpvView?.setPropertyString("aid", aidVal)
    }

    fun addAudio(path: String) {
        mpvView?.command(arrayOf("audio-add", path, "cached"))
    }

    fun getCurrentAid(): Int {
        val str = mpvView?.getPropertyString("aid")
        return str?.toIntOrNull() ?: 0
    }

    // Decoder methods
    fun setDecoder(value: String) {
        mpvView?.setPropertyString("hwdec", value)
    }

    fun getCurrentDecoderValue(): String {
        val view = mpvView ?: return "no"
        if (view.getPropertyBoolean("idle-active") == true) return "no"
        return view.getPropertyString("hwdec-current")
            ?: view.getPropertyString("hwdec")
            ?: "no"
    }

    // Speed methods
    fun setPlaybackSpeed(speed: Float) {
        mpvView?.setPropertyFloat("speed", speed)
    }

    fun getPlaybackSpeed(): Float {
        return mpvView?.getPropertyDouble("speed")?.toFloat() ?: 1.0f
    }

    // Chapter methods
    fun getChapters(): List<ChapterNode> {
        return mpvView?.getChapterList() ?: emptyList()
    }

    fun getCurrentChapterIndex(): Int? {
        return mpvView?.getCurrentChapter()
    }

    fun selectChapter(index: Int) {
        mpvView?.setPropertyInt("chapter", index)
        mpvView?.unpause()
    }

    // Property helpers
    fun setPropertyString(property: String, value: String) {
        mpvView?.setPropertyString(property, value)
    }

    fun setPropertyInt(property: String, value: Int) {
        mpvView?.setPropertyInt(property, value)
    }

    fun setPropertyFloat(property: String, value: Float) {
        mpvView?.setPropertyFloat(property, value)
    }

    fun setPropertyBoolean(property: String, value: Boolean) {
        mpvView?.setPropertyBoolean(property, value)
    }

    companion object {
        private const val TAG = "MPVController"
    }
}
