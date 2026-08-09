package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.components.thinScrollbar

@Composable
fun AudioTracksSheet(
    tracks: List<TrackNode>,
    currentAudioId: Int,
    onSelectAudio: (Int) -> Unit,
    onAddAudioFile: () -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "المسارات الصوتية",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = onAddAudioFile) {
                Icon(Icons.Default.Add, "إضافة ملف صوت")
            }
        }

        HorizontalDivider()

        val audioTracks = tracks.filter { it.isAudio || it.type == "audio" }

        if (audioTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد مسارات صوتية",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .thinScrollbar(listState)
        ) {
            items(audioTracks) { track ->
                ListItem(
                    headlineContent = { Text(track.displayName) },
                    supportingContent = {
                        val info = buildString {
                            if (track.lang.isNotBlank()) append(track.lang.uppercase())
                            if (track.external) {
                                if (isNotEmpty()) append(" • ")
                                append("خارجي")
                            }
                        }
                        if (info.isNotBlank()) Text(info)
                    },
                    leadingContent = {
                        RadioButton(
                            selected = track.id == currentAudioId,
                            onClick = { onSelectAudio(track.id) }
                        )
                    },
                    modifier = Modifier.clickable { onSelectAudio(track.id) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
