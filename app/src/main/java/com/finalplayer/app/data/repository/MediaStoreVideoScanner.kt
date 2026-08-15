package com.finalplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.finalplayer.app.data.database.entities.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreVideoScanner(private val context: Context) {

    suspend fun scanDeviceVideos(): List<VideoEntity> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoEntity>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(nameColumn) ?: "Unknown Video"
                    val duration = c.getLong(durationColumn)
                    val sizeBytes = c.getLong(sizeColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val dateModified = c.getLong(dateModifiedColumn)
                    val fullPath = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: "video/*"

                    val fileOnDisk = if (fullPath.isNotEmpty()) File(fullPath) else null
                    val fileLmSeconds = if (fileOnDisk != null && fileOnDisk.exists()) fileOnDisk.lastModified() / 1000L else 0L
                    val trueDateAdded = maxOf(dateAdded, dateModified, fileLmSeconds)

                    val folderPath = if (fullPath.isNotEmpty()) {
                        fileOnDisk?.parent ?: "/storage/emulated/0"
                    } else {
                        "/storage/emulated/0"
                    }

                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else null
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                    val finalDuration = if (duration > 0L) {
                        duration
                    } else {
                        extractDurationForVideo(fullPath, uri)
                    }

                    videoList.add(
                        VideoEntity(
                            id = id.toString(),
                            uri = uri,
                            title = title,
                            duration = finalDuration,
                            sizeBytes = sizeBytes,
                            thumbnailPath = null,
                            dateAdded = trueDateAdded,
                            resolution = resolution,
                            folderPath = folderPath,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Direct directory scan for newly downloaded videos that MediaStore hasn't indexed yet
        scanPhysicalDirectories(videoList)

        videoList
    }

    private fun scanPhysicalDirectories(videoList: MutableList<VideoEntity>) {
        val existingPaths = videoList.mapTo(mutableSetOf()) { 
            if (it.uri.startsWith("file://")) it.uri.substring(7) else it.uri 
        }
        
        videoList.forEach {
            if (it.folderPath.isNotBlank()) {
                existingPaths.add("${it.folderPath}/${it.title}")
            }
        }

        val videoExtensions = setOf("mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "m4v", "ts", "wmv", "asf")

        val storageRoot = File("/storage/emulated/0")
        val directoriesToScan = mutableListOf(
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DCIM),
            android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Downloads"),
            File("/storage/emulated/0/Movies"),
            File("/storage/emulated/0/DCIM"),
            File("/storage/emulated/0/DCIM/Camera"),
            File("/storage/emulated/0/DCIM/Screenrecordings"),
            File("/storage/emulated/0/DCIM/ScreenRecorder"),
            File("/storage/emulated/0/Pictures"),
            File("/storage/emulated/0/Video"),
            File("/storage/emulated/0/Videos"),
            File("/storage/emulated/0/Telegram"),
            File("/storage/emulated/0/Telegram/Telegram Video"),
            File("/storage/emulated/0/WhatsApp/Media/WhatsApp Video"),
            File("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"),
            File("/storage/emulated/0/Android/media/org.telegram.messenger/Telegram/Telegram Video"),
            File("/storage/emulated/0/Snaptube"),
            File("/storage/emulated/0/Vidmate"),
            File("/storage/emulated/0/Xender"),
            File("/storage/emulated/0/SHAREit"),
            File("/storage/emulated/0/ADM"),
            File("/storage/emulated/0/Bluetooth")
        )

        if (storageRoot.exists() && storageRoot.isDirectory) {
            storageRoot.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.startsWith(".") && file.name != "Android") {
                    directoriesToScan.add(file)
                }
            }
        }

        val unindexedFiles = mutableListOf<File>()
        val validDirs = directoriesToScan.filter { it.exists() && it.isDirectory }.distinctBy { it.canonicalPath }

        for (dir in validDirs) {
            try {
                dir.walkTopDown()
                    .onEnter { subDir ->
                        val name = subDir.name
                        !name.startsWith(".") && name != "Android" && name != "cache"
                    }
                    .maxDepth(3)
                    .filter { file ->
                        file.isFile && 
                        file.extension.lowercase() in videoExtensions && 
                        file.length() > 0 &&
                        !file.name.startsWith(".")
                    }
                    .forEach { file ->
                        val path = file.absolutePath
                        if (!existingPaths.contains(path)) {
                            existingPaths.add(path)
                            unindexedFiles.add(file)

                            val parentPath = file.parent ?: "/storage/emulated/0"
                            val fileDuration = extractDurationForVideo(path, "")
                            val entity = VideoEntity(
                                id = "file_${path.hashCode()}",
                                uri = file.absolutePath,
                                title = file.name,
                                duration = fileDuration,
                                sizeBytes = file.length(),
                                thumbnailPath = null,
                                dateAdded = file.lastModified() / 1000L,
                                resolution = null,
                                folderPath = parentPath,
                                mimeType = "video/${file.extension.lowercase()}"
                            )
                            videoList.add(entity)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Trigger MediaScanner for newly discovered files so Android system indexes them
        if (unindexedFiles.isNotEmpty()) {
            val paths = unindexedFiles.map { it.absolutePath }.toTypedArray()
            try {
                android.media.MediaScannerConnection.scanFile(context, paths, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun extractDurationForVideo(filePath: String?, uriString: String): Long {
        var retriever: android.media.MediaMetadataRetriever? = null
        return try {
            retriever = android.media.MediaMetadataRetriever()
            if (!filePath.isNullOrEmpty() && File(filePath).exists()) {
                retriever.setDataSource(filePath)
            } else if (uriString.isNotEmpty()) {
                retriever.setDataSource(context, android.net.Uri.parse(uriString))
            } else {
                return 0L
            }
            val durStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            try {
                retriever?.release()
            } catch (_: Throwable) {}
        }
    }
}
