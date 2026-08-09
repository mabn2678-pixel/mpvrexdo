package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.ui.player.ChapterNode
import java.util.Locale

@Composable
fun ChaptersSheet(
    chapters: List<ChapterNode>,
    currentChapterIndex: Int?,
    onSeekToChapter: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    SidePanel(onDismissRequest = onDismiss) {
        Text(
            "الفصول (${chapters.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        if (chapters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "لا توجد فصول في هذا الفيديو",
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
            itemsIndexed(chapters) { index, chapter ->
                val isCurrent = index == currentChapterIndex
                ListItem(
                    headlineContent = {
                        Text(
                            chapter.title.ifBlank { "الفصل ${index + 1}" },
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(formatSecondsToTime(chapter.time))
                    },
                    leadingContent = {
                        if (isCurrent) {
                            Icon(
                                Icons.Default.PlayArrow,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier
                        .clickable { onSeekToChapter(index) }
                        .background(
                            if (isCurrent)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                )
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun formatSecondsToTime(seconds: Double): String {
    val t = seconds.toLong().coerceAtLeast(0)
    val h = t / 3600; val m = (t % 3600) / 60; val s = t % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
           else String.format(Locale.US, "%02d:%02d", m, s)
}
