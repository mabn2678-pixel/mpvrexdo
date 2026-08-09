package com.finalplayer.app.ui.components

import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.utils.FileInfo

data class VideoContextMenuState(
    val selectedItems: List<VideoItem> = emptyList(),
    val isSheetVisible: Boolean = false,
    val isRenameDialogVisible: Boolean = false,
    val isFolderPickerVisible: Boolean = false,
    val folderPickerMode: FolderPickerMode = FolderPickerMode.MOVE,
    val isDeleteDialogVisible: Boolean = false,
    val isInfoDialogVisible: Boolean = false,
    val fileInfo: FileInfo? = null,
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null
)
