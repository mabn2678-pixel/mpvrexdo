package com.finalplayer.app.domain.repository

import com.finalplayer.app.domain.model.PlaybackProgress
import kotlinx.coroutines.flow.Flow

interface PlaybackRepository {
    suspend fun saveProgress(progress: PlaybackProgress)
    fun getProgress(videoId: String): Flow<PlaybackProgress?>
    fun getAllProgress(): Flow<List<PlaybackProgress>>
    fun getRecentlyPlayed(limit: Int): Flow<List<PlaybackProgress>>
    suspend fun removeFromHistory(videoId: String)
    suspend fun deleteOlderThan(cutoffTimestamp: Long)
    suspend fun trimExcessHistory()
    suspend fun clearHistory()
}
