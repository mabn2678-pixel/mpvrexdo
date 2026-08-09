package com.finalplayer.app.domain.usecase

import com.finalplayer.app.domain.repository.VideoRepository

class ScanForVideosUseCase(
    private val videoRepository: VideoRepository
) {
    suspend operator fun invoke() {
        videoRepository.scanDeviceForVideos()
    }
}
