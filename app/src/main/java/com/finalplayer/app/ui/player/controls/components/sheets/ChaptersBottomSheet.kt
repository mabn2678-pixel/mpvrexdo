package com.finalplayer.app.ui.player.controls.components.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.R
import com.finalplayer.app.player.VideoChapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersBottomSheet(
    chapters: List<VideoChapter>,
    currentPosSeconds: Float,
    onDismiss: () -> Unit,
    onChapterClick: (VideoChapter) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val listState = rememberLazyListState()

    // Find the currently playing chapter
    val activeIndex = chapters.indexOfLast { chapter ->
        currentPosSeconds >= chapter.timePos
    }.takeIf { it >= 0 } ?: 0

    // Auto-scroll to active chapter when opened
    LaunchedEffect(activeIndex) {
        if (chapters.isNotEmpty() && activeIndex in chapters.indices) {
            try {
                listState.animateScrollToItem(activeIndex)
            } catch (_: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF161616),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF555555))
            )
        },
        modifier = Modifier.testTag("chapters_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header: Title & Close Button (YouTube style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.chapters),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_chapters_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }
            }

            if (chapters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_chapters),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF9E9E9E)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    itemsIndexed(chapters, key = { idx, ch -> "${ch.index}_${ch.timePos}" }) { index, chapter ->
                        val isCurrent = index == activeIndex

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isCurrent) Color(0xFF262626) else Color.Transparent
                                )
                                .clickable {
                                    onChapterClick(chapter)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail Preview Box (YouTube style)
                            Box(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) Color(0xFF1E293B) else Color(0xFF222222))
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 0.5.dp,
                                        color = if (isCurrent) Color(0xFF3B82F6) else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.PlayArrow else Icons.Outlined.Movie,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color(0xFF60A5FA) else Color(0x77FFFFFF),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Chapter Details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) Color.White else Color(0xFFE2E8F0)
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Distinct YouTube-styled Time Badge (Formatted Time)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isCurrent) Color(0xFF1E3A8A) else Color(0xFF1E2430),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isCurrent) Color(0xFF3B82F6) else Color(0x33475569)
                                    )
                                ) {
                                    Text(
                                        text = chapter.formattedTime,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) Color(0xFF93C5FD) else Color(0xFF60A5FA)
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (isCurrent) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "جاري التشغيل",
                                    tint = Color(0xFF3B82F6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
