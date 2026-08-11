package com.finalplayer.app.music.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.finalplayer.app.music.data.model.Album
import com.finalplayer.app.music.data.model.Artist
import com.finalplayer.app.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    private val audioExtensions = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus", "m4b", "wma")

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songMap = LinkedHashMap<String, Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE
        )

        // Relaxed selection to catch freshly downloaded songs where IS_MUSIC or DURATION may not be updated in MediaStore yet
        val selection = "(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.mp3' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.m4a' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.flac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.wav' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.ogg' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.aac' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.opus' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.m4b' OR " +
                "${MediaStore.Audio.Media.DATA} LIKE '%.wma') AND " +
                "(${MediaStore.Audio.Media.SIZE} > 20000 OR ${MediaStore.Audio.Media.SIZE} IS NULL)"

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    var duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(pathColumn) ?: ""
                    val trackNumber = cursor.getInt(trackColumn)
                    val year = cursor.getInt(yearColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val dateModified = cursor.getLong(dateModifiedColumn)
                    val size = cursor.getLong(sizeColumn)

                    if (path.isEmpty()) continue
                    val fileOnDisk = File(path)
                    if (!fileOnDisk.exists() || !fileOnDisk.isFile) continue

                    val canonicalPath = try { fileOnDisk.canonicalPath } catch (e: Exception) { path }

                    val fileLmSeconds = fileOnDisk.lastModified() / 1000L
                    val trueDateAdded = maxOf(dateAdded, dateModified, fileLmSeconds)

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId")

                    // Fallback to extract metadata if title or duration is missing in MediaStore
                    var finalTitle = title
                    var finalArtist = artist
                    var finalAlbum = album

                    if (duration <= 0 || title == "Unknown Title" || title.isBlank()) {
                        val mmr = MediaMetadataRetriever()
                        try {
                            mmr.setDataSource(canonicalPath)
                            if (duration <= 0) {
                                val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                duration = durStr?.toLongOrNull() ?: 0L
                            }
                            if (finalTitle == "Unknown Title" || finalTitle.isBlank()) {
                                val tStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                if (!tStr.isNullOrBlank()) finalTitle = tStr else finalTitle = fileOnDisk.nameWithoutExtension
                            }
                            if (finalArtist == "Unknown Artist" || finalArtist.isBlank()) {
                                val aStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                if (!aStr.isNullOrBlank()) finalArtist = aStr
                            }
                            if (finalAlbum == "Unknown Album" || finalAlbum.isBlank()) {
                                val albStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                if (!albStr.isNullOrBlank()) finalAlbum = albStr
                            }
                        } catch (e: Exception) {
                            if (finalTitle == "Unknown Title" || finalTitle.isBlank()) {
                                finalTitle = fileOnDisk.nameWithoutExtension
                            }
                        } finally {
                            try { mmr.release() } catch (e: Exception) {}
                        }
                    }

                    songMap[canonicalPath] = Song(
                        id = id,
                        title = finalTitle,
                        artist = finalArtist,
                        album = finalAlbum,
                        albumId = albumId,
                        duration = duration,
                        path = canonicalPath,
                        uri = uri,
                        albumArtUri = albumArtUri,
                        trackNumber = trackNumber,
                        year = year,
                        dateAdded = trueDateAdded,
                        size = size
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct Storage Directory Scan for newly downloaded files not yet indexed by MediaStore
        scanPhysicalDirectories(songMap)

        songMap.values.toList()
    }

    private fun scanPhysicalDirectories(songMap: LinkedHashMap<String, Song>) {
        val rootStorage = Environment.getExternalStorageDirectory() ?: return
        val targetDirectories = listOf(
            File(rootStorage, "Music"),
            File(rootStorage, "Download"),
            File(rootStorage, "Downloads"),
            File(rootStorage, "Audio"),
            File(rootStorage, "Podcasts"),
            File(rootStorage, "Audiobooks"),
            File(rootStorage, "Telegram"),
            File(rootStorage, "WhatsApp/Media/WhatsApp Audio"),
            File(rootStorage, "Snaptube"),
            File(rootStorage, "Vidmate"),
            rootStorage
        )

        val scannedFiles = mutableListOf<File>()

        for (dir in targetDirectories) {
            if (dir.exists() && dir.isDirectory) {
                val maxDepth = if (dir == rootStorage) 1 else 3
                scanDirectoryRecursive(dir, scannedFiles, 0, maxDepth)
            }
        }

        for (file in scannedFiles) {
            val canonicalPath = try { file.canonicalPath } catch (e: Exception) { file.absolutePath }
            if (songMap.containsKey(canonicalPath)) continue
            if (file.length() < 20000) continue // Skip files smaller than 20KB

            // Trigger MediaScannerConnection so MediaStore indexes the file for future queries
            try {
                MediaScannerConnection.scanFile(context, arrayOf(canonicalPath), null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Parse metadata with MediaMetadataRetriever
            val mmr = MediaMetadataRetriever()
            var title = file.nameWithoutExtension
            var artist = "Unknown Artist"
            var album = "Unknown Album"
            var duration = 0L
            var trackNumber = 0
            var year = 0

            try {
                mmr.setDataSource(canonicalPath)
                val tStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!tStr.isNullOrBlank()) title = tStr

                val aStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!aStr.isNullOrBlank()) artist = aStr

                val albStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                if (!albStr.isNullOrBlank()) album = albStr

                val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durStr?.toLongOrNull() ?: 0L

                val trkStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                trackNumber = trkStr?.toIntOrNull() ?: 0

                val yrStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                year = yrStr?.toIntOrNull() ?: 0
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { mmr.release() } catch (e: Exception) {}
            }

            val fileHashId = (canonicalPath.hashCode().toLong() and 0x7FFFFFFF) + 1_000_000_000L
            val albumHashId = album.hashCode().toLong() and 0x7FFFFFFF

            songMap[canonicalPath] = Song(
                id = fileHashId,
                title = title,
                artist = artist,
                album = album,
                albumId = albumHashId,
                duration = duration,
                path = canonicalPath,
                uri = Uri.fromFile(file),
                albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumHashId"),
                trackNumber = trackNumber,
                year = year,
                dateAdded = file.lastModified() / 1000L,
                size = file.length()
            )
        }
    }

    private fun scanDirectoryRecursive(dir: File, result: MutableList<File>, currentDepth: Int, maxDepth: Int) {
        if (currentDepth > maxDepth) return
        val files = dir.listFiles() ?: return

        for (file in files) {
            if (file.name.startsWith(".")) continue // Skip hidden files/folders
            if (file.isDirectory) {
                scanDirectoryRecursive(file, result, currentDepth + 1, maxDepth)
            } else if (file.isFile) {
                val ext = file.extension.lowercase()
                if (audioExtensions.contains(ext)) {
                    result.add(file)
                }
            }
        }
    }

    suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(albumColumn) ?: "Unknown Album"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val songCount = cursor.getInt(songCountColumn)
                    val year = cursor.getInt(yearColumn)
                    val albumArtUri = Uri.parse("content://media/external/audio/albumart/$id")

                    albums.add(
                        Album(
                            id = id,
                            title = title,
                            artist = artist,
                            songCount = songCount,
                            year = year,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        albums
    }

    suspend fun scanArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()
        val collection = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
                val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val albumCount = cursor.getInt(albumCountColumn)
                    val songCount = cursor.getInt(trackCountColumn)

                    artists.add(
                        Artist(
                            id = id,
                            name = name,
                            albumCount = albumCount,
                            songCount = songCount
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        artists
    }

    suspend fun getSongsByAlbum(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
        scanSongs().filter { it.albumId == albumId }
    }

    suspend fun getSongsByArtist(artistName: String): List<Song> = withContext(Dispatchers.IO) {
        scanSongs().filter { it.artist.equals(artistName, ignoreCase = true) }
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        scanSongs().filter {
            it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
        }
    }
}

