package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.player.Decoder

@Composable
fun DecoderSheet(
    currentDecoder: Decoder,
    onSelect: (Decoder) -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss, scrollable = true) {
        Text(
            "جودة فك الترميز",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Decoder.entries.forEach { decoder ->
            val (badge, badgeColor, description) = when(decoder) {
                Decoder.HW_PLUS  -> Triple(
                    "موصى به", Color(0xFF4CAF50),
                    "الأسرع — أقل استهلاكاً للبطارية\nHardware Direct Decode"
                )
                Decoder.HW_COPY  -> Triple(
                    "متوافق", Color(0xFF2196F3),
                    "متوافق مع الفلاتر والشيدرات\nHardware Copy Decode"
                )
                Decoder.SOFTWARE -> Triple(
                    "بطيء", Color(0xFFFF9800),
                    "أبطأ لكن الأكثر توافقاً\nSoftware Decode"
                )
            }

            ListItem(
                headlineContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            decoder.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                badge, color = badgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                },
                supportingContent = { Text(description) },
                leadingContent = {
                    RadioButton(
                        selected = currentDecoder == decoder,
                        onClick = { onSelect(decoder) }
                    )
                },
                modifier = Modifier.clickable { onSelect(decoder) }
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
