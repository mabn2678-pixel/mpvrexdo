package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finalplayer.app.player.VideoChapter
import com.finalplayer.app.ui.components.SidePanel
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.ui.player.ChapterNode
import java.util.Locale

@Composable
fun ChaptersSheet(
    chapters: List<ChapterNode> = emptyList(),
    chaptersList: List<VideoChapter> = emptyList(),
    currentChapterIndex: Int? = null,
    currentPosSeconds: Float = 0f,
    onSeekToChapter: (Int) -> Unit = {},
    onSeekToChapterNode: ((VideoChapter) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val effectiveChapters = remember(chaptersList, chapters) {
        if (chaptersList.isNotEmpty()) {
            chaptersList
        } else {
            chapters.mapIndexed { idx, ch ->
                VideoChapter(
                    index = idx,
                    title = ch.title.ifBlank { "الفصل ${idx + 1}" },
                    timePos = ch.time,
                    formattedTime = formatSecondsToTime(ch.time)
                )
            }
        }
    }

    val activeIndex = remember(effectiveChapters, currentChapterIndex, currentPosSeconds) {
        if (currentChapterIndex != null && currentChapterIndex in effectiveChapters.indices) {
            currentChapterIndex
        } else {
            val idx = effectiveChapters.indexOfLast { currentPosSeconds >= it.timePos }
            if (idx >= 0) idx else 0
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (effectiveChapters.isNotEmpty() && activeIndex in effectiveChapters.indices) {
            try {
                listState.animateScrollToItem(activeIndex)
            } catch (_: Exception) {}
        }
    }

    SidePanel(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (effectiveChapters.isNotEmpty()) "الفصول (${effectiveChapters.size})" else "الفصول",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "إغلاق",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (effectiveChapters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد فصول في هذا الفيديو",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .thinScrollbar(listState)
            ) {
                itemsIndexed(effectiveChapters, key = { idx, ch -> "${ch.index}_${ch.timePos}" }) { index, chapter ->
                    val isCurrent = index == activeIndex

                    ListItem(
                        headlineContent = {
                            Text(
                                text = chapter.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isCurrent)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCurrent)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    0.5.dp,
                                    if (isCurrent)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    else
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = chapter.formattedTime,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrent)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        },
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(width = 44.dp, height = 36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isCurrent) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = if (isCurrent) {
                            {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "جاري التشغيل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        modifier = Modifier
                            .clickable {
                                onSeekToChapterNode?.invoke(chapter) ?: onSeekToChapter(chapter.index)
                                onDismiss()
                            }
                            .background(
                                if (isCurrent)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else
                                    Color.Transparent
                            )
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }
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
