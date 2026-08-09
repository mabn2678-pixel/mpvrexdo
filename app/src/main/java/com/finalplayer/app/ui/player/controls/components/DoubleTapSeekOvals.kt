package com.finalplayer.app.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.player.DoubleTapSeekState
import org.koin.compose.koinInject

@Composable
fun DoubleTapSeekOvals(
    doubleTapState: DoubleTapSeekState?,
    modifier: Modifier = Modifier,
    playerPrefs: PlayerPreferences = koinInject()
) {
    val showOvals by playerPrefs.showDoubleTapOvals.asFlow().collectAsState(initial = true)

    if (!showOvals) return

    AnimatedVisibility(
        visible = doubleTapState != null,
        enter = fadeIn(tween(150)) + slideInHorizontally(
            initialOffsetX = { fullWidth -> if (doubleTapState?.isLeft == true) -fullWidth / 4 else fullWidth / 4 },
            animationSpec = tween(200)
        ),
        exit = fadeOut(tween(250)) + slideOutHorizontally(
            targetOffsetX = { fullWidth -> if (doubleTapState?.isLeft == true) -fullWidth / 4 else fullWidth / 4 },
            animationSpec = tween(200)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        if (doubleTapState == null) return@AnimatedVisibility

        val isLeft = doubleTapState.isLeft

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            // Semi-capsule edge overlay (matching exact screenshot curve & dark translucent tint)
            val capsuleShape = if (isLeft) {
                RoundedCornerShape(
                    topStart = 0.dp,
                    bottomStart = 0.dp,
                    topEnd = 160.dp,
                    bottomEnd = 160.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 160.dp,
                    bottomStart = 160.dp,
                    topEnd = 0.dp,
                    bottomEnd = 0.dp
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxHeight(0.82f)
                    .width(150.dp)
                    .clip(capsuleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .testTag(if (isLeft) "double_tap_oval_left" else "double_tap_oval_right"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Animated Circular Ring Indicator
                    CircularSeekRing(isLeft = isLeft)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seconds Text (e.g. "+10s" for forward, "10s-" for rewind)
                    val timeText = if (isLeft) "${doubleTapState.amountSeconds}s-" else "+${doubleTapState.amountSeconds}s"
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Label ("Forward" or "Rewind")
                    val labelText = if (isLeft) "Rewind" else "Forward"
                    Text(
                        text = labelText,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CircularSeekRing(isLeft: Boolean) {
    val transition = rememberInfiniteTransition(label = "RingRotation")
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 3.5.dp.toPx()
            // Track arc
            drawArc(
                color = Color.White.copy(alpha = 0.25f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth)
            )
            // Animated active arc segment
            drawArc(
                color = Color.White,
                startAngle = if (isLeft) -rotationAngle else rotationAngle,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Fast rewind / fast forward double chevron icon
        Icon(
            imageVector = if (isLeft) Icons.Default.FastRewind else Icons.Default.FastForward,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
