package com.finalplayer.app.domain.usecase

import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class GetVideosByFolderUseCase(
    private val videoRepository: VideoRepository
) {
    operator fun invoke(folderPath: String): Flow<List<VideoItem>> {
        return videoRepository.getVideosByFolder(folderPath)
    }
}
