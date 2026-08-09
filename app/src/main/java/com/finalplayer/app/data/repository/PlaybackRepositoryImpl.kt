package com.finalplayer.app.data.repository

import com.finalplayer.app.data.database.dao.PlaybackProgressDao
import com.finalplayer.app.data.mapper.toDomainModel
import com.finalplayer.app.data.mapper.toEntity
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaybackRepositoryImpl(
    private val playbackProgressDao: PlaybackProgressDao
) : PlaybackRepository {

    override suspend fun saveProgress(progress: PlaybackProgress) {
        playbackProgressDao.upsertProgress(progress.toEntity())
    }

    override fun getProgress(videoId: String): Flow<PlaybackProgress?> {
        return playbackProgressDao.getProgress(videoId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getAllProgress(): Flow<List<PlaybackProgress>> {
        return playbackProgressDao.getAllProgress().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getRecentlyPlayed(limit: Int): Flow<List<PlaybackProgress>> {
        return playbackProgressDao.getRecentlyPlayed(limit).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun removeFromHistory(videoId: String) {
        playbackProgressDao.deleteProgress(videoId)
    }

    override suspend fun deleteOlderThan(cutoffTimestamp: Long) {
        playbackProgressDao.deleteOlderThan(cutoffTimestamp)
    }

    override suspend fun trimExcessHistory() {
        playbackProgressDao.trimExcessHistory()
    }

    override suspend fun clearHistory() {
        playbackProgressDao.clearAll()
    }
}
