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
        val scannedVideos = mediaStoreScanner.scanDeviceVideos()
        if (scannedVideos.isNotEmpty()) {
            videoDao.insertVideos(scannedVideos)
        }
    }

    override suspend fun deleteVideo(videoId: String) {
        videoDao.deleteVideo(videoId)
    }
}
