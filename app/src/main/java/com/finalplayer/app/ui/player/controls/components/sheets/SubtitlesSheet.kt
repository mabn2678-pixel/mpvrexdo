package com.finalplayer.app.ui.player.controls.components.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.components.thinScrollbar

@Composable
fun SubtitlesSheet(
    tracks: List<TrackNode>,
    selectedSubId: Int?,
    selectedSecondarySubId: Int?,
    onSelectSubtitle: (Int) -> Unit,
    onDisableSubtitles: () -> Unit,
    onAddExternalSubtitle: (Uri) -> Unit,
    onRemoveSubtitle: ((Int) -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onAddExternalSubtitle(uri)
        }
    }

    SidePanel(onDismissRequest = onDismiss, modifier = Modifier.testTag("subtitles_bottom_sheet")) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val subTracks = tracks.filter { it.isSubtitle || it.type == "sub" }

            // Header: Title + Settings Icon + Add File Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (subTracks.isEmpty())
                        "الترجمة"
                    else
                        "الترجمة (${subTracks.size})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenSettings != null) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("subtitle_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "إعدادات الترجمة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            documentPickerLauncher.launch(
                                arrayOf(
                                    "text/plain",
                                    "text/srt",
                                    "text/vtt",
                                    "application/x-subrip",
                                    "text/x-ssa",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier.testTag("add_external_sub_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة ملف ترجمة",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة ملف ترجمة")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disable Subtitles Option
            val isNoneSelected = (selectedSubId == null || selectedSubId <= 0) &&
                    (selectedSecondarySubId == null || selectedSecondarySubId <= 0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDisableSubtitles() }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isNoneSelected,
                    onClick = { onDisableSubtitles() }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.SubtitlesOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "إيقاف الترجمة",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (isNoneSelected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "إيقاف",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Subtitle Tracks List
            if (subTracks.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ClosedCaptionDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "لا توجد ملفات ترجمة",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "استخدم + لإضافة ملف ترجمة خارجي",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .thinScrollbar(listState)
                ) {
                    items(subTracks) { track ->
                        val isPrimary = selectedSubId == track.id
                        val isSecondary = selectedSecondarySubId == track.id
                        val isSelected = isPrimary || isSecondary

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSubtitle(track.id) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectSubtitle(track.id) }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.displayName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )

                                if (track.lang.isNotBlank() && track.title.isNotBlank()) {
                                    Text(
                                        text = track.lang.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (isPrimary) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("P", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else if (isSecondary) {
                                    Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                                        Text("S", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (track.external && onRemoveSubtitle != null) {
                                    IconButton(
                                        onClick = { onRemoveSubtitle(track.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "حذف",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
