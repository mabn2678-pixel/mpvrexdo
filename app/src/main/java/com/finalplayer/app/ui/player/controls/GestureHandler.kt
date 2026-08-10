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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.finalplayer.app.data.preferences.GesturePreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
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
    onSubtitlePositionDrag: (Float) -> Unit = {},
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
    modifier: Modifier = Modifier,
    gesturePrefs: GesturePreferences = koinInject(),
    playerPrefs: PlayerPreferences = koinInject(),
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
    val enableSubtitleDrag by playerPrefs.enableSubtitleDrag.asFlow().collectAsState(initial = false)
    val enableSubtitleSeekGesture by playerPrefs.enableSubtitleSeekGesture.asFlow().collectAsState(initial = true)
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

    var lastTapInfo by remember { mutableStateOf<TapInfo?>(null) }
    var singleTapJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(
                isShortsMode,
                isLocked,
                isAnySheetOpen,
                brightnessEnabled,
                playerBrightnessEnabled,
                effectiveBrightnessEnabled,
                volumeEnabled,
                playerVolumeEnabled,
                effectiveVolumeEnabled,
                seekEnabled,
                playerHorizontalSeekEnabled,
                effectiveSeekEnabled,
                isPinchAllowed,
                swapVolBright,
                enableSubtitleDrag,
                enableSubtitleSeekGesture,
                sensitivity,
                swipeSpeed,
                seekSensitivity,
                preventAccidental,
                allowPanelGestures,
                controlsVisible
            ) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downX = down.position.x
                    val downY = down.position.y
                    val screenWidth = size.width.toFloat().coerceAtLeast(100f)
                    val screenHeight = size.height.toFloat().coerceAtLeast(100f)

                    val edgeMarginPx = 36.dp.toPx()
                    val topEdgeMarginPx = 56.dp.toPx()
                    val bottomEdgeMarginPx = 56.dp.toPx()

                    val isNearHorizontalEdge = downX < edgeMarginPx || downX > screenWidth - edgeMarginPx
                    val isNearTopEdge = downY < topEdgeMarginPx
                    val isNearBottomEdge = downY > screenHeight - bottomEdgeMarginPx

                    var activeGesture = ActiveGesture.NONE
                    var longPressTriggered = false
                    var horizontalSeekStarted = false

                    var totalDx = 0f
                    var totalDy = 0f
                    var lastX = downX
                    var lastY = downY

                    // Launch long press timer only if not locked and no sheet open
                    val longPressJob: Job? = if (!isLocked && !isAnySheetOpen) {
                        scope.launch {
                            delay(350)
                            if (activeGesture == ActiveGesture.NONE) {
                                activeGesture = ActiveGesture.LONG_PRESS
                                longPressTriggered = true
                                onLongPressStart()
                            }
                        }
                    } else null

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.filter { it.pressed }

                            if (pressedPointers.isEmpty()) {
                                break
                            }

                            // Multi-touch pinch zoom
                            if (pressedPointers.size >= 2) {
                                longPressJob?.cancel()
                                if (activeGesture != ActiveGesture.PINCH_ZOOM) {
                                    if (horizontalSeekStarted) {
                                        onHorizontalDragEnd()
                                        horizontalSeekStarted = false
                                    }
                                    if (longPressTriggered) {
                                        onLongPressEnd()
                                        longPressTriggered = false
                                    }
                                    activeGesture = if (isPinchAllowed) ActiveGesture.PINCH_ZOOM else ActiveGesture.COMPLETED
                                }

                                if (activeGesture == ActiveGesture.PINCH_ZOOM && isPinchAllowed) {
                                    val p1 = pressedPointers[0]
                                    val p2 = pressedPointers[1]
                                    val prevP1 = p1.previousPosition
                                    val prevP2 = p2.previousPosition
                                    val currentDist = (p1.position - p2.position).getDistance()
                                    val prevDist = (prevP1 - prevP2).getDistance()
                                    if (prevDist > 0f) {
                                        val zoomRatio = currentDist / prevDist
                                        if (abs(zoomRatio - 1.0f) > 0.001f) {
                                            onPinchZoom(zoomRatio)
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

                            val touchSlop = 18.dp.toPx()

                            if (activeGesture == ActiveGesture.NONE) {
                                val totalDist = hypot(totalDx, totalDy)
                                if (totalDist > touchSlop) {
                                    longPressJob?.cancel()

                                    if (isLocked || (isAnySheetOpen && !allowPanelGestures)) {
                                        activeGesture = ActiveGesture.COMPLETED
                                    } else {
                                        val isHorizontal = abs(totalDx) > abs(totalDy)

                                        if (isHorizontal) {
                                            if (effectiveSeekEnabled && !isNearHorizontalEdge) {
                                                activeGesture = ActiveGesture.HORIZONTAL_SEEK
                                                horizontalSeekStarted = true
                                                onHorizontalDragStart()
                                            } else {
                                                activeGesture = ActiveGesture.COMPLETED
                                            }
                                        } else {
                                            if (isShortsMode) {
                                                activeGesture = ActiveGesture.SHORTS_FLIP
                                            } else {
                                                val isLeftZone = downX < screenWidth * 0.40f
                                                val isRightZone = downX > screenWidth * 0.60f

                                                if (isLeftZone) {
                                                    activeGesture = if (swapVolBright) ActiveGesture.VOLUME else ActiveGesture.BRIGHTNESS
                                                } else if (isRightZone) {
                                                    activeGesture = if (swapVolBright) ActiveGesture.BRIGHTNESS else ActiveGesture.VOLUME
                                                } else {
                                                    // Center zone: normal vertical swipes adjust Volume or Brightness depending on screen side
                                                    activeGesture = if (downX < screenWidth * 0.5f) {
                                                        if (swapVolBright) ActiveGesture.VOLUME else ActiveGesture.BRIGHTNESS
                                                    } else {
                                                        if (swapVolBright) ActiveGesture.BRIGHTNESS else ActiveGesture.VOLUME
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
                                    if (enableSubtitleDrag && abs(dy) > abs(dx) * 1.2f) {
                                        onSubtitlePositionDrag(dy)
                                    } else {
                                        onLongPressDrag(dx)
                                    }
                                }
                                ActiveGesture.HORIZONTAL_SEEK -> {
                                    change.consume()
                                    val minThreshold = if (preventAccidental) 120f else if (controlsVisible) 60f else 30f
                                    if (abs(totalDx) > minThreshold) {
                                        val sensMultiplier = (seekSensitivity / 50f)
                                        val scaledDx = dx * swipeSpeed * sensMultiplier
                                        onHorizontalDrag(scaledDx, screenWidth)
                                    }
                                }
                                ActiveGesture.BRIGHTNESS -> {
                                    change.consume()
                                    if (effectiveBrightnessEnabled) {
                                        val deltaRatio = (-dy / screenHeight) * sensitivity
                                        onVerticalBrightnessDrag(deltaRatio)
                                    }
                                }
                                ActiveGesture.VOLUME -> {
                                    change.consume()
                                    if (effectiveVolumeEnabled) {
                                        val deltaRatio = (-dy / screenHeight) * sensitivity
                                        onVerticalVolumeDrag(deltaRatio)
                                    }
                                }
                                ActiveGesture.SUBTITLE_POSITION -> {
                                    change.consume()
                                    onSubtitlePositionDrag(dy)
                                }
                                ActiveGesture.SHORTS_FLIP -> {
                                    change.consume()
                                    val threshold = 80.dp.toPx()
                                    if (totalDy < -threshold) {
                                        onNextClick()
                                        activeGesture = ActiveGesture.COMPLETED
                                    } else if (totalDy > threshold) {
                                        onPreviousClick()
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
                        val touchSlop = 18.dp.toPx()

                        if (longPressTriggered) {
                            onLongPressEnd()
                        } else if (horizontalSeekStarted) {
                            onHorizontalDragEnd()
                        } else if (activeGesture == ActiveGesture.NONE && totalDist <= touchSlop && duration < 350) {
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

                                if (!isLocked) {
                                    when {
                                        tapX < screenWidth * 0.33f -> onLeftDoubleTap()
                                        tapX > screenWidth * 0.67f -> onRightDoubleTap()
                                        else -> onCenterDoubleTap()
                                    }
                                } else {
                                    onSingleTap()
                                }
                            } else {
                                lastTapInfo = TapInfo(tapX, downY, tapTime)
                                singleTapJob?.cancel()
                                singleTapJob = scope.launch {
                                    delay(280)
                                    onSingleTap()
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
