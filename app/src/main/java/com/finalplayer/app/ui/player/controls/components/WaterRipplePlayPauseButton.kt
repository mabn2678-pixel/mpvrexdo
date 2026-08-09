package com.finalplayer.app.ui.player.controls.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun WaterRipplePlayPauseButton(
    isPaused: Boolean,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rippleTrigger by remember { mutableLongStateOf(0L) }
    val progress = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger > 0L) {
            launch {
                scale.snapTo(0.85f)
                scale.animateTo(1.0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            }
            launch {
                progress.snapTo(0f)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(56.dp)
            .drawBehind {
                val p = progress.value
                if (p > 0f && p < 1f) {
                    val baseRadiusPx = 28.dp.toPx() // base size of 56.dp button radius
                    val maxExpansionPx = 90.dp.toPx()

                    // Wave 1: Primary expanding water ring
                    val radius1 = baseRadiusPx + maxExpansionPx * p
                    val alpha1 = (1f - p) * 0.8f
                    val strokeWidth1 = (3.5.dp.toPx() * (1f - p * 0.6f)).coerceAtLeast(1.dp.toPx())
                    drawCircle(
                        color = Color.White.copy(alpha = alpha1),
                        radius = radius1,
                        center = center,
                        style = Stroke(width = strokeWidth1)
                    )
                    drawCircle(
                        color = Color(0xFF81D4FA).copy(alpha = alpha1 * 0.2f),
                        radius = radius1,
                        center = center
                    )

                    // Wave 2: Secondary concentric ripple
                    val p2 = ((p - 0.12f) / 0.88f).coerceIn(0f, 1f)
                    if (p2 > 0f && p2 < 1f) {
                        val radius2 = baseRadiusPx + maxExpansionPx * 0.75f * p2
                        val alpha2 = (1f - p2) * 0.6f
                        val strokeWidth2 = (2.5.dp.toPx() * (1f - p2 * 0.5f)).coerceAtLeast(1.dp.toPx())
                        drawCircle(
                            color = Color(0xFF29B6F6).copy(alpha = alpha2),
                            radius = radius2,
                            center = center,
                            style = Stroke(width = strokeWidth2)
                        )
                    }

                    // Wave 3: Tertiary inner pebble ripple
                    val p3 = ((p - 0.25f) / 0.75f).coerceIn(0f, 1f)
                    if (p3 > 0f && p3 < 1f) {
                        val radius3 = baseRadiusPx + maxExpansionPx * 0.50f * p3
                        val alpha3 = (1f - p3) * 0.4f
                        val strokeWidth3 = (2.dp.toPx() * (1f - p3 * 0.5f)).coerceAtLeast(0.8.dp.toPx())
                        drawCircle(
                            color = Color.White.copy(alpha = alpha3),
                            radius = radius3,
                            center = center,
                            style = Stroke(width = strokeWidth3)
                        )
                    }
                }
            }
    ) {
        Surface(
            onClick = {
                rippleTrigger = System.currentTimeMillis()
                onPlayPause()
            },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.55f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier
                .size(56.dp)
                .scale(scale.value)
                .testTag("player_play_pause_button")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "تشغيل" else "إيقاف مؤقت",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
