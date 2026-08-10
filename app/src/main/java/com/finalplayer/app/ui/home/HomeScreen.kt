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
import com.finalplayer.app.ui.shorts.ShortsScreen
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.finalplayer.app.ui.components.VideoContextMenuSheet
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    secureFolderViewModel: SecureFolderViewModel = koinViewModel(),
    onFolderClick: (String) -> Unit = {},
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
    var contextMenuVideo by remember { mutableStateOf<VideoItem?>(null) }
    val sortSheetState = rememberModalBottomSheetState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

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
            if (uiState.selectedTab == HomeTab.HOME) {
                HomeTopBar(
                    onSettingsClick = onSettingsClick,
                    onSortClick = { showSortSheet = true },
                    onSearchClick = onSearchClick,
                    onRefreshClick = { viewModel.refreshVideos() },
                    onSecureFolderClick = onSecureFolderClick
                )
            }
        },
        bottomBar = {
            HomeBottomBar(
                selectedTab = uiState.selectedTab,
                onTabSelected = { tab ->
                    viewModel.selectTab(tab)
                },
                onMusicClick = onMusicClick
            )
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
                        onVideoClick = onRecentVideoClick
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
                                        ) { _, video ->
                                            VideoListItem(
                                                video = video,
                                                isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                onClick = { onRecentVideoClick(video.uri, video.title) },
                                                onLongClick = { contextMenuVideo = video }
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
                                        ) { _, video ->
                                            VideoListItem(
                                                video = video,
                                                isOpened = uiState.playedVideoIds.contains(video.id) || uiState.playedVideoIds.contains(video.uri),
                                                onClick = { onRecentVideoClick(video.uri, video.title) },
                                                onLongClick = { contextMenuVideo = video }
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

    contextMenuVideo?.let { video ->
        VideoContextMenuSheet(
            selectedItems = listOf(video),
            onDismiss = { contextMenuVideo = null },
            onPlay = { selectedVideo ->
                onRecentVideoClick(selectedVideo.uri, selectedVideo.title)
            },
            onShare = { items ->
                FileOperationsUtil.shareVideos(context, items)
            },
            onRename = { selectedVideo, newName ->
                viewModel.renameVideo(selectedVideo, newName, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onMove = { items, destination ->
                viewModel.moveVideos(items, destination, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onCopy = { items, destination ->
                viewModel.copyVideos(items, destination, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onHide = { items ->
                viewModel.hideVideosToSecureFolder(items, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onDelete = { items ->
                viewModel.deleteVideos(items, context) { _, message ->
                    coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                }
            },
            onInfo = { /* Handled internally in sheet */ }
        )
    }
}
