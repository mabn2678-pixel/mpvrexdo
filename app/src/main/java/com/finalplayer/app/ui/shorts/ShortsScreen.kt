package com.finalplayer.app.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.components.VideoStatusBadge
import com.finalplayer.app.ui.components.VideoThumbnailImage
import com.finalplayer.app.ui.home.HomeViewModel
import com.finalplayer.app.ui.home.components.SortBottomSheet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    viewModel: HomeViewModel,
    onVideoClick: (VideoItem, List<VideoItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    val allShorts by viewModel.shortsVideos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val playedIds by viewModel.playedVideoIds.collectAsState(initial = emptySet())

    var isGridView by remember(uiState.layoutMode) { mutableStateOf(uiState.layoutMode == "grid") }
    var showSortSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sortedShorts = remember(allShorts, uiState.sortBy, uiState.sortAscending) {
        val shortsOnly = allShorts.filter { it.isShortPlatformVideo }
        val sorted = when (uiState.sortBy) {
            "title" -> shortsOnly.sortedBy { it.title.lowercase(Locale.ROOT) }
            "date" -> shortsOnly.sortedBy { it.dateAdded }
            "size" -> shortsOnly.sortedBy { it.sizeBytes }
            "duration" -> shortsOnly.sortedBy { it.duration }
            else -> shortsOnly.sortedBy { it.dateAdded }
        }
        if (uiState.sortAscending) sorted else sorted.reversed()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title "Shorts" styled with the theme's primary color
                Text(
                    text = "Shorts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 22.sp
                )

                // Actions: Sort & View icon + Grid/List toggle icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Sort & View Icon Button
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("shorts_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "العرض والفرز",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Layout toggle button (Grid / List)
                    IconButton(
                        onClick = {
                            isGridView = !isGridView
                            viewModel.setLayoutMode(if (isGridView) "grid" else "list")
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("shorts_layout_toggle")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                            contentDescription = "تغيير العرض",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Shorts List Content
        if (sortedShorts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "لا توجد مقاطع قصيرة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يتم التعرّف تلقائياً على مقاطع TikTok و YouTube Shorts والفيديوهات الطولية.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedShorts, key = { it.id }) { video ->
                        ShortsGridCard(
                            video = video,
                            isPlayed = playedIds.contains(video.id),
                            onClick = { onVideoClick(video, sortedShorts) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedShorts, key = { it.id }) { video ->
                        ShortsListCard(
                            video = video,
                            isPlayed = playedIds.contains(video.id),
                            onClick = { onVideoClick(video, sortedShorts) }
                        )
                    }
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            sheetState = sortSheetState,
            sortBy = uiState.sortBy,
            sortAscending = uiState.sortAscending,
            viewMode = uiState.viewMode,
            layoutMode = if (isGridView) "grid" else "list",
            visibleFields = uiState.visibleFields,
            onlyForFolderList = uiState.onlyForFolderList,
            showAudioFiles = uiState.showAudioFiles,
            onDismiss = { showSortSheet = false },
            onSortByChanged = { viewModel.setSortBy(it) },
            onSortAscendingChanged = { viewModel.setSortAscending(it) },
            onViewModeChanged = { viewModel.setViewMode(it) },
            onLayoutModeChanged = { mode ->
                viewModel.setLayoutMode(mode)
                isGridView = (mode == "grid")
            },
            onVisibleFieldsChanged = { viewModel.setVisibleFields(it) },
            onOnlyForFolderListChanged = { viewModel.setOnlyForFolderList(it) },
            onShowAudioFilesChanged = { viewModel.setShowAudioFiles(it) }
        )
    }
}

@Composable
private fun ShortsGridCard(
    video: VideoItem,
    isPlayed: Boolean,
    onClick: () -> Unit
) {
    val tagLabel = remember(video) { getSourceTag(video) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 14f) // Aspect ratio optimized for vertical TikTok/Shorts previews
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color.Black)
            ) {
                VideoThumbnailImage(
                    videoUri = video.uri,
                    modifier = Modifier.fillMaxSize(),
                    thumbnailUrl = video.thumbnailPath,
                    videoDurationMs = video.duration,
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay for bottom text contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                                startY = 100f
                            )
                        )
                )

                // Source Badge (TikTok / Shorts / Vertical)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = tagLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Play Icon Overlay
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Duration & Status Badges
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f)
                    ) {
                        Text(
                            text = formatShortsDuration(video.duration),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    VideoStatusBadge(isOpened = isPlayed)
                }
            }

            // Title & Date Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatShortsDate(video.dateAdded),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShortsListCard(
    video: VideoItem,
    isPlayed: Boolean,
    onClick: () -> Unit
) {
    val tagLabel = remember(video) { getSourceTag(video) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .height(110.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                VideoThumbnailImage(
                    videoUri = video.uri,
                    modifier = Modifier.fillMaxSize(),
                    thumbnailUrl = video.thumbnailPath,
                    videoDurationMs = video.duration,
                    contentScale = ContentScale.Crop
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = formatShortsDuration(video.duration),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = tagLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    VideoStatusBadge(isOpened = isPlayed)
                }

                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "تاريخ الإضافة: ${formatShortsDate(video.dateAdded)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "تشغيل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

private fun getSourceTag(video: VideoItem): String {
    val path = video.folderPath.lowercase(Locale.ROOT)
    val title = video.title.lowercase(Locale.ROOT)
    val uri = video.uri.lowercase(Locale.ROOT)

    return when {
        path.contains("tiktok") || title.contains("tiktok") || uri.contains("tiktok") -> "TikTok"
        path.contains("shorts") || title.contains("shorts") || title.contains("ytshort") -> "Shorts"
        path.contains("reels") || title.contains("reels") -> "Reels"
        else -> "مقطع قصير"
    }
}

private fun formatShortsDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    val totalSec = durationMs / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}

private fun formatShortsDate(dateAdded: Long): String {
    if (dateAdded <= 0) return ""
    val ms = if (dateAdded < 10_000_000_000L) dateAdded * 1000L else dateAdded
    return try {
        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(ms))
    } catch (_: Exception) {
        ""
    }
}
