package com.finalplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.R
import kotlinx.coroutines.delay

/**
 * شاشة انتقال وتحميل تظهر عند فتح أي فيديو لمدة 1.6 ثانية بالضبط
 * مع تأثير موجات ونبض انسيابي لأيقونة التطبيق بلون السمة الحالي (App Theme Color)
 * وتختفي بسلاسة عبر Fade Out ليكشف المشغل في الخلفية.
 */
@Composable
fun PlayerLoadingOverlay(
    videoTitle: String,
    isVideoReady: Boolean,
    isPipMode: Boolean,
    displayTimeMs: Long = 1600L,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    var isCompletelyFinished by remember { mutableStateOf(false) }

    LaunchedEffect(isPipMode) {
        if (isPipMode) {
            isVisible = false
            isCompletelyFinished = true
            return@LaunchedEffect
        }
        delay(displayTimeMs)
        isVisible = false
        delay(450L)
        isCompletelyFinished = true
    }

    if (isCompletelyFinished || isPipMode) return

    AnimatedVisibility(
        visible = isVisible && !isPipMode,
        exit = fadeOut(animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer

        // حركات التموج والنبض المستمر
        val infiniteTransition = rememberInfiniteTransition(label = "PulseWavesTransition")

        // الموجة الأولى
        val wave1Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave1"
        )

        // الموجة الثانية (مزاحة زمنياً)
        val wave2Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, delayMillis = 400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave2"
        )

        // الموجة الثالثة (مزاحة زمنياً)
        val wave3Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1600, delayMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave3"
        )

        // حركة نبض خفيفة للأيقونة
        val iconBreathingScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "IconBreathing"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.22f),
                            Color(0xFF141722),
                            Color(0xFF0A0C12),
                            Color.Black
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // رسم الموجات المتوسعة في الخلفية بلون التطبيق
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .drawBehind {
                        val baseRadiusPx = 54.dp.toPx()
                        val maxExpansionPx = 82.dp.toPx()

                        // رسم موجة دائرية
                        fun drawWave(progress: Float, waveColor: Color) {
                            if (progress in 0.001f..0.999f) {
                                val currentRadius = baseRadiusPx + (maxExpansionPx * progress)
                                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.75f
                                val strokeWidth = (3.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(1.dp.toPx())

                                // حلقة الموجة الخارجية
                                drawCircle(
                                    color = waveColor.copy(alpha = alpha),
                                    radius = currentRadius,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )

                                // توهج ناعم خفيف
                                drawCircle(
                                    color = waveColor.copy(alpha = alpha * 0.18f),
                                    radius = currentRadius,
                                    center = center
                                )
                            }
                        }

                        // الموجة 1: لون التطبيق الأساسي (Primary)
                        drawWave(wave1Progress, primaryColor)
                        // الموجة 2: اللون الثانوي أو المشتق من سمة التطبيق
                        drawWave(wave2Progress, secondaryColor)
                        // الموجة 3: حاوية اللون الأساسي الفاتحة
                        drawWave(wave3Progress, primaryContainer)
                    },
                contentAlignment = Alignment.Center
            ) {
                // أيقونة التطبيق داخل دائرة فخمة ملونة بلون التطبيق
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .scale(iconBreathingScale)
                        .shadow(
                            elevation = 22.dp,
                            shape = CircleShape,
                            ambientColor = primaryColor,
                            spotColor = primaryColor
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    Color(0xFF161922)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.85f),
                                    primaryColor.copy(alpha = 0.8f),
                                    secondaryColor.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Final Player Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .padding(4.dp)
                    )
                }
            }

            // عنوان الفيديو ومؤشر التحميل الهادئ أسفل الأيقونة
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 54.dp, start = 24.dp, end = 24.dp)
            ) {
                if (videoTitle.isNotBlank()) {
                    Text(
                        text = videoTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(0.9f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "جارٍ تجهيز المشغل...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = primaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(0.85f)
                )
            }
        }
    }
}
