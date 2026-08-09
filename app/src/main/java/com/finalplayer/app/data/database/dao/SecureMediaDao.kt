package com.finalplayer.app.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.finalplayer.app.data.database.entities.SecureMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureMediaDao {
    @Query("SELECT * FROM secure_media ORDER BY addedAt DESC")
    fun getAllSecureMedia(): Flow<List<SecureMediaEntity>>

    @Query("SELECT videoId FROM secure_media")
    fun getAllSecureVideoIds(): Flow<List<String>>

    @Query("SELECT videoId FROM secure_media")
    suspend fun getAllSecureVideoIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SecureMediaEntity)

    @Query("DELETE FROM secure_media WHERE videoId = :videoId")
    suspend fun remove(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM secure_media WHERE videoId = :videoId)")
    suspend fun isSecure(videoId: String): Boolean

    @Query("SELECT COUNT(*) FROM secure_media")
    fun getCount(): Flow<Int>
}
