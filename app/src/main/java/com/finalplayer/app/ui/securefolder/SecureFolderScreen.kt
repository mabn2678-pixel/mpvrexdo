package com.finalplayer.app.ui.securefolder

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.components.DeleteConfirmDialog
import com.finalplayer.app.ui.components.FileInfoDialog
import com.finalplayer.app.ui.components.FileTransferProgressDialog
import com.finalplayer.app.ui.components.SecureSelectionBottomActionBar
import com.finalplayer.app.ui.components.SelectionTopAppBar
import com.finalplayer.app.ui.securefolder.components.PinInputDialog
import com.finalplayer.app.ui.securefolder.components.SecureFolderSortBottomSheet
import com.finalplayer.app.ui.securefolder.components.SecureVideoGridItem
import com.finalplayer.app.ui.securefolder.components.SecureVideoItem
import com.finalplayer.app.utils.BiometricHelper
import com.finalplayer.app.utils.FileInfo
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderScreen(
    viewModel: SecureFolderViewModel = koinViewModel(),
    onVideoClick: (VideoItem, List<VideoItem>, Int) -> Unit,
    onBack: () -> Unit
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()
    val secureVideos by viewModel.secureVideos.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    var selectedVideos by remember { mutableStateOf<Set<VideoItem>>(emptySet()) }
    val isSelectionMode = isUnlocked && selectedVideos.isNotEmpty()
    val isAllSelected = secureVideos.isNotEmpty() && selectedVideos.size == secureVideos.size

    var showPinDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var fileInfoForDialog by remember { mutableStateOf<FileInfo?>(null) }

    val sortSheetState = rememberModalBottomSheetState()
    val activity = LocalActivity.current as? FragmentActivity
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isSelectionMode) {
        selectedVideos = emptySet()
    }

    if (!isPinSet) {
        SecureFolderSetupScreen(
            viewModel = viewModel,
            onSetupComplete = {
                viewModel.unlock()
            },
            onBack = onBack
        )
        return
    }

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            selectedVideos = emptySet()
            if (viewModel.isBiometricEnabled && activity != null && BiometricHelper.canUseBiometric(activity)) {
                BiometricHelper.authenticate(
                    activity = activity,
                    onSuccess = {
                        viewModel.unlock()
                        showPinDialog = false
                    },
                    onFallback = { showPinDialog = true },
                    onError = { showPinDialog = true }
                )
            } else {
                showPinDialog = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopAppBar(
                    totalCount = secureVideos.size,
                    selectedCount = selectedVideos.size,
                    isAllSelected = isAllSelected,
                    onToggleSelectAll = {
                        selectedVideos = if (isAllSelected) {
                            emptySet()
                        } else {
                            secureVideos.toSet()
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
                    onCloseClick = {
                        selectedVideos = emptySet()
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "المجلد الآمن",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                    },
                    actions = {
                        if (isUnlocked) {
                            IconButton(onClick = { showSortSheet = true }) {
                                Icon(
                                    Icons.Default.SwapVert,
                                    "الفرز والعرض",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { 
                                selectedVideos = emptySet()
                                viewModel.lock() 
                            }) {
                                Icon(
                                    Icons.Default.LockOpen,
                                    "قفل المجلد",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isSelectionMode) {
                SecureSelectionBottomActionBar(
                    selectedCount = selectedVideos.size,
                    onRestoreClick = {
                        val items = selectedVideos.toList()
                        viewModel.restoreVideosFromSecureFolder(items, context) { _, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                        selectedVideos = emptySet()
                    },
                    onShareClick = {
                        FileOperationsUtil.shareVideos(context, selectedVideos.toList())
                    },
                    onDeleteClick = {
                        showDeleteDialog = true
                    },
                    onPlayClick = {
                        val first = selectedVideos.firstOrNull()
                        if (first != null) {
                            val idx = secureVideos.indexOf(first).coerceAtLeast(0)
                            onVideoClick(first, secureVideos, idx)
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                // Locked State
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        Icons.Default.FolderSpecial,
                        null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        "المجلد الآمن",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "محتوى محمي ومشفر",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            if (viewModel.isBiometricEnabled &&
                                activity != null &&
                                BiometricHelper.canUseBiometric(activity)) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    onSuccess = { viewModel.unlock() },
                                    onFallback = { showPinDialog = true },
                                    onError = { showPinDialog = true }
                                )
                            } else {
                                showPinDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("فتح المجلد")
                    }
                }
            } else {
                // Unlocked State
                if (secureVideos.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            "المجلد فارغ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "أضف فيديوهات من الشاشة الرئيسية",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (layoutMode == "grid") {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(secureVideos, key = { _, it -> it.id }) { index, video ->
                            val isSelected = selectedVideos.contains(video)
                            SecureVideoGridItem(
                                video = video,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedVideos = if (isSelected) selectedVideos - video else selectedVideos + video
                                    } else {
                                        onVideoClick(video, secureVideos, index)
                                    }
                                },
                                onLongClick = {
                                    selectedVideos = if (isSelected) selectedVideos - video else selectedVideos + video
                                },
                                onRemove = {
                                    viewModel.removeFromSecureFolder(video.id, context) { _, msg ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(secureVideos, key = { _, it -> it.id }) { index, video ->
                            val isSelected = selectedVideos.contains(video)
                            SecureVideoItem(
                                video = video,
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedVideos = if (isSelected) selectedVideos - video else selectedVideos + video
                                    } else {
                                        onVideoClick(video, secureVideos, index)
                                    }
                                },
                                onLongClick = {
                                    selectedVideos = if (isSelected) selectedVideos - video else selectedVideos + video
                                },
                                onRemove = {
                                    viewModel.removeFromSecureFolder(video.id, context) { _, msg ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedVideos.isNotEmpty()) {
        DeleteConfirmDialog(
            itemCount = selectedVideos.size,
            onConfirm = {
                showDeleteDialog = false
                val items = selectedVideos.toList()
                viewModel.deleteSecureVideos(items, context) { _, msg ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(msg)
                    }
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

    if (showSortSheet) {
        SecureFolderSortBottomSheet(
            sheetState = sortSheetState,
            sortBy = sortBy,
            sortAscending = sortAscending,
            layoutMode = layoutMode,
            onDismiss = { showSortSheet = false },
            onSortByChanged = { viewModel.setSortBy(it) },
            onSortAscendingChanged = { viewModel.setSortAscending(it) },
            onLayoutModeChanged = { viewModel.setLayoutMode(it) }
        )
    }

    if (showPinDialog) {
        PinInputDialog(
            maxDigits = viewModel.pinLength,
            onPinEntered = { pin -> viewModel.verifyPinAndUnlock(pin) },
            onBiometric = if (viewModel.isBiometricEnabled && activity != null &&
                BiometricHelper.canUseBiometric(activity)) {
                {
                    BiometricHelper.authenticate(
                        activity = activity,
                        onSuccess = {
                            viewModel.unlock()
                            showPinDialog = false
                        },
                        onFallback = {},
                        onError = {}
                    )
                }
            } else null,
            onDismiss = { showPinDialog = false }
        )
    }

    FileTransferProgressDialog(
        progress = transferProgress,
        onCancel = { viewModel.cancelTransfer() },
        onMoveToBackground = { viewModel.moveToBackground() }
    )
}
