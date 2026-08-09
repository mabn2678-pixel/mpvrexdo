package com.finalplayer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.utils.FileInfo
import com.finalplayer.app.utils.FileOperationsUtil
import java.io.File

enum class FolderPickerMode { MOVE, COPY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoContextMenuSheet(
    selectedItems: List<VideoItem>,
    onDismiss: () -> Unit,
    onPlay: (VideoItem) -> Unit,
    onShare: (List<VideoItem>) -> Unit,
    onRename: (VideoItem, String) -> Unit,
    onMove: (List<VideoItem>, File) -> Unit,
    onCopy: (List<VideoItem>, File) -> Unit,
    onHide: (List<VideoItem>) -> Unit,
    onDelete: (List<VideoItem>) -> Unit,
    onInfo: (VideoItem) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
) {
    val context = LocalContext.current
    val isSingleSelection = selectedItems.size == 1
    val singleVideo = selectedItems.firstOrNull()

    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var folderPickerMode by remember { mutableStateOf(FolderPickerMode.MOVE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var fileInfoForDialog by remember { mutableStateOf<FileInfo?>(null) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("video_context_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(bottom = 24.dp)
        ) {
            // Header
            if (isSingleSelection && singleVideo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VideoThumbnailImage(
                        videoUri = singleVideo.uri,
                        thumbnailUrl = singleVideo.thumbnailPath,
                        videoDurationMs = singleVideo.duration,
                        modifier = Modifier
                            .size(width = 64.dp, height = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = singleVideo.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val sizeText = FileOperationsUtil.formatFileSize(singleVideo.sizeBytes)
                        Text(
                            text = sizeText,
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
                        text = "${selectedItems.size} عناصر محددة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable menu list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                // 1. Play (Single selection only)
                if (isSingleSelection && singleVideo != null) {
                    ContextMenuItem(
                        icon = Icons.Default.PlayArrow,
                        title = "شغّل",
                        onClick = {
                            onDismiss()
                            onPlay(singleVideo)
                        },
                        testTag = "menu_item_play"
                    )
                }

                // 2. Share (All)
                ContextMenuItem(
                    icon = Icons.Default.Share,
                    title = "مشاركة",
                    onClick = {
                        onDismiss()
                        onShare(selectedItems)
                    },
                    testTag = "menu_item_share"
                )

                // 3. Rename (Single selection only)
                if (isSingleSelection && singleVideo != null) {
                    ContextMenuItem(
                        icon = Icons.Default.Edit,
                        title = "إعادة تسمية",
                        onClick = {
                            showRenameDialog = true
                        },
                        testTag = "menu_item_rename"
                    )
                }

                // 4. Move to
                ContextMenuItem(
                    icon = Icons.Default.DriveFileMove,
                    title = "نقل إلى",
                    onClick = {
                        folderPickerMode = FolderPickerMode.MOVE
                        showFolderPicker = true
                    },
                    testTag = "menu_item_move"
                )

                // 5. Copy to
                ContextMenuItem(
                    icon = Icons.Default.FileCopy,
                    title = "نسخ إلى",
                    onClick = {
                        folderPickerMode = FolderPickerMode.COPY
                        showFolderPicker = true
                    },
                    testTag = "menu_item_copy"
                )

                // 6. Hide
                ContextMenuItem(
                    icon = Icons.Default.VisibilityOff,
                    title = "إخفاء",
                    onClick = {
                        onDismiss()
                        onHide(selectedItems)
                    },
                    testTag = "menu_item_hide"
                )

                // 7. Info (Single selection only)
                if (isSingleSelection && singleVideo != null) {
                    ContextMenuItem(
                        icon = Icons.Default.Info,
                        title = "معلومات",
                        onClick = {
                            val file = FileOperationsUtil.getVideoFile(singleVideo)
                            fileInfoForDialog = FileOperationsUtil.getFileInfo(file, singleVideo)
                            showInfoDialog = true
                        },
                        testTag = "menu_item_info"
                    )
                }

                // Divider before delete
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )

                // 8. Delete
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    title = "حذف",
                    textColor = MaterialTheme.colorScheme.error,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        showDeleteDialog = true
                    },
                    testTag = "menu_item_delete"
                )
            }
        }
    }

    // Dialogs triggered from sheet
    if (showRenameDialog && isSingleSelection && singleVideo != null) {
        RenameDialog(
            currentName = singleVideo.title,
            onConfirm = { newName ->
                showRenameDialog = false
                onDismiss()
                onRename(singleVideo, newName)
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showFolderPicker) {
        val defaultPath = remember(singleVideo) {
            if (singleVideo != null && singleVideo.folderPath.isNotBlank()) {
                File(singleVideo.folderPath)
            } else {
                FileOperationsUtil.getVideoFile(selectedItems.first()).parentFile ?: File("/storage/emulated/0")
            }
        }

        FolderPickerDialog(
            initialPath = defaultPath,
            onFolderSelected = { targetFolder ->
                showFolderPicker = false
                onDismiss()
                if (folderPickerMode == FolderPickerMode.MOVE) {
                    onMove(selectedItems, targetFolder)
                } else {
                    onCopy(selectedItems, targetFolder)
                }
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showInfoDialog && fileInfoForDialog != null) {
        FileInfoDialog(
            fileInfo = fileInfoForDialog!!,
            onDismiss = {
                showInfoDialog = false
                fileInfoForDialog = null
                onDismiss()
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            itemCount = selectedItems.size,
            onConfirm = {
                showDeleteDialog = false
                onDismiss()
                onDelete(selectedItems)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
