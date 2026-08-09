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
import kotlinx.coroutines.launch
import java.util.Locale

data class FolderSortConfig(
    val sortBy: String = "title",
    val sortAscending: Boolean = true,
    val layoutMode: String = "list",
    val visibleFields: Set<String> = setOf("Path", "Folder Size", "Total Media")
)

class HomeViewModel(
    private val getVideoLibraryUseCase: GetVideoLibraryUseCase,
    private val scanForVideosUseCase: ScanForVideosUseCase,
    private val getVideosByFolderUseCase: GetVideosByFolderUseCase,
    private val videoRepository: VideoRepository,
    private val sortPreferences: SortPreferences,
    private val playbackRepository: PlaybackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _folderConfigs = MutableStateFlow<Map<String, FolderSortConfig>>(emptyMap())
    val folderConfigs: StateFlow<Map<String, FolderSortConfig>> = _folderConfigs.asStateFlow()

    fun getFolderConfig(folderPath: String): FolderSortConfig {
        return _folderConfigs.value[folderPath] ?: FolderSortConfig()
    }

    fun updateFolderConfig(folderPath: String, config: FolderSortConfig) {
        _folderConfigs.update { it + (folderPath to config) }
    }

    private var hasAutoScanned = false

    init {
        observeFoldersAndSort()
        refreshVideos()
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

                _uiState.value.copy(
                    folders = finalFolders,
                    allVideos = finalVideos,
                    playedVideoIds = playedIds,
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
}

