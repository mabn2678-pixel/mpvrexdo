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
import java.io.File

class VideoRepositoryImpl(
    private val videoDao: VideoDao,
    private val secureMediaDao: SecureMediaDao,
    private val mediaStoreScanner: MediaStoreVideoScanner
) : VideoRepository {

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
        return videoDao.getAllVideos()
            .combine(secureMediaDao.getAllSecureVideoIds()) { videos, secureIds ->
                val secureSet = secureIds.toSet()
                videos
                    .filter { it.id in secureSet }
                    .map { it.toDomainModel() }
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
                    coverThumbnail = null
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

    override suspend fun scanDeviceForVideos() {
        videoDao.deleteMockVideos()
        val scannedVideos = mediaStoreScanner.scanDeviceVideos()
        if (scannedVideos.isNotEmpty()) {
            videoDao.insertVideos(scannedVideos)
            videoDao.deleteMissingVideos(scannedVideos.map { it.id })
        } else {
            videoDao.clearAllVideos()
        }
    }

    override suspend fun deleteVideo(videoId: String) {
        videoDao.deleteVideo(videoId)
    }

    override suspend fun hideVideosToSecureFolder(videos: List<VideoItem>, context: android.content.Context): Result<Unit> {
        return try {
            val files = videos.map { FileOperationsUtil.getVideoFile(it) }
            FileOperationsUtil.hideFiles(context, files)
            for (video in videos) {
                val fullPath = if (video.folderPath.isNotBlank()) "${video.folderPath}/${video.title}" else video.uri
                secureMediaDao.insert(
                    SecureMediaEntity(
                        videoId = video.id,
                        originalPath = fullPath
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameVideo(video: VideoItem, newName: String, context: android.content.Context): Result<File> {
        return try {
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

    override suspend fun moveVideos(videos: List<VideoItem>, destination: File, context: android.content.Context): Result<List<File>> {
        return try {
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

    override suspend fun copyVideos(videos: List<VideoItem>, destination: File, context: android.content.Context): Result<List<File>> {
        return try {
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

    override suspend fun deleteVideos(videos: List<VideoItem>, context: android.content.Context): Result<Unit> {
        return try {
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
