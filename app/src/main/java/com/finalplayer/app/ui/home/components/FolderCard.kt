package com.finalplayer.app.ui.home.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.ui.components.AppFlowRow
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onClick() }
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
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

            // Folder Icon with optional Badge / Selection Checkmark
            Box(
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "محدد",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

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

            Spacer(modifier = Modifier.width(2.dp))

            // 3-dots button placed right next to folder icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = { onOptionsClick?.invoke() ?: onLongClick?.invoke() },
                        onLongClick = { onOptionsLongClick?.invoke() ?: onLongClick?.invoke() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "خيارات المجلد",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .then(
                if (isSelectionMode) {
                    Modifier.clickable { onClick() }
                } else if (onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier.clickable { onClick() }
                }
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "محدد",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

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

                // 3-dots top-start in grid
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(32.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            onClick = { onOptionsClick?.invoke() ?: onLongClick?.invoke() },
                            onLongClick = { onOptionsLongClick?.invoke() ?: onLongClick?.invoke() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات المجلد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
