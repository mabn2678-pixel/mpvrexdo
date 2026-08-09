package com.finalplayer.app.domain.usecase

import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class GetVideoLibraryUseCase(
    private val videoRepository: VideoRepository
) {
    operator fun invoke(): Flow<List<VideoItem>> {
        return videoRepository.getAllVideos()
    }
}
