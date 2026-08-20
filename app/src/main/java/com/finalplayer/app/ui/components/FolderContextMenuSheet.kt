package com.finalplayer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.utils.FileInfo
import com.finalplayer.app.utils.FileOperationsUtil
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderContextMenuSheet(
    selectedFolders: List<VideoFolder>,
    allVideos: List<VideoItem>,
    onDismiss: () -> Unit,
    onPlay: (VideoFolder) -> Unit,
    onSelectFolder: (VideoFolder) -> Unit,
    onShare: (List<VideoFolder>) -> Unit,
    onRename: (VideoFolder, String) -> Unit,
    onMove: (List<VideoFolder>, File) -> Unit,
    onCopy: (List<VideoFolder>, File) -> Unit,
    onHide: (List<VideoFolder>) -> Unit,
    onDelete: (List<VideoFolder>) -> Unit,
    onInfo: (VideoFolder) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val isSingleSelection = selectedFolders.size == 1
    val singleFolder = selectedFolders.firstOrNull()

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.8f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("folder_context_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(bottom = 24.dp)
        ) {
            // Header
            if (isSingleSelection && singleFolder != null) {
                val cleanPath = singleFolder.path.replace("//", "/").trimEnd('/')
                val displayName = singleFolder.name.ifEmpty { File(cleanPath).name }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sizeText = FileOperationsUtil.formatFileSize(singleFolder.totalSizeBytes)
                        Text(
                            text = "${singleFolder.videoCount} عناصر • $sizeText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "${selectedFolders.size} مجلدات محددة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Scrollable Menu Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Play all in folder
                if (isSingleSelection && singleFolder != null) {
                    FolderMenuItem(
                        icon = Icons.Default.PlayArrow,
                        title = "تشغيل الكل في المجلد",
                        onClick = {
                            onDismiss()
                            onPlay(singleFolder)
                        },
                        tag = "folder_menu_play"
                    )

                    // Select
                    FolderMenuItem(
                        icon = Icons.Default.CheckCircle,
                        title = "تحديد المجلد",
                        onClick = {
                            onDismiss()
                            onSelectFolder(singleFolder)
                        },
                        tag = "folder_menu_select"
                    )
                }

                // Share
                FolderMenuItem(
                    icon = Icons.Default.Share,
                    title = "مشاركة الكل",
                    onClick = {
                        onDismiss()
                        onShare(selectedFolders)
                    },
                    tag = "folder_menu_share"
                )

                // Hide to secure folder
                FolderMenuItem(
                    icon = Icons.Default.Lock,
                    title = "إخفاء إلى المجلد الآمن",
                    onClick = {
                        onDismiss()
                        onHide(selectedFolders)
                    },
                    tag = "folder_menu_hide"
                )

                // Rename (single folder only)
                if (isSingleSelection && singleFolder != null) {
                    FolderMenuItem(
                        icon = Icons.Default.Edit,
                        title = "إعادة تسمية المجلد",
                        onClick = {
                            onDismiss()
                            // Handled by caller dialog
                            onInfo(singleFolder)
                        },
                        tag = "folder_menu_rename"
                    )
                }

                // Copy to
                FolderMenuItem(
                    icon = Icons.Default.FileCopy,
                    title = "نسخ محتويات المجلد إلى...",
                    onClick = {
                        onDismiss()
                        onCopy(selectedFolders, File(""))
                    },
                    tag = "folder_menu_copy"
                )

                // Move to
                FolderMenuItem(
                    icon = Icons.Default.DriveFileMove,
                    title = "نقل محتويات المجلد إلى...",
                    onClick = {
                        onDismiss()
                        onMove(selectedFolders, File(""))
                    },
                    tag = "folder_menu_move"
                )

                // Info (single folder)
                if (isSingleSelection && singleFolder != null) {
                    FolderMenuItem(
                        icon = Icons.Default.Info,
                        title = "معلومات المجلد",
                        onClick = {
                            onDismiss()
                            onInfo(singleFolder)
                        },
                        tag = "folder_menu_info"
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Delete
                FolderMenuItem(
                    icon = Icons.Default.Delete,
                    title = if (isSingleSelection) "حذف المجلد" else "حذف المجلدات المحددة",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        onDismiss()
                        onDelete(selectedFolders)
                    },
                    tag = "folder_menu_delete"
                )
            }
        }
    }
}

@Composable
private fun FolderMenuItem(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    tag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}
