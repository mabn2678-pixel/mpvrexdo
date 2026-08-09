package com.finalplayer.app.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun ResumeSnackbar(
    savedPositionSec: Double,
    onResume: () -> Unit,
    onStartFromBeginning: () -> Unit
) {
    LaunchedEffect(savedPositionSec) {
        delay(3000)
        onResume()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Snackbar(
            containerColor = Color(0xFF1B5E20),
            contentColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("resume_snackbar")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val formattedTime = formatSeconds(savedPositionSec)
                Text(
                    text = "استئناف من $formattedTime",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onStartFromBeginning,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFA5D6A7)),
                    modifier = Modifier.testTag("start_from_beginning_button")
                ) {
                    Text("من البداية")
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Double): String {
    val totalSeconds = seconds.toLong()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}
