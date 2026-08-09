package com.finalplayer.app.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VolumeSlider(
    volumePercent: Float, // 0.0f to 150.0f
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val volumeIcon = when {
            volumePercent <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
            volumePercent <= 33f -> Icons.AutoMirrored.Filled.VolumeMute
            volumePercent <= 66f -> Icons.AutoMirrored.Filled.VolumeDown
            else -> Icons.AutoMirrored.Filled.VolumeUp
        }

        Surface(
            color = Color(0xEB12141C),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .padding(16.dp)
                .testTag("volume_slider_container")
        ) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Volume Value Text (Top)
                Text(
                    text = "${volumePercent.toInt()}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp
                    )
                )

                // Thick Vertical Capsule Slider (Middle)
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(160.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val isBoosted = volumePercent > 100f
                    val lightBlue = Color(0xFF9EBBFF)
                    val darkRed = Color(0xFF900000)

                    val maxPercent = 150f
                    val totalFillFraction = (volumePercent / maxPercent).coerceIn(0f, 1f)

                    if (!isBoosted) {
                        // Normal Volume Fill (Light Blue)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = totalFillFraction)
                                .clip(CircleShape)
                                .background(lightBlue)
                        )
                    } else {
                        // Boosted Volume Fill (>100%): Stacked Red (base) and Light Blue (boost) matching Image 1
                        val boostFractionOfTotal = (volumePercent - 100f) / volumePercent
                        val normalFractionOfTotal = 100f / volumePercent

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction = totalFillFraction),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Top part of fill (Light Blue)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(boostFractionOfTotal.coerceAtLeast(0.01f))
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    .background(lightBlue)
                            )
                            // Bottom part of fill (Dark Red)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(normalFractionOfTotal.coerceAtLeast(0.01f))
                                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                    .background(darkRed)
                            )
                        }
                    }
                }

                // Volume Icon (Bottom)
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "مستوى الصوت",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
