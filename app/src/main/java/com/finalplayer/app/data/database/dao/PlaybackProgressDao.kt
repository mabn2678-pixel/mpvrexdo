package com.finalplayer.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finalplayer.app.data.database.entities.PlaybackProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaybackProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlaybackProgressEntity)

    @Query("SELECT * FROM playback_progress WHERE videoId = :videoId")
    fun getProgress(videoId: String): Flow<PlaybackProgressEntity?>

    @Query("SELECT * FROM playback_progress")
    fun getAllProgress(): Flow<List<PlaybackProgressEntity>>

    @Query("SELECT * FROM playback_progress ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int): Flow<List<PlaybackProgressEntity>>

    @Query("DELETE FROM playback_progress WHERE videoId = :videoId")
    suspend fun deleteProgress(videoId: String)

    @Query("DELETE FROM playback_progress WHERE lastPlayedTimestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM playback_progress WHERE videoId NOT IN (SELECT videoId FROM playback_progress ORDER BY lastPlayedTimestamp DESC LIMIT 50)")
    suspend fun trimExcessHistory()

    @Query("DELETE FROM playback_progress")
    suspend fun clearAll()
}
