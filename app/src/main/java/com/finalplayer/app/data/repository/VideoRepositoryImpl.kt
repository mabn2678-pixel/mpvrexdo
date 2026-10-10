package com.finalplayer.app.data.repository

import com.finalplayer.app.data.database.dao.SecureMediaDao
import com.finalplayer.app.data.database.dao.VideoDao
import com.finalplayer.app.data.mapper.toDomainModel
import com.finalplayer.app.data.mapper.toVideoFolders
import com.finalplayer.app.domain.model.SearchResults
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

import com.finalplayer.app.data.database.entities.SecureMediaEntity
import com.finalplayer.app.data.mapper.toEntity
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class VideoRepositoryImpl(
    private val videoDao: VideoDao,
    private val secureMediaDao: SecureMediaDao,
    private val mediaStoreScanner: MediaStoreVideoScanner
) : VideoRepository {

    private fun getVaultDirForFile(context: android.content.Context, file: File?): File {
        return FileOperationsUtil.getVaultDir(context, file)
    }

    override fun getAllVideos(): Flow<List<VideoItem>> {
        return videoDao.getAllVideos()
            .combine(secureMediaDao.getAllSecureVideoIds()) { videos, secureIds ->
                val secureSet = secureIds.toSet()
                videos
                    .filter { it.id !in secureSet }
                    .map { it.toDomainModel() }
            }
    }

    override fun getSecureVideos(): Flow<List<VideoItem>> {
        return secureMediaDao.getAllSecureMedia().map { entities ->
            entities.map { entity ->
                val displayUri = if (entity.vaultPath.isNotBlank() && File(entity.vaultPath).exists()) {
                    entity.vaultPath
                } else if (entity.originalPath.isNotBlank() && File(entity.originalPath).exists()) {
                    entity.originalPath
                } else {
                    entity.vaultPath.ifBlank { entity.originalPath }
                }

                val displayTitle = if (entity.title.isNotBlank()) {
                    entity.title
                } else {
                    File(entity.originalPath).name.ifBlank { "فيديو محمي" }
                }

                VideoItem(
                    id = entity.videoId,
                    uri = displayUri,
                    title = displayTitle,
                    duration = entity.duration,
                    sizeBytes = entity.sizeBytes,
                    thumbnailPath = displayUri,
                    dateAdded = entity.dateAdded,
                    resolution = entity.resolution,
                    folderPath = "المجلد الآمن"
                )
            }
        }
    }

    override fun getVideosByFolder(folderPath: String): Flow<List<VideoItem>> {
        return videoDao.getVideosByFolder(folderPath)
            .combine(secureMediaDao.getAllSecureVideoIds()) { entities, secureIds ->
                val secureSet = secureIds.toSet()
                entities.filter { it.id !in secureSet }.map { it.toDomainModel() }
            }
    }

    override fun getAllFolders(): Flow<List<VideoFolder>> {
        return getAllVideos().map { videos ->
            videos.groupBy { it.folderPath }.map { (path, folderVideos) ->
                val folderFile = java.io.File(path)
                val folderDate = if (folderFile.exists() && folderFile.lastModified() > 0) {
                    folderFile.lastModified()
                } else {
                    val maxDate = folderVideos.maxOfOrNull { it.dateAdded } ?: 0L
                    if (maxDate > 0 && maxDate < 100_000_000_000L) maxDate * 1000L else maxDate
                }
                VideoFolder(
                    path = path,
                    name = path.substringAfterLast("/"),
                    videoCount = folderVideos.size,
                    totalDuration = folderVideos.sumOf { it.duration },
                    totalSizeBytes = folderVideos.sumOf { it.sizeBytes },
                    lastModified = folderDate,
                    coverThumbnail = folderVideos.firstOrNull { it.thumbnailPath != null }?.thumbnailPath ?: folderVideos.firstOrNull()?.uri,
                    previewThumbnails = folderVideos.take(3).map { it.thumbnailPath ?: it.uri }
                )
            }.sortedByDescending { it.lastModified }
        }
    }

    override fun search(query: String): Flow<SearchResults> {
        return videoDao.searchVideos("%$query%")
            .combine(secureMediaDao.getAllSecureVideoIds()) { entities, secureIds ->
                val secureSet = secureIds.toSet()
                val filtered = entities.filter { it.id !in secureSet }
                val videos = filtered.map { it.toDomainModel() }
                val folders = filtered.toVideoFolders()
                SearchResults(folders = folders, videos = videos, isEmpty = videos.isEmpty())
            }
    }

    override suspend fun scanDeviceForVideos() = withContext(Dispatchers.IO) {
        videoDao.deleteMockVideos()
        val scannedVideos = mediaStoreScanner.scanDeviceVideos()
        if (scannedVideos.isNotEmpty()) {
            videoDao.insertVideos(scannedVideos)
            videoDao.deleteMissingVideos(scannedVideos.map { it.id })
            
            try {
                val zeroDurationList = videoDao.getZeroDurationVideos()
                if (zeroDurationList.isNotEmpty()) {
                    val repaired = zeroDurationList.mapNotNull { entity ->
                        val f = if (entity.uri.startsWith("content://")) null else File(entity.uri)
                        val path = f?.absolutePath ?: if (entity.folderPath.isNotBlank()) "${entity.folderPath}/${entity.title}" else ""
                        val dur = mediaStoreScanner.extractDurationForVideo(path, entity.uri)
                        if (dur > 0L) entity.copy(duration = dur) else null
                    }
                    if (repaired.isNotEmpty()) {
                        videoDao.insertVideos(repaired)
                    }
                }
            } catch (_: Throwable) {}
        } else {
            videoDao.clearAllVideos()
        }
    }

    override suspend fun deleteVideo(videoId: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideo(videoId)
    }

    override suspend fun hideVideosToSecureFolder(videos: List<VideoItem>, context: android.content.Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (video in videos) {
                val originalFile = FileOperationsUtil.getVideoFile(video, context)
                val originalPathStr = if (originalFile.exists()) originalFile.absolutePath else if (video.folderPath.isNotBlank()) "${video.folderPath}/${video.title}" else video.uri
                
                val vaultDir = FileOperationsUtil.getVaultDir(context, if (originalFile.exists()) originalFile else null)
                val safeId = video.id.replace(Regex("[^a-zA-Z0-9_]"), "_")
                val vaultFile = File(vaultDir, ".sec_${safeId}_${System.currentTimeMillis()}.vlt")

                var finalVaultPath = originalPathStr
                var moved = false
                if (originalFile.exists()) {
                    moved = try {
                        originalFile.renameTo(vaultFile)
                    } catch (_: Exception) {
                        false
                    }
                    if (!moved) {
                        try {
                            java.nio.file.Files.move(
                                originalFile.toPath(),
                                vaultFile.toPath(),
                                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                                java.nio.file.StandardCopyOption.REPLACE_EXISTING
                            )
                            moved = vaultFile.exists() && vaultFile.length() > 0L
                        } catch (_: Exception) {
                            try {
                                java.nio.file.Files.move(
                                    originalFile.toPath(),
                                    vaultFile.toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                                )
                                moved = vaultFile.exists() && vaultFile.length() > 0L
                            } catch (_: Exception) {}
                        }
                    }
                    if (!moved) {
                        try {
                            FileInputStream(originalFile).use { fis ->
                                FileOutputStream(vaultFile).use { fos ->
                                    val inChannel = fis.channel
                                    val outChannel = fos.channel
                                    val size = inChannel.size()
                                    var position = 0L
                                    val chunkSize = 4 * 1024 * 1024L
                                    while (position < size) {
                                        val transferred = inChannel.transferTo(position, (size - position).coerceAtMost(chunkSize), outChannel)
                                        if (transferred <= 0) break
                                        position += transferred
                                    }
                                    fos.flush()
                                }
                            }
                            originalFile.delete()
                            moved = true
                        } catch (e: Exception) {
                            false
                        }
                    }
                } else if (video.uri.startsWith("content://")) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(android.net.Uri.parse(video.uri))
                        if (inputStream != null) {
                            inputStream.use { input ->
                                FileOutputStream(vaultFile).use { output ->
                                    input.copyTo(output, bufferSize = 128 * 1024)
                                }
                            }
                            try {
                                context.contentResolver.delete(android.net.Uri.parse(video.uri), null, null)
                            } catch (_: Exception) {}
                            moved = true
                        }
                    } catch (_: Exception) {}
                }

                if (moved && vaultFile.exists()) {
                    finalVaultPath = vaultFile.absolutePath
                    try {
                        if (!originalFile.exists()) {
                            android.media.MediaScannerConnection.scanFile(
                                context,
                                arrayOf(originalFile.absolutePath),
                                null,
                                null
                            )
                        }
                    } catch (_: Exception) {}
                }

                val actualSize = if (File(finalVaultPath).exists()) File(finalVaultPath).length() else video.sizeBytes

                secureMediaDao.insert(
                    SecureMediaEntity(
                        videoId = video.id,
                        title = video.title,
                        vaultPath = finalVaultPath,
                        originalPath = originalPathStr,
                        duration = video.duration,
                        sizeBytes = actualSize,
                        dateAdded = video.dateAdded,
                        resolution = video.resolution,
                        folderPath = video.folderPath,
                        addedAt = System.currentTimeMillis()
                    )
                )

                // Remove from regular video database
                videoDao.deleteVideo(video.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreVideoFromSecureFolder(videoId: String, context: android.content.Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = secureMediaDao.getByVideoId(videoId) ?: return@withContext Result.failure(Exception("الملف غير موجود في المجلد الآمن"))
            val vaultFile = File(entity.vaultPath)
            val originalTargetFile = File(entity.originalPath)
            val parent = originalTargetFile.parentFile ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
            if (!parent.exists()) {
                parent.mkdirs()
            }
            val destinationFile = File(parent, originalTargetFile.name.removePrefix("."))

            if (vaultFile.exists()) {
                val restored = if (vaultFile.renameTo(destinationFile)) {
                    true
                } else {
                    try {
                        FileInputStream(vaultFile).use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output, bufferSize = 128 * 1024)
                            }
                        }
                        vaultFile.delete()
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (restored && destinationFile.exists()) {
                    try {
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destinationFile.absolutePath),
                            arrayOf("video/*"),
                            null
                        )
                    } catch (_: Exception) {}

                    val videoEntity = com.finalplayer.app.data.database.entities.VideoEntity(
                        id = entity.videoId,
                        title = entity.title.ifBlank { destinationFile.name },
                        uri = destinationFile.absolutePath,
                        duration = entity.duration,
                        sizeBytes = destinationFile.length(),
                        dateAdded = if (entity.dateAdded > 0) entity.dateAdded else (System.currentTimeMillis() / 1000L),
                        resolution = entity.resolution,
                        folderPath = parent.absolutePath,
                        mimeType = "video/*"
                    )
                    videoDao.insertVideos(listOf(videoEntity))
                }
            }

            secureMediaDao.remove(videoId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreVideosFromSecureFolder(videos: List<VideoItem>, context: android.content.Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (video in videos) {
                restoreVideoFromSecureFolder(video.id, context)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSecureVideos(videos: List<VideoItem>, context: android.content.Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (video in videos) {
                val entity = secureMediaDao.getByVideoId(video.id)
                if (entity != null) {
                    if (entity.vaultPath.isNotBlank()) {
                        val vf = File(entity.vaultPath)
                        if (vf.exists()) vf.delete()
                    }
                    secureMediaDao.remove(video.id)
                } else {
                    val f = File(video.uri)
                    if (f.exists()) f.delete()
                    secureMediaDao.remove(video.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameVideo(video: VideoItem, newName: String, context: android.content.Context): Result<File> = withContext(Dispatchers.IO) {
        try {
            val file = FileOperationsUtil.getVideoFile(video)
            val result = FileOperationsUtil.renameFile(context, file, newName)
            val targetFile = result.getOrNull() ?: File(file.parentFile ?: File(video.folderPath), newName)

            val updatedEntity = video.toEntity().copy(
                title = newName,
                uri = if (targetFile.exists()) targetFile.absolutePath else video.uri
            )
            videoDao.insertVideos(listOf(updatedEntity))
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveVideos(videos: List<VideoItem>, destination: File, context: android.content.Context): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = videos.map { FileOperationsUtil.getVideoFile(it) }
            val diskResult = FileOperationsUtil.moveFiles(context, files, destination)
            val movedFiles = diskResult.getOrDefault(emptyList())

            val updatedEntities = videos.map { video ->
                val targetFile = File(destination, video.title)
                video.toEntity().copy(
                    folderPath = destination.absolutePath,
                    uri = if (targetFile.exists()) targetFile.absolutePath else "${destination.absolutePath}/${video.title}"
                )
            }
            videoDao.insertVideos(updatedEntities)
            Result.success(movedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun copyVideos(videos: List<VideoItem>, destination: File, context: android.content.Context): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            val files = videos.map { FileOperationsUtil.getVideoFile(it) }
            val diskResult = FileOperationsUtil.copyFiles(context, files, destination)
            val copiedFiles = diskResult.getOrDefault(emptyList())

            val newEntities = videos.mapIndexed { idx, video ->
                val targetFile = File(destination, video.title)
                video.toEntity().copy(
                    id = "${video.id}_copy_${System.currentTimeMillis()}_$idx",
                    folderPath = destination.absolutePath,
                    uri = if (targetFile.exists()) targetFile.absolutePath else "${destination.absolutePath}/${video.title}",
                    dateAdded = System.currentTimeMillis() / 1000L
                )
            }
            videoDao.insertVideos(newEntities)
            Result.success(copiedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteVideos(videos: List<VideoItem>, context: android.content.Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val files = videos.map { FileOperationsUtil.getVideoFile(it) }
            FileOperationsUtil.deleteFiles(context, files)
            for (video in videos) {
                videoDao.deleteVideo(video.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
