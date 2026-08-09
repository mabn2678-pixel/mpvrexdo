package com.finalplayer.app.domain.repository

import com.finalplayer.app.domain.model.SearchResults
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    fun getAllVideos(): Flow<List<VideoItem>>
    fun getSecureVideos(): Flow<List<VideoItem>>
    fun getVideosByFolder(folderPath: String): Flow<List<VideoItem>>
    fun getAllFolders(): Flow<List<VideoFolder>>
    fun search(query: String): Flow<SearchResults>
    suspend fun scanDeviceForVideos()
    suspend fun deleteVideo(videoId: String)
}
