package com.finalplayer.app.player

import android.content.Context
import android.widget.Toast
import com.finalplayer.app.domain.model.PlayerButtonType
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.data.preferences.AppearancePreferences
import com.finalplayer.app.data.preferences.AudioPreferences
import com.finalplayer.app.data.preferences.DecoderPreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import com.finalplayer.app.player.core.MPVController
import com.finalplayer.app.player.service.MediaPlaybackService
import com.finalplayer.app.player.core.MPVLib
import com.finalplayer.app.player.core.TrackSelector
import com.finalplayer.app.ui.player.ChapterNode
import com.finalplayer.app.ui.player.Decoder
import com.finalplayer.app.ui.player.Sheets
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.PlaybackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import android.provider.MediaStore
import java.io.File
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs

data class SeekState(
    val targetPositionSec: Float = 0f,
    val diffSeconds: Float = 0f,
    val isForwards: Boolean = true,
    val isDragging: Boolean = false
)

data class DoubleTapSeekState(
    val isLeft: Boolean,
    val amountSeconds: Int = 10,
    val timestamp: Long = System.currentTimeMillis()
)

class PlayerViewModel(
    val mpvController: MPVController,
    val playerPrefs: PlayerPreferences? = null,
    val subtitlesPrefs: SubtitlesPreferences? = null,
    val audioPrefs: AudioPreferences? = null,
    val decoderPrefs: DecoderPreferences? = null,
    val appearancePrefs: AppearancePreferences? = null
) : ViewModel(), KoinComponent {

    private val context: Context by inject()
    private val playbackRepository: PlaybackRepository by inject()

    private val trackSelector: TrackSelector? by lazy {
        if (subtitlesPrefs != null && audioPrefs != null) {
            TrackSelector(subtitlesPrefs, audioPrefs)
        } else null
    }

    private val subtitleAddMutex = Mutex()

    private val _paused = MutableStateFlow<Boolean?>(false)
    val paused: StateFlow<Boolean?> = _paused.asStateFlow()

    private val _precisePosition = MutableStateFlow(0f) // In seconds
    val precisePosition: StateFlow<Float> = _precisePosition.asStateFlow()

    private var lastKnownPositionMs: Long = 0L

    private val _preciseDuration = MutableStateFlow(0f) // In seconds
    val preciseDuration: StateFlow<Float> = _preciseDuration.asStateFlow()

    private val _controlsShown = MutableStateFlow(true)
    val controlsShown: StateFlow<Boolean> = _controlsShown.asStateFlow()

    private val _videoTitle = MutableStateFlow("Video Player")
    val videoTitle: StateFlow<String> = _videoTitle.asStateFlow()

    // Brightness state (0.0f to 1.0f)
    private val _currentBrightness = MutableStateFlow(0.5f)
    val currentBrightness: StateFlow<Float> = _currentBrightness.asStateFlow()

    private val _isBrightnessSliderShown = MutableStateFlow(false)
    val isBrightnessSliderShown: StateFlow<Boolean> = _isBrightnessSliderShown.asStateFlow()

    // Volume state (0.0f to 100.0f percent)
    private val _currentVolumePercent = MutableStateFlow(50f)
    val currentVolumePercent: StateFlow<Float> = _currentVolumePercent.asStateFlow()

    private val _isVolumeSliderShown = MutableStateFlow(false)
    val isVolumeSliderShown: StateFlow<Boolean> = _isVolumeSliderShown.asStateFlow()

    // Drag Seek Overlay State
    private val _dragSeekState = MutableStateFlow<SeekState?>(null)
    val dragSeekState: StateFlow<SeekState?> = _dragSeekState.asStateFlow()

    // Double Tap Seek State
    private val _doubleTapSeekState = MutableStateFlow<DoubleTapSeekState?>(null)
    val doubleTapSeekState: StateFlow<DoubleTapSeekState?> = _doubleTapSeekState.asStateFlow()

    // Sleep Timer
    private val _remainingTime = MutableStateFlow(0) // In seconds
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    // Subtitle & Track States
    private val _subtitleTracks = MutableStateFlow<List<TrackNode>>(emptyList())
    val subtitleTracks: StateFlow<List<TrackNode>> = _subtitleTracks.asStateFlow()

    private val _audioTracks = MutableStateFlow<List<TrackNode>>(emptyList())
    val audioTracks: StateFlow<List<TrackNode>> = _audioTracks.asStateFlow()

    private val _currentSubText = MutableStateFlow<String?>(null)
    val currentSubText: StateFlow<String?> = _currentSubText.asStateFlow()

    private val _selectedSubId = MutableStateFlow<Int?>(0)
    val selectedSubId: StateFlow<Int?> = _selectedSubId.asStateFlow()

    private val _selectedSecondarySubId = MutableStateFlow<Int?>(0)
    val selectedSecondarySubId: StateFlow<Int?> = _selectedSecondarySubId.asStateFlow()

    private val _selectedAudioId = MutableStateFlow<Int?>(0)
    val selectedAudioId: StateFlow<Int?> = _selectedAudioId.asStateFlow()
    val currentAudioId: StateFlow<Int> = _selectedAudioId
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _currentDecoder = MutableStateFlow(Decoder.HW_PLUS)
    val currentDecoder: StateFlow<Decoder> = _currentDecoder.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isLongPressSpeedActive = MutableStateFlow(false)
    val isLongPressSpeedActive: StateFlow<Boolean> = _isLongPressSpeedActive.asStateFlow()

    private val _longPressSpeedValue = MutableStateFlow(2.5f)
    val longPressSpeedValue: StateFlow<Float> = _longPressSpeedValue.asStateFlow()

    private val _zoomOverlayText = MutableStateFlow<String?>(null)
    val zoomOverlayText: StateFlow<String?> = _zoomOverlayText.asStateFlow()

    private val _subPosOverlayText = MutableStateFlow<String?>(null)
    val subPosOverlayText: StateFlow<String?> = _subPosOverlayText.asStateFlow()

    private val _isSliderDragging = MutableStateFlow(false)
    val isSliderDragging: StateFlow<Boolean> = _isSliderDragging.asStateFlow()

    val isBuffering: StateFlow<Boolean> = mpvController.playerState
        .map { it.isBuffering }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var normalPlaybackSpeed: Float = 1.0f
    private var lastSeekTimeMs: Long = 0L
    private var zoomHideJob: Job? = null
    private var subPosHideJob: Job? = null

    private val _chapters = MutableStateFlow<List<ChapterNode>>(emptyList())
    val chapters: StateFlow<List<ChapterNode>> = _chapters.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow<Int?>(null)
    val currentChapterIndex: StateFlow<Int?> = _currentChapterIndex.asStateFlow()
    val currentChapter: StateFlow<Int?> = _currentChapterIndex.asStateFlow()

    private val _sheetShown = MutableStateFlow<Sheets>(Sheets.None)
    val sheetShown: StateFlow<Sheets> = _sheetShown.asStateFlow()

    // Playlist & Progress States
    private val _currentVideoId = MutableStateFlow<String?>(null)
    val currentVideoId: StateFlow<String?> = _currentVideoId.asStateFlow()

    private val _playlistItems = MutableStateFlow<List<VideoItem>>(emptyList())
    val playlistItems: StateFlow<List<VideoItem>> = _playlistItems.asStateFlow()

    private val _currentPlaylistIndex = MutableStateFlow(0)
    val currentPlaylistIndex: StateFlow<Int> = _currentPlaylistIndex.asStateFlow()

    private val _isPlaylistMode = MutableStateFlow(false)
    val isPlaylistMode: StateFlow<Boolean> = _isPlaylistMode.asStateFlow()

    private val _isShortsMode = MutableStateFlow(false)
    val isShortsMode: StateFlow<Boolean> = _isShortsMode.asStateFlow()

    private val _resumePositionSec = MutableStateFlow<Double?>(null)
    val resumePositionSec: StateFlow<Double?> = _resumePositionSec.asStateFlow()

    private val _finishActivityEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val finishActivityEvent: SharedFlow<Unit> = _finishActivityEvent.asSharedFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isFlipV = MutableStateFlow(false)
    val isFlipV: StateFlow<Boolean> = _isFlipV.asStateFlow()

    private val _isFlipH = MutableStateFlow(false)
    val isFlipH: StateFlow<Boolean> = _isFlipH.asStateFlow()

    private val _isCinemaMode = MutableStateFlow(false)
    val isCinemaMode: StateFlow<Boolean> = _isCinemaMode.asStateFlow()

    private val _isBackgroundPlay = MutableStateFlow(false)
    val isBackgroundPlay: StateFlow<Boolean> = _isBackgroundPlay.asStateFlow()

    private val _currentAspectRatio = MutableStateFlow("default")
    val currentAspectRatio: StateFlow<String> = _currentAspectRatio.asStateFlow()

    private val _videoAspect = MutableStateFlow<Double?>(null)
    val videoAspect: StateFlow<Double?> = _videoAspect.asStateFlow()

    private val _userOrientationOverride = MutableStateFlow<String?>(null)
    val userOrientationOverride: StateFlow<String?> = _userOrientationOverride.asStateFlow()

    fun toggleRotateOrientation() {
        val currentOverride = _userOrientationOverride.value
        val currentAspect = _videoAspect.value ?: 1.7777
        val defaultIsPortrait = currentAspect < 0.95
        if (currentOverride == null) {
            _userOrientationOverride.value = if (defaultIsPortrait) "landscape" else "portrait"
        } else if (currentOverride == "landscape") {
            _userOrientationOverride.value = "portrait"
        } else {
            _userOrientationOverride.value = "landscape"
        }
    }

    fun resetOrientationOverride() {
        _userOrientationOverride.value = null
        // Do NOT reset _videoAspect here — it causes orientation flash
        // _videoAspect will be updated by prefetchVideoAspect() or MPV polling
    }

    fun prefetchVideoAspect(videoUri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val retriever = android.media.MediaMetadataRetriever()
                try {
                    val uri = android.net.Uri.parse(videoUri)
                    if (uri.scheme == "content") {
                        retriever.setDataSource(context, uri)
                    } else {
                        retriever.setDataSource(videoUri)
                    }
                    val width = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0
                    val height = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0
                    val rotation = retriever.extractMetadata(
                        android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toIntOrNull() ?: 0

                    if (width > 0 && height > 0) {
                        val aspect = if (rotation == 90 || rotation == 270) {
                            height.toDouble() / width.toDouble()
                        } else {
                            width.toDouble() / height.toDouble()
                        }
                        withContext(Dispatchers.Main) {
                            _videoAspect.value = aspect
                        }
                    }
                } finally {
                    retriever.release()
                }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Could not prefetch video aspect: ${e.message}")
            }
        }
    }

    private val _currentVideoZoom = MutableStateFlow(1.0f)
    val currentVideoZoom: StateFlow<Float> = _currentVideoZoom.asStateFlow()

    private var autoSaveProgressJob: Job? = null
    private val _externalSubtitles = mutableListOf<String>()
    private var hasAttemptedAutoSelectSub = false

    private var sleepTimerJob: Job? = null
    private var seekCoalescingJob: Job? = null
    private var pollingJob: Job? = null
    private var brightnessHideJob: Job? = null
    private var volumeHideJob: Job? = null
    private var doubleTapHideJob: Job? = null

    private var seekStartPositionSec: Float = 0f
    private var cumulativeSeekDeltaSec: Float = 0f

    init {
        startAdaptivePolling()
        observePreferences()
        startAutoSaveProgress()
        setAspectRatio("default")
    }

    private var hasAppliedAutoResume = false

    private fun startAutoSaveProgress() {
        autoSaveProgressJob?.cancel()
        autoSaveProgressJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(3000)
                saveCurrentProgressNow()
            }
        }
    }

    fun saveCurrentProgressNow() {
        val saveOnExit = playerPrefs?.savePositionOnQuit?.get() ?: true
        if (!saveOnExit) return

        val videoId = _currentVideoId.value ?: return
        if (videoId.isEmpty()) return

        val livePosSec = try { MPVLib.getPropertyInt("time-pos") } catch (e: Exception) { null }
        val posMs = when {
            livePosSec != null && livePosSec > 0 -> livePosSec.toLong() * 1000L
            mpvController.playerState.value.positionMs > 0L -> mpvController.playerState.value.positionMs
            _precisePosition.value > 0f -> (_precisePosition.value * 1000f).toLong()
            else -> 0L
        }

        if (posMs > lastKnownPositionMs) {
            lastKnownPositionMs = posMs
        }
        val effectivePosMs = if (posMs > 0L) posMs else lastKnownPositionMs
        val durMs = (_preciseDuration.value * 1000f).toLong().coerceAtLeast(0L)

        if (durMs > 0 && effectivePosMs > 0) {
            val isCompleted = (effectivePosMs.toFloat() / durMs.toFloat()) > 0.95f
            val progress = PlaybackProgress(
                videoId = videoId,
                positionMs = if (isCompleted) 0L else effectivePosMs,
                durationMs = durMs,
                lastPlayedTimestamp = System.currentTimeMillis(),
                isCompleted = isCompleted
            )
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                try {
                    playbackRepository.saveProgress(progress)
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error saving playback progress", e)
                }
            }
        }
    }

    private suspend fun saveCurrentProgress() {
        saveCurrentProgressNow()
    }

    fun setShortsPlaylist(items: List<VideoItem>, startIndex: Int = 0) {
        _isShortsMode.value = true
        setPlaylist(items, startIndex)
    }

    fun setPlaylist(items: List<VideoItem>, startIndex: Int = 0) {
        _playlistItems.value = items
        _isPlaylistMode.value = items.isNotEmpty()
        if (items.isNotEmpty() && startIndex in items.indices) {
            _currentPlaylistIndex.value = startIndex
            playPlaylistItem(startIndex)
        }
    }

    fun playPlaylistItem(index: Int) {
        val items = _playlistItems.value
        if (index in items.indices) {
            _currentPlaylistIndex.value = index
            val item = items[index]
            _currentVideoId.value = item.id
            _videoTitle.value = item.title
            prefetchVideoAspect(item.uri)
            mpvController.play(item.uri)
            checkSavedProgress(item.id)
        }
    }

    fun playNextVideo() {
        val items = _playlistItems.value
        if (items.isNotEmpty()) {
            val nextIndex = (_currentPlaylistIndex.value + 1) % items.size
            playPlaylistItem(nextIndex)
        }
    }

    fun playPreviousVideo() {
        val items = _playlistItems.value
        if (items.isNotEmpty()) {
            val prevIndex = if (_currentPlaylistIndex.value - 1 < 0) items.size - 1 else _currentPlaylistIndex.value - 1
            playPlaylistItem(prevIndex)
        }
    }

    fun reorderPlaylist(fromIndex: Int, toIndex: Int) {
        val currentList = _playlistItems.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _playlistItems.value = currentList
            if (_currentPlaylistIndex.value == fromIndex) {
                _currentPlaylistIndex.value = toIndex
            } else if (_currentPlaylistIndex.value in (fromIndex + 1)..toIndex) {
                _currentPlaylistIndex.value -= 1
            } else if (_currentPlaylistIndex.value in toIndex until fromIndex) {
                _currentPlaylistIndex.value += 1
            }
        }
    }

    fun setCurrentVideoDetails(id: String, title: String) {
        _currentVideoId.value = id
        _videoTitle.value = title
        hasAppliedAutoResume = false
        checkSavedProgress(id)
        if (id.isNotEmpty()) {
            prefetchVideoAspect(id)
        }
    }

    fun checkSavedProgress(videoId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saveOnExit = playerPrefs?.savePositionOnQuit?.get() ?: true
                if (!saveOnExit) {
                    _resumePositionSec.value = null
                    return@launch
                }
                val progress = playbackRepository.getProgress(videoId).first()
                if (progress != null && !progress.isCompleted && progress.positionMs > 2000L) {
                    val savedTimeInSeconds = progress.positionMs / 1000.0
                    lastKnownPositionMs = progress.positionMs
                    hasAppliedAutoResume = false
                    withContext(Dispatchers.Main) {
                        _resumePositionSec.value = savedTimeInSeconds
                        mpvController.setPropertyInt("start", savedTimeInSeconds.toInt())
                        MPVLib.setPropertyInt("start", savedTimeInSeconds.toInt())
                        MPVLib.setOptionString("start", savedTimeInSeconds.toInt().toString())
                    }
                } else {
                    _resumePositionSec.value = null
                }
            } catch (e: Exception) {
                _resumePositionSec.value = null
            }
        }
    }

    fun clearResumePosition() {
        _resumePositionSec.value = null
    }

    private fun observePreferences() {
        observePreferencesAndApply()
        playerPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.defaultSpeed.changes().collect { speed ->
                    if (abs(speed - _playbackSpeed.value) > 0.01f) {
                        setPlaybackSpeed(speed)
                    }
                }
            }
        }
        subtitlesPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.fontSize.changes().collect { fontSize ->
                    mpvController.setPropertyInt("sub-font-size", fontSize)
                }
            }
            viewModelScope.launch {
                prefs.subScale.changes().collect { scale ->
                    mpvController.setPropertyFloat("sub-scale", scale)
                }
            }
            viewModelScope.launch {
                prefs.bold.changes().collect { isBold ->
                    mpvController.setPropertyBoolean("sub-bold", isBold)
                }
            }
        }
        audioPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.defaultAudioDelay.changes().collect { delay ->
                    mpvController.setPropertyInt("audio-delay", delay)
                }
            }
            viewModelScope.launch {
                prefs.audioPitchCorrection.changes().collect { pitchCorr ->
                    mpvController.setPropertyString("audio-pitch-correction", if (pitchCorr) "yes" else "no")
                }
            }
            viewModelScope.launch {
                prefs.volumeNormalization.changes().collect { norm ->
                    mpvController.setPropertyString("af", if (norm) "dynaudnorm" else "")
                }
            }
            viewModelScope.launch {
                prefs.preferredLanguages.changes().collect { langs ->
                    if (langs.isNotBlank()) {
                        mpvController.setPropertyString("alang", langs)
                    }
                }
            }
            viewModelScope.launch {
                prefs.audioChannels.changes().collect { channels ->
                    if (channels.isNotBlank()) {
                        mpvController.setPropertyString("audio-channels", channels)
                    }
                }
            }
            viewModelScope.launch {
                prefs.volumeBoostCap.changes().collect { cap ->
                    mpvController.setPropertyInt("volume-max", 100 + cap)
                }
            }
        }
        decoderPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.tryHWDecoding.changes().collect { tryHW ->
                    if (tryHW) {
                        setDecoder(Decoder.HW_PLUS)
                    } else {
                        setDecoder(Decoder.SOFTWARE)
                    }
                }
            }
            viewModelScope.launch {
                prefs.profile.changes().collect { profile ->
                    val mpvVal = when (profile.lowercase()) {
                        "fast" -> "fast"
                        "default" -> "default"
                        "high quality" -> "high-quality"
                        "gpu hq" -> "gpu-hq"
                        "low latency" -> "low-latency"
                        "sw fast" -> "sw-fast"
                        else -> "fast"
                    }
                    mpvController.setPropertyString("profile", mpvVal)
                }
            }
            viewModelScope.launch {
                prefs.gpuNext.changes().collect { enabled ->
                    mpvController.setPropertyString("vo", if (enabled) "gpu-next" else "gpu")
                }
            }
            viewModelScope.launch {
                prefs.useVulkan.changes().collect { enabled ->
                    mpvController.setPropertyString("gpu-api", if (enabled) "vulkan" else "opengl")
                }
            }
            viewModelScope.launch {
                prefs.debanding.changes().collect { mode ->
                    when (mode) {
                        "GPU" -> {
                            mpvController.setPropertyString("deband", "yes")
                            mpvController.getAttachedView()?.command(arrayOf("vf", "remove", "@deband"))
                        }
                        "CPU" -> {
                            mpvController.setPropertyString("deband", "no")
                            mpvController.getAttachedView()?.command(arrayOf("vf", "add", "@deband:gradfun=radius=12"))
                        }
                        else -> {
                            mpvController.setPropertyString("deband", "no")
                            mpvController.getAttachedView()?.command(arrayOf("vf", "remove", "@deband"))
                        }
                    }
                }
            }
            viewModelScope.launch {
                prefs.useYUV420P.changes().collect { enabled ->
                    if (enabled) {
                        mpvController.getAttachedView()?.command(arrayOf("vf", "add", "@yuv420p:format=yuv420p"))
                    } else {
                        mpvController.getAttachedView()?.command(arrayOf("vf", "remove", "@yuv420p"))
                    }
                }
            }
            viewModelScope.launch {
                prefs.anime4k.changes().collect { enabled ->
                    if (enabled) {
                        mpvController.setPropertyString("scale", "eowa")
                        mpvController.setPropertyString("cscale", "eowa")
                        mpvController.getAttachedView()?.command(arrayOf("vf", "add", "@anime4k:unsharp=5:5:1.0:5:5:0.0"))
                    } else {
                        mpvController.setPropertyString("scale", "bilinear")
                        mpvController.setPropertyString("cscale", "bilinear")
                        mpvController.getAttachedView()?.command(arrayOf("vf", "remove", "@anime4k"))
                    }
                }
            }
            viewModelScope.launch {
                prefs.hdrToSdr.changes().collect { enabled ->
                    if (enabled) {
                        mpvController.setPropertyString("tone-mapping", "bt.2446a")
                        mpvController.setPropertyString("target-peak", "auto")
                        mpvController.setPropertyString("hdr-compute-peak", "yes")
                        mpvController.setPropertyString("tone-mapping-mode", "hybrid")
                    } else {
                        mpvController.setPropertyString("tone-mapping", "auto")
                        mpvController.setPropertyString("hdr-compute-peak", "auto")
                    }
                }
            }
        }
    }

    private fun formatLongToHex(value: Long): String {
        val a = ((value shr 24) and 0xFF).toInt()
        val r = ((value shr 16) and 0xFF).toInt()
        val g = ((value shr 8) and 0xFF).toInt()
        val b = (value and 0xFF).toInt()
        return String.format(java.util.Locale.US, "#%02X%02X%02X%02X", a, r, g, b)
    }

    fun applyAllSubtitlePreferences() {
        val prefs = subtitlesPrefs ?: return
        viewModelScope.launch {
            val fontSize = prefs.fontSize.get()
            val isBold = prefs.bold.get()
            val isItalic = prefs.italic.get()
            val scale = prefs.subScale.get()
            val pos = prefs.subPos.get()
            val border = prefs.borderSize.get()
            val shadow = prefs.shadowOffset.get()
            val bStyle = prefs.borderStyle.get()
            val overrideAss = prefs.overrideAssSubs.get()
            val fontStr = prefs.font.get()
            val preferredLangs = prefs.preferredLanguages.get()
            val disableByDefault = prefs.disableByDefault.get()
            val autoLoad = prefs.autoLoadSubtitles.get()
            val scaleByWin = prefs.scaleByWindow.get()
            val fontsDir = prefs.fontsFolder.get()
            val encodings = prefs.preferredEncodings.get()

            if (preferredLangs.isNotBlank()) {
                MPVLib.setPropertyString("slang", preferredLangs)
                MPVLib.setOptionString("slang", preferredLangs)
            }

            if (disableByDefault) {
                MPVLib.setPropertyString("sub-visibility", "no")
                MPVLib.setOptionString("sub-visibility", "no")
                MPVLib.setPropertyString("sid", "no")
            } else {
                MPVLib.setPropertyString("sub-visibility", "yes")
                MPVLib.setOptionString("sub-visibility", "yes")
            }

            MPVLib.setPropertyString("sub-auto", if (autoLoad) "fuzzy" else "no")
            MPVLib.setOptionString("sub-auto", if (autoLoad) "fuzzy" else "no")

            val scaleVal = if (scaleByWin) "yes" else "no"
            MPVLib.setPropertyString("sub-scale-by-window", scaleVal)
            MPVLib.setOptionString("sub-scale-by-window", scaleVal)
            MPVLib.setPropertyString("sub-scale-with-window", scaleVal)
            MPVLib.setOptionString("sub-scale-with-window", scaleVal)

            if (fontsDir.isNotBlank()) {
                MPVLib.setPropertyString("sub-fonts-dir", fontsDir)
                MPVLib.setOptionString("sub-fonts-dir", fontsDir)
            }

            if (encodings.isNotBlank()) {
                val codepage = when {
                    encodings.contains("1256", ignoreCase = true) -> "cp1256"
                    encodings.contains("8859-6", ignoreCase = true) -> "iso-8859-6"
                    encodings.contains("1252", ignoreCase = true) -> "cp1252"
                    encodings.contains("UTF-8", ignoreCase = true) -> "utf-8"
                    else -> "auto"
                }
                MPVLib.setPropertyString("sub-codepage", codepage)
                MPVLib.setOptionString("sub-codepage", codepage)
            }

            val textCLong = prefs.textColor.get()
            val borderCLong = prefs.borderColor.get()
            val bgCLong = prefs.backgroundColor.get()

            val textHex = formatLongToHex(textCLong)
            val borderHex = formatLongToHex(borderCLong)

            val effectiveBgCLong = if (bStyle == "box" && (bgCLong and 0xFF000000L) == 0L) {
                0xA6000000L or (bgCLong and 0x00FFFFFFL)
            } else bgCLong
            val bgHex = formatLongToHex(effectiveBgCLong)

            MPVLib.setPropertyInt("sub-font-size", fontSize)
            MPVLib.setOptionString("sub-font-size", fontSize.toString())

            MPVLib.setPropertyBoolean("sub-bold", isBold)
            MPVLib.setOptionString("sub-bold", if (isBold) "yes" else "no")

            MPVLib.setPropertyBoolean("sub-italic", isItalic)
            MPVLib.setOptionString("sub-italic", if (isItalic) "yes" else "no")

            MPVLib.setPropertyFloat("sub-scale", scale)

            MPVLib.setPropertyString("sub-use-margins", "yes")
            MPVLib.setOptionString("sub-use-margins", "yes")
            MPVLib.setPropertyInt("sub-margin-y", 0)
            MPVLib.setOptionString("sub-margin-y", "0")
            MPVLib.setPropertyInt("sub-pos", pos)
            MPVLib.setOptionString("sub-pos", pos.toString())

            MPVLib.setPropertyInt("sub-outline-size", border.toInt())
            MPVLib.setPropertyFloat("sub-border-size", border)
            MPVLib.setOptionString("sub-outline-size", border.toInt().toString())

            MPVLib.setPropertyInt("sub-shadow-offset", shadow)
            MPVLib.setPropertyFloat("sub-shadow-offset", shadow.toFloat())
            MPVLib.setOptionString("sub-shadow-offset", shadow.toString())

            if (fontStr.isNotBlank()) {
                MPVLib.setPropertyString("sub-font", fontStr)
                MPVLib.setOptionString("sub-font", fontStr)
            }

            if (bStyle == "box") {
                MPVLib.setPropertyString("sub-border-style", "opaque-box")
                MPVLib.setOptionString("sub-border-style", "opaque-box")
                MPVLib.setPropertyString("sub-back-color", bgHex)
                MPVLib.setOptionString("sub-back-color", bgHex)
                MPVLib.setPropertyString("sub-bg-color", bgHex)
                MPVLib.setOptionString("sub-bg-color", bgHex)
            } else {
                MPVLib.setPropertyString("sub-border-style", "outline-and-shadow")
                MPVLib.setOptionString("sub-border-style", "outline-and-shadow")
                MPVLib.setPropertyString("sub-back-color", "#00000000")
                MPVLib.setOptionString("sub-back-color", "#00000000")
            }

            MPVLib.setPropertyString("sub-color", textHex)
            MPVLib.setOptionString("sub-color", textHex)

            MPVLib.setPropertyString("sub-border-color", borderHex)
            MPVLib.setOptionString("sub-border-color", borderHex)

            MPVLib.setOptionString("sub-ass-override", if (overrideAss) "force" else "scale")
        }
    }

    fun updateSubPositionByDelta(deltaYPx: Float) {
        val prefs = subtitlesPrefs ?: return
        viewModelScope.launch {
            val currentPos = prefs.subPos.get()
            val sensitivity = 0.08f
            val newPos = (currentPos + (deltaYPx * sensitivity).toInt()).coerceIn(0, 120)

            MPVLib.setPropertyString("sub-use-margins", "yes")
            MPVLib.setOptionString("sub-use-margins", "yes")
            MPVLib.setPropertyInt("sub-margin-y", 0)
            MPVLib.setOptionString("sub-margin-y", "0")
            MPVLib.setPropertyInt("sub-pos", newPos)
            MPVLib.setOptionString("sub-pos", newPos.toString())

            _subPosOverlayText.value = "موضع الترجمة: $newPos%"

            subPosHideJob?.cancel()
            subPosHideJob = viewModelScope.launch {
                delay(1500)
                _subPosOverlayText.value = null
            }

            prefs.subPos.set(newPos)
        }
    }

    private fun observePreferencesAndApply() {
        applyAllSubtitlePreferences()
        subtitlesPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.subPos.changes().collect { pos ->
                    MPVLib.setPropertyString("sub-use-margins", "yes")
                    MPVLib.setOptionString("sub-use-margins", "yes")
                    MPVLib.setPropertyInt("sub-margin-y", 0)
                    MPVLib.setOptionString("sub-margin-y", "0")
                    MPVLib.setPropertyInt("sub-pos", pos)
                    MPVLib.setOptionString("sub-pos", pos.toString())
                }
            }
            viewModelScope.launch {
                prefs.borderStyle.changes().collect { style ->
                    val bgCLong = prefs.backgroundColor.get()
                    val effectiveBgCLong = if (style == "box" && (bgCLong and 0xFF000000L) == 0L) {
                        0xA6000000L or (bgCLong and 0x00FFFFFFL)
                    } else bgCLong
                    val bgHex = formatLongToHex(effectiveBgCLong)

                    if (style == "box") {
                        MPVLib.setPropertyString("sub-border-style", "opaque-box")
                        MPVLib.setOptionString("sub-border-style", "opaque-box")
                        MPVLib.setPropertyString("sub-back-color", bgHex)
                        MPVLib.setOptionString("sub-back-color", bgHex)
                        MPVLib.setPropertyString("sub-bg-color", bgHex)
                        MPVLib.setOptionString("sub-bg-color", bgHex)
                    } else {
                        MPVLib.setPropertyString("sub-border-style", "outline-and-shadow")
                        MPVLib.setOptionString("sub-border-style", "outline-and-shadow")
                        MPVLib.setPropertyString("sub-back-color", "#00000000")
                        MPVLib.setOptionString("sub-back-color", "#00000000")
                    }
                }
            }
            viewModelScope.launch {
                prefs.fontSize.changes().collect { size ->
                    MPVLib.setPropertyInt("sub-font-size", size)
                    MPVLib.setOptionString("sub-font-size", size.toString())
                }
            }
            viewModelScope.launch {
                prefs.subScale.changes().collect { scale ->
                    MPVLib.setPropertyFloat("sub-scale", scale)
                }
            }
            viewModelScope.launch {
                prefs.bold.changes().collect { bold ->
                    MPVLib.setPropertyBoolean("sub-bold", bold)
                    MPVLib.setOptionString("sub-bold", if (bold) "yes" else "no")
                }
            }
            viewModelScope.launch {
                prefs.textColor.changes().collect { color ->
                    val hex = formatLongToHex(color)
                    MPVLib.setPropertyString("sub-color", hex)
                    MPVLib.setOptionString("sub-color", hex)
                }
            }
            viewModelScope.launch {
                prefs.borderColor.changes().collect { color ->
                    val hex = formatLongToHex(color)
                    MPVLib.setPropertyString("sub-border-color", hex)
                    MPVLib.setOptionString("sub-border-color", hex)
                }
            }
            viewModelScope.launch {
                prefs.backgroundColor.changes().collect { color ->
                    val style = prefs.borderStyle.get()
                    if (style == "box") {
                        val hex = formatLongToHex(if ((color and 0xFF000000L) == 0L) (0xA6000000L or (color and 0x00FFFFFFL)) else color)
                        MPVLib.setPropertyString("sub-back-color", hex)
                        MPVLib.setOptionString("sub-back-color", hex)
                        MPVLib.setPropertyString("sub-bg-color", hex)
                        MPVLib.setOptionString("sub-bg-color", hex)
                    }
                }
            }
            viewModelScope.launch {
                prefs.preferredLanguages.changes().collect { langs ->
                    if (langs.isNotBlank()) {
                        MPVLib.setPropertyString("slang", langs)
                        MPVLib.setOptionString("slang", langs)
                    }
                }
            }
            viewModelScope.launch {
                prefs.disableByDefault.changes().collect { disable ->
                    if (disable) {
                        MPVLib.setPropertyString("sub-visibility", "no")
                        MPVLib.setOptionString("sub-visibility", "no")
                        MPVLib.setPropertyString("sid", "no")
                    } else {
                        MPVLib.setPropertyString("sub-visibility", "yes")
                        MPVLib.setOptionString("sub-visibility", "yes")
                    }
                }
            }
            viewModelScope.launch {
                prefs.autoLoadSubtitles.changes().collect { autoLoad ->
                    MPVLib.setPropertyString("sub-auto", if (autoLoad) "fuzzy" else "no")
                    MPVLib.setOptionString("sub-auto", if (autoLoad) "fuzzy" else "no")
                }
            }
            viewModelScope.launch {
                prefs.overrideAssSubs.changes().collect { overrideAss ->
                    MPVLib.setPropertyString("sub-ass-override", if (overrideAss) "force" else "scale")
                    MPVLib.setOptionString("sub-ass-override", if (overrideAss) "force" else "scale")
                }
            }
            viewModelScope.launch {
                prefs.scaleByWindow.changes().collect { scaleByWin ->
                    val scaleVal = if (scaleByWin) "yes" else "no"
                    MPVLib.setPropertyString("sub-scale-by-window", scaleVal)
                    MPVLib.setOptionString("sub-scale-by-window", scaleVal)
                    MPVLib.setPropertyString("sub-scale-with-window", scaleVal)
                    MPVLib.setOptionString("sub-scale-with-window", scaleVal)
                }
            }
            viewModelScope.launch {
                prefs.fontsFolder.changes().collect { fontsDir ->
                    if (fontsDir.isNotBlank()) {
                        MPVLib.setPropertyString("sub-fonts-dir", fontsDir)
                        MPVLib.setOptionString("sub-fonts-dir", fontsDir)
                    }
                }
            }
            viewModelScope.launch {
                prefs.preferredEncodings.changes().collect { encodings ->
                    val codepage = when {
                        encodings.contains("1256", ignoreCase = true) -> "cp1256"
                        encodings.contains("8859-6", ignoreCase = true) -> "iso-8859-6"
                        encodings.contains("1252", ignoreCase = true) -> "cp1252"
                        encodings.contains("UTF-8", ignoreCase = true) -> "utf-8"
                        else -> "auto"
                    }
                    MPVLib.setPropertyString("sub-codepage", codepage)
                    MPVLib.setOptionString("sub-codepage", codepage)
                }
            }
        }
        playerPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.usePreciseSeeking.changes().collect { precise ->
                    MPVLib.setOptionString("hr-seek", if (precise) "yes" else "no")
                }
            }
        }
    }

    fun toggleBoxStyle(isEnabled: Boolean) {
        val style = if (isEnabled) "box" else "outline"
        subtitlesPrefs?.let { prefs ->
            viewModelScope.launch {
                prefs.borderStyle.set(style)
            }
        }
    }

    fun setVideoTitle(title: String) {
        _videoTitle.value = title
    }

    fun onFileLoaded(fileName: String) {
        hasAttemptedAutoSelectSub = false
        _videoTitle.value = fileName
        resetOrientationOverride()
        setAspectRatio("default")
        applyAllSubtitlePreferences()
    }

    fun updateTracks() {
        val allTracks = mpvController.getTracks()
        val subs = allTracks.filter { it.type == "sub" }
        val audios = allTracks.filter { it.type == "audio" }

        _subtitleTracks.value = subs
        _audioTracks.value = audios

        _selectedSubId.value = mpvController.getCurrentSid()
        _selectedSecondarySubId.value = mpvController.getCurrentSecondarySid()
        _selectedAudioId.value = mpvController.getCurrentAid()

        // Auto select best sub if not done yet
        if (!hasAttemptedAutoSelectSub && subs.isNotEmpty()) {
            hasAttemptedAutoSelectSub = true
            viewModelScope.launch {
                trackSelector?.onFileLoaded(false, mpvController)
            }
        }
    }

    fun selectAudioTrack(id: Int) {
        mpvController.selectAudioTrack(id)
        _selectedAudioId.value = id
    }

    fun addAudio(uri: Uri, context: Context) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Could not take persistable permission for audio", e)
        }

        val audioPath = uri.toString()
        mpvController.addAudio(audioPath)
        updateTracks()
    }

    fun setDecoder(decoder: Decoder) {
        mpvController.setDecoder(decoder.value)
        _currentDecoder.value = decoder
    }

    fun updateDecoder(decoder: Decoder) {
        setDecoder(decoder)
    }

    fun setPlaybackSpeed(speed: Float) {
        val rounded = (speed.coerceIn(0.25f, 4.0f) * 100).toInt() / 100f
        mpvController.setPlaybackSpeed(rounded)
        _playbackSpeed.value = rounded
    }

    fun selectChapter(index: Int) {
        mpvController.selectChapter(index)
        _currentChapterIndex.value = index
    }

    fun seekToChapter(index: Int) {
        selectChapter(index)
        unpause()
    }

    fun unpause() {
        mpvController.resume()
        _paused.value = false
    }

    fun toggleLock() {
        _isLocked.value = !_isLocked.value
        if (_isLocked.value) {
            _controlsShown.value = false
        }
    }

    fun toggleRepeatMode() {
        val next = (_repeatMode.value + 1) % 3
        _repeatMode.value = next
        mpvController.setPropertyString("loop-file", if (next == 1) "inf" else "no")
        mpvController.setPropertyString("loop-playlist", if (next == 2) "inf" else "no")
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        mpvController.setPropertyBoolean("shuffle", next)
    }

    fun toggleFlipV() {
        val next = !_isFlipV.value
        _isFlipV.value = next
        val curRot = mpvController.getAttachedView()?.getPropertyInt("video-rotate") ?: 0
        mpvController.setPropertyInt("video-rotate", (curRot + 180) % 360)
    }

    fun toggleFlipH() {
        val next = !_isFlipH.value
        _isFlipH.value = next
        val curRot = mpvController.getAttachedView()?.getPropertyInt("video-rotate") ?: 0
        mpvController.setPropertyInt("video-rotate", (curRot + 180) % 360)
    }

    fun stepFrame(forward: Boolean) {
        if (forward) {
            mpvController.getAttachedView()?.command(arrayOf("frame-step"))
        } else {
            mpvController.getAttachedView()?.command(arrayOf("frame-back-step"))
        }
    }

    fun toggleAbRepeat() {
        val pos = _precisePosition.value
        mpvController.setPropertyString("ab-loop-a", pos.toString())
    }

    fun toggleCinemaMode() {
        _isCinemaMode.value = !_isCinemaMode.value
    }

    fun toggleBackgroundPlay() {
        _isBackgroundPlay.value = !_isBackgroundPlay.value
    }

    fun setBackgroundPlay(enabled: Boolean) {
        _isBackgroundPlay.value = enabled
    }

    fun setAspectRatio(ratio: String) {
        _currentAspectRatio.value = ratio
        val overrideVal = when (ratio) {
            "default", "fit", "fill", "crop", "stretch" -> "-1"
            else -> ratio
        }
        val keepAspectVal = if (ratio == "stretch") "no" else "yes"
        val panscanVal = if (ratio in listOf("fill", "crop")) 1.0f else 0.0f
        val unscaledVal = if (ratio == "100%") "yes" else "no"

        try {
            MPVLib.setPropertyString("keepaspect", keepAspectVal)
            MPVLib.setPropertyString("video-aspect-override", overrideVal)
            MPVLib.setPropertyFloat("panscan", panscanVal)
            MPVLib.setPropertyString("video-unscaled", unscaledVal)
        } catch (e: Exception) {
            // Ignore
        }

        mpvController.setPropertyString("keepaspect", keepAspectVal)
        mpvController.setPropertyString("video-aspect-override", overrideVal)
        mpvController.setPropertyFloat("panscan", panscanVal)
        mpvController.setPropertyString("video-unscaled", unscaledVal)
    }

    fun cycleNextAspectRatio(context: Context) {
        val list = listOf("default", "fill", "stretch", "16:9", "4:3", "18:9", "21:9", "1:1")
        val currentIndex = list.indexOf(_currentAspectRatio.value).let { if (it == -1) 0 else it }
        val nextIndex = (currentIndex + 1) % list.size
        val nextRatio = list[nextIndex]
        setAspectRatio(nextRatio)

        val label = when (nextRatio) {
            "default" -> "الملاءمة الأفضل (Fit)"
            "fill" -> "قص / تعبئة (Crop)"
            "stretch" -> "تمدد (Stretch)"
            "16:9" -> "16:9"
            "4:3" -> "4:3"
            "18:9" -> "18:9"
            "21:9" -> "21:9"
            "1:1" -> "1:1"
            else -> nextRatio
        }
        android.widget.Toast.makeText(context, label, android.widget.Toast.LENGTH_SHORT).show()
    }

    fun setVideoZoom(zoom: Float) {
        val clamped = zoom.coerceIn(0.5f, 4.0f)
        _currentVideoZoom.value = clamped
        mpvController.setPropertyFloat("video-zoom", clamped - 1.0f)
        _zoomOverlayText.value = "${(clamped * 100).toInt()}%"
        zoomHideJob?.cancel()
        zoomHideJob = viewModelScope.launch {
            delay(1500)
            _zoomOverlayText.value = null
        }
    }

    fun onPinchZoom(zoomDelta: Float) {
        val current = _currentVideoZoom.value
        val newZoom = (current * zoomDelta).coerceIn(0.5f, 4.0f)
        setVideoZoom(newZoom)
    }

    fun takeScreenshot(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val finalPlayerDir = File(picturesDir, "FinalPlayer")
                if (!finalPlayerDir.exists()) {
                    finalPlayerDir.mkdirs()
                }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US).format(java.util.Date())
                val imageFile = File(finalPlayerDir, "FinalPlayer_$timeStamp.jpg")

                // Configure MPV for JPG screenshot format
                mpvController.getAttachedView()?.setPropertyString("screenshot-format", "jpg")
                mpvController.getAttachedView()?.setPropertyString("screenshot-jpeg-quality", "95")

                // screenshot-to-file <filename> sub captures video + subtitles only (no UI overlay)
                mpvController.getAttachedView()?.command(arrayOf("screenshot-to-file", imageFile.absolutePath, "sub"))

                // Register file in Android MediaScanner
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/jpeg")
                ) { _, _ -> }

                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "تم حفظ لقطة الشاشة في Pictures/FinalPlayer",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error taking screenshot", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        context,
                        "فشل حفظ لقطة الشاشة",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun openSheet(sheet: Sheets) {
        _sheetShown.update { sheet }
        setControlsShown(true)
    }

    fun closeSheet() {
        _sheetShown.update { Sheets.None }
        setControlsShown(true)
    }

    private val SUBTITLE_EXTENSIONS = setOf(
        "srt", "ass", "ssa", "vtt", "sub", "idx",
        "smi", "sup", "txt", "lrc"
    )

    fun autoLoadSubtitlesFromVideoFolder(videoUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoPath = when (videoUri.scheme) {
                    "file" -> videoUri.path ?: return@launch
                    "content" -> {
                        context.contentResolver.query(
                            videoUri,
                            arrayOf(MediaStore.Video.Media.DATA),
                            null, null, null
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val col = cursor.getColumnIndexOrThrow(
                                    MediaStore.Video.Media.DATA
                                )
                                cursor.getString(col)
                            } else null
                        } ?: return@launch
                    }
                    else -> videoUri.path ?: videoUri.toString()
                }

                val videoFile = File(videoPath)
                val videoName = videoFile.nameWithoutExtension
                val parentDir = videoFile.parentFile ?: return@launch

                parentDir.listFiles()?.filter { file ->
                    file.isFile &&
                    file.extension.lowercase() in SUBTITLE_EXTENSIONS &&
                    (
                        file.nameWithoutExtension == videoName ||
                        file.nameWithoutExtension.startsWith("$videoName.") ||
                        file.nameWithoutExtension.startsWith("${videoName}_")
                    )
                }?.sortedBy { it.name }
                 ?.forEach { subFile ->
                    addSubtitle(
                        uri = Uri.fromFile(subFile),
                        context = context,
                        select = false
                    )
                 }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "Auto subtitle scan failed", e)
            }
        }
    }

    fun addSubtitle(uri: Uri, context: Context, select: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            subtitleAddMutex.withLock {
                val subPath = uri.toString()
                if (_externalSubtitles.contains(subPath)) return@withLock

                if (uri.scheme == "content") {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        Log.e("PlayerViewModel", "Could not take persistable permission", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    mpvController.addSubtitle(subPath, select)
                    updateTracks()
                }
                _externalSubtitles.add(subPath)
            }
        }
    }

    fun removeSubtitle(id: Int) {
        mpvController.getAttachedView()?.command(arrayOf("sub-remove", id.toString()))
        updateTracks()
    }

    fun onVideoFileLoaded(hasState: Boolean = false) {
        hasAttemptedAutoSelectSub = false
        setAspectRatio("default")
        viewModelScope.launch {
            trackSelector?.onFileLoaded(hasState, mpvController)
            updateTracks()
        }
    }

    fun toggleSubtitle(id: Int) {
        val currentP = _selectedSubId.value ?: 0
        val currentS = _selectedSecondarySubId.value ?: 0

        when {
            id == currentP -> {
                // Turn off primary
                mpvController.setPrimarySubtitle(0)
                _selectedSubId.value = 0
            }
            id == currentS -> {
                // Turn off secondary
                mpvController.setSecondarySubtitle(0)
                _selectedSecondarySubId.value = 0
            }
            currentP == 0 -> {
                // Set as primary
                mpvController.setPrimarySubtitle(id)
                _selectedSubId.value = id
            }
            currentS == 0 -> {
                // Set as secondary
                mpvController.setSecondarySubtitle(id)
                _selectedSecondarySubId.value = id
            }
            else -> {
                // Replace primary
                mpvController.setPrimarySubtitle(id)
                _selectedSubId.value = id
            }
        }
    }

    fun disableSubtitles() {
        mpvController.setPrimarySubtitle(0)
        mpvController.setSecondarySubtitle(0)
        _selectedSubId.value = 0
        _selectedSecondarySubId.value = 0
    }

    fun isSubtitleSelected(id: Int): Boolean {
        return id == _selectedSubId.value || id == _selectedSecondarySubId.value
    }

    fun subtitleSelectionIndicator(id: Int): String? {
        return when (id) {
            _selectedSubId.value -> "P"
            _selectedSecondarySubId.value -> "S"
            else -> null
        }
    }

    fun toggleControls() {
        _controlsShown.update { !it }
    }

    fun setControlsShown(shown: Boolean) {
        _controlsShown.value = shown
    }

    fun pauseUnpause() {
        mpvController.togglePlayPause()
        val currentIsPlaying = mpvController.playerState.value.isPlaying
        _paused.value = !currentIsPlaying
    }

    fun play() {
        mpvController.resume()
        _paused.value = false
    }

    fun pause() {
        mpvController.pause()
        _paused.value = true
    }

    fun seekBy(offsetSeconds: Int) {
        val currentPos = _precisePosition.value
        val duration = _preciseDuration.value.coerceAtLeast(1f)
        val clampedPos = (currentPos + offsetSeconds).coerceIn(0f, duration)

        seekTo(clampedPos)
        if (offsetSeconds != 0) {
            showDoubleTapFeedback(isLeft = offsetSeconds < 0, amount = kotlin.math.abs(offsetSeconds))
        }
    }

    fun leftSeek() {
        val duration = playerPrefs?.doubleTapToSeekDuration?.get() ?: 10
        seekBy(-duration)
    }

    fun rightSeek() {
        val duration = playerPrefs?.doubleTapToSeekDuration?.get() ?: 10
        seekBy(duration)
    }

    private fun showDoubleTapFeedback(isLeft: Boolean, amount: Int) {
        doubleTapHideJob?.cancel()
        val current = _doubleTapSeekState.value
        val newAmount = if (current != null && current.isLeft == isLeft) {
            current.amountSeconds + amount
        } else {
            amount
        }
        _doubleTapSeekState.value = DoubleTapSeekState(
            isLeft = isLeft,
            amountSeconds = newAmount,
            timestamp = System.currentTimeMillis()
        )
        doubleTapHideJob = viewModelScope.launch {
            delay(1000)
            _doubleTapSeekState.value = null
        }
    }

    fun onSliderDragStart() {
        _isSliderDragging.value = true
    }

    fun seekTo(positionSeconds: Float) {
        val duration = _preciseDuration.value.coerceAtLeast(1f)
        val clampedPos = positionSeconds.coerceIn(0f, duration)

        _isSliderDragging.value = false
        _precisePosition.value = clampedPos
        lastKnownPositionMs = (clampedPos * 1000f).toLong()
        lastSeekTimeMs = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            val precise = playerPrefs?.usePreciseSeeking?.get() ?: true
            val seekMode = if (precise) "absolute+exact" else "absolute+keyframes"
            try {
                mpvController.getAttachedView()?.seekTo(clampedPos.toDouble(), seekMode)
            } catch (e: Exception) {
                try {
                    mpvController.seekTo((clampedPos * 1000f).toLong())
                } catch (e2: Exception) {
                    MPVLib.command("seek", clampedPos.toString(), seekMode)
                }
            }
        }
    }

    fun customSkip() {
        viewModelScope.launch {
            val duration = playerPrefs?.customSkipDuration?.get() ?: 10
            seekBy(duration)
        }
    }

    fun onLongPressSpeedStart() {
        viewModelScope.launch {
            val holdSpeed = playerPrefs?.holdForMultipleSpeed?.get() ?: 2.5f
            normalPlaybackSpeed = _playbackSpeed.value
            mpvController.setPlaybackSpeed(holdSpeed)
            _longPressSpeedValue.value = holdSpeed
            _isLongPressSpeedActive.value = true
        }
    }

    fun onLongPressSpeedDrag(deltaPx: Float) {
        viewModelScope.launch {
            val showDynamic = playerPrefs?.showDynamicSpeed?.get() ?: true
            if (!showDynamic || !_isLongPressSpeedActive.value) return@launch
            val sensitivity = 0.005f
            val newSpeed = (_longPressSpeedValue.value + deltaPx * sensitivity).coerceIn(0.5f, 4.0f)
            val rounded = (newSpeed * 20).toInt() / 20f
            if (kotlin.math.abs(rounded - _longPressSpeedValue.value) >= 0.05f) {
                mpvController.setPlaybackSpeed(rounded)
                _longPressSpeedValue.value = rounded
            }
        }
    }

    fun onLongPressSpeedEnd() {
        if (_isLongPressSpeedActive.value) {
            mpvController.setPlaybackSpeed(normalPlaybackSpeed)
            _isLongPressSpeedActive.value = false
        }
    }

    // --- BRIGHTNESS CONTROLS ---
    fun initBrightness(window: Window?, context: Context) {
        val remember = playerPrefs?.rememberBrightness?.get() ?: false
        val savedBrightness = if (remember) playerPrefs?.defaultBrightness?.get() ?: -1f else -1f

        if (savedBrightness > 0f) {
            _currentBrightness.value = savedBrightness.coerceIn(0.01f, 1f)
            window?.let {
                val lp = it.attributes
                lp.screenBrightness = savedBrightness
                it.attributes = lp
            }
            return
        }

        val currentVal = window?.attributes?.screenBrightness
        if (currentVal != null && currentVal >= 0f) {
            _currentBrightness.value = currentVal.coerceIn(0.01f, 1f)
        } else {
            try {
                val sysVal = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                _currentBrightness.value = (sysVal / 255f).coerceIn(0.01f, 1f)
            } catch (e: Exception) {
                _currentBrightness.value = 0.5f
            }
        }
    }

    fun changeBrightnessBy(deltaRatio: Float, window: Window?, context: Context) {
        val newBrightness = (_currentBrightness.value + deltaRatio).coerceIn(0.01f, 1.0f)
        _currentBrightness.value = newBrightness

        window?.let {
            val lp = it.attributes
            lp.screenBrightness = newBrightness
            it.attributes = lp
        }

        val remember = playerPrefs?.rememberBrightness?.get() ?: false
        if (remember) {
            viewModelScope.launch {
                playerPrefs?.defaultBrightness?.set(newBrightness)
            }
        }

        try {
            val val255 = (newBrightness * 255).toInt().coerceIn(1, 255)
            if (Settings.System.canWrite(context)) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, val255)
            }
        } catch (e: Exception) {
            // Permission or write error ignored
        }

        displayBrightnessSlider()
    }

    fun displayBrightnessSlider() {
        _isBrightnessSliderShown.value = true
        brightnessHideJob?.cancel()
        brightnessHideJob = viewModelScope.launch {
            delay(1000)
            _isBrightnessSliderShown.value = false
        }
    }

    // --- VOLUME CONTROLS ---
    fun initVolume(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        _currentVolumePercent.value = (currentVol.toFloat() / maxVol.toFloat() * 100f).coerceIn(0f, 100f)
    }

    fun changeVolumeBy(deltaRatio: Float, context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)

        val newPercent = (_currentVolumePercent.value + (deltaRatio * 100f)).coerceIn(0f, 150f)
        _currentVolumePercent.value = newPercent

        if (newPercent <= 100f) {
            val targetVol = kotlin.math.round((newPercent / 100f) * maxVol).toInt().coerceIn(0, maxVol)
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error setting volume", e)
            }
            mpvController.setPropertyInt("volume-max", 150)
            mpvController.setPropertyInt("volume", 100)
        } else {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error setting volume", e)
            }
            mpvController.setPropertyInt("volume-max", 150)
            mpvController.setPropertyInt("volume", newPercent.toInt())
        }

        displayVolumeSlider()
    }

    fun displayVolumeSlider() {
        _isVolumeSliderShown.value = true
        volumeHideJob?.cancel()
        volumeHideJob = viewModelScope.launch {
            delay(1000)
            _isVolumeSliderShown.value = false
        }
    }

    // --- HORIZONTAL DRAG SEEK CONTROLS ---
    fun onHorizontalDragStart() {
        val currentSec = if (_precisePosition.value > 0f) {
            _precisePosition.value
        } else if (lastKnownPositionMs > 0L) {
            lastKnownPositionMs / 1000f
        } else {
            0f
        }
        seekStartPositionSec = currentSec
        cumulativeSeekDeltaSec = 0f
        _dragSeekState.value = SeekState(
            targetPositionSec = seekStartPositionSec,
            diffSeconds = 0f,
            isForwards = true,
            isDragging = true
        )
    }

    fun onHorizontalDrag(deltaPx: Float, screenWidthPx: Float) {
        val duration = _preciseDuration.value.coerceAtLeast(1f)
        // Smooth controlled seek: 1 full screen width = 90 seconds base (scaled by swipeSpeed in GestureHandler)
        val pxToSecRatio = 90f / screenWidthPx.coerceAtLeast(1f)

        val deltaSec = deltaPx * pxToSecRatio
        cumulativeSeekDeltaSec += deltaSec

        val newTargetSec = (seekStartPositionSec + cumulativeSeekDeltaSec).coerceIn(0f, duration)
        val actualDiffSec = newTargetSec - seekStartPositionSec

        _dragSeekState.value = SeekState(
            targetPositionSec = newTargetSec,
            diffSeconds = actualDiffSec,
            isForwards = actualDiffSec >= 0,
            isDragging = true
        )
    }

    fun onHorizontalDragEnd() {
        val finalState = _dragSeekState.value
        if (finalState != null) {
            val duration = _preciseDuration.value.coerceAtLeast(1f)
            val clampedTargetSec = finalState.targetPositionSec.coerceIn(0f, duration)
            seekTo(clampedTargetSec)
        }
        _dragSeekState.value = null
    }

    fun startTimer(seconds: Int) {
        sleepTimerJob?.cancel()
        _remainingTime.value = seconds
        if (seconds <= 0) return

        sleepTimerJob = viewModelScope.launch {
            while (isActive && _remainingTime.value > 0) {
                delay(1000)
                _remainingTime.update { current ->
                    val next = current - 1
                    if (next <= 0) {
                        pause()
                        _isBackgroundPlay.value = false
                        try {
                            MediaPlaybackService.stopService(context)
                        } catch (e: Exception) {
                            Log.e("PlayerViewModel", "Error stopping service on sleep timer end", e)
                        }
                        0
                    } else {
                        next
                    }
                }
            }
        }
    }

    fun cancelTimer() {
        sleepTimerJob?.cancel()
        _remainingTime.value = 0
    }

    private var eofHandled = false

    private fun startAdaptivePolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var loopCount = 0
            while (isActive) {
                val state = mpvController.playerState.value
                val posSec = state.positionMs / 1000f
                val durSec = state.durationMs / 1000f

                val isEof = try { MPVLib.getPropertyBoolean("eof-reached") ?: false } catch (e: Exception) { false }
                if (isEof || (durSec > 2f && posSec >= durSec - 0.5f)) {
                    if (!eofHandled) {
                        eofHandled = true
                        saveCurrentProgressNow()

                        val autoPlayNext = playerPrefs?.autoPlayNext?.get() ?: true
                        val closeAfterPlayback = playerPrefs?.closeAfterPlayback?.get() ?: true

                        val items = _playlistItems.value
                        val currentIndex = _currentPlaylistIndex.value
                        val hasNext = items.isNotEmpty() && currentIndex + 1 < items.size

                        if (autoPlayNext && hasNext) {
                            playNextVideo()
                        } else if (closeAfterPlayback) {
                            pause()
                            _isBackgroundPlay.value = false
                            try {
                                MediaPlaybackService.stopService(context)
                            } catch (e: Exception) {
                                Log.e("PlayerViewModel", "Error stopping service on playback finish", e)
                            }
                            _finishActivityEvent.tryEmit(Unit)
                        }
                    }
                } else if (posSec < durSec - 2f) {
                    eofHandled = false
                }

                val timeSinceSeek = System.currentTimeMillis() - lastSeekTimeMs
                if (_dragSeekState.value == null && !_isSliderDragging.value && timeSinceSeek > 1200L) {
                    if (kotlin.math.abs(_precisePosition.value - posSec) >= 0.1f) {
                        _precisePosition.value = posSec
                    }
                    if (state.positionMs > 0L) {
                        lastKnownPositionMs = state.positionMs
                    }
                }

                // Auto resume to saved progress when playback starts
                if (!hasAppliedAutoResume && _resumePositionSec.value != null && durSec > 0f) {
                    val targetResume = _resumePositionSec.value!!
                    if (targetResume > 2.0 && posSec < targetResume - 1.0) {
                        hasAppliedAutoResume = true
                        seekTo(targetResume.toFloat())
                    } else if (posSec >= targetResume - 1.0) {
                        hasAppliedAutoResume = true
                    }
                }

                if (_preciseDuration.value != durSec) {
                    _preciseDuration.value = durSec
                }
                val isPausedState = !state.isPlaying
                if (_paused.value != isPausedState) {
                    _paused.value = isPausedState
                }

                // Subtitle text (lightweight text query)
                _currentSubText.value = mpvController.getSubtitleText()

                // Heavy metadata & track updates only fetched periodically (every ~2.5 to 5 seconds) to eliminate CPU thrashing
                if (loopCount % 10 == 0 || _sheetShown.value != null) {
                    updateTracks()
                    _currentDecoder.value = Decoder.getDecoderFromValue(mpvController.getCurrentDecoderValue())
                    if (!_isLongPressSpeedActive.value) {
                        _playbackSpeed.value = mpvController.getPlaybackSpeed()
                    }
                    _chapters.value = mpvController.getChapters()
                    _currentChapterIndex.value = mpvController.getCurrentChapterIndex()

                    val aspect = mpvController.getAttachedView()?.videoAspect
                    if (aspect != null && aspect > 0.05 && _videoAspect.value != aspect) {
                        _videoAspect.value = aspect
                    }
                }
                loopCount++

                // Efficient polling delay: 250ms when controls or seeking are active, 500ms during normal video playback
                val pollInterval = if (_controlsShown.value || seekCoalescingJob?.isActive == true) 250L else 500L
                delay(pollInterval)
            }
        }
    }

    fun handlePlayerAction(
        action: PlayerButtonType,
        context: Context,
        onEnterPiP: (() -> Unit)? = null
    ) {
        when (action) {
            PlayerButtonType.CHAPTERS, PlayerButtonType.CURRENT_CHAPTER -> openSheet(Sheets.Chapters)
            PlayerButtonType.PLAYBACK_SPEED -> openSheet(Sheets.PlaybackSpeed)
            PlayerButtonType.DECODER -> openSheet(Sheets.Decoders)
            PlayerButtonType.SCREEN_ROTATION -> toggleRotateOrientation()
            PlayerButtonType.FRAME_BY_FRAME -> openSheet(Sheets.FrameNav)
            PlayerButtonType.VIDEO_ZOOM -> openSheet(Sheets.VideoZoom)
            PlayerButtonType.PIP_MODE -> {
                if (onEnterPiP != null) {
                    onEnterPiP()
                } else {
                    Toast.makeText(context, "قريباً: ${action.title}", Toast.LENGTH_SHORT).show()
                }
            }
            PlayerButtonType.ASPECT_RATIO -> cycleNextAspectRatio(context)
            PlayerButtonType.LOCK_CONTROLS -> toggleLock()
            PlayerButtonType.AUDIO_TRACK -> openSheet(Sheets.AudioTracks)
            PlayerButtonType.SUBTITLES -> openSheet(Sheets.SubtitleTracks)
            PlayerButtonType.MORE_OPTIONS -> openSheet(Sheets.More)
            PlayerButtonType.LOOP_MODE -> toggleRepeatMode()
            PlayerButtonType.SHUFFLE -> toggleShuffle()
            PlayerButtonType.FLIP_HORIZONTAL -> toggleFlipH()
            PlayerButtonType.FLIP_VERTICAL -> toggleFlipV()
            PlayerButtonType.AB_REPEAT -> toggleAbRepeat()
            PlayerButtonType.CUSTOM_SKIP -> {
                seekBy(10)
                Toast.makeText(context, "تخطي مخصص (10 ثوانٍ)", Toast.LENGTH_SHORT).show()
            }
            PlayerButtonType.BACKGROUND_PLAY -> toggleBackgroundPlay()
            PlayerButtonType.CINEMA_MODE -> toggleCinemaMode()
            PlayerButtonType.SLEEP_TIMER -> openSheet(Sheets.SleepTimer)
        }
    }

    override fun onCleared() {
        saveCurrentProgressNow()
        pollingJob?.cancel()
        sleepTimerJob?.cancel()
        seekCoalescingJob?.cancel()
        brightnessHideJob?.cancel()
        volumeHideJob?.cancel()
        doubleTapHideJob?.cancel()
        autoSaveProgressJob?.cancel()
        super.onCleared()
    }
}
