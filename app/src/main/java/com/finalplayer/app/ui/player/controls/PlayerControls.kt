package com.finalplayer.app.ui.player.controls

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import com.finalplayer.app.R
import com.finalplayer.app.ui.player.ChapterNode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import com.finalplayer.app.ui.player.controls.components.SubPositionOverlay
import com.finalplayer.app.ui.player.controls.components.WaterRipplePlayPauseButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.CropOriginal
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ZoomIn
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.player.controls.components.sheets.PlaylistSheet
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.finalplayer.app.player.DoubleTapSeekState
import com.finalplayer.app.player.SeekState
import com.finalplayer.app.ui.components.FinalPlayerSeekbar
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.player.controls.components.BrightnessSlider
import com.finalplayer.app.ui.player.controls.components.DoubleTapSeekOvals
import com.finalplayer.app.ui.player.controls.components.SeekOverlay
import com.finalplayer.app.ui.player.controls.components.SpeedOverlay
import com.finalplayer.app.ui.player.controls.components.VolumeSlider
import com.finalplayer.app.ui.player.controls.components.ZoomOverlay
import androidx.compose.material.icons.filled.MoreVert
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
import kotlinx.coroutines.delay
import java.util.Locale

import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import com.finalplayer.app.domain.model.PlayerButtonType
import com.finalplayer.app.ui.settings.layout.ControlTools
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControls(
    title: String,
    isPaused: Boolean,
    positionSeconds: Float,
    durationSeconds: Float,
    controlsVisible: Boolean,
    remainingSleepTimerSeconds: Int,
    brightnessValue: Float,
    isBrightnessSliderShown: Boolean,
    volumePercent: Float,
    isVolumeSliderShown: Boolean,
    dragSeekState: SeekState?,
    doubleTapSeekState: DoubleTapSeekState?,
    isLongPressSpeedActive: Boolean = false,
    longPressSpeedValue: Float = 2.5f,
    zoomOverlayText: String? = null,
    subPosOverlayText: String? = null,
    isSubtitleBoxDragging: Boolean = false,
    currentSubPos: Int = 100,
    onSubtitleDragStart: () -> Unit = {},
    onSubtitlePositionDrag: (deltaPx: Float, screenHeightPx: Float) -> Unit = { _, _ -> },
    onSubtitleDragEnd: () -> Unit = {},
    onPinchZoom: (Float) -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressDrag: (Float) -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onSliderDragStart: () -> Unit = {},
    onToggleControls: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onSeekBy: (Int) -> Unit,
    onLeftDoubleTap: () -> Unit,
    onRightDoubleTap: () -> Unit,
    onCenterDoubleTap: () -> Unit,
    onVerticalBrightnessDrag: (Float) -> Unit,
    onVerticalVolumeDrag: (Float) -> Unit,
    onHorizontalDragStart: () -> Unit,
    onHorizontalDrag: (deltaPx: Float, screenWidthPx: Float) -> Unit,
    onHorizontalDragEnd: () -> Unit,
    onBackClick: () -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    subtitleTracks: List<TrackNode> = emptyList(),
    audioTracks: List<TrackNode> = emptyList(),
    selectedSubId: Int? = 0,
    selectedSecondarySubId: Int? = 0,
    selectedAudioId: Int? = 0,
    currentDecoder: Decoder = Decoder.HW_PLUS,
    playbackSpeed: Float = 1.0f,
    chapters: List<ChapterNode> = emptyList(),
    currentChapterIndex: Int? = null,
    sheetShown: Sheets = Sheets.None,
    onOpenSheet: (Sheets) -> Unit = {},
    onCloseSheet: () -> Unit = {},
    onSelectSubtitle: (Int) -> Unit = {},
    onDisableSubtitles: () -> Unit = {},
    onAddExternalSubtitle: (Uri) -> Unit = {},
    onRemoveSubtitle: (Int) -> Unit = {},
    onSelectAudioTrack: (Int) -> Unit = {},
    onAddAudio: (Uri) -> Unit = {},
    onSelectDecoder: (Decoder) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onSelectChapter: (Int) -> Unit = {},
    isPlaylistMode: Boolean = false,
    isShortsMode: Boolean = false,
    currentPlaylistIndex: Int = 0,
    totalPlaylistCount: Int = 0,
    playlistItems: List<VideoItem> = emptyList(),
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onReorderPlaylist: (Int, Int) -> Unit = { _, _ -> },
    onSelectPlaylistItem: (Int) -> Unit = {},
    isLocked: Boolean = false,
    repeatMode: Int = 0,
    isShuffle: Boolean = false,
    isCinemaMode: Boolean = false,
    isBackgroundPlay: Boolean = false,
    currentAspectRatio: String = "default",
    currentVideoZoom: Float = 1.0f,
    onToggleLock: () -> Unit = {},
    onToggleRotate: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onFrameStep: (Boolean) -> Unit = {},
    onFlipVideo: (Boolean) -> Unit = {},
    onToggleAbRepeat: () -> Unit = {},
    onCustomSkip: () -> Unit = {},
    onToggleCinema: () -> Unit = {},
    onToggleBackgroundPlay: () -> Unit = {},
    onSetAspectRatio: (String) -> Unit = {},
    onSetVideoZoom: (Float) -> Unit = {},
    onCycleAspectRatio: () -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    modifier: Modifier = Modifier,
    layoutPrefs: PlayerLayoutPreferences = koinInject(),
    playerPrefs: com.finalplayer.app.data.preferences.PlayerPreferences = koinInject()
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPositionSeconds by remember { mutableFloatStateOf(0f) }
    var showRemainingTimeText by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var interactionKey by remember { mutableIntStateOf(0) }

    val isAnySheetOpen = sheetShown !is Sheets.None || showSleepTimerSheet

    val hideTimeoutMs by layoutPrefs.controlsHideTimeoutMs.asFlow().collectAsState(initial = 3000)
    val gradientOpacity by layoutPrefs.controlsGradientOpacity.asFlow().collectAsState(initial = 0.45f)
    val enablePrevNext by playerPrefs.enablePrevNextButtons.asFlow().collectAsState(initial = true)

    val topRightRaw by layoutPrefs.topRightControls.asFlow().collectAsState(initial = PlayerLayoutPreferences.DEFAULT_TOP_RIGHT)
    val bottomRightRaw by layoutPrefs.bottomRightControls.asFlow().collectAsState(initial = PlayerLayoutPreferences.DEFAULT_BOTTOM_RIGHT)
    val bottomLeftRaw by layoutPrefs.bottomLeftControls.asFlow().collectAsState(initial = PlayerLayoutPreferences.DEFAULT_BOTTOM_LEFT)
    val portraitBottomRaw by layoutPrefs.portraitBottomControls.asFlow().collectAsState(initial = PlayerLayoutPreferences.DEFAULT_PORTRAIT_BOTTOM)

    val topRightControlIds = layoutPrefs.parseControlList(topRightRaw)
    val bottomRightControlIds = layoutPrefs.parseControlList(bottomRightRaw)
    val bottomLeftControlIds = layoutPrefs.parseControlList(bottomLeftRaw)
    val portraitBottomControlIds = layoutPrefs.parseControlList(portraitBottomRaw)

    // Auto-hide controls after configured timeout unless paused, dragging, or sheet open
    LaunchedEffect(controlsVisible, isPaused, isDraggingSlider, isAnySheetOpen, hideTimeoutMs, interactionKey) {
        if (controlsVisible && !isPaused && !isDraggingSlider && !isAnySheetOpen && hideTimeoutMs > 0) {
            delay(hideTimeoutMs.toLong())
            onToggleControls()
        }
    }

    val hasActiveSubtitles = subtitleTracks.isNotEmpty() &&
            ((selectedSubId ?: 0) > 0 || (selectedSecondarySubId ?: 0) > 0) &&
            subtitleTracks.any { it.id == (selectedSubId ?: 0) || it.id == (selectedSecondarySubId ?: 0) }

    GestureHandler(
        onSingleTap = onToggleControls,
        onLeftDoubleTap = onLeftDoubleTap,
        onRightDoubleTap = onRightDoubleTap,
        onCenterDoubleTap = onCenterDoubleTap,
        onVerticalBrightnessDrag = onVerticalBrightnessDrag,
        onVerticalVolumeDrag = onVerticalVolumeDrag,
        onHorizontalDragStart = onHorizontalDragStart,
        onHorizontalDrag = onHorizontalDrag,
        onHorizontalDragEnd = onHorizontalDragEnd,
        onSubtitleDragStart = onSubtitleDragStart,
        onSubtitlePositionDrag = onSubtitlePositionDrag,
        onSubtitleDragEnd = onSubtitleDragEnd,
        onSubtitleClick = { onOpenSheet(Sheets.SubtitleSettings) },
        hasActiveSubtitles = hasActiveSubtitles,
        onPinchZoom = onPinchZoom,
        onLongPressStart = onLongPressStart,
        onLongPressDrag = onLongPressDrag,
        onLongPressEnd = onLongPressEnd,
        controlsVisible = controlsVisible,
        isLocked = isLocked,
        isAnySheetOpen = isAnySheetOpen,
        isShortsMode = isShortsMode,
        onNextClick = onNextClick,
        onPreviousClick = onPreviousClick,
        onUserInteraction = { interactionKey++ },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Dark Overlay when controls are visible
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = gradientOpacity))
                )
            }

            if (isLocked) {
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "إلغاء القفل",
                        tint = Color.White
                    )
                }
            }

            // CONTROLS OVERLAY
            ConstraintLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val (topBar, centerControls, bottomBar) = createRefs()

                // 1. TOP BAR (Slide from top)
                AnimatedVisibility(
                    visible = controlsVisible && !isLocked,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier.constrainAs(topBar) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.fillMaxSize().testTag("player_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        val displayTitle = if (isPlaylistMode && totalPlaylistCount > 0) {
                            "$title • ${currentPlaylistIndex + 1}/$totalPlaylistCount"
                        } else {
                            title
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .then(
                                        if (isPlaylistMode) {
                                            Modifier.clickable { onOpenSheet(Sheets.Playlist) }
                                        } else Modifier
                                    )
                                    .testTag("player_title_text")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            topRightControlIds.forEach { id ->
                                RenderControlToolItem(
                                    id = id,
                                    currentDecoder = currentDecoder,
                                    playbackSpeed = playbackSpeed,
                                    chapters = chapters,
                                    currentChapterIndex = currentChapterIndex,
                                    selectedSubId = selectedSubId,
                                    selectedSecondarySubId = selectedSecondarySubId,
                                    remainingSleepTimerSeconds = remainingSleepTimerSeconds,
                                    repeatMode = repeatMode,
                                    isShuffle = isShuffle,
                                    currentAspectRatio = currentAspectRatio,
                                    onOpenSheet = onOpenSheet,
                                    onSpeedChange = onSpeedChange,
                                    onToggleRotate = onToggleRotate,
                                    onToggleLock = onToggleLock,
                                    onEnterPiP = onEnterPiP,
                                    onToggleRepeat = onToggleRepeat,
                                    onToggleShuffle = onToggleShuffle,
                                    onFrameStep = onFrameStep,
                                    onFlipVideo = onFlipVideo,
                                    onToggleAbRepeat = onToggleAbRepeat,
                                    onCustomSkip = onCustomSkip,
                                    onToggleCinema = onToggleCinema,
                                    onToggleBackgroundPlay = onToggleBackgroundPlay,
                                    onCycleAspectRatio = onCycleAspectRatio,
                                    onTakeScreenshot = onTakeScreenshot,
                                    isLocked = isLocked
                                )
                            }
                        }
                    }
                }

                // 2. CENTER CONTROLS (Fade in/out)
                AnimatedVisibility(
                    visible = controlsVisible && !isLocked,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier.constrainAs(centerControls) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Previous Video
                        if (enablePrevNext) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    IconButton(
                                        onClick = onPreviousClick,
                                        modifier = Modifier.fillMaxSize().testTag("player_prev_video_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = "الفيديو السابق",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Rewind 10s
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                IconButton(
                                    onClick = { onSeekBy(-10) },
                                    modifier = Modifier.fillMaxSize().testTag("player_rewind_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay10,
                                        contentDescription = "تأخير 10 ثواني",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 3. Play/Pause
                        WaterRipplePlayPauseButton(
                            isPaused = isPaused,
                            onPlayPause = onPlayPause
                        )

                        // 4. Fast Forward 10s
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                IconButton(
                                    onClick = { onSeekBy(10) },
                                    modifier = Modifier.fillMaxSize().testTag("player_fast_forward_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Forward10,
                                        contentDescription = "تقديم 10 ثواني",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        // 5. Next Video
                        if (enablePrevNext) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    IconButton(
                                        onClick = onNextClick,
                                        modifier = Modifier.fillMaxSize().testTag("player_next_video_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "الفيديو التالي",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. BOTTOM BAR (Slide from bottom)
                AnimatedVisibility(
                    visible = controlsVisible && !isLocked,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.constrainAs(bottomBar) {
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        width = Dimension.fillToConstraints
                    }
                ) {
                    Column {
                        val currentPos = if (isDraggingSlider) dragPositionSeconds else positionSeconds
                        val safeDuration = if (durationSeconds > 0f) durationSeconds else 1f

                        // 1. Seekbar row with time on both sides (left & right)
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatTime(currentPos),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                    ),
                                    maxLines = 1,
                                    softWrap = false
                                )

                                FinalPlayerSeekbar(
                                    position = currentPos,
                                    duration = safeDuration,
                                    onValueChange = { newValue ->
                                        if (!isDraggingSlider) {
                                            onSliderDragStart()
                                        }
                                        isDraggingSlider = true
                                        dragPositionSeconds = newValue
                                    },
                                    onValueChangeFinished = {
                                        onSeekTo(dragPositionSeconds)
                                        isDraggingSlider = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("player_seek_slider")
                                )

                                Text(
                                    text = if (showRemainingTimeText) {
                                        "-${formatTime((safeDuration - currentPos).coerceAtLeast(0f))}"
                                    } else {
                                        formatTime(safeDuration)
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textDirection = androidx.compose.ui.text.style.TextDirection.Ltr
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.clickable {
                                        showRemainingTimeText = !showRemainingTimeText
                                    }
                                )
                            }
                        }

                        // 2. Control toolbar row below seekbar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (bottomLeftControlIds.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        bottomLeftControlIds.forEach { id ->
                                            RenderControlToolItem(
                                                id = id,
                                                currentDecoder = currentDecoder,
                                                playbackSpeed = playbackSpeed,
                                                chapters = chapters,
                                                currentChapterIndex = currentChapterIndex,
                                                selectedSubId = selectedSubId,
                                                selectedSecondarySubId = selectedSecondarySubId,
                                                remainingSleepTimerSeconds = remainingSleepTimerSeconds,
                                                repeatMode = repeatMode,
                                                isShuffle = isShuffle,
                                                currentAspectRatio = currentAspectRatio,
                                                onOpenSheet = onOpenSheet,
                                                onSpeedChange = onSpeedChange,
                                                onToggleRotate = onToggleRotate,
                                                onToggleLock = onToggleLock,
                                                onEnterPiP = onEnterPiP,
                                                onToggleRepeat = onToggleRepeat,
                                                onToggleShuffle = onToggleShuffle,
                                                onFrameStep = onFrameStep,
                                                onFlipVideo = onFlipVideo,
                                                onToggleAbRepeat = onToggleAbRepeat,
                                                onCustomSkip = onCustomSkip,
                                                onToggleCinema = onToggleCinema,
                                                onToggleBackgroundPlay = onToggleBackgroundPlay,
                                                onCycleAspectRatio = onCycleAspectRatio,
                                                onTakeScreenshot = onTakeScreenshot,
                                                isLocked = isLocked
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (bottomRightControlIds.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        bottomRightControlIds.forEach { id ->
                                            RenderControlToolItem(
                                                id = id,
                                                currentDecoder = currentDecoder,
                                                playbackSpeed = playbackSpeed,
                                                chapters = chapters,
                                                currentChapterIndex = currentChapterIndex,
                                                selectedSubId = selectedSubId,
                                                selectedSecondarySubId = selectedSecondarySubId,
                                                remainingSleepTimerSeconds = remainingSleepTimerSeconds,
                                                repeatMode = repeatMode,
                                                isShuffle = isShuffle,
                                                currentAspectRatio = currentAspectRatio,
                                                onOpenSheet = onOpenSheet,
                                                onSpeedChange = onSpeedChange,
                                                onToggleRotate = onToggleRotate,
                                                onToggleLock = onToggleLock,
                                                onEnterPiP = onEnterPiP,
                                                onToggleRepeat = onToggleRepeat,
                                                onToggleShuffle = onToggleShuffle,
                                                onFrameStep = onFrameStep,
                                                onFlipVideo = onFlipVideo,
                                                onToggleAbRepeat = onToggleAbRepeat,
                                                onCustomSkip = onCustomSkip,
                                                onToggleCinema = onToggleCinema,
                                                onToggleBackgroundPlay = onToggleBackgroundPlay,
                                                onCycleAspectRatio = onCycleAspectRatio,
                                                onTakeScreenshot = onTakeScreenshot,
                                                isLocked = isLocked
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                        val isPortrait = configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
                        if (isPortrait && portraitBottomControlIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                portraitBottomControlIds.forEach { id ->
                                    RenderControlToolItem(
                                        id = id,
                                        currentDecoder = currentDecoder,
                                        playbackSpeed = playbackSpeed,
                                        chapters = chapters,
                                        currentChapterIndex = currentChapterIndex,
                                        selectedSubId = selectedSubId,
                                        selectedSecondarySubId = selectedSecondarySubId,
                                        remainingSleepTimerSeconds = remainingSleepTimerSeconds,
                                        repeatMode = repeatMode,
                                        isShuffle = isShuffle,
                                        currentAspectRatio = currentAspectRatio,
                                        onOpenSheet = onOpenSheet,
                                        onSpeedChange = onSpeedChange,
                                        onToggleRotate = onToggleRotate,
                                        onToggleLock = onToggleLock,
                                        onEnterPiP = onEnterPiP,
                                        onToggleRepeat = onToggleRepeat,
                                        onToggleShuffle = onToggleShuffle,
                                        onFrameStep = onFrameStep,
                                        onFlipVideo = onFlipVideo,
                                        onToggleAbRepeat = onToggleAbRepeat,
                                        onCustomSkip = onCustomSkip,
                                        onToggleCinema = onToggleCinema,
                                        onToggleBackgroundPlay = onToggleBackgroundPlay,
                                        onCycleAspectRatio = onCycleAspectRatio,
                                        onTakeScreenshot = onTakeScreenshot,
                                        isLocked = isLocked
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // GESTURE OVERLAYS (BRIGHTNESS, VOLUME, DRAG SEEK, DOUBLE TAP)
            BrightnessSlider(
                brightnessValue = brightnessValue,
                isVisible = isBrightnessSliderShown,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            VolumeSlider(
                volumePercent = volumePercent,
                isVisible = isVolumeSliderShown,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            SeekOverlay(
                seekState = dragSeekState,
                modifier = Modifier.align(Alignment.Center)
            )

            DoubleTapSeekOvals(
                doubleTapState = doubleTapSeekState,
                modifier = Modifier.fillMaxSize()
            )

            SpeedOverlay(
                isVisible = isLongPressSpeedActive,
                speed = longPressSpeedValue,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            ZoomOverlay(
                zoomText = zoomOverlayText,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
            )

            SubPositionOverlay(
                subPosText = subPosOverlayText,
                isDragging = isSubtitleBoxDragging,
                subPosPercent = currentSubPos,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            onAddAudio(uri)
        }
    }

    when (sheetShown) {
        is Sheets.SubtitleTracks -> {
            SubtitlesSheet(
                tracks = subtitleTracks,
                selectedSubId = selectedSubId,
                selectedSecondarySubId = selectedSecondarySubId,
                onSelectSubtitle = { trackId ->
                    onSelectSubtitle(trackId)
                    onCloseSheet()
                },
                onDisableSubtitles = {
                    onDisableSubtitles()
                    onCloseSheet()
                },
                onAddExternalSubtitle = { uri ->
                    onAddExternalSubtitle(uri)
                    onCloseSheet()
                },
                onRemoveSubtitle = onRemoveSubtitle,
                onOpenSettings = { onOpenSheet(Sheets.SubtitleSettings) },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.SubtitleSettings -> {
            SubtitleSettingsPanel(
                onDismiss = onCloseSheet
            )
        }
        is Sheets.AudioTracks -> {
            AudioTracksSheet(
                tracks = audioTracks,
                currentAudioId = selectedAudioId ?: 0,
                onSelectAudio = { id ->
                    onSelectAudioTrack(id)
                    onCloseSheet()
                },
                onAddAudioFile = {
                    audioPicker.launch(arrayOf("audio/*", "video/*", "*/*"))
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.Decoders -> {
            DecoderSheet(
                currentDecoder = currentDecoder,
                onSelect = { dec ->
                    onSelectDecoder(dec)
                    onCloseSheet()
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.PlaybackSpeed -> {
            PlaybackSpeedSheet(
                currentSpeed = playbackSpeed,
                onSpeedChange = { speed ->
                    onSpeedChange(speed)
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.Chapters -> {
            ChaptersSheet(
                chapters = chapters,
                currentChapterIndex = currentChapterIndex,
                onSeekToChapter = { idx ->
                    onSelectChapter(idx)
                    onCloseSheet()
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.More -> {
            MoreSheet(
                sleepTimerRemaining = remainingSleepTimerSeconds,
                currentSpeed = playbackSpeed,
                currentDecoder = currentDecoder,
                currentAspectRatio = currentAspectRatio,
                currentZoom = currentVideoZoom,
                repeatMode = repeatMode,
                isShuffle = isShuffle,
                isCinemaMode = isCinemaMode,
                isBackgroundPlay = isBackgroundPlay,
                onOpenSheet = onOpenSheet,
                onDismiss = onCloseSheet,
                onToggleRotate = onToggleRotate,
                onToggleLock = onToggleLock,
                onEnterPiP = onEnterPiP,
                onToggleRepeat = onToggleRepeat,
                onToggleShuffle = onToggleShuffle,
                onFrameStep = onFrameStep,
                onFlipVideo = onFlipVideo,
                onToggleAbRepeat = onToggleAbRepeat,
                onCustomSkip = onCustomSkip,
                onToggleCinema = onToggleCinema,
                onToggleBackgroundPlay = onToggleBackgroundPlay
            )
        }
        is Sheets.AspectRatios -> {
            AspectRatiosSheet(
                currentRatio = currentAspectRatio,
                onSelectRatio = { ratio ->
                    onSetAspectRatio(ratio)
                    onCloseSheet()
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.VideoZoom -> {
            VideoZoomSheet(
                currentZoom = currentVideoZoom,
                onSelectZoom = { zoom ->
                    onSetVideoZoom(zoom)
                    onCloseSheet()
                },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.FrameNav -> {
            FrameNavSheet(
                onStepFrame = { forward -> onFrameStep(forward) },
                onDismiss = onCloseSheet
            )
        }
        is Sheets.Playlist -> {
            PlaylistSheet(
                playlistItems = playlistItems,
                currentVideoIndex = currentPlaylistIndex,
                onDismiss = onCloseSheet,
                onVideoSelect = { idx ->
                    onSelectPlaylistItem(idx)
                    onCloseSheet()
                },
                onReorder = onReorderPlaylist
            )
        }
        is Sheets.SleepTimer -> {
            SleepTimerBottomSheet(
                currentRemainingSeconds = remainingSleepTimerSeconds,
                onDismiss = onCloseSheet,
                onStartTimer = { minutes ->
                    onStartSleepTimer(minutes * 60)
                    onCloseSheet()
                },
                onCancelTimer = {
                    onCancelSleepTimer()
                    onCloseSheet()
                }
            )
        }
        else -> {
            if (showSleepTimerSheet) {
                SleepTimerBottomSheet(
                    currentRemainingSeconds = remainingSleepTimerSeconds,
                    onDismiss = { showSleepTimerSheet = false },
                    onStartTimer = { minutes ->
                        onStartSleepTimer(minutes * 60)
                        showSleepTimerSheet = false
                    },
                    onCancelTimer = {
                        onCancelSleepTimer()
                        showSleepTimerSheet = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    currentRemainingSeconds: Int,
    onDismiss: () -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(if (currentRemainingSeconds > 0) currentRemainingSeconds / 60 else 30) }

    SidePanel(onDismissRequest = onDismiss, scrollable = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مؤقت النوم",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "$selectedMinutes دقيقة",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (selectedMinutes > 5) selectedMinutes -= 5 },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "تقليل")
                }

                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.toInt() },
                    valueRange = 5f..180f,
                    steps = 34,
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp),
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(3.dp),
                            thumbTrackGapSize = 0.dp,
                            trackInsideCornerSize = 2.dp
                        )
                    },
                    thumb = {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                        )
                    }
                )

                IconButton(
                    onClick = { if (selectedMinutes < 180) selectedMinutes += 5 },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "زيادة")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick preset chips (horizontal scrollable row)
            val presets = listOf(15, 30, 45, 60, 90, 120)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { min ->
                    FilterChip(
                        selected = selectedMinutes == min,
                        onClick = { selectedMinutes = min },
                        label = {
                            Text(
                                text = "${min}د",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelTimer,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("إلغاء المؤقت")
                }

                Button(
                    onClick = { onStartTimer(selectedMinutes) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("بدء المؤقت")
                }
            }
        }
    }
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60

    return if (hrs > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RenderControlToolItem(
    id: String,
    currentDecoder: Decoder,
    playbackSpeed: Float,
    chapters: List<ChapterNode>,
    currentChapterIndex: Int?,
    selectedSubId: Int?,
    selectedSecondarySubId: Int?,
    remainingSleepTimerSeconds: Int,
    repeatMode: Int = 0,
    isShuffle: Boolean = false,
    currentAspectRatio: String = "default",
    onOpenSheet: (Sheets) -> Unit,
    onSpeedChange: (Float) -> Unit = {},
    onToggleRotate: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onFrameStep: (Boolean) -> Unit = {},
    onFlipVideo: (Boolean) -> Unit = {},
    onToggleAbRepeat: () -> Unit = {},
    onCustomSkip: () -> Unit = {},
    onToggleCinema: () -> Unit = {},
    onToggleBackgroundPlay: () -> Unit = {},
    onCycleAspectRatio: () -> Unit = {},
    onTakeScreenshot: () -> Unit = {},
    isLocked: Boolean = false
) {
    when (id) {
        "decoder" -> {
            val decoderBadgeColor = when (currentDecoder) {
                Decoder.HW_PLUS -> Color(0xFF4CAF50)
                Decoder.HW_COPY -> Color(0xFF2196F3)
                Decoder.SOFTWARE -> Color(0xFFFF9800)
            }
            Surface(
                color = decoderBadgeColor,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenSheet(Sheets.Decoders) }
                    .testTag("decoder_top_badge")
            ) {
                Text(
                    text = currentDecoder.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        "speed" -> {
            Surface(
                color = if (kotlin.math.abs(playbackSpeed - 1.0f) > 0.01f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .combinedClickable(
                        onClick = {
                            val speedCycle = listOf(1.00f, 1.25f, 1.50f, 1.75f, 2.00f, 0.25f, 0.50f, 0.75f)
                            val currIndex = speedCycle.indexOfFirst { kotlin.math.abs(it - playbackSpeed) < 0.05f }
                            val nextIndex = if (currIndex != -1) (currIndex + 1) % speedCycle.size else 1
                            onSpeedChange(speedCycle[nextIndex])
                        },
                        onLongClick = {
                            onOpenSheet(Sheets.PlaybackSpeed)
                        }
                    )
                    .testTag("speed_top_badge")
            ) {
                Text(
                    text = String.format(Locale.US, "%.2fx", playbackSpeed),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        "chapters", "current_chapter" -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenSheet(Sheets.Chapters) }
                    .testTag("chapter_top_badge")
            ) {
                val currentChapterText = if (chapters.isNotEmpty()) "فصل ${(currentChapterIndex ?: 0) + 1}/${chapters.size}" else "فصول"
                Text(
                    text = currentChapterText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        "subtitles" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onOpenSheet(Sheets.SubtitleTracks) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("subtitles_button")
                    ) {
                        val hasSubSelected = (selectedSubId != null && selectedSubId > 0) ||
                                (selectedSecondarySubId != null && selectedSecondarySubId > 0)
                        Icon(
                            imageVector = Icons.Outlined.Subtitles,
                            contentDescription = "الترجمة",
                            tint = if (hasSubSelected) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "audio_track" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onOpenSheet(Sheets.AudioTracks) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("audio_tracks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "الصوت",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "aspect_ratio" -> {
            val (aspectIcon, aspectDesc) = when (currentAspectRatio) {
                "default", "fit" -> Icons.Default.FitScreen to "الملاءمة الأفضل"
                "fill", "crop" -> Icons.Default.Crop to "قص / تعبئة"
                "stretch" -> Icons.Default.OpenInFull to "تمدد"
                "16:9" -> Icons.Default.Tv to "16:9"
                "4:3" -> Icons.Default.CropSquare to "4:3"
                "18:9", "21:9" -> Icons.Default.CropFree to currentAspectRatio
                "1:1" -> Icons.Default.CropSquare to "1:1"
                else -> Icons.Default.AspectRatio to currentAspectRatio
            }
            Surface(
                shape = CircleShape,
                color = if (currentAspectRatio != "default") MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = onCycleAspectRatio,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("aspect_ratio_button")
                    ) {
                        Icon(
                            imageVector = aspectIcon,
                            contentDescription = aspectDesc,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "zoom" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onOpenSheet(Sheets.VideoZoom) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("zoom_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "تكبير",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "sleep_timer" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onOpenSheet(Sheets.SleepTimer) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("sleep_timer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "مؤقت النوم",
                            tint = if (remainingSleepTimerSeconds > 0) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "more" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onOpenSheet(Sheets.More) },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("more_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "المزيد",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "screenshot" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onTakeScreenshot, modifier = Modifier.fillMaxSize().testTag("screenshot_button")) {
                        Icon(painter = painterResource(id = R.drawable.ic_player_screenshot), contentDescription = "لقطة شاشة", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "rotate" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleRotate, modifier = Modifier.fillMaxSize().testTag("rotate_button")) {
                        Icon(painter = painterResource(id = R.drawable.ic_player_rotate), contentDescription = "تدوير الشاشة", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "lock" -> {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleLock, modifier = Modifier.fillMaxSize().testTag("lock_button")) {
                        Icon(
                            painter = painterResource(id = if (isLocked) R.drawable.ic_player_lock else R.drawable.ic_player_unlock),
                            contentDescription = "قفل عناصر التحكم",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "pip" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onEnterPiP, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "صورة داخل صورة", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "repeat_mode" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleRepeat, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = tool?.icon ?: Icons.Default.MoreVert,
                            contentDescription = "وضع التكرار",
                            tint = if (repeatMode > 0) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "shuffle" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleShuffle, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = tool?.icon ?: Icons.Default.MoreVert,
                            contentDescription = "تشغيل عشوائي",
                            tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        "flip_v" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = { onFlipVideo(true) }, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "قلب رأسي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "flip_h" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = { onFlipVideo(false) }, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "قلب أفقي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "ab_repeat" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleAbRepeat, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "تكرار A-B", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "frame_nav" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = { onOpenSheet(Sheets.FrameNav) }, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "التنقل بين الإطارات", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "custom_skip" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onCustomSkip, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "تخطي مخصص", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "cinema" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleCinema, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "الوضع السينمائي", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        "background_play" -> {
            val tool = ControlTools.getById(id)
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    IconButton(onClick = onToggleBackgroundPlay, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = tool?.icon ?: Icons.Default.MoreVert, contentDescription = "التشغيل في الخلفية", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        else -> {
            val tool = ControlTools.getById(id)
            if (tool != null) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        IconButton(onClick = { onOpenSheet(Sheets.More) }, modifier = Modifier.fillMaxSize()) {
                            Icon(imageVector = tool.icon, contentDescription = tool.title, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

fun handlePlayerAction(
    action: PlayerButtonType,
    context: android.content.Context,
    onOpenSheet: (Sheets) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onToggleRotate: () -> Unit = {},
    onToggleLock: () -> Unit = {},
    onEnterPiP: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onFrameStep: (Boolean) -> Unit = {},
    onFlipVideo: (Boolean) -> Unit = {},
    onToggleAbRepeat: () -> Unit = {},
    onCustomSkip: () -> Unit = {},
    onToggleCinema: () -> Unit = {},
    onToggleBackgroundPlay: () -> Unit = {},
    onCycleAspectRatio: () -> Unit = {}
) {
    when (action) {
        PlayerButtonType.CHAPTERS, PlayerButtonType.CURRENT_CHAPTER -> onOpenSheet(Sheets.Chapters)
        PlayerButtonType.PLAYBACK_SPEED -> onOpenSheet(Sheets.PlaybackSpeed)
        PlayerButtonType.DECODER -> onOpenSheet(Sheets.Decoders)
        PlayerButtonType.SCREEN_ROTATION -> onToggleRotate()
        PlayerButtonType.FRAME_BY_FRAME -> onOpenSheet(Sheets.FrameNav)
        PlayerButtonType.VIDEO_ZOOM -> onOpenSheet(Sheets.VideoZoom)
        PlayerButtonType.PIP_MODE -> onEnterPiP()
        PlayerButtonType.ASPECT_RATIO -> onCycleAspectRatio()
        PlayerButtonType.LOCK_CONTROLS -> onToggleLock()
        PlayerButtonType.AUDIO_TRACK -> onOpenSheet(Sheets.AudioTracks)
        PlayerButtonType.SUBTITLES -> onOpenSheet(Sheets.SubtitleTracks)
        PlayerButtonType.MORE_OPTIONS -> onOpenSheet(Sheets.More)
        PlayerButtonType.LOOP_MODE -> onToggleRepeat()
        PlayerButtonType.SHUFFLE -> onToggleShuffle()
        PlayerButtonType.FLIP_HORIZONTAL -> onFlipVideo(false)
        PlayerButtonType.FLIP_VERTICAL -> onFlipVideo(true)
        PlayerButtonType.AB_REPEAT -> onToggleAbRepeat()
        PlayerButtonType.CUSTOM_SKIP -> onCustomSkip()
        PlayerButtonType.BACKGROUND_PLAY -> onToggleBackgroundPlay()
        PlayerButtonType.CINEMA_MODE -> onToggleCinema()
        PlayerButtonType.SLEEP_TIMER -> onOpenSheet(Sheets.SleepTimer)
    }
}

@Composable
fun AspectRatiosSheet(
    currentRatio: String,
    onSelectRatio: (String) -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss, scrollable = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("نسبة العرض إلى الارتفاع", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            val options = listOf(
                "default" to "ملائمة الشاشة (Fit)",
                "fill" to "تعبئة الشاشة (Fill)",
                "16:9" to "16:9",
                "4:3" to "4:3",
                "18:9" to "18:9",
                "21:9" to "21:9",
                "1:1" to "1:1"
            )
            options.forEach { (key, label) ->
                Surface(
                    color = if (currentRatio == key) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRatio(key) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoZoomSheet(
    currentZoom: Float,
    onSelectZoom: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss, scrollable = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("تكبير الفيديو", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            val options = listOf(1.0f to "100% (عادي)", 1.25f to "125%", 1.5f to "150%", 1.75f to "175%", 2.0f to "200%")
            options.forEach { (zoom, label) ->
                Surface(
                    color = if (kotlin.math.abs(currentZoom - zoom) < 0.05f) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectZoom(zoom) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
fun FrameNavSheet(
    onStepFrame: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss, scrollable = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("التنقل بين الإطارات", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(onClick = { onStepFrame(false) }) {
                    Text("إطار للخلف")
                }
                Button(onClick = { onStepFrame(true) }) {
                    Text("إطار للأمام")
                }
            }
        }
    }
}
