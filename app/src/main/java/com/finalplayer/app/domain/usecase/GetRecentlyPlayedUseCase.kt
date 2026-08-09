package com.finalplayer.app.domain.usecase

import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow

class GetRecentlyPlayedUseCase(
    private val playbackRepository: PlaybackRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<PlaybackProgress>> {
        return playbackRepository.getRecentlyPlayed(limit)
    }
}
