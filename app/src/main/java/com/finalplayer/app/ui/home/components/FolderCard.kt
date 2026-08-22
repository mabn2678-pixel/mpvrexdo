package com.finalplayer.app.ui.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.ui.components.AppFlowRow
import com.finalplayer.app.ui.components.VideoThumbnailImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderCard(
    folder: VideoFolder,
    visibleFields: Set<String> = emptySet(),
    unwatchedCount: Int = 0,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    onOptionsLongClick: (() -> Unit)? = null
) {
    val cleanPath = folder.path.replace("//", "/").trimEnd('/')
    val displayName = folder.name.ifEmpty { File(cleanPath).name }
    val showFullName = visibleFields.contains("Full Name")
    val showPath = visibleFields.contains("Path")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onClick() }
                } else if (onLongClick != null || onOptionsClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { onLongClick?.invoke() ?: onOptionsClick?.invoke() }
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Details Column
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (showFullName) 5 else 2,
                overflow = TextOverflow.Ellipsis
            )

            if (showPath) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cleanPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val showDate = visibleFields.contains("Date") && folder.lastModified > 0
            val showFolderSize = visibleFields.contains("Folder Size") && folder.totalSizeBytes > 0
            val showTotalMedia = visibleFields.contains("Total Media")
            val showTotalDuration = visibleFields.contains("Total Duration") && folder.totalDuration > 0

            if (showDate || showFolderSize || showTotalMedia || showTotalDuration) {
                Spacer(modifier = Modifier.height(8.dp))

                // Chips Flow
                AppFlowRow(
                    horizontalSpacing = 6.dp,
                    verticalSpacing = 4.dp
                ) {
                    if (showTotalDuration) {
                        InfoChip(text = formatDuration(folder.totalDuration))
                    }
                    if (showTotalMedia) {
                        InfoChip(text = "Items ${folder.videoCount}")
                    }
                    if (showFolderSize) {
                        InfoChip(text = formatFileSize(folder.totalSizeBytes))
                    }
                    if (showDate) {
                        val dateStr = formatDate(folder.lastModified)
                        if (dateStr.isNotEmpty()) InfoChip(text = dateStr)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Stacked Folder Preview with optional Unwatched Badge & Selection Checkmark
        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            FolderStackedPreview(
                previewThumbnails = folder.previewThumbnails,
                size = 56.dp,
                isSelected = isSelected
            )

            if (!isSelected && unwatchedCount > 0) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unwatchedCount > 99) "99+" else unwatchedCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridCard(
    folder: VideoFolder,
    visibleFields: Set<String> = emptySet(),
    unwatchedCount: Int = 0,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    onOptionsLongClick: (() -> Unit)? = null
) {
    val cleanPath = folder.path.replace("//", "/").trimEnd('/')
    val displayName = folder.name.ifEmpty { File(cleanPath).name }
    val showFullName = visibleFields.contains("Full Name")
    val showPath = visibleFields.contains("Path")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onClick() }
                } else if (onLongClick != null || onOptionsClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = { onLongClick?.invoke() ?: onOptionsClick?.invoke() }
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            FolderStackedPreview(
                previewThumbnails = folder.previewThumbnails,
                size = 64.dp,
                isSelected = isSelected
            )

            if (!isSelected && unwatchedCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .size(20.dp)
                        .background(Color(0xFFD32F2F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unwatchedCount > 99) "99+" else unwatchedCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (showFullName) 4 else 2,
            overflow = TextOverflow.Ellipsis
        )

        if (showPath) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = cleanPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val showDate = visibleFields.contains("Date") && folder.lastModified > 0
        val showFolderSize = visibleFields.contains("Folder Size") && folder.totalSizeBytes > 0
        val showTotalMedia = visibleFields.contains("Total Media")
        val showTotalDuration = visibleFields.contains("Total Duration") && folder.totalDuration > 0

        if (showDate || showFolderSize || showTotalMedia || showTotalDuration) {
            Spacer(modifier = Modifier.height(6.dp))
            AppFlowRow(
                horizontalSpacing = 4.dp,
                verticalSpacing = 4.dp
            ) {
                if (showTotalDuration) {
                    InfoChip(text = formatDuration(folder.totalDuration))
                }
                if (showTotalMedia) {
                    InfoChip(text = "${folder.videoCount} items")
                }
                if (showFolderSize) {
                    InfoChip(text = formatFileSize(folder.totalSizeBytes))
                }
                if (showDate) {
                    val dateStr = formatDate(folder.lastModified)
                    if (dateStr.isNotEmpty()) InfoChip(text = dateStr)
                }
            }
        }
    }
}

@Composable
private fun FolderStackedPreview(
    previewThumbnails: List<String>,
    size: Dp,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val nonBlankThumbnails = previewThumbnails.filter { it.isNotBlank() }.take(3)
    val containerSize = if (nonBlankThumbnails.size > 1) size + 8.dp else size

    Box(
        modifier = modifier.size(containerSize),
        contentAlignment = Alignment.Center
    ) {
        if (nonBlankThumbnails.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "محدد",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size * 0.55f)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Folder",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(size * 0.58f)
                    )
                }
            }
        } else {
            val count = nonBlankThumbnails.size
            // Render back layer first (index 2 down to 0)
            for (i in (count - 1) downTo 0) {
                val offsetVal = (i * 4).dp
                Box(
                    modifier = Modifier
                        .offset(x = offsetVal - ((count - 1) * 2).dp, y = offsetVal - ((count - 1) * 2).dp)
                        .size(size)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    VideoThumbnailImage(
                        videoUri = nonBlankThumbnails[i],
                        thumbnailUrl = nonBlankThumbnails[i],
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "محدد",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(size * 0.55f)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatFileSize(sizeBytes: Long): String {
    val gb = sizeBytes / (1024.0 * 1024.0 * 1024.0)
    val mb = sizeBytes / (1024.0 * 1024.0)
    return if (gb >= 1.0) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val ms = if (timestamp > 0 && timestamp < 100_000_000_000L) timestamp * 1000L else timestamp
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(Date(ms))
}
