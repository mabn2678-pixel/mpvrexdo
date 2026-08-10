package com.finalplayer.app.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.music.data.local.LrcParser
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Artist
import com.finalplayer.app.music.data.model.Song
import com.finalplayer.app.music.data.repository.MusicRepository
import com.finalplayer.app.music.player.MusicController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(
    private val repository: MusicRepository,
    val musicController: MusicController
) : ViewModel() {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LrcParser.ParsedLrc?>(null)
    val currentLyrics: StateFlow<LrcParser.ParsedLrc?> = _currentLyrics.asStateFlow()

    init {
        loadMusic()
    }

    fun loadMusic() {
        viewModelScope.launch {
            _songs.value = repository.getAllSongs()
            _albums.value = repository.getAllAlbums()
            _artists.value = repository.getAllArtists()
        }
    }

    fun playSong(song: Song, queue: List<Song> = _songs.value) {
        musicController.playSong(song, queue)
        loadLyrics(song)
    }

    private fun loadLyrics(song: Song) {
        viewModelScope.launch {
            _currentLyrics.value = repository.getLyricsForSong(song)
        }
    }
}
