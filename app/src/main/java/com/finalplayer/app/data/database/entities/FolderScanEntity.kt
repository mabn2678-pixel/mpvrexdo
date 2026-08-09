package com.finalplayer.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_scans")
data class FolderScanEntity(
    @PrimaryKey val folderPath: String,
    val lastScanTimestamp: Long,
    val videoCount: Int
)
