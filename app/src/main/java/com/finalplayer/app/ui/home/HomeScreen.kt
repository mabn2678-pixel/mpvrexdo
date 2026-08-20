package com.finalplayer.app.ui.home

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.components.DeleteConfirmDialog
import com.finalplayer.app.ui.components.FileInfoDialog
import com.finalplayer.app.ui.components.FolderContextMenuSheet
import com.finalplayer.app.ui.components.FolderPickerDialog
import com.finalplayer.app.ui.components.FolderPickerMode
import com.finalplayer.app.ui.components.RenameDialog
import com.finalplayer.app.ui.components.SelectionBottomActionBar
import com.finalplayer.app.ui.components.SelectionTopAppBar
import com.finalplayer.app.ui.components.VideoContextMenuSheet
import com.finalplayer.app.ui.components.thinScrollbar
import com.finalplayer.app.ui.home.components.FolderCard
import com.finalplayer.app.ui.home.components.FolderGridCard
import com.finalplayer.app.ui.home.components.HomeBottomBar
import com.finalplayer.app.ui.home.components.HomeTopBar
import com.finalplayer.app.ui.home.components.SortBottomSheet
import com.finalplayer.app.ui.recents.RecentsScreen
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel
import com.finalplayer.app.ui.shorts.ShortsScreen
import com.finalplayer.app.utils.FileInfo
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    secureFolderViewModel: SecureFolderViewModel = koinViewModel(),
    onFolderClick: (String) -> Unit = {},
    onVideoClick: (VideoItem, List<VideoItem>, Int) -> Unit = { _, _, _ -> },
    onRecentVideoClick: (String, String) -> Unit = { _, _ -> },
    onShortsVideoClick: (List<VideoItem>, Int) -> Unit = { _, _ -> },
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
    var contextMenuFolders by remember { mutableStateOf<List<VideoFolder>?>(null) }
    val sortSheetState = rememberModalBottomSheetState()

    var selectedVideos by remember { mutableStateOf<Set<VideoItem>>(emptySet()) }
    var selectedFolders by remember { mutableStateOf<Set<VideoFolder>>(emptySet()) }

    val isFolderSelectionMode = selectedFolders.isNotEmpty() && uiState.selectedTab == HomeTab.HOME
    val isVideoSelectionMode = selectedVideos.isNotEmpty() && uiState.selectedTab == HomeTab.HOME
    val isSelectionMode = (isFolderSelectionMode || isVideoSelectionMode) && uiState.selectedTab == HomeTab.HOME

    var showRenameDialog by remember { mutableStateOf(false) }
    var showRenameFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<VideoFolder?>(null) }

    var showFolderPicker by remember { mutableStateOf(false) }
    var folderPickerMode by remember { mutableStateOf(FolderPickerMode.MOVE) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
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

    fun toggleFolderSelection(folder: VideoFolder) {
        selectedFolders = if (selectedFolders.contains(folder)) {
            selectedFolders - folder
        } else {
            selectedFolders + folder
        }
    }

    fun getVideosInFolders(folders: Collection<VideoFolder>): List<VideoItem> {
        val cleanFolderPaths = folders.map { it.path.replace("//", "/").trimEnd('/') }.toSet()
        return uiState.allVideos.filter { video ->
            val cleanVideoFolder = video.folderPath.replace("//", "/").trimEnd('/')
            cleanFolderPaths.contains(cleanVideoFolder)
        }
    }

    BackHandler {
        if (selectedFolders.isNotEmpty()) {
            selectedFolders = emptySet()
        } else if (selectedVideos.isNotEmpty()) {
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
                if (isFolderSelectionMode) {
                    SelectionTopAppBar(
                        totalCount = uiState.folders.size,
                        selectedCount = selectedFolders.size,
                        isAllSelected = selectedFolders.size == uiState.folders.size && uiState.folders.isNotEmpty(),
                        onToggleSelectAll = {
                            selectedFolders = if (selectedFolders.size == uiState.folders.size) {
                                emptySet()
                            } else {
                                uiState.folders.toSet()
                            }
                        },
                        onInfoClick = {
                            val first = selectedFolders.firstOrNull()
                            if (first != null) {
                                val cleanPath = first.path.replace("//", "/").trimEnd('/')
                                val vInFolder = uiState.allVideos.filter {
                                    it.folderPath.replace("//", "/").trimEnd('/') == cleanPath
                                }
                                val totalSize = if (first.totalSizeBytes > 0) first.totalSizeBytes else vInFolder.sumOf { it.sizeBytes }
                                val totalDuration = if (first.totalDuration > 0) first.totalDuration else vInFolder.sumOf { it.duration }
                                val totalCount = if (vInFolder.isNotEmpty()) vInFolder.size else first.videoCount
                                val lastMod = if (first.lastModified > 0) first.lastModified else File(cleanPath).lastModified()
                                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                val formattedDate = if (lastMod > 0) dateFormat.format(Date(if (lastMod < 100_000_000_000L) lastMod * 1000L else lastMod)) else "غير معروف"

                                fileInfoForDialog = FileInfo(
                                    name = first.name.ifEmpty { File(cleanPath).name },
                                    path = cleanPath,
                                    size = totalSize,
                                    sizeFormatted = FileOperationsUtil.formatFileSize(totalSize),
                                    duration = totalDuration,
                                    durationFormatted = FileOperationsUtil.formatDurationMs(totalDuration),
                                    resolution = "$totalCount عناصر",
                                    format = "مجلد فيديو",
                                    lastModified = formattedDate
                                )
                                showInfoDialog = true
                            }
                        },
                        onMoreOptionsClick = {
                            contextMenuFolders = selectedFolders.toList()
                        },
                        onCloseClick = {
                            selectedFolders = emptySet()
                        }
                    )
                } else {
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
                }
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
                if (isFolderSelectionMode) {
                    val folderVideos = remember(selectedFolders, uiState.allVideos) {
                        getVideosInFolders(selectedFolders)
                    }
                    SelectionBottomActionBar(
                        selectedCount = selectedFolders.size,
                        onHideClick = {
                            if (folderVideos.isNotEmpty()) {
                                viewModel.hideVideosToSecureFolder(folderVideos, context) { _, message ->
                                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                                }
                                selectedFolders = emptySet()
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("لا توجد فيديوهات في المجلدات المحددة") }
                            }
                        },
                        onShareClick = {
                            if (folderVideos.isNotEmpty()) {
                                FileOperationsUtil.shareVideos(context, folderVideos)
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar("لا توجد فيديوهات للمشاركة") }
                            }
                        },
                        onRenameClick = {
                            if (selectedFolders.size == 1) {
                                folderToRename = selectedFolders.first()
                                showRenameFolderDialog = true
                            }
                        },
                        onDeleteClick = {
                            if (selectedFolders.isNotEmpty()) {
                                showDeleteFolderDialog = true
                            }
                        },
                        onCopyClick = {
                            if (folderVideos.isNotEmpty()) {
                                selectedVideos = folderVideos.toSet()
                                selectedFolders = emptySet()
                                folderPickerMode = FolderPickerMode.COPY
                                showFolderPicker = true
                            }
                        },
                        onMoveClick = {
                            if (folderVideos.isNotEmpty()) {
                                selectedVideos = folderVideos.toSet()
                                selectedFolders = emptySet()
                                folderPickerMode = FolderPickerMode.MOVE
                                showFolderPicker = true
                            }
                        },
                        onPlayClick = {
                            val firstVideo = folderVideos.firstOrNull()
                            if (firstVideo != null) {
                                val idx = uiState.allVideos.indexOf(firstVideo).coerceAtLeast(0)
                                onVideoClick(firstVideo, uiState.allVideos, idx)
                            }
                        }
                    )
                } else {
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
                }
            } else {
                HomeBottomBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { tab ->
                        selectedVideos = emptySet()
                        selectedFolders = emptySet()
                        viewModel.selectTab(tab)
                    },
                    onMusicClick = onMusicClick
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState.selectedTab) {
                HomeTab.RECENTS -> {
                    RecentsScreen(
                        onVideoClick = { video, list, idx ->
                            onVideoClick(video, list, idx)
                        }
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
                                                val videoProgress = uiState.playbackProgressMap[video.id] ?: uiState.playbackProgressMap[video.uri]
                                                VideoGridItem(
                                                    video = video,
                                                    isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                    playbackProgress = videoProgress,
                                                    visibleFields = uiState.visibleFields,
                                                    isSelected = selectedVideos.contains(video),
                                                    isSelectionMode = isVideoSelectionMode,
                                                    onClick = {
                                                        if (isVideoSelectionMode) {
                                                            toggleSelection(video)
                                                        } else {
                                                            onVideoClick(video, uiState.allVideos, index)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleSelection(video)
                                                    },
                                                    onOptionsClick = {
                                                        if (isVideoSelectionMode) {
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
                                                val videoProgress = uiState.playbackProgressMap[video.id] ?: uiState.playbackProgressMap[video.uri]
                                                VideoListItem(
                                                    video = video,
                                                    isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                    playbackProgress = videoProgress,
                                                    visibleFields = uiState.visibleFields,
                                                    isSelected = selectedVideos.contains(video),
                                                    isSelectionMode = isVideoSelectionMode,
                                                    onClick = {
                                                        if (isVideoSelectionMode) {
                                                            toggleSelection(video)
                                                        } else {
                                                            onVideoClick(video, uiState.allVideos, index)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleSelection(video)
                                                    },
                                                    onOptionsClick = {
                                                        if (isVideoSelectionMode) {
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
                                                    isSelected = selectedFolders.contains(folder),
                                                    isSelectionMode = isFolderSelectionMode,
                                                    onClick = {
                                                        if (isFolderSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            onFolderClick(folder.path)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleFolderSelection(folder)
                                                    },
                                                    onOptionsClick = {
                                                        if (isFolderSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            contextMenuFolders = listOf(folder)
                                                        }
                                                    },
                                                    onOptionsLongClick = {
                                                        toggleFolderSelection(folder)
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
                                                    isSelected = selectedFolders.contains(folder),
                                                    isSelectionMode = isFolderSelectionMode,
                                                    onClick = {
                                                        if (isFolderSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            onFolderClick(folder.path)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        toggleFolderSelection(folder)
                                                    },
                                                    onOptionsClick = {
                                                        if (isFolderSelectionMode) {
                                                            toggleFolderSelection(folder)
                                                        } else {
                                                            contextMenuFolders = listOf(folder)
                                                        }
                                                    },
                                                    onOptionsLongClick = {
                                                        toggleFolderSelection(folder)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fast-Scroll Indicator Badge
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

                        // Floating Action Button
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

    // Video Context Menu Sheet
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

    // Folder Context Menu Sheet
    contextMenuFolders?.let { folders ->
        FolderContextMenuSheet(
            selectedFolders = folders,
            allVideos = uiState.allVideos,
            onDismiss = { contextMenuFolders = null },
            onPlay = { folder ->
                val cleanPath = folder.path.replace("//", "/").trimEnd('/')
                val vInFolder = uiState.allVideos.filter {
                    it.folderPath.replace("//", "/").trimEnd('/') == cleanPath
                }
                val first = vInFolder.firstOrNull()
                if (first != null) {
                    val idx = uiState.allVideos.indexOf(first).coerceAtLeast(0)
                    onVideoClick(first, uiState.allVideos, idx)
                } else {
                    coroutineScope.launch { snackbarHostState.showSnackbar("لا توجد فيديوهات في هذا المجلد") }
                }
            },
            onSelectFolder = { folder ->
                selectedFolders = setOf(folder)
            },
            onShare = { shareFolders ->
                val videos = getVideosInFolders(shareFolders)
                if (videos.isNotEmpty()) {
                    FileOperationsUtil.shareVideos(context, videos)
                } else {
                    coroutineScope.launch { snackbarHostState.showSnackbar("لا توجد فيديوهات للمشاركة") }
                }
            },
            onRename = { folder, newName ->
                viewModel.renameFolder(folder, newName, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onMove = { moveFolders, _ ->
                val videos = getVideosInFolders(moveFolders)
                if (videos.isNotEmpty()) {
                    selectedVideos = videos.toSet()
                    selectedFolders = emptySet()
                    folderPickerMode = FolderPickerMode.MOVE
                    showFolderPicker = true
                }
            },
            onCopy = { copyFolders, _ ->
                val videos = getVideosInFolders(copyFolders)
                if (videos.isNotEmpty()) {
                    selectedVideos = videos.toSet()
                    selectedFolders = emptySet()
                    folderPickerMode = FolderPickerMode.COPY
                    showFolderPicker = true
                }
            },
            onHide = { hideFolders ->
                val videos = getVideosInFolders(hideFolders)
                if (videos.isNotEmpty()) {
                    viewModel.hideVideosToSecureFolder(videos, context) { _, message ->
                        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                    }
                    selectedFolders = emptySet()
                } else {
                    coroutineScope.launch { snackbarHostState.showSnackbar("لا توجد فيديوهات لإخفائها") }
                }
            },
            onDelete = { deleteFolders ->
                viewModel.deleteFolders(deleteFolders, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedFolders = emptySet()
            },
            onInfo = { folder ->
                val cleanPath = folder.path.replace("//", "/").trimEnd('/')
                val vInFolder = uiState.allVideos.filter {
                    it.folderPath.replace("//", "/").trimEnd('/') == cleanPath
                }
                val totalSize = if (folder.totalSizeBytes > 0) folder.totalSizeBytes else vInFolder.sumOf { it.sizeBytes }
                val totalDuration = if (folder.totalDuration > 0) folder.totalDuration else vInFolder.sumOf { it.duration }
                val totalCount = if (vInFolder.isNotEmpty()) vInFolder.size else folder.videoCount
                val lastMod = if (folder.lastModified > 0) folder.lastModified else File(cleanPath).lastModified()
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                val formattedDate = if (lastMod > 0) dateFormat.format(Date(if (lastMod < 100_000_000_000L) lastMod * 1000L else lastMod)) else "غير معروف"

                fileInfoForDialog = FileInfo(
                    name = folder.name.ifEmpty { File(cleanPath).name },
                    path = cleanPath,
                    size = totalSize,
                    sizeFormatted = FileOperationsUtil.formatFileSize(totalSize),
                    duration = totalDuration,
                    durationFormatted = FileOperationsUtil.formatDurationMs(totalDuration),
                    resolution = "$totalCount عناصر",
                    format = "مجلد فيديو",
                    lastModified = formattedDate
                )
                showInfoDialog = true
            }
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

    if (showRenameFolderDialog && folderToRename != null) {
        val f = folderToRename!!
        RenameDialog(
            currentName = f.name.ifEmpty { File(f.path).name },
            onConfirm = { newName ->
                showRenameFolderDialog = false
                viewModel.renameFolder(f, newName, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedFolders = emptySet()
                folderToRename = null
            },
            onDismiss = {
                showRenameFolderDialog = false
                folderToRename = null
            }
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

    if (showDeleteFolderDialog && selectedFolders.isNotEmpty()) {
        DeleteConfirmDialog(
            itemCount = selectedFolders.size,
            onConfirm = {
                showDeleteFolderDialog = false
                val items = selectedFolders.toList()
                viewModel.deleteFolders(items, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
                selectedFolders = emptySet()
            },
            onDismiss = { showDeleteFolderDialog = false }
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
