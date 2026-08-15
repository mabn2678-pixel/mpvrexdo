package com.finalplayer.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.finalplayer.app.ui.components.AppFlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.components.DeleteConfirmDialog
import com.finalplayer.app.ui.components.FileInfoDialog
import com.finalplayer.app.ui.components.FolderPickerDialog
import com.finalplayer.app.ui.components.FolderPickerMode
import com.finalplayer.app.ui.components.RenameDialog
import com.finalplayer.app.ui.components.SelectionBottomActionBar
import com.finalplayer.app.ui.components.SelectionTopAppBar
import com.finalplayer.app.ui.components.VideoStatusBadge
import com.finalplayer.app.ui.components.VideoThumbnailImage
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.utils.FileInfo
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.rememberModalBottomSheetState
import com.finalplayer.app.ui.home.components.SortBottomSheet
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel

import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.layout.aspectRatio

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.finalplayer.app.ui.components.VideoContextMenuSheet
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folderPath: String,
    viewModel: HomeViewModel = koinViewModel(),
    secureFolderViewModel: SecureFolderViewModel = koinViewModel(),
    onVideoClick: (VideoItem, List<VideoItem>, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val videos by viewModel.getVideosInFolder(folderPath)
        .collectAsState(initial = emptyList())
    val playedVideoIds by viewModel.playedVideoIds
        .collectAsState(initial = emptySet())
    val playbackProgresses by viewModel.playbackProgresses
        .collectAsState(initial = emptyList())

    val folderConfigs by viewModel.folderConfigs.collectAsState()
    val folderConfig = folderConfigs[folderPath] ?: FolderSortConfig()

    val sortedVideos = remember(videos, folderConfig.sortBy, folderConfig.sortAscending) {
        val sorted = when (folderConfig.sortBy) {
            "date" -> videos.sortedBy { it.dateAdded }
            "size" -> videos.sortedBy { it.sizeBytes }
            "duration" -> videos.sortedBy { it.duration }
            "name", "title" -> videos.sortedBy { it.title.lowercase() }
            else -> videos.sortedBy { it.title.lowercase() }
        }
        if (folderConfig.sortAscending) sorted else sorted.reversed()
    }

    var subtitleLangsMap by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    LaunchedEffect(videos, folderPath) {
        if (videos.isNotEmpty()) {
            subtitleLangsMap = computeSubtitleLanguagesForFolder(videos, folderPath)
        } else {
            subtitleLangsMap = emptyMap()
        }
    }

    val folderName = folderPath.substringAfterLast("/").ifEmpty { "الفولدر" }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    var selectedVideos by remember { mutableStateOf<Set<VideoItem>>(emptySet()) }
    val isSelectionMode = selectedVideos.isNotEmpty()

    var contextMenuVideos by remember { mutableStateOf<List<VideoItem>?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var folderPickerMode by remember { mutableStateOf(FolderPickerMode.MOVE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var fileInfoForDialog by remember { mutableStateOf<FileInfo?>(null) }

    var showSortSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState()

    fun toggleSelection(video: VideoItem) {
        selectedVideos = if (selectedVideos.contains(video)) {
            selectedVideos - video
        } else {
            selectedVideos + video
        }
    }

    BackHandler(enabled = isSelectionMode) {
        selectedVideos = emptySet()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    totalCount = sortedVideos.size,
                    selectedCount = selectedVideos.size,
                    isAllSelected = selectedVideos.size == sortedVideos.size && sortedVideos.isNotEmpty(),
                    onToggleSelectAll = {
                        selectedVideos = if (selectedVideos.size == sortedVideos.size) {
                            emptySet()
                        } else {
                            sortedVideos.toSet()
                        }
                    },
                    onInfoClick = {
                        val first = selectedVideos.firstOrNull()
                        if (first != null) {
                            coroutineScope.launch {
                                val file = FileOperationsUtil.getVideoFile(first)
                                fileInfoForDialog = FileOperationsUtil.getFileInfo(file, first)
                                showInfoDialog = true
                            }
                        }
                    },
                    onMoreOptionsClick = {
                        contextMenuVideos = selectedVideos.toList()
                    },
                    onCloseClick = {
                        selectedVideos = emptySet()
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(text = folderName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val newLayout = if (folderConfig.layoutMode == "grid") "list" else "grid"
                                viewModel.updateFolderConfig(folderPath, folderConfig.copy(layoutMode = newLayout))
                            }
                        ) {
                            Icon(
                                imageVector = if (folderConfig.layoutMode == "grid") Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                contentDescription = "طريقة العرض"
                            )
                        }

                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "فرز وترتيب"
                            )
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionBottomActionBar(
                    selectedCount = selectedVideos.size,
                    onTagClick = {
                        val first = selectedVideos.firstOrNull()
                        if (first != null) {
                            coroutineScope.launch {
                                val file = FileOperationsUtil.getVideoFile(first)
                                fileInfoForDialog = FileOperationsUtil.getFileInfo(file, first)
                                showInfoDialog = true
                            }
                        }
                    },
                    onShareClick = {
                        FileOperationsUtil.shareVideos(context, selectedVideos.toList())
                    },
                    onRenameClick = {
                        if (selectedVideos.size == 1) {
                            showRenameDialog = true
                        }
                    },
                    onDeleteClick = {
                        if (selectedVideos.isNotEmpty()) {
                            showDeleteDialog = true
                        }
                    },
                    onCopyClick = {
                        if (selectedVideos.isNotEmpty()) {
                            folderPickerMode = FolderPickerMode.COPY
                            showFolderPicker = true
                        }
                    },
                    onMoveClick = {
                        if (selectedVideos.isNotEmpty()) {
                            folderPickerMode = FolderPickerMode.MOVE
                            showFolderPicker = true
                        }
                    },
                    onPlayClick = {
                        val first = selectedVideos.firstOrNull()
                        if (first != null) {
                            val idx = sortedVideos.indexOf(first).coerceAtLeast(0)
                            onVideoClick(first, sortedVideos, idx)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode && sortedVideos.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        val lastPlayedVideo = sortedVideos.mapNotNull { v ->
                            val progress = playbackProgresses.find { it.videoId == v.id || it.videoId == v.uri }
                            if (progress != null && progress.lastPlayedTimestamp > 0) {
                                v to progress.lastPlayedTimestamp
                            } else null
                        }.maxByOrNull { it.second }?.first ?: sortedVideos.first()

                        val idx = sortedVideos.indexOf(lastPlayedVideo).coerceAtLeast(0)
                        onVideoClick(lastPlayedVideo, sortedVideos, idx)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل آخر فيديو تم فتحه"
                    )
                }
            }
        }
    ) { padding ->
        if (sortedVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد فيديوهات في هذا الفولدر",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (folderConfig.layoutMode == "grid") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = padding
            ) {
                itemsIndexed(sortedVideos, key = { index, video -> "${video.id}_$index" }) { index, video ->
                    VideoGridItem(
                        video = video,
                        isOpened = playedVideoIds.contains(video.id) || playedVideoIds.contains(video.uri),
                        subtitleLangs = subtitleLangsMap[video.id] ?: emptyList(),
                        isSelected = selectedVideos.contains(video),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(video)
                            } else {
                                onVideoClick(video, sortedVideos, index)
                            }
                        },
                        onLongClick = {
                            toggleSelection(video)
                        },
                        onOptionsClick = {
                            if (isSelectionMode) {
                                toggleSelection(video)
                            } else {
                                contextMenuVideos = listOf(video)
                            }
                        },
                        onOptionsLongClick = {
                            toggleSelection(video)
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .thinScrollbar(state = listState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                contentPadding = padding
            ) {
                itemsIndexed(sortedVideos, key = { index, video -> "${video.id}_$index" }) { index, video ->
                    VideoListItem(
                        video = video,
                        isOpened = playedVideoIds.contains(video.id) || playedVideoIds.contains(video.uri),
                        subtitleLangs = subtitleLangsMap[video.id] ?: emptyList(),
                        isSelected = selectedVideos.contains(video),
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(video)
                            } else {
                                onVideoClick(video, sortedVideos, index)
                            }
                        },
                        onLongClick = {
                            toggleSelection(video)
                        },
                        onOptionsClick = {
                            if (isSelectionMode) {
                                toggleSelection(video)
                            } else {
                                contextMenuVideos = listOf(video)
                            }
                        },
                        onOptionsLongClick = {
                            toggleSelection(video)
                        }
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            sheetState = sortSheetState,
            sortBy = folderConfig.sortBy,
            sortAscending = folderConfig.sortAscending,
            viewMode = "folder",
            layoutMode = folderConfig.layoutMode,
            visibleFields = folderConfig.visibleFields,
            onlyForFolderList = false,
            showAudioFiles = false,
            onDismiss = { showSortSheet = false },
            onSortByChanged = { newSort ->
                viewModel.updateFolderConfig(folderPath, folderConfig.copy(sortBy = newSort))
            },
            onSortAscendingChanged = { newAsc ->
                viewModel.updateFolderConfig(folderPath, folderConfig.copy(sortAscending = newAsc))
            },
            onViewModeChanged = { },
            onLayoutModeChanged = { newLayout ->
                viewModel.updateFolderConfig(folderPath, folderConfig.copy(layoutMode = newLayout))
            },
            onVisibleFieldsChanged = { newFields ->
                viewModel.updateFolderConfig(folderPath, folderConfig.copy(visibleFields = newFields))
            },
            onOnlyForFolderListChanged = { },
            onShowAudioFilesChanged = { }
        )
    }

    contextMenuVideos?.let { items ->
        VideoContextMenuSheet(
            selectedItems = items,
            onDismiss = { contextMenuVideos = null },
            onPlay = { selectedVideo ->
                val idx = sortedVideos.indexOf(selectedVideo).coerceAtLeast(0)
                onVideoClick(selectedVideo, sortedVideos, idx)
            },
            onShare = { shareItems ->
                FileOperationsUtil.shareVideos(context, shareItems)
            },
            onRename = { selectedVideo, newName ->
                viewModel.renameVideo(selectedVideo, newName, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onMove = { moveItems, destination ->
                viewModel.moveVideos(moveItems, destination, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onCopy = { copyItems, destination ->
                viewModel.copyVideos(copyItems, destination, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onHide = { hideItems ->
                viewModel.hideVideosToSecureFolder(hideItems, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onDelete = { deleteItems ->
                viewModel.deleteVideos(deleteItems, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onInfo = { /* Handled internally in sheet */ }
        )
    }

    if (showRenameDialog && selectedVideos.size == 1) {
        val singleVideo = selectedVideos.first()
        RenameDialog(
            currentName = singleVideo.title,
            onConfirm = { newName ->
                showRenameDialog = false
                viewModel.renameVideo(singleVideo, newName, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showFolderPicker && selectedVideos.isNotEmpty()) {
        val defaultPath = remember(selectedVideos) {
            val first = selectedVideos.first()
            if (first.folderPath.isNotBlank()) {
                File(first.folderPath)
            } else {
                FileOperationsUtil.getVideoFile(first).parentFile ?: File("/storage/emulated/0")
            }
        }

        FolderPickerDialog(
            initialPath = defaultPath,
            onFolderSelected = { targetFolder ->
                showFolderPicker = false
                val items = selectedVideos.toList()
                if (folderPickerMode == FolderPickerMode.MOVE) {
                    viewModel.moveVideos(items, targetFolder, context) { _, message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                } else {
                    viewModel.copyVideos(items, targetFolder, context) { _, message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                }
                selectedVideos = emptySet()
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showDeleteDialog && selectedVideos.isNotEmpty()) {
        DeleteConfirmDialog(
            itemCount = selectedVideos.size,
            onConfirm = {
                showDeleteDialog = false
                val items = selectedVideos.toList()
                viewModel.deleteVideos(items, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedVideos = emptySet()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showInfoDialog && fileInfoForDialog != null) {
        FileInfoDialog(
            fileInfo = fileInfoForDialog!!,
            onDismiss = {
                showInfoDialog = false
                fileInfoForDialog = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoListItem(
    video: VideoItem,
    isOpened: Boolean = false,
    subtitleLangs: List<String> = emptyList(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    onOptionsLongClick: (() -> Unit)? = null
) {
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Details Column (Title + Chips + Subtitle languages)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                AppFlowRow(
                    horizontalSpacing = 4.dp,
                    verticalSpacing = 4.dp
                ) {
                    // Resolution tag e.g. 1080p
                    val resText = when {
                        video.resolution != null && video.resolution.contains("x") -> {
                            val h = video.resolution.substringAfter("x").toIntOrNull() ?: 0
                            if (h > 0) "${h}p" else video.resolution
                        }
                        else -> "1080p"
                    }
                    MetaChip(text = resText)

                    // File size tag
                    val sizeText = formatFileSize(video.sizeBytes)
                    if (sizeText.isNotEmpty()) {
                        MetaChip(text = sizeText)
                    }

                    // Date tag
                    if (video.dateAdded > 0) {
                        MetaChip(text = formatDateShort(video.dateAdded))
                    }

                    // Subtitle Language Tags
                    val langs = subtitleLangs
                    langs.forEach { lang ->
                        SubtitleChip(language = lang)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Thumbnail with Duration Overlay & Status Badge & Selection Overlay
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                VideoThumbnailImage(
                    videoUri = video.uri,
                    thumbnailUrl = video.thumbnailPath,
                    videoDurationMs = video.duration,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = video.title
                )

                // Duration Overlay (Bottom-Start)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }

                // New / Running Status Badge (Top-End)
                VideoStatusBadge(
                    isOpened = isOpened,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                )

                // Selection checkmark overlay
                if (isSelected) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 3-dots button on the other side of the thumbnail
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
                    contentDescription = "خيارات الفيديو",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoGridItem(
    video: VideoItem,
    isOpened: Boolean = false,
    subtitleLangs: List<String> = emptyList(),
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
    onOptionsLongClick: (() -> Unit)? = null
) {
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                VideoThumbnailImage(
                    videoUri = video.uri,
                    thumbnailUrl = video.thumbnailPath,
                    videoDurationMs = video.duration,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = video.title
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    contentColor = Color.White
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                VideoStatusBadge(
                    isOpened = isOpened,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )

                // Selection checkmark overlay
                if (isSelected) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "محدد",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
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
                            contentDescription = "خيارات الفيديو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                AppFlowRow(
                    horizontalSpacing = 4.dp,
                    verticalSpacing = 4.dp
                ) {
                    val resText = when {
                        video.resolution != null && video.resolution.contains("x") -> {
                            val h = video.resolution.substringAfter("x").toIntOrNull() ?: 0
                            if (h > 0) "${h}p" else video.resolution
                        }
                        else -> "1080p"
                    }
                    MetaChip(text = resText)

                    val sizeText = formatFileSize(video.sizeBytes)
                    if (sizeText.isNotEmpty()) {
                        MetaChip(text = sizeText)
                    }

                    val langs = subtitleLangs
                    langs.forEach { lang ->
                        SubtitleChip(language = lang)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SubtitleChip(language: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Text(
            text = language,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatDateShort(timestampSec: Long): String {
    return try {
        val ms = if (timestampSec < 100000000000L) timestampSec * 1000L else timestampSec
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        sdf.format(Date(ms))
    } catch (_: Exception) {
        ""
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    return try {
        val sec = ms / 1000
        val m = (sec % 3600) / 60
        val s = sec % 60
        val h = sec / 3600
        if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    } catch (_: Exception) {
        "00:00"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    return try {
        val mb = bytes.toDouble() / (1024 * 1024)
        if (mb >= 1024) {
            String.format(Locale.getDefault(), "%.2f GB", mb / 1024)
        } else {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    } catch (_: Exception) {
        ""
    }
}

private fun getSubtitleLanguagesForVideo(video: VideoItem): List<String> {
    return try {
        val folder = File(video.folderPath)
        if (!folder.exists() || !folder.isDirectory) return emptyList()

        val videoFile = File(video.uri.removePrefix("file://"))
        val baseName = if (videoFile.exists()) videoFile.nameWithoutExtension else {
            video.title.substringBeforeLast(".")
        }

        if (baseName.isBlank()) return emptyList()

        val subExtensions = setOf("srt", "vtt", "ass", "sub", "ssa", "idx", "mks")
        val matchingFiles = folder.listFiles { file ->
            if (!file.isFile) return@listFiles false
            val ext = file.extension.lowercase()
            if (ext !in subExtensions) return@listFiles false
            val fileWithoutExt = file.nameWithoutExtension
            file.name.startsWith(baseName, ignoreCase = true) ||
                    baseName.startsWith(fileWithoutExt.substringBefore("."), ignoreCase = true) ||
                    fileWithoutExt.contains(baseName, ignoreCase = true)
        } ?: emptyArray()

        if (matchingFiles.isEmpty()) return emptyList()

        val langCounts = mutableMapOf<String, Int>()
        val langPatterns = mapOf(
            "de" to listOf("de", "ger", "deu", "german"),
            "ar" to listOf("ar", "ara", "arabic"),
            "en" to listOf("en", "eng", "english"),
            "fr" to listOf("fr", "fre", "fra", "french"),
            "es" to listOf("es", "spa", "spanish"),
            "tr" to listOf("tr", "tur", "turkish"),
            "ru" to listOf("ru", "rus", "russian"),
            "it" to listOf("it", "ita", "italian"),
            "fa" to listOf("fa", "per", "fas", "persian"),
            "ur" to listOf("ur", "urd", "urdu"),
            "zh" to listOf("zh", "chi", "zho", "chinese"),
            "ja" to listOf("ja", "jpn", "japanese")
        )

        for (file in matchingFiles) {
            val nameLower = file.name.lowercase()
            var foundLang: String? = null
            for ((key, patterns) in langPatterns) {
                if (patterns.any { p ->
                        nameLower.contains(".$p.") ||
                        nameLower.contains(".$p-") ||
                        nameLower.contains("-$p.") ||
                        nameLower.contains("_$p.") ||
                        nameLower.endsWith(".$p") ||
                        nameLower.contains("$p.srt") ||
                        nameLower.contains("$p.vtt") ||
                        nameLower.contains("$p.ass")
                    }
                ) {
                    foundLang = key
                    break
                }
            }
            val target = foundLang ?: "sub"
            langCounts[target] = (langCounts[target] ?: 0) + 1
        }

        langCounts.map { (lang, count) ->
            if (count > 1) "$lang ($count)" else lang
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private suspend fun computeSubtitleLanguagesForFolder(
    videos: List<VideoItem>,
    folderPath: String
): Map<String, List<String>> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val result = mutableMapOf<String, List<String>>()
    try {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) return@withContext emptyMap()

        val subExtensions = setOf("srt", "vtt", "ass", "sub", "ssa", "idx", "mks")
        val allSubFiles = folder.listFiles { file ->
            file.isFile && file.extension.lowercase() in subExtensions
        } ?: emptyArray()

        if (allSubFiles.isEmpty()) return@withContext emptyMap()

        val langPatterns = mapOf(
            "de" to listOf("de", "ger", "deu", "german"),
            "ar" to listOf("ar", "ara", "arabic"),
            "en" to listOf("en", "eng", "english"),
            "fr" to listOf("fr", "fre", "fra", "french"),
            "es" to listOf("es", "spa", "spanish"),
            "tr" to listOf("tr", "tur", "turkish"),
            "ru" to listOf("ru", "rus", "russian"),
            "it" to listOf("it", "ita", "italian"),
            "fa" to listOf("fa", "per", "fas", "persian"),
            "ur" to listOf("ur", "urd", "urdu"),
            "zh" to listOf("zh", "chi", "zho", "chinese"),
            "ja" to listOf("ja", "jpn", "japanese")
        )

        for (v in videos) {
            val videoFile = File(v.uri.removePrefix("file://"))
            val baseName = if (videoFile.exists()) videoFile.nameWithoutExtension else {
                v.title.substringBeforeLast(".")
            }
            if (baseName.isBlank()) continue

            val matchingFiles = allSubFiles.filter { file ->
                val fileWithoutExt = file.nameWithoutExtension
                file.name.startsWith(baseName, ignoreCase = true) ||
                        baseName.startsWith(fileWithoutExt.substringBefore("."), ignoreCase = true) ||
                        fileWithoutExt.contains(baseName, ignoreCase = true)
            }

            if (matchingFiles.isEmpty()) continue

            val langCounts = mutableMapOf<String, Int>()
            for (file in matchingFiles) {
                val nameLower = file.name.lowercase()
                var foundLang: String? = null
                for ((key, patterns) in langPatterns) {
                    if (patterns.any { p ->
                            nameLower.contains(".$p.") ||
                            nameLower.contains(".$p-") ||
                            nameLower.contains("-$p.") ||
                            nameLower.contains("_$p.") ||
                            nameLower.endsWith(".$p") ||
                            nameLower.contains("$p.srt") ||
                            nameLower.contains("$p.vtt") ||
                            nameLower.contains("$p.ass")
                        }
                    ) {
                        foundLang = key
                        break
                    }
                }
                val target = foundLang ?: "sub"
                langCounts[target] = (langCounts[target] ?: 0) + 1
            }

            result[v.id] = langCounts.map { (lang, count) ->
                if (count > 1) "$lang ($count)" else lang
            }
        }
    } catch (_: Exception) { }
    result
}

