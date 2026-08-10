package com.finalplayer.app.music.data.repository

import android.content.Context
import com.finalplayer.app.music.data.local.LrcParser
import com.finalplayer.app.music.data.local.MediaStoreScanner
import com.finalplayer.app.music.data.local.PreviewSongs
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Artist
import com.finalplayer.app.music.data.model.Song

class MusicRepositoryImpl(
    private val scanner: MediaStoreScanner,
    private val lrcParser: LrcParser,
    private val context: Context? = null
) : MusicRepository {

    constructor(context: Context, scanner: MediaStoreScanner, lrcParser: LrcParser) : this(scanner, lrcParser, context)

    override suspend fun getAllSongs(): List<Song> {
        val realSongs = scanner.scanSongs()
        return if (realSongs.isEmpty()) {
            if (context != null) {
                PreviewSongs.getPreviewSongs(context) + realSongs
            } else {
                realSongs
            }
        } else {
            realSongs
        }
    }

    override suspend fun getAllAlbums(): List<Album> {
        val realAlbums = scanner.scanAlbums()
        if (realAlbums.isNotEmpty()) return realAlbums
        val songs = getAllSongs()
        return songs.groupBy { it.albumId }.map { (albumId, songList) ->
            val first = songList.first()
            Album(
                id = albumId,
                title = first.album,
                artist = first.artist,
                songCount = songList.size,
                albumArtUri = first.albumArtUri,
                year = first.year
            )
        }
    }

    override suspend fun getAllArtists(): List<Artist> {
        val songs = getAllSongs()
        val realArtists = scanner.scanArtists()

        val artistMap = LinkedHashMap<String, MutableList<Song>>()

        // Populate with real artists from scanner
        for (artist in realArtists) {
            if (artist.name.isNotBlank() && !artist.name.contains("<unknown>", ignoreCase = true)) {
                artistMap.putIfAbsent(artist.name, mutableListOf())
            }
        }

        val artistSplitRegex = Regex("[,&;/]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\band\\b", RegexOption.IGNORE_CASE)

        for (song in songs) {
            val artistStr = song.artist.trim()
            if (artistStr.isBlank() || artistStr.contains("<unknown>", ignoreCase = true)) continue

            val tokens = artistStr.split(artistSplitRegex)
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.contains("<unknown>", ignoreCase = true) }

            if (tokens.isEmpty()) {
                val key = artistMap.keys.firstOrNull { it.equals(artistStr, ignoreCase = true) } ?: artistStr
                artistMap.getOrPut(key) { mutableListOf() }.add(song)
            } else {
                for (token in tokens) {
                    val key = artistMap.keys.firstOrNull { it.equals(token, ignoreCase = true) } ?: token
                    artistMap.getOrPut(key) { mutableListOf() }.add(song)
                }
            }
        }

        return artistMap.entries.mapIndexed { index, (artistName, songList) ->
            val distinctSongs = songList.distinctBy { it.id }
            val firstArt = distinctSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri
            val albumCount = distinctSongs.map { it.albumId }.distinct().size
            Artist(
                id = index + 1L,
                name = artistName,
                songCount = distinctSongs.size,
                albumCount = albumCount,
                albumArtUri = firstArt
            )
        }.sortedBy { it.name.lowercase() }
    }

    override suspend fun getSongsByAlbum(albumId: Long): List<Song> = scanner.getSongsByAlbum(albumId)

    override suspend fun getSongsByArtist(artistName: String): List<Song> {
        val songs = getAllSongs()
        val target = artistName.trim().lowercase()
        val splitRegex = Regex("[,&;/]|\\bfeat\\.?\\b|\\bft\\.?\\b|\\band\\b", RegexOption.IGNORE_CASE)

        return songs.filter { song ->
            val songArtist = song.artist.trim().lowercase()
            if (songArtist == target || songArtist.contains(target)) return@filter true
            val tokens = songArtist.split(splitRegex).map { it.trim() }
            tokens.any { it == target || (it.length > 2 && target.length > 2 && (it.contains(target) || target.contains(it))) }
        }
    }

    override suspend fun searchSongs(query: String): List<Song> = scanner.searchSongs(query)

    override suspend fun getLyricsForSong(song: Song): LrcParser.ParsedLrc? = lrcParser.findLrcForSong(song)
}
