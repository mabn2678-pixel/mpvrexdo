package com.finalplayer.app.domain.usecase

import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.repository.PlaybackRepository

class SavePlaybackProgressUseCase(
    private val playbackRepository: PlaybackRepository
) {
    suspend operator fun invoke(progress: PlaybackProgress) {
        playbackRepository.saveProgress(progress)
    }
}
