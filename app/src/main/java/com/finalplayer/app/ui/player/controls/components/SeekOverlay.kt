package com.finalplayer.app.ui.player.controls.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.player.SeekState
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun SeekOverlay(
    seekState: SeekState?,
    modifier: Modifier = Modifier,
    playerPrefs: PlayerPreferences = koinInject()
) {
    val showTime by playerPrefs.showSeekTimeWhileSeeking.asFlow().collectAsState(initial = true)

    if (!showTime) return

    AnimatedVisibility(
        visible = seekState != null && seekState.isDragging,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (seekState == null) return@AnimatedVisibility

        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.testTag("seek_overlay_container")
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (seekState.isForwards) Icons.Default.FastForward else Icons.Default.FastRewind,
                        contentDescription = if (seekState.isForwards) "تقديم" else "ترجيع",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )

                    val diffFormatted = formatDiffTime(seekState.diffSeconds)
                    Text(
                        text = diffFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (seekState.isForwards) MaterialTheme.colorScheme.primary else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                }

                val targetFormatted = formatTime(seekState.targetPositionSec)
                Text(
                    text = targetFormatted,
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                )
            }
        }
    }
}

private fun formatDiffTime(diffSeconds: Float): String {
    val totalSec = diffSeconds.toInt()
    val isPos = totalSec >= 0
    val absSec = kotlin.math.abs(totalSec)
    val mins = absSec / 60
    val secs = absSec % 60
    val sign = if (isPos) "+" else "-"
    return String.format(Locale.getDefault(), "%s%d:%02d", sign, mins, secs)
}

private fun formatTime(seconds: Float): String {
    val totalSec = seconds.toInt().coerceAtLeast(0)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60

    return if (hrs > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
