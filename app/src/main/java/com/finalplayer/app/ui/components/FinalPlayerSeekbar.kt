package com.finalplayer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.finalplayer.app.data.preferences.AppearancePreferences
import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import org.koin.compose.koinInject

@Composable
fun FinalPlayerSeekbar(
    position: Float,
    duration: Float,
    buffered: Float = 0f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    appearancePrefs: AppearancePreferences = koinInject(),
    layoutPrefs: PlayerLayoutPreferences = koinInject()
) {
    val layoutSeekbarStyle by layoutPrefs.seekbarStyle.asFlow().collectAsState(initial = "standard")
    val whiteProgressbar by layoutPrefs.whiteProgressbar.asFlow().collectAsState(initial = false)
    val appSeekbarStyle by appearancePrefs.seekbarStyle.asFlow().collectAsState(initial = "thin")
    val isGlass by appearancePrefs.glassmorphismSeekbar.asFlow().collectAsState(initial = false)

    val effectiveStyle = if (layoutSeekbarStyle != "standard") layoutSeekbarStyle else appSeekbarStyle

    val barHeight = when (effectiveStyle) {
        "thin", "simple" -> 3.dp
        "thick" -> 8.dp
        "wavy" -> 5.dp
        "circular" -> 4.dp
        else -> 4.dp
    }

    val progressColor = if (whiteProgressbar) Color.White else MaterialTheme.colorScheme.primary

    var isDragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(position) }
    val displayValue = if (isDragging) dragValue else position

    val trackBgColor = if (isGlass) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.35f)

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val startAlignment = if (isRtl) Alignment.CenterEnd else Alignment.CenterStart
    val thumbAlignment = if (isRtl) Alignment.Start else Alignment.End

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(vertical = 4.dp)
    ) {
        // خلفية الشريط
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(barHeight / 2))
                .background(trackBgColor)
        )

        // مخزون (Buffered)
        if (buffered > 0f && duration > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((buffered / duration).coerceIn(0f, 1f))
                    .height(barHeight)
                    .align(startAlignment)
                    .clip(RoundedCornerShape(barHeight / 2))
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        // التقدم (Progress)
        if (duration > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((displayValue / duration).coerceIn(0f, 1f))
                    .height(barHeight)
                    .align(startAlignment)
                    .clip(RoundedCornerShape(barHeight / 2))
                    .background(progressColor)
            )
        }

        // Thumb دائري
        if (duration > 0f) {
            val fraction = (displayValue / duration).coerceIn(0f, 1f)
            val thumbSize = when (effectiveStyle) {
                "thick" -> 16.dp
                "circular" -> 14.dp
                "simple" -> 8.dp
                else -> 12.dp
            }
            Box(
                modifier = Modifier
                    .align(startAlignment)
                    .fillMaxWidth(fraction)
                    .wrapContentWidth(thumbAlignment)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isDragging) thumbSize + 4.dp else thumbSize)
                        .background(
                            if (whiteProgressbar) Color.White else progressColor,
                            CircleShape
                        )
                )
            }
        }

        // منطقة اللمس الذكية (تمنع القفز المفاجئ أثناء سحب إيماءات النظام مثل الخروج للرئيسية)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(duration, isRtl) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downX = down.position.x
                        val downY = down.position.y
                        var isConfirmedDrag = false
                        var isCancelled = false
                        var totalDx = 0f
                        var totalDy = 0f
                        var lastX = downX
                        var lastY = downY

                        fun getValueFromX(x: Float): Float {
                            if (duration > 0f && size.width > 0) {
                                val rawFraction = (x / size.width.toFloat()).coerceIn(0f, 1f)
                                val fraction = if (isRtl) (1f - rawFraction) else rawFraction
                                return fraction * duration
                            }
                            return position
                        }

                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                break
                            }
                            val currentX = change.position.x
                            val currentY = change.position.y
                            val dx = currentX - lastX
                            val dy = currentY - lastY
                            totalDx += dx
                            totalDy += dy
                            lastX = currentX
                            lastY = currentY

                            val touchSlop = 16.dp.toPx()

                            if (!isConfirmedDrag && !isCancelled) {
                                if (kotlin.math.abs(totalDy) > touchSlop && kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx)) {
                                    // إلغاء التفاعل عند السحب الرأسي (مثل سحب إيماءة الخروج للرئيسية)
                                    isCancelled = true
                                } else if (kotlin.math.abs(totalDx) > touchSlop) {
                                    // تأكيد السحب الأفقي على الشريط
                                    isConfirmedDrag = true
                                    isDragging = true
                                    change.consume()
                                    dragValue = getValueFromX(currentX)
                                    onValueChange(dragValue)
                                }
                            } else if (isConfirmedDrag) {
                                change.consume()
                                dragValue = getValueFromX(currentX)
                                onValueChange(dragValue)
                            }
                        }

                        if (isConfirmedDrag) {
                            isDragging = false
                            onValueChangeFinished(dragValue)
                        } else if (!isCancelled) {
                            // نقرة مباشرة سريعة بدون سحب رأسي
                            val totalDist = kotlin.math.hypot(totalDx, totalDy)
                            if (totalDist <= 16.dp.toPx()) {
                                val tapValue = getValueFromX(downX)
                                dragValue = tapValue
                                onValueChange(tapValue)
                                onValueChangeFinished(tapValue)
                            }
                        }
                    }
                }
        )
    }
}

