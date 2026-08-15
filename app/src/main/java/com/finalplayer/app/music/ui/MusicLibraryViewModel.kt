package com.finalplayer.app.music.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.music.data.db.MusicDatabase
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Artist
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.music.data.repository.MusicRepository
import com.finalplayer.app.music.player.MusicController
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicLibraryViewModel(
    private val repository: MusicRepository,
    val controller: MusicController
) : ViewModel() {

    val musicController: MusicController get() = controller

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _sortBy = MutableStateFlow("date") // "date", "title", "artist", "duration"
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _sortAscending = MutableStateFlow(false)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    val filteredSongs: StateFlow<List<Song>> = combine(
        _songs,
        _searchQuery,
        _sortBy,
        _sortAscending
    ) { songList, query, sort, asc ->
        val filtered = if (query.isBlank()) songList
        else songList.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true)
        }

        val sorted = when (sort) {
            "date" -> filtered.sortedBy { it.dateAdded }
            "artist" -> filtered.sortedBy { it.artist.lowercase() }
            "duration" -> filtered.sortedBy { it.duration }
            else -> filtered.sortedBy { it.title.lowercase() }
        }

        if (asc) sorted else sorted.reversed()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredAlbums: StateFlow<List<Album>> = combine(_albums, _searchQuery) { albumList, query ->
        if (query.isBlank()) albumList
        else albumList.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredArtists: StateFlow<List<Artist>> = combine(_artists, _searchQuery) { artistList, query ->
        if (query.isBlank()) artistList
        else artistList.filter {
            it.name.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAll()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTabChange(index: Int) {
        _currentTab.value = index
    }

    fun setSortBy(field: String) {
        if (_sortBy.value == field) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortBy.value = field
            _sortAscending.value = field == "title" || field == "artist"
        }
    }

    fun toggleSortOrder() {
        _sortAscending.value = !_sortAscending.value
    }

    fun setSortAscending(asc: Boolean) {
        _sortAscending.value = asc
    }

    fun refresh() {
        loadAll()
    }

    fun scanSongs() {
        refresh()
    }

    private fun loadAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val loadedSongs = repository.getAllSongs()
            val loadedAlbums = repository.getAllAlbums()
            val loadedArtists = repository.getAllArtists()
            _songs.value = loadedSongs
            _albums.value = loadedAlbums
            _artists.value = loadedArtists
            _isLoading.value = false
        }
    }

    fun playSong(song: Song, allSongs: List<Song> = filteredSongs.value) {
        val index = allSongs.indexOf(song).coerceAtLeast(0)
        controller.play(allSongs, index)
    }

    fun playAll() {
        val currentSongs = filteredSongs.value
        if (currentSongs.isNotEmpty()) {
            controller.play(currentSongs, 0)
        }
    }

    fun shuffleAll() {
        val currentSongs = filteredSongs.value
        if (currentSongs.isNotEmpty()) {
            val shuffled = currentSongs.shuffled()
            controller.play(shuffled, 0)
            if (!controller.state.value.shuffleEnabled) {
                controller.toggleShuffle()
            }
        }
    }

    fun deleteSongs(
        songsToDelete: List<Song>,
        context: Context,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        if (songsToDelete.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If currently playing song is deleted, stop playback
                val currentSong = controller.state.value.currentSong
                if (currentSong != null && songsToDelete.any { it.id == currentSong.id }) {
                    controller.stop()
                }

                // Remove from playlist database
                try {
                    val db = MusicDatabase.getInstance(context)
                    val dao = db.playlistDao()
                    for (s in songsToDelete) {
                        dao.removeSongFromAllPlaylists(s.id)
                    }
                } catch (_: Exception) {}

                // Delete physical files and clean MediaStore
                val result = FileOperationsUtil.deleteSongs(context, songsToDelete)

                // Update UI state immediately
                val deletedIds = songsToDelete.map { it.id }.toSet()
                _songs.value = _songs.value.filterNot { deletedIds.contains(it.id) }

                // Refresh everything
                loadAll()

                withContext(Dispatchers.Main) {
                    val message = if (songsToDelete.size == 1) {
                        "تم حذف الأغنية بنجاح"
                    } else {
                        "تم حذف ${songsToDelete.size} أغاني بنجاح"
                    }
                    onResult?.invoke(result.isSuccess, message)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, e.localizedMessage ?: "حدث خطأ أثناء الحذف")
                }
            }
        }
    }

    fun deleteSong(
        song: Song,
        context: Context,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        deleteSongs(listOf(song), context, onResult)
    }
}
