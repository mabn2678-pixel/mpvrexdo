package com.finalplayer.app.ui.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
 * شاشة انتقال وتحميل أنيقة تظهر عند فتح أي فيديو لمدة 2-3 ثوانٍ
 * حتى يتم تهيئة وتحميل مشغل mpv في الخلفية، مع تأثير تموجات وموجات ناعمة (Ripple Waves)
 * ونبض انسيابي لأيقونة التطبيق، وتختفي بسلاسة عبر Fade Out.
 */
@Composable
fun PlayerLoadingOverlay(
    videoTitle: String,
    isVideoReady: Boolean,
    isPipMode: Boolean,
    minDisplayTimeMs: Long = 2200L,
    maxDisplayTimeMs: Long = 3500L,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isVideoReady, isPipMode) {
        if (isPipMode) {
            isVisible = false
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis()
        // انتظر حتى انقضاء الحد الأدنى لزمن العرض (2.2 ثانية)
        delay(minDisplayTimeMs)

        // إذا كان الفيديو جاهزاً بعد انتهاء الحد الأدنى، قم بالإخفاء
        val elapsed = System.currentTimeMillis() - startTime
        if (isVideoReady) {
            isVisible = false
        } else {
            // انتظر حتى يصبح جاهزاً أو حتى بلوغ الحد الأقصى للمهلة
            val remainingMax = (maxDisplayTimeMs - elapsed).coerceAtLeast(0L)
            delay(remainingMax)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && !isPipMode,
        exit = fadeOut(animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)),
        modifier = modifier
    ) {
        // حركات التموج والنبض المستمر
        val infiniteTransition = rememberInfiniteTransition(label = "PulseWavesTransition")

        // الموجة الأولى
        val wave1Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave1"
        )

        // الموجة الثانية (مزاحة زمنياً)
        val wave2Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, delayMillis = 500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave2"
        )

        // الموجة الثالثة (مزاحة زمنياً)
        val wave3Progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, delayMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Wave3"
        )

        // حركة نبض خفيفة للأيقونة
        val iconBreathingScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
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
                            Color(0xFF1E2638),
                            Color(0xFF0F131D),
                            Color(0xFF07090E),
                            Color.Black
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // رسم الموجات المتوسعة في الخلفية
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .drawBehind {
                        val baseRadiusPx = 54.dp.toPx()
                        val maxExpansionPx = 80.dp.toPx()

                        // رسم موجة دائرية
                        fun drawWave(progress: Float, primaryColor: Color, secondaryColor: Color) {
                            if (progress in 0.001f..0.999f) {
                                val currentRadius = baseRadiusPx + (maxExpansionPx * progress)
                                val alpha = (1f - progress).coerceIn(0f, 1f) * 0.65f
                                val strokeWidth = (3.dp.toPx() * (1f - progress * 0.5f)).coerceAtLeast(1.dp.toPx())

                                // حلقة الموجة الخارجية
                                drawCircle(
                                    color = primaryColor.copy(alpha = alpha),
                                    radius = currentRadius,
                                    center = center,
                                    style = Stroke(width = strokeWidth)
                                )

                                // توهج ناعم خفيف
                                drawCircle(
                                    color = secondaryColor.copy(alpha = alpha * 0.15f),
                                    radius = currentRadius,
                                    center = center
                                )
                            }
                        }

                        // الموجة 1: لون سماوي/أزرق نيون
                        drawWave(wave1Progress, Color(0xFF64B5F6), Color(0xFF2196F3))
                        // الموجة 2: لون أزرق فيروزي مائل للبنفسجي
                        drawWave(wave2Progress, Color(0xFF90CAF9), Color(0xFF42A5F5))
                        // الموجة 3: أبيض ناصع مع زرقة
                        drawWave(wave3Progress, Color(0xFFE3F2FD), Color(0xFFBBDEFB))
                    },
                contentAlignment = Alignment.Center
            ) {
                // أيقونة التطبيق داخل دائرة فخمة
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .scale(iconBreathingScale)
                        .shadow(
                            elevation = 20.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF2196F3),
                            spotColor = Color(0xFF64B5F6)
                        )
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF25334D),
                                    Color(0xFF141B29)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.8f),
                                    Color(0xFF64B5F6).copy(alpha = 0.6f),
                                    Color(0xFF1E88E5).copy(alpha = 0.3f)
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
                        color = Color(0xFF90CAF9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(0.75f)
                )
            }
        }
    }
}
