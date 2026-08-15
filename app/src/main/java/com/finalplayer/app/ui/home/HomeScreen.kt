package com.finalplayer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.ui.home.components.FolderCard
import com.finalplayer.app.ui.home.components.FolderGridCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.home.components.HomeBottomBar
import com.finalplayer.app.ui.home.components.HomeTopBar
import com.finalplayer.app.ui.home.components.SortBottomSheet
import com.finalplayer.app.ui.recents.RecentsScreen
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableLongStateOf
import com.finalplayer.app.ui.shorts.ShortsScreen
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.finalplayer.app.ui.components.DeleteConfirmDialog
import com.finalplayer.app.ui.components.FileInfoDialog
import com.finalplayer.app.ui.components.FolderPickerDialog
import com.finalplayer.app.ui.components.FolderPickerMode
import com.finalplayer.app.ui.components.RenameDialog
import com.finalplayer.app.ui.components.SelectionBottomActionBar
import com.finalplayer.app.ui.components.SelectionTopAppBar
import com.finalplayer.app.ui.components.VideoContextMenuSheet
import com.finalplayer.app.utils.FileInfo
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    secureFolderViewModel: SecureFolderViewModel = koinViewModel(),
    onFolderClick: (String) -> Unit = {},
    onVideoClick: (VideoItem, List<VideoItem>, Int) -> Unit = { _, _, _ -> },
    onRecentVideoClick: (String, String) -> Unit = { _, _ -> },
    onShortsVideoClick: (List<com.finalplayer.app.domain.model.VideoItem>, Int) -> Unit = { _, _ -> },
    onPlayButtonClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSecureFolderClick: () -> Unit = {},
    onMusicClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSortSheet by remember { mutableStateOf(false) }
    var contextMenuVideos by remember { mutableStateOf<List<VideoItem>?>(null) }
    val sortSheetState = rememberModalBottomSheetState()

    var selectedVideos by remember { mutableStateOf<Set<VideoItem>>(emptySet()) }
    val isSelectionMode = selectedVideos.isNotEmpty() && uiState.selectedTab == HomeTab.HOME

    var showRenameDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var folderPickerMode by remember { mutableStateOf(FolderPickerMode.MOVE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var fileInfoForDialog by remember { mutableStateOf<FileInfo?>(null) }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    fun toggleSelection(video: VideoItem) {
        selectedVideos = if (selectedVideos.contains(video)) {
            selectedVideos - video
        } else {
            selectedVideos + video
        }
    }

    BackHandler {
        if (isSelectionMode) {
            selectedVideos = emptySet()
        } else if (uiState.selectedTab != HomeTab.HOME) {
            viewModel.selectTab(HomeTab.HOME)
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "اضغط مرة أخرى للخروج", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshVideos()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshVideos()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    totalCount = uiState.allVideos.size,
                    selectedCount = selectedVideos.size,
                    isAllSelected = selectedVideos.size == uiState.allVideos.size && uiState.allVideos.isNotEmpty(),
                    onToggleSelectAll = {
                        selectedVideos = if (selectedVideos.size == uiState.allVideos.size) {
                            emptySet()
                        } else {
                            uiState.allVideos.toSet()
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
            } else if (uiState.selectedTab == HomeTab.HOME) {
                HomeTopBar(
                    onSettingsClick = onSettingsClick,
                    onSortClick = { showSortSheet = true },
                    onSearchClick = onSearchClick,
                    onSecureFolderClick = onSecureFolderClick
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SelectionBottomActionBar(
                    selectedCount = selectedVideos.size,
                    onHideClick = {
                        if (selectedVideos.isNotEmpty()) {
                            val items = selectedVideos.toList()
                            viewModel.hideVideosToSecureFolder(items, context) { _, message ->
                                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                            }
                            selectedVideos = emptySet()
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
                            val idx = uiState.allVideos.indexOf(first).coerceAtLeast(0)
                            onVideoClick(first, uiState.allVideos, idx)
                        }
                    }
                )
            } else {
                HomeBottomBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { tab ->
                        viewModel.selectTab(tab)
                    },
                    onMusicClick = onMusicClick
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.selectedTab) {
                HomeTab.RECENTS -> {
                    RecentsScreen(
                        onVideoClick = onVideoClick
                    )
                }
                HomeTab.SHORTS -> {
                    ShortsScreen(
                        viewModel = viewModel,
                        onVideoClick = { video, shortsList ->
                            val index = shortsList.indexOf(video).coerceAtLeast(0)
                            onShortsVideoClick(shortsList, index)
                        }
                    )
                }
                HomeTab.HOME -> {
                    val isLibraryView = uiState.viewMode == "library"
                    val isGridView = uiState.layoutMode == "grid"

                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.refreshVideos() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Gesture detector for Pinching on file list to toggle list <-> grid mode
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, _, zoom, _ ->
                                        if (zoom > 1.25f && !isGridView) {
                                            viewModel.setLayoutMode("grid")
                                        } else if (zoom < 0.75f && isGridView) {
                                            viewModel.setLayoutMode("list")
                                        }
                                    }
                                }
                        ) {
                        if (uiState.isLoading && uiState.folders.isEmpty() && uiState.allVideos.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (!uiState.isLoading && (isLibraryView && uiState.allVideos.isEmpty() || !isLibraryView && uiState.folders.isEmpty())) {
                            Text(
                                text = "لم يتم العثور على فيديوهات / No videos found",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            if (isLibraryView) {
                                if (isGridView) {
                                    LazyVerticalGrid(
                                        state = lazyGridState,
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp, start = 8.dp, end = 8.dp)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.allVideos,
                                            key = { index, video -> "${video.id}_$index" }
                                        ) { index, video ->
                                            VideoListItem(
                                                video = video,
                                                isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                isSelected = selectedVideos.contains(video),
                                                isSelectionMode = isSelectionMode,
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleSelection(video)
                                                    } else {
                                                        onVideoClick(video, uiState.allVideos, index)
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
                                        state = lazyListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .thinScrollbar(state = lazyListState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.allVideos,
                                            key = { index, video -> "${video.id}_$index" }
                                        ) { index, video ->
                                            VideoListItem(
                                                video = video,
                                                isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                isSelected = selectedVideos.contains(video),
                                                isSelectionMode = isSelectionMode,
                                                onClick = {
                                                    if (isSelectionMode) {
                                                        toggleSelection(video)
                                                    } else {
                                                        onVideoClick(video, uiState.allVideos, index)
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
                            } else {
                                if (isGridView) {
                                    LazyVerticalGrid(
                                        state = lazyGridState,
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp, start = 8.dp, end = 8.dp)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.folders,
                                            key = { index, folder -> "${folder.path}_$index" }
                                        ) { _, folder ->
                                            val cleanFolder = folder.path.replace("//", "/").trimEnd('/')
                                            val vInFolder = uiState.allVideos.filter { v ->
                                                v.folderPath.replace("//", "/").trimEnd('/') == cleanFolder
                                            }
                                            val total = if (vInFolder.isNotEmpty()) vInFolder.size else folder.videoCount
                                            val played = vInFolder.count { v ->
                                                uiState.playedVideoIds.contains(v.id) || uiState.playedVideoIds.contains(v.uri)
                                            }
                                            val unwatchedCount = (total - played).coerceAtLeast(0)

                                            FolderGridCard(
                                                folder = folder,
                                                visibleFields = uiState.visibleFields,
                                                unwatchedCount = unwatchedCount,
                                                onClick = { onFolderClick(folder.path) }
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = lazyListState,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .thinScrollbar(state = lazyListState, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                                    ) {
                                        itemsIndexed(
                                            items = uiState.folders,
                                            key = { index, folder -> "${folder.path}_$index" }
                                        ) { _, folder ->
                                            val cleanFolder = folder.path.replace("//", "/").trimEnd('/')
                                            val vInFolder = uiState.allVideos.filter { v ->
                                                v.folderPath.replace("//", "/").trimEnd('/') == cleanFolder
                                            }
                                            val total = if (vInFolder.isNotEmpty()) vInFolder.size else folder.videoCount
                                            val played = vInFolder.count { v ->
                                                uiState.playedVideoIds.contains(v.id) || uiState.playedVideoIds.contains(v.uri)
                                            }
                                            val unwatchedCount = (total - played).coerceAtLeast(0)

                                            FolderCard(
                                                folder = folder,
                                                visibleFields = uiState.visibleFields,
                                                unwatchedCount = unwatchedCount,
                                                onClick = { onFolderClick(folder.path) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Thin Circular Fast-Scroll Indicator Badge
                        if (lazyListState.isScrollInProgress) {
                            val firstIdx = lazyListState.firstVisibleItemIndex
                            val totalCount = lazyListState.layoutInfo.totalItemsCount
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${firstIdx + 1}/$totalCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Circular Floating Action Button
                    if (!isSelectionMode) {
                        FloatingActionButton(
                            onClick = onPlayButtonClick,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 20.dp, bottom = 20.dp),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play"
                            )
                        }
                    }
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
            layoutMode = uiState.layoutMode,
            visibleFields = uiState.visibleFields,
            onlyForFolderList = uiState.onlyForFolderList,
            showAudioFiles = uiState.showAudioFiles,
            onDismiss = { showSortSheet = false },
            onSortByChanged = { viewModel.setSortBy(it) },
            onSortAscendingChanged = { viewModel.setSortAscending(it) },
            onViewModeChanged = { viewModel.setViewMode(it) },
            onLayoutModeChanged = { viewModel.setLayoutMode(it) },
            onVisibleFieldsChanged = { viewModel.setVisibleFields(it) },
            onOnlyForFolderListChanged = { viewModel.setOnlyForFolderList(it) },
            onShowAudioFilesChanged = { viewModel.setShowAudioFiles(it) }
        )
    }

    contextMenuVideos?.let { items ->
        VideoContextMenuSheet(
            selectedItems = items,
            onDismiss = { contextMenuVideos = null },
            onPlay = { selectedVideo ->
                val idx = uiState.allVideos.indexOf(selectedVideo).coerceAtLeast(0)
                onVideoClick(selectedVideo, uiState.allVideos, idx)
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
