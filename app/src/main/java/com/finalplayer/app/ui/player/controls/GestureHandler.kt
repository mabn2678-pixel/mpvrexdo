package com.finalplayer.app.ui.player.controls

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.finalplayer.app.data.preferences.GesturePreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.abs
import kotlin.math.hypot

private enum class ActiveGesture {
    NONE,
    LONG_PRESS,
    BRIGHTNESS,
    VOLUME,
    SUBTITLE_POSITION,
    HORIZONTAL_SEEK,
    SHORTS_FLIP,
    PINCH_ZOOM,
    COMPLETED
}

private data class TapInfo(val x: Float, val y: Float, val time: Long)

@Composable
fun GestureHandler(
    onSingleTap: () -> Unit,
    onLeftDoubleTap: () -> Unit,
    onRightDoubleTap: () -> Unit,
    onCenterDoubleTap: () -> Unit,
    onVerticalBrightnessDrag: (Float) -> Unit,
    onVerticalVolumeDrag: (Float) -> Unit,
    onHorizontalDragStart: () -> Unit,
    onHorizontalDrag: (deltaPx: Float, screenWidthPx: Float) -> Unit,
    onHorizontalDragEnd: () -> Unit,
    onSubtitleDragStart: () -> Unit = {},
    onSubtitlePositionDrag: (deltaPx: Float, screenHeightPx: Float) -> Unit = { _, _ -> },
    onSubtitleDragEnd: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    hasActiveSubtitles: Boolean = false,
    onPinchZoom: (zoomDelta: Float) -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressDrag: (deltaPx: Float) -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    controlsVisible: Boolean = false,
    isLocked: Boolean = false,
    isAnySheetOpen: Boolean = false,
    isShortsMode: Boolean = false,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
    modifier: Modifier = Modifier,
    gesturePrefs: GesturePreferences = koinInject(),
    playerPrefs: PlayerPreferences = koinInject(),
    subtitlesPrefs: SubtitlesPreferences = koinInject(),
    content: @Composable () -> Unit = {}
) {
    val brightnessEnabled by gesturePrefs.brightnessGestureEnabled.asFlow().collectAsState(initial = true)
    val playerBrightnessEnabled by playerPrefs.enableBrightnessGesture.asFlow().collectAsState(initial = true)
    val volumeEnabled by gesturePrefs.volumeGestureEnabled.asFlow().collectAsState(initial = true)
    val playerVolumeEnabled by playerPrefs.enableVolumeGesture.asFlow().collectAsState(initial = true)
    val seekEnabled by gesturePrefs.seekGestureEnabled.asFlow().collectAsState(initial = true)
    val playerHorizontalSeekEnabled by playerPrefs.enableHorizontalSeek.asFlow().collectAsState(initial = true)
    val pinchEnabled by gesturePrefs.pinchToZoom.asFlow().collectAsState(initial = true)
    val playerPinchEnabled by playerPrefs.enablePinchToZoom.asFlow().collectAsState(initial = true)
    val enableSubtitleDrag by playerPrefs.enableSubtitleDrag.asFlow().collectAsState(initial = true)
    val gestSubDrag by gesturePrefs.subtitleDrag.asFlow().collectAsState(initial = true)
    val effectiveSubtitleDrag = enableSubtitleDrag || gestSubDrag
    val subPosPref by subtitlesPrefs.subPos.asFlow().collectAsState(initial = 100)
    val seekSensitivity by playerPrefs.seekSensitivity.asFlow().collectAsState(initial = 50)
    val allowPanelGestures by playerPrefs.allowPanelGestures.asFlow().collectAsState(initial = false)
    val sensitivity by gesturePrefs.gestureSensitivity.asFlow().collectAsState(initial = 1.0f)
    val swipeSpeed by gesturePrefs.swipeSeekSpeed.asFlow().collectAsState(initial = 1.0f)
    val preventAccidental by gesturePrefs.preventAccidentalSeek.asFlow().collectAsState(initial = false)
    val swapVolBright by playerPrefs.swapVolumeAndBrightness.asFlow().collectAsState(initial = false)

    val effectiveBrightnessEnabled = brightnessEnabled && playerBrightnessEnabled
    val effectiveVolumeEnabled = volumeEnabled && playerVolumeEnabled
    val effectiveSeekEnabled = seekEnabled && playerHorizontalSeekEnabled
    val isPinchAllowed = pinchEnabled && playerPinchEnabled && !isLocked && (!isAnySheetOpen || allowPanelGestures)
    val scope = rememberCoroutineScope()

    // Wrap callbacks in rememberUpdatedState so pointerInput never restarts mid-gesture
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnLeftDoubleTap by rememberUpdatedState(onLeftDoubleTap)
    val currentOnRightDoubleTap by rememberUpdatedState(onRightDoubleTap)
    val currentOnCenterDoubleTap by rememberUpdatedState(onCenterDoubleTap)
    val currentOnVerticalBrightnessDrag by rememberUpdatedState(onVerticalBrightnessDrag)
    val currentOnVerticalVolumeDrag by rememberUpdatedState(onVerticalVolumeDrag)
    val currentOnHorizontalDragStart by rememberUpdatedState(onHorizontalDragStart)
    val currentOnHorizontalDrag by rememberUpdatedState(onHorizontalDrag)
    val currentOnHorizontalDragEnd by rememberUpdatedState(onHorizontalDragEnd)
    val currentOnSubtitleDragStart by rememberUpdatedState(onSubtitleDragStart)
    val currentOnSubtitlePositionDrag by rememberUpdatedState(onSubtitlePositionDrag)
    val currentOnSubtitleDragEnd by rememberUpdatedState(onSubtitleDragEnd)
    val currentOnSubtitleClick by rememberUpdatedState(onSubtitleClick)
    val currentOnPinchZoom by rememberUpdatedState(onPinchZoom)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressDrag by rememberUpdatedState(onLongPressDrag)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnNextClick by rememberUpdatedState(onNextClick)
    val currentOnPreviousClick by rememberUpdatedState(onPreviousClick)
    val currentOnUserInteraction by rememberUpdatedState(onUserInteraction)

    // Wrap dynamic state in rememberUpdatedState
    val currentControlsVisible by rememberUpdatedState(controlsVisible)
    val currentIsLocked by rememberUpdatedState(isLocked)
    val currentIsAnySheetOpen by rememberUpdatedState(isAnySheetOpen)
    val currentIsShortsMode by rememberUpdatedState(isShortsMode)
    val currentHasActiveSubtitles by rememberUpdatedState(hasActiveSubtitles)
    val currentEffectiveSubtitleDrag by rememberUpdatedState(effectiveSubtitleDrag)
    val currentSubPosPref by rememberUpdatedState(subPosPref)
    val currentEffectiveBrightnessEnabled by rememberUpdatedState(effectiveBrightnessEnabled)
    val currentEffectiveVolumeEnabled by rememberUpdatedState(effectiveVolumeEnabled)
    val currentEffectiveSeekEnabled by rememberUpdatedState(effectiveSeekEnabled)
    val currentIsPinchAllowed by rememberUpdatedState(isPinchAllowed)
    val currentSwapVolBright by rememberUpdatedState(swapVolBright)
    val currentSensitivity by rememberUpdatedState(sensitivity)
    val currentSwipeSpeed by rememberUpdatedState(swipeSpeed)
    val currentSeekSensitivity by rememberUpdatedState(seekSensitivity)
    val currentPreventAccidental by rememberUpdatedState(preventAccidental)
    val currentAllowPanelGestures by rememberUpdatedState(allowPanelGestures)

    var lastTapInfo by remember { mutableStateOf<TapInfo?>(null) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }

    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    currentOnUserInteraction()
                    val downTime = System.currentTimeMillis()
                    val downX = down.position.x
                    val downY = down.position.y
                    val screenWidth = size.width.toFloat().coerceAtLeast(100f)
                    val screenHeight = size.height.toFloat().coerceAtLeast(100f)

                    val edgeMarginPx = 36.dp.toPx()
                    val isNearHorizontalEdge = downX < edgeMarginPx || downX > screenWidth - edgeMarginPx

                    // Subtitle touch box boundary based on subPos (centered horizontally with side safety margins)
                    val subCenterY = (currentSubPosPref / 100f) * screenHeight
                    val subMinY = (subCenterY - 0.14f * screenHeight).coerceAtLeast(0f)
                    val subMaxY = (subCenterY + 0.10f * screenHeight).coerceAtMost(screenHeight)
                    val isTouchOnSubtitleBox = currentHasActiveSubtitles && currentEffectiveSubtitleDrag &&
                            (downY in subMinY..subMaxY) &&
                            (downX in (screenWidth * 0.18f)..(screenWidth * 0.82f))

                    var activeGesture = ActiveGesture.NONE
                    var longPressTriggered = false
                    var horizontalSeekStarted = false
                    var isConsumedByChild = down.isConsumed

                    var totalDx = 0f
                    var totalDy = 0f
                    var lastX = downX
                    var lastY = downY

                    // Long-press timer: ONLY a long press on the subtitle box enables subtitle dragging;
                    // otherwise long press activates 2.5x speed.
                    val longPressJob: Job? = if (!currentIsLocked && (!currentIsAnySheetOpen || currentAllowPanelGestures)) {
                        scope.launch {
                            delay(350)
                            if (activeGesture == ActiveGesture.NONE) {
                                if (isTouchOnSubtitleBox) {
                                    activeGesture = ActiveGesture.SUBTITLE_POSITION
                                    longPressTriggered = true
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Throwable) {}
                                    currentOnSubtitleDragStart()
                                    currentOnSubtitlePositionDrag(0f, screenHeight)
                                } else {
                                    activeGesture = ActiveGesture.LONG_PRESS
                                    longPressTriggered = true
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    } catch (_: Throwable) {}
                                    currentOnLongPressStart()
                                }
                            }
                        }
                    } else null

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) {
                                isConsumedByChild = true
                            }
                            val pressedPointers = event.changes.filter { it.pressed }

                            if (pressedPointers.isEmpty()) {
                                break
                            }

                            // Multi-touch pinch zoom
                            if (pressedPointers.size >= 2) {
                                longPressJob?.cancel()
                                if (activeGesture != ActiveGesture.PINCH_ZOOM) {
                                    if (horizontalSeekStarted) {
                                        currentOnHorizontalDragEnd()
                                        horizontalSeekStarted = false
                                    }
                                    if (longPressTriggered) {
                                        if (activeGesture == ActiveGesture.SUBTITLE_POSITION) {
                                            currentOnSubtitleDragEnd()
                                        } else {
                                            currentOnLongPressEnd()
                                        }
                                        longPressTriggered = false
                                    }
                                    activeGesture = if (currentIsPinchAllowed) ActiveGesture.PINCH_ZOOM else ActiveGesture.COMPLETED
                                }

                                if (activeGesture == ActiveGesture.PINCH_ZOOM && currentIsPinchAllowed) {
                                    val p1 = pressedPointers[0]
                                    val p2 = pressedPointers[1]
                                    val prevP1 = p1.previousPosition
                                    val prevP2 = p2.previousPosition
                                    val currentDist = (p1.position - p2.position).getDistance()
                                    val prevDist = (prevP1 - prevP2).getDistance()
                                    if (prevDist > 0f) {
                                        val zoomRatio = currentDist / prevDist
                                        if (abs(zoomRatio - 1.0f) > 0.001f) {
                                            currentOnPinchZoom(zoomRatio)
                                        }
                                    }
                                }
                                continue
                            }

                            val change = pressedPointers.first()
                            val currentX = change.position.x
                            val currentY = change.position.y
                            val dx = currentX - lastX
                            val dy = currentY - lastY
                            totalDx += dx
                            totalDy += dy
                            lastX = currentX
                            lastY = currentY

                            val touchSlop = 14.dp.toPx()

                            if (activeGesture == ActiveGesture.NONE) {
                                val totalDist = hypot(totalDx, totalDy)
                                if (totalDist > touchSlop) {
                                    longPressJob?.cancel()

                                    if (currentIsLocked || (currentIsAnySheetOpen && !currentAllowPanelGestures) || isConsumedByChild) {
                                        activeGesture = ActiveGesture.COMPLETED
                                    } else {
                                        val isHorizontal = abs(totalDx) > abs(totalDy)

                                        if (isHorizontal) {
                                            if (currentEffectiveSeekEnabled && !isNearHorizontalEdge) {
                                                activeGesture = ActiveGesture.HORIZONTAL_SEEK
                                                horizontalSeekStarted = true
                                                currentOnHorizontalDragStart()
                                            } else {
                                                activeGesture = ActiveGesture.COMPLETED
                                            }
                                        } else {
                                            if (currentIsShortsMode) {
                                                activeGesture = ActiveGesture.SHORTS_FLIP
                                            } else {
                                                val isLeftZone = downX < screenWidth * 0.45f
                                                val isRightZone = downX > screenWidth * 0.55f

                                                if (isLeftZone) {
                                                    activeGesture = if (currentSwapVolBright) ActiveGesture.VOLUME else ActiveGesture.BRIGHTNESS
                                                } else if (isRightZone) {
                                                    activeGesture = if (currentSwapVolBright) ActiveGesture.BRIGHTNESS else ActiveGesture.VOLUME
                                                } else {
                                                    activeGesture = if (downX < screenWidth * 0.5f) {
                                                        if (currentSwapVolBright) ActiveGesture.VOLUME else ActiveGesture.BRIGHTNESS
                                                    } else {
                                                        if (currentSwapVolBright) ActiveGesture.BRIGHTNESS else ActiveGesture.VOLUME
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            when (activeGesture) {
                                ActiveGesture.LONG_PRESS -> {
                                    change.consume()
                                    currentOnLongPressDrag(dx)
                                }
                                ActiveGesture.SUBTITLE_POSITION -> {
                                    change.consume()
                                    currentOnSubtitlePositionDrag(dy, screenHeight)
                                }
                                ActiveGesture.HORIZONTAL_SEEK -> {
                                    change.consume()
                                    val minThreshold = if (currentPreventAccidental) 120f else if (currentControlsVisible) 60f else 30f
                                    if (abs(totalDx) > minThreshold) {
                                        val sensMultiplier = (currentSeekSensitivity / 50f)
                                        val scaledDx = dx * currentSwipeSpeed * sensMultiplier
                                        currentOnHorizontalDrag(scaledDx, screenWidth)
                                    }
                                }
                                ActiveGesture.BRIGHTNESS -> {
                                    change.consume()
                                    if (currentEffectiveBrightnessEnabled) {
                                        val deltaRatio = (-dy / screenHeight) * currentSensitivity
                                        currentOnVerticalBrightnessDrag(deltaRatio)
                                    }
                                }
                                ActiveGesture.VOLUME -> {
                                    change.consume()
                                    if (currentEffectiveVolumeEnabled) {
                                        val deltaRatio = (-dy / screenHeight) * currentSensitivity
                                        currentOnVerticalVolumeDrag(deltaRatio)
                                    }
                                }
                                ActiveGesture.SHORTS_FLIP -> {
                                    change.consume()
                                    val threshold = 80.dp.toPx()
                                    if (totalDy < -threshold) {
                                        currentOnNextClick()
                                        activeGesture = ActiveGesture.COMPLETED
                                    } else if (totalDy > threshold) {
                                        currentOnPreviousClick()
                                        activeGesture = ActiveGesture.COMPLETED
                                    }
                                }
                                else -> {}
                            }
                        }
                    } finally {
                        longPressJob?.cancel()

                        val upTime = System.currentTimeMillis()
                        val duration = upTime - downTime
                        val totalDist = hypot(totalDx, totalDy)
                        val touchSlop = 14.dp.toPx()

                        if (activeGesture == ActiveGesture.SUBTITLE_POSITION) {
                            currentOnSubtitleDragEnd()
                        } else if (longPressTriggered && activeGesture == ActiveGesture.LONG_PRESS) {
                            currentOnLongPressEnd()
                        } else if (horizontalSeekStarted) {
                            currentOnHorizontalDragEnd()
                        } else if (!isConsumedByChild && !currentIsAnySheetOpen && activeGesture == ActiveGesture.NONE && totalDist <= touchSlop && duration < 350) {
                            val tapX = downX
                            val tapTime = upTime

                            val prevTap = lastTapInfo
                            val isDoubleTap = prevTap != null &&
                                    (tapTime - prevTap.time) < 300 &&
                                    hypot(tapX - prevTap.x, downY - prevTap.y) < 60.dp.toPx()

                            if (isDoubleTap) {
                                singleTapJob?.cancel()
                                singleTapJob = null
                                lastTapInfo = null

                                if (!currentIsLocked) {
                                    when {
                                        tapX < screenWidth * 0.33f -> currentOnLeftDoubleTap()
                                        tapX > screenWidth * 0.67f -> currentOnRightDoubleTap()
                                        else -> currentOnCenterDoubleTap()
                                    }
                                } else {
                                    currentOnSingleTap()
                                }
                            } else {
                                lastTapInfo = TapInfo(tapX, downY, tapTime)
                                singleTapJob?.cancel()
                                singleTapJob = scope.launch {
                                    delay(280)
                                    if (isTouchOnSubtitleBox && !currentIsLocked) {
                                        currentOnSubtitleClick()
                                    } else {
                                        currentOnSingleTap()
                                    }
                                    lastTapInfo = null
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
