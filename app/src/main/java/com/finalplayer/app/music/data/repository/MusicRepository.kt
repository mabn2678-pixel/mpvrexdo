package com.finalplayer.app.music.data.repository

import com.finalplayer.app.music.data.local.LrcParser
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Artist
import com.finalplayer.app.music.data.model.Song

interface MusicRepository {
    suspend fun getAllSongs(): List<Song>
    suspend fun getAllAlbums(): List<Album>
    suspend fun getAllArtists(): List<Artist>
    suspend fun getSongsByAlbum(albumId: Long): List<Song>
    suspend fun getSongsByArtist(artistName: String): List<Song>
    suspend fun searchSongs(query: String): List<Song>
    suspend fun getLyricsForSong(song: Song): LrcParser.ParsedLrc?
}
