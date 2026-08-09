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
import androidx.compose.material.icons.filled.WbSunny
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

import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.WbSunny

@Composable
fun BrightnessSlider(
    brightnessValue: Float, // 0.0f to 1.0f
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val brightnessPercent = (brightnessValue * 100).toInt().coerceIn(0, 100)
        val sunIcon = when {
            brightnessValue < 0.33f -> Icons.Default.BrightnessLow
            brightnessValue < 0.66f -> Icons.Default.BrightnessMedium
            else -> Icons.Default.BrightnessHigh
        }

        Surface(
            color = Color(0xEB12141C),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier
                .padding(16.dp)
                .testTag("brightness_slider_container")
        ) {
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brightness Percentage Text (Top)
                Text(
                    text = "$brightnessPercent",
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
                    val lightBlue = Color(0xFF82A2DE)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction = brightnessValue.coerceIn(0f, 1f))
                            .clip(CircleShape)
                            .background(lightBlue)
                    )
                }

                // Sun Icon (Bottom)
                Icon(
                    imageVector = sunIcon,
                    contentDescription = "سطوع الشاشة",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
