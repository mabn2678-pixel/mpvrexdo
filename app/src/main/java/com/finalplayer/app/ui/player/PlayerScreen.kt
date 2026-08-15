package com.finalplayer.app.ui.player

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finalplayer.app.player.core.MPVLib
import com.finalplayer.app.player.PlayerActivity
import com.finalplayer.app.player.PlayerViewModel
import com.finalplayer.app.player.core.MPVView
import com.finalplayer.app.ui.player.controls.PlayerControls

@Composable
fun PlayerScreen(
    videoPath: String,
    videoTitle: String,
    viewModel: PlayerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val configuration = LocalConfiguration.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mpvView = remember { MPVView(context) }

    LaunchedEffect(configuration.orientation) {
        val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
        viewModel.adjustSubtitleScaleForOrientation(isPortrait)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                val hasSleepTimer = viewModel.remainingTime.value > 0
                if (activity?.isFinishing == true || !hasSleepTimer) {
                    try {
                        viewModel.pause()
                        viewModel.stopPlayback()
                        mpvView.stop()
                        if (activity?.isFinishing == true) {
                            mpvView.destroy()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val hasSleepTimer = viewModel.remainingTime.value > 0
            if (activity?.isFinishing == true || !hasSleepTimer) {
                try {
                    viewModel.pause()
                    viewModel.stopPlayback()
                    mpvView.stop()
                    if (activity?.isFinishing == true) {
                        mpvView.destroy()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val isPaused by viewModel.paused.collectAsStateWithLifecycle()
    val positionSeconds by viewModel.precisePosition.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.preciseDuration.collectAsStateWithLifecycle()
    val controlsVisible by viewModel.controlsShown.collectAsStateWithLifecycle()
    val sleepTimerSeconds by viewModel.remainingTime.collectAsStateWithLifecycle()

    val currentBrightness by viewModel.currentBrightness.collectAsStateWithLifecycle()
    val isBrightnessSliderShown by viewModel.isBrightnessSliderShown.collectAsStateWithLifecycle()
    val currentVolumePercent by viewModel.currentVolumePercent.collectAsStateWithLifecycle()
    val isVolumeSliderShown by viewModel.isVolumeSliderShown.collectAsStateWithLifecycle()
    val dragSeekState by viewModel.dragSeekState.collectAsStateWithLifecycle()
    val doubleTapSeekState by viewModel.doubleTapSeekState.collectAsStateWithLifecycle()
    val isLongPressSpeedActive by viewModel.isLongPressSpeedActive.collectAsStateWithLifecycle()
    val longPressSpeedValue by viewModel.longPressSpeedValue.collectAsStateWithLifecycle()
    val zoomOverlayText by viewModel.zoomOverlayText.collectAsStateWithLifecycle()
    val subPosOverlayText by viewModel.subPosOverlayText.collectAsStateWithLifecycle()
    val isSubtitleBoxDragging by viewModel.isSubtitleBoxDragging.collectAsStateWithLifecycle()
    val currentSubPos by viewModel.currentSubPos.collectAsStateWithLifecycle()

    val subtitleTracks by viewModel.subtitleTracks.collectAsStateWithLifecycle()
    val audioTracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val selectedSubId by viewModel.selectedSubId.collectAsStateWithLifecycle()
    val selectedSecondarySubId by viewModel.selectedSecondarySubId.collectAsStateWithLifecycle()
    val selectedAudioId by viewModel.selectedAudioId.collectAsStateWithLifecycle()
    val currentSubText by viewModel.currentSubText.collectAsStateWithLifecycle()

    val currentDecoder by viewModel.currentDecoder.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val currentChapterIndex by viewModel.currentChapterIndex.collectAsStateWithLifecycle()
    val sheetShown by viewModel.sheetShown.collectAsStateWithLifecycle()

    val playlistItems by viewModel.playlistItems.collectAsStateWithLifecycle()
    val currentPlaylistIndex by viewModel.currentPlaylistIndex.collectAsStateWithLifecycle()
    val isPlaylistMode by viewModel.isPlaylistMode.collectAsStateWithLifecycle()
    val isShortsMode by viewModel.isShortsMode.collectAsStateWithLifecycle()

    val isLocked by viewModel.isLocked.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val isShuffle by viewModel.isShuffle.collectAsStateWithLifecycle()
    val isCinemaMode by viewModel.isCinemaMode.collectAsStateWithLifecycle()
    val isBackgroundPlay by viewModel.isBackgroundPlay.collectAsStateWithLifecycle()
    val currentAspectRatio by viewModel.currentAspectRatio.collectAsStateWithLifecycle()
    val currentVideoZoom by viewModel.currentVideoZoom.collectAsStateWithLifecycle()
    val videoAspect by viewModel.videoAspect.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()

    // Initialize initial system audio & brightness
    LaunchedEffect(Unit) {
        viewModel.initBrightness(activity?.window, context)
        viewModel.initVolume(context)
    }

    val playerPrefs = viewModel.playerPrefs
    val keepScreenOnPauseState = playerPrefs?.keepScreenOnPause?.asFlow()?.collectAsState(initial = false)
    val keepScreenOnPause = keepScreenOnPauseState?.value ?: false
    val showLoadingCircleState = playerPrefs?.showLoadingCircle?.asFlow()?.collectAsState(initial = true)
    val showLoadingCircle = showLoadingCircleState?.value ?: true

    // Keep screen on while player is active and playing (or if keepScreenOnPause is enabled)
    DisposableEffect(isPaused, keepScreenOnPause) {
        val window = activity?.window
        if (window != null) {
            if (isPaused != true || keepScreenOnPause) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Attach view & initialize title
    DisposableEffect(mpvView) {
        viewModel.onFileLoaded(videoTitle)

        val playCurrentTarget = {
            val pendingPath = viewModel.mpvController.playerState.value.currentFilePath
            val currentId = viewModel.currentVideoId.value
            val target = when {
                videoPath.isNotEmpty() -> videoPath
                !pendingPath.isNullOrEmpty() -> pendingPath
                !currentId.isNullOrEmpty() -> currentId
                else -> null
            }
            if (target != null) {
                val isAlreadyPlaying = viewModel.mpvController.playerState.value.currentFilePath == target && !viewModel.mpvController.isIdle()
                if (!isAlreadyPlaying) {
                    viewModel.mpvController.play(target)
                }
                viewModel.autoLoadSubtitlesFromVideoFolder(android.net.Uri.parse(target))
                viewModel.applyAllSubtitlePreferences()
                viewModel.updateTracks()
            }
        }

        mpvView.onSurfaceReady = {
            playCurrentTarget()
        }

        viewModel.mpvController.attachView(mpvView)

        if (mpvView.isSurfaceReady) {
            playCurrentTarget()
        }

        onDispose {
            viewModel.saveCurrentProgressNow()
            val hasSleepTimer = viewModel.remainingTime.value > 0
            if (activity?.isFinishing == true || !hasSleepTimer) {
                try {
                    mpvView.stop()
                    if (activity?.isFinishing == true) {
                        mpvView.destroy()
                    }
                } catch (_: Throwable) {}
            }
            viewModel.mpvController.detachView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                (mpvView.parent as? ViewGroup)?.removeView(mpvView)
                mpvView
            },
            modifier = Modifier.fillMaxSize()
        )

        PlayerControls(
            title = videoTitle,
            isPaused = isPaused ?: true,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds,
            controlsVisible = controlsVisible,
            remainingSleepTimerSeconds = sleepTimerSeconds,
            brightnessValue = currentBrightness,
            isBrightnessSliderShown = isBrightnessSliderShown,
            volumePercent = currentVolumePercent,
            isVolumeSliderShown = isVolumeSliderShown,
            dragSeekState = dragSeekState,
            doubleTapSeekState = doubleTapSeekState,
            isLongPressSpeedActive = isLongPressSpeedActive,
            longPressSpeedValue = longPressSpeedValue,
            zoomOverlayText = zoomOverlayText,
            subPosOverlayText = subPosOverlayText,
            isSubtitleBoxDragging = isSubtitleBoxDragging,
            currentSubPos = currentSubPos,
            onSubtitleDragStart = { viewModel.onSubtitleDragStart() },
            onSubtitlePositionDrag = { delta, screenH -> viewModel.handleSubtitleVerticalDrag(delta, screenH) },
            onSubtitleDragEnd = { viewModel.onSubtitleDragEnd() },
            onPinchZoom = { delta -> viewModel.onPinchZoom(delta) },
            onLongPressStart = { viewModel.onLongPressSpeedStart() },
            onLongPressDrag = { delta -> viewModel.onLongPressSpeedDrag(delta) },
            onLongPressEnd = { viewModel.onLongPressSpeedEnd() },
            onSliderDragStart = { viewModel.onSliderDragStart() },
            onToggleControls = { viewModel.toggleControls() },
            onPlayPause = { viewModel.pauseUnpause() },
            onSeekTo = { pos -> viewModel.seekTo(pos) },
            onSeekBy = { offset -> viewModel.seekBy(offset) },
            onLeftDoubleTap = { viewModel.leftSeek() },
            onRightDoubleTap = { viewModel.rightSeek() },
            onCenterDoubleTap = { viewModel.pauseUnpause() },
            onVerticalBrightnessDrag = { delta ->
                viewModel.changeBrightnessBy(delta, activity?.window, context)
            },
            onVerticalVolumeDrag = { delta ->
                viewModel.changeVolumeBy(delta, context)
            },
            onHorizontalDragStart = { viewModel.onHorizontalDragStart() },
            onHorizontalDrag = { delta, screenWidth ->
                viewModel.onHorizontalDrag(delta, screenWidth)
            },
            onHorizontalDragEnd = { viewModel.onHorizontalDragEnd() },
            onBackClick = onBackClick,
            onStartSleepTimer = { seconds -> viewModel.startTimer(seconds) },
            onCancelSleepTimer = { viewModel.cancelTimer() },
            subtitleTracks = subtitleTracks,
            audioTracks = audioTracks,
            selectedSubId = selectedSubId,
            selectedSecondarySubId = selectedSecondarySubId,
            selectedAudioId = selectedAudioId,
            currentDecoder = currentDecoder,
            playbackSpeed = playbackSpeed,
            chapters = chapters,
            currentChapterIndex = currentChapterIndex,
            sheetShown = sheetShown,
            onOpenSheet = { sheet -> viewModel.openSheet(sheet) },
            onCloseSheet = { viewModel.closeSheet() },
            onSelectSubtitle = { trackId -> viewModel.toggleSubtitle(trackId) },
            onDisableSubtitles = { viewModel.disableSubtitles() },
            onAddExternalSubtitle = { uri -> viewModel.addSubtitle(uri, context) },
            onRemoveSubtitle = { id -> viewModel.removeSubtitle(id) },
            onSelectAudioTrack = { id -> viewModel.selectAudioTrack(id) },
            onAddAudio = { uri -> viewModel.addAudio(uri, context) },
            onSelectDecoder = { dec -> viewModel.setDecoder(dec) },
            onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
            onSelectChapter = { index -> viewModel.selectChapter(index) },
            isPlaylistMode = isPlaylistMode,
            isShortsMode = isShortsMode,
            currentPlaylistIndex = currentPlaylistIndex,
            totalPlaylistCount = playlistItems.size,
            playlistItems = playlistItems,
            onNextClick = { viewModel.playNextVideo() },
            onPreviousClick = { viewModel.playPreviousVideo() },
            onReorderPlaylist = { from, to -> viewModel.reorderPlaylist(from, to) },
            onSelectPlaylistItem = { idx -> viewModel.playPlaylistItem(idx) },
            isLocked = isLocked,
            repeatMode = repeatMode,
            isShuffle = isShuffle,
            isCinemaMode = isCinemaMode,
            isBackgroundPlay = isBackgroundPlay,
            currentAspectRatio = currentAspectRatio,
            currentVideoZoom = currentVideoZoom,
            onToggleRotate = { viewModel.toggleRotateOrientation() },
            onToggleLock = { viewModel.toggleLock() },
            onToggleRepeat = { viewModel.toggleRepeatMode() },
            onToggleShuffle = { viewModel.toggleShuffle() },
            onFrameStep = { forward -> viewModel.stepFrame(forward) },
            onFlipVideo = { vertical -> if (vertical) viewModel.toggleFlipV() else viewModel.toggleFlipH() },
            onToggleAbRepeat = { viewModel.toggleAbRepeat() },
            onCustomSkip = { viewModel.customSkip() },
            onToggleCinema = { viewModel.toggleCinemaMode() },
            onToggleBackgroundPlay = { viewModel.toggleBackgroundPlay() },
            onCycleAspectRatio = { viewModel.cycleNextAspectRatio(context) },
            onSetAspectRatio = { ratio -> viewModel.setAspectRatio(ratio) },
            onSetVideoZoom = { zoom -> viewModel.setVideoZoom(zoom) },
            onTakeScreenshot = { viewModel.takeScreenshot(context) },
            onEnterPiP = {
                if (activity is PlayerActivity) {
                    activity.enterPiPMode()
                } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    activity?.enterPictureInPictureMode()
                }
            }
        )

        if (isBuffering && showLoadingCircle) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
