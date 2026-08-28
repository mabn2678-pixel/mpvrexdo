package com.finalplayer.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.data.preferences.SortPreferences
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.repository.PlaybackRepository
import com.finalplayer.app.domain.repository.VideoRepository
import com.finalplayer.app.domain.usecase.GetVideoLibraryUseCase
import com.finalplayer.app.domain.usecase.GetVideosByFolderUseCase
import com.finalplayer.app.domain.usecase.ScanForVideosUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.finalplayer.app.data.preferences.DEFAULT_VISIBLE_FIELDS
import java.util.Locale

data class FolderSortConfig(
    val sortBy: String = "title",
    val sortAscending: Boolean = true,
    val layoutMode: String = "list",
    val visibleFields: Set<String> = DEFAULT_VISIBLE_FIELDS
)

class HomeViewModel(
    private val context: android.content.Context,
    private val getVideoLibraryUseCase: GetVideoLibraryUseCase,
    private val scanForVideosUseCase: ScanForVideosUseCase,
    private val getVideosByFolderUseCase: GetVideosByFolderUseCase,
    private val videoRepository: VideoRepository,
    private val sortPreferences: SortPreferences,
    private val playbackRepository: PlaybackRepository,
    private val fileTransferManager: com.finalplayer.app.data.transfer.FileTransferManager
) : ViewModel() {

    val transferProgress = fileTransferManager.transferState
    val transferCompletionEvents = fileTransferManager.transferCompletionEvents

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val folderPrefs = context.getSharedPreferences("folder_view_preferences", android.content.Context.MODE_PRIVATE)

    private val _folderConfigs = MutableStateFlow<Map<String, FolderSortConfig>>(loadInitialFolderConfigs())
    val folderConfigs: StateFlow<Map<String, FolderSortConfig>> = _folderConfigs.asStateFlow()

    private fun loadInitialFolderConfigs(): Map<String, FolderSortConfig> {
        val map = mutableMapOf<String, FolderSortConfig>()
        try {
            val allEntries = folderPrefs.all
            val folderPaths = allEntries.keys
                .filter { it.startsWith("folder_path_") }
                .mapNotNull { folderPrefs.getString(it, null) }
                .toSet()

            for (path in folderPaths) {
                val keyPrefix = "folder_${path.hashCode()}"
                val sortBy = folderPrefs.getString("${keyPrefix}_sort_by", null) ?: "title"
                val sortAsc = folderPrefs.getBoolean("${keyPrefix}_sort_asc", true)
                val layout = folderPrefs.getString("${keyPrefix}_layout", null) ?: "list"
                val fields = folderPrefs.getStringSet("${keyPrefix}_fields", null) ?: DEFAULT_VISIBLE_FIELDS
                map[path] = FolderSortConfig(
                    sortBy = sortBy,
                    sortAscending = sortAsc,
                    layoutMode = layout,
                    visibleFields = fields
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    fun getFolderConfig(folderPath: String): FolderSortConfig {
        return _folderConfigs.value[folderPath] ?: FolderSortConfig(
            sortBy = _uiState.value.sortBy,
            sortAscending = _uiState.value.sortAscending,
            layoutMode = _uiState.value.layoutMode,
            visibleFields = _uiState.value.visibleFields
        )
    }

    fun updateFolderConfig(folderPath: String, config: FolderSortConfig) {
        _folderConfigs.update { it + (folderPath to config) }
        try {
            val keyPrefix = "folder_${folderPath.hashCode()}"
            folderPrefs.edit()
                .putString("folder_path_${folderPath.hashCode()}", folderPath)
                .putString("${keyPrefix}_sort_by", config.sortBy)
                .putBoolean("${keyPrefix}_sort_asc", config.sortAscending)
                .putString("${keyPrefix}_layout", config.layoutMode)
                .putStringSet("${keyPrefix}_fields", config.visibleFields)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var hasAutoScanned = false
    private var contentObserver: android.database.ContentObserver? = null
    private var periodicScanJob: kotlinx.coroutines.Job? = null

    init {
        observeFoldersAndSort()
        registerMediaStoreObserver()
        refreshVideos()
    }

    private fun registerMediaStoreObserver() {
        try {
            val observer = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    super.onChange(selfChange)
                    refreshVideos()
                }
            }
            context.contentResolver.registerContentObserver(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
            contentObserver = observer
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        contentObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val playbackProgresses: Flow<List<PlaybackProgress>> = playbackRepository.getAllProgress()

    val playedVideoIds: Flow<Set<String>> = playbackProgresses.map { list ->
        list.filter { it.lastPlayedTimestamp > 0 || it.positionMs > 0 }
            .map { it.videoId }
            .toSet()
    }

    val shortsVideos: StateFlow<List<VideoItem>> = getVideoLibraryUseCase()
        .map { list ->
            val filtered = list.filter { it.isShortPlatformVideo }
            // Sort videos from OLDEST to NEWEST (من الأقدم إلى الأحدث)
            filtered.sortedBy { it.dateAdded }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun isShortOrSocialVideo(item: VideoItem): Boolean {
        return item.isShortPlatformVideo
    }

private data class SortConfig(
    val sortBy: String,
    val sortAscending: Boolean,
    val viewMode: String,
    val layoutMode: String,
    val visibleFields: Set<String>
)

    private fun observeFoldersAndSort() {
        viewModelScope.launch {
            val sortPrefsFlow = combine(
                sortPreferences.sortBy.asFlow(),
                sortPreferences.sortAscending.asFlow(),
                sortPreferences.viewMode.asFlow(),
                sortPreferences.layoutMode.asFlow(),
                sortPreferences.visibleFields.asFlow()
            ) { sortBy, sortAscending, viewMode, layoutMode, visibleFields ->
                SortConfig(sortBy, sortAscending, viewMode, layoutMode, visibleFields)
            }

            val otherPrefsFlow = combine(
                sortPreferences.onlyForFolderList.asFlow(),
                sortPreferences.showAudioFiles.asFlow()
            ) { onlyForFolderList, showAudioFiles ->
                onlyForFolderList to showAudioFiles
            }

            combine(
                videoRepository.getAllFolders(),
                getVideoLibraryUseCase(),
                sortPrefsFlow,
                otherPrefsFlow,
                playbackRepository.getAllProgress()
            ) { folders, videos, sortConfig, otherPrefs, progressList ->
                val (onlyForFolderList, showAudioFiles) = otherPrefs
                val playedIds = progressList.filter { it.lastPlayedTimestamp > 0 || it.positionMs > 0 }
                    .map { it.videoId }
                    .toSet()

                val sortedFolders = when (sortConfig.sortBy) {
                    "date" -> folders.sortedBy { it.lastModified }
                    "size" -> folders.sortedBy { it.totalSizeBytes }
                    "duration" -> folders.sortedBy { it.totalDuration }
                    else -> folders.sortedBy { it.name.lowercase(Locale.ROOT) }
                }
                val finalFolders = if (sortConfig.sortAscending) sortedFolders else sortedFolders.reversed()

                val sortedVideos = when (sortConfig.sortBy) {
                    "date" -> videos.sortedBy { it.dateAdded }
                    "size" -> videos.sortedBy { it.sizeBytes }
                    "duration" -> videos.sortedBy { it.duration }
                    else -> videos.sortedBy { it.title.lowercase(Locale.ROOT) }
                }
                val finalVideos = if (sortConfig.sortAscending) sortedVideos else sortedVideos.reversed()

                val progressMap = progressList.associateBy { it.videoId }

                _uiState.value.copy(
                    folders = finalFolders,
                    allVideos = finalVideos,
                    playedVideoIds = playedIds,
                    playbackProgressMap = progressMap,
                    sortBy = sortConfig.sortBy,
                    sortAscending = sortConfig.sortAscending,
                    viewMode = sortConfig.viewMode,
                    layoutMode = sortConfig.layoutMode,
                    visibleFields = sortConfig.visibleFields,
                    onlyForFolderList = otherPrefs.first,
                    showAudioFiles = otherPrefs.second
                )
            }.collect { newState ->
                _uiState.value = newState
                if (newState.folders.isEmpty() && !_uiState.value.isLoading && !hasAutoScanned) {
                    hasAutoScanned = true
                    refreshVideos()
                }
            }
        }
    }

    fun setSortBy(sortBy: String) {
        viewModelScope.launch { sortPreferences.sortBy.set(sortBy) }
    }

    fun setSortAscending(ascending: Boolean) {
        viewModelScope.launch { sortPreferences.sortAscending.set(ascending) }
    }

    fun setViewMode(viewMode: String) {
        viewModelScope.launch { sortPreferences.viewMode.set(viewMode) }
    }

    fun setLayoutMode(layoutMode: String) {
        viewModelScope.launch { sortPreferences.layoutMode.set(layoutMode) }
    }

    fun setVisibleFields(fields: Set<String>) {
        viewModelScope.launch { sortPreferences.visibleFields.set(fields) }
    }

    fun setOnlyForFolderList(only: Boolean) {
        viewModelScope.launch { sortPreferences.onlyForFolderList.set(only) }
    }

    fun setShowAudioFiles(show: Boolean) {
        viewModelScope.launch { sortPreferences.showAudioFiles.set(show) }
    }

    val shortsSortBy: StateFlow<String> = sortPreferences.shortsSortBy.asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "date")

    val shortsSortAscending: StateFlow<Boolean> = sortPreferences.shortsSortAscending.asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val shortsLayoutMode: StateFlow<String> = sortPreferences.shortsLayoutMode.asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "grid")

    val shortsVisibleFields: StateFlow<Set<String>> = sortPreferences.shortsVisibleFields.asFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_VISIBLE_FIELDS)

    fun setShortsSortBy(sortBy: String) {
        viewModelScope.launch { sortPreferences.shortsSortBy.set(sortBy) }
    }

    fun setShortsSortAscending(ascending: Boolean) {
        viewModelScope.launch { sortPreferences.shortsSortAscending.set(ascending) }
    }

    fun setShortsLayoutMode(layoutMode: String) {
        viewModelScope.launch { sortPreferences.shortsLayoutMode.set(layoutMode) }
    }

    fun setShortsVisibleFields(fields: Set<String>) {
        viewModelScope.launch { sortPreferences.shortsVisibleFields.set(fields) }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                scanForVideosUseCase()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectTab(tab: HomeTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun getVideosInFolder(folderPath: String): Flow<List<VideoItem>> {
        return getVideosByFolderUseCase(folderPath)
    }

    fun hideVideosToSecureFolder(
        videos: List<VideoItem>,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        fileTransferManager.startTransfer(
            videos = videos,
            destination = null,
            type = com.finalplayer.app.data.transfer.TransferType.HIDE_TO_SECURE,
            runInBackground = false,
            onComplete = { success, msg ->
                if (success) {
                    refreshVideos()
                }
                onComplete(success, msg)
            }
        )
    }

    fun renameVideo(
        video: VideoItem,
        newName: String,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = videoRepository.renameVideo(video, newName, context)
            if (result.isSuccess) {
                onComplete(true, "تمت إعادة التسمية بنجاح")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "حدث خطأ أثناء إعادة التسمية"
                onComplete(false, "فشلت العملية: $msg")
            }
        }
    }

    fun moveVideos(
        videos: List<VideoItem>,
        destination: java.io.File,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        fileTransferManager.startTransfer(
            videos = videos,
            destination = destination,
            type = com.finalplayer.app.data.transfer.TransferType.MOVE,
            runInBackground = false,
            onComplete = { success, msg ->
                if (success) {
                    refreshVideos()
                }
                onComplete(success, msg)
            }
        )
    }

    fun copyVideos(
        videos: List<VideoItem>,
        destination: java.io.File,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        fileTransferManager.startTransfer(
            videos = videos,
            destination = destination,
            type = com.finalplayer.app.data.transfer.TransferType.COPY,
            runInBackground = false,
            onComplete = { success, msg ->
                if (success) {
                    refreshVideos()
                }
                onComplete(success, msg)
            }
        )
    }

    fun cancelTransfer() {
        fileTransferManager.cancelTransfer()
    }

    fun moveTransferToBackground() {
        fileTransferManager.moveToBackground()
    }

    fun deleteVideos(
        videos: List<VideoItem>,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = videoRepository.deleteVideos(videos, context)
            if (result.isSuccess) {
                onComplete(true, "تم حذف ${videos.size} ملف بنجاح")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "حدث خطأ أثناء الحذف"
                onComplete(false, "فشلت العملية: $msg")
            }
        }
    }

    fun renameFolder(
        folder: com.finalplayer.app.domain.model.VideoFolder,
        newName: String,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cleanPath = folder.path.replace("//", "/").trimEnd('/')
                val currentDir = java.io.File(cleanPath)
                val parentDir = currentDir.parentFile ?: java.io.File(cleanPath.substringBeforeLast('/'))
                val targetDir = java.io.File(parentDir, newName)

                val success = if (currentDir.exists()) {
                    currentDir.renameTo(targetDir)
                } else false

                if (success) {
                    com.finalplayer.app.utils.FileOperationsUtil.scanFile(context, targetDir)
                    scanForVideosUseCase()
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(true, "تمت إعادة تسمية المجلد بنجاح")
                    }
                } else {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onComplete(false, "تعذرت إعادة تسمية المجلد على وحدة التخزين")
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "فشلت العملية: ${e.message}")
                }
            }
        }
    }

    fun deleteFolders(
        folders: List<com.finalplayer.app.domain.model.VideoFolder>,
        context: android.content.Context,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var deletedCount = 0
                for (folder in folders) {
                    val cleanPath = folder.path.replace("//", "/").trimEnd('/')
                    val dir = java.io.File(cleanPath)
                    if (dir.exists()) {
                        dir.deleteRecursively()
                        deletedCount++
                    }
                }
                scanForVideosUseCase()
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(true, "تم حذف $deletedCount مجلد بنجاح")
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(false, "فشلت عملية الحذف: ${e.message}")
                }
            }
        }
    }
}

