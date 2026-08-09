package com.finalplayer.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.finalplayer.app.data.database.dao.PlaybackProgressDao
import com.finalplayer.app.data.database.dao.SecureMediaDao
import com.finalplayer.app.data.database.dao.VideoDao
import com.finalplayer.app.data.database.entities.FolderScanEntity
import com.finalplayer.app.data.database.entities.PlaybackProgressEntity
import com.finalplayer.app.data.database.entities.SecureMediaEntity
import com.finalplayer.app.data.database.entities.VideoEntity

@Database(
    entities = [
        VideoEntity::class,
        FolderScanEntity::class,
        PlaybackProgressEntity::class,
        SecureMediaEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class FinalPlayerDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun playbackProgressDao(): PlaybackProgressDao
    abstract fun secureMediaDao(): SecureMediaDao
}
