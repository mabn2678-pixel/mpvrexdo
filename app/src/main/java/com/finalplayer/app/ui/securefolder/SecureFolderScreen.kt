package com.finalplayer.app.ui.securefolder

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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.securefolder.components.PinInputDialog
import com.finalplayer.app.ui.securefolder.components.SecureFolderSortBottomSheet
import com.finalplayer.app.ui.securefolder.components.SecureVideoGridItem
import com.finalplayer.app.ui.securefolder.components.SecureVideoItem
import com.finalplayer.app.utils.BiometricHelper
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureFolderScreen(
    viewModel: SecureFolderViewModel = koinViewModel(),
    onVideoClick: (VideoItem) -> Unit,
    onBack: () -> Unit
) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isPinSet by viewModel.isPinSet.collectAsState()
    val secureVideos by viewModel.secureVideos.collectAsState()
    val secureCount by viewModel.secureVideoCount.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val layoutMode by viewModel.layoutMode.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val sortSheetState = rememberModalBottomSheetState()
    val activity = LocalContext.current as? FragmentActivity

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

    androidx.compose.runtime.LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
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
        topBar = {
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
                        IconButton(onClick = { viewModel.lock() }) {
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
                        "$secureCount ${if (secureCount == 1) "فيديو" else "فيديوهات"} محمية",
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
                        items(secureVideos, key = { it.id }) { video ->
                            SecureVideoGridItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onRemove = {
                                    viewModel.removeFromSecureFolder(video.id)
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
                        items(secureVideos, key = { it.id }) { video ->
                            SecureVideoItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onRemove = {
                                    viewModel.removeFromSecureFolder(video.id)
                                }
                            )
                        }
                    }
                }
            }
        }
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
}
