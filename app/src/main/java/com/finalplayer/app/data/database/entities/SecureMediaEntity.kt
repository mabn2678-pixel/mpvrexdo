package com.finalplayer.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_media")
data class SecureMediaEntity(
    @PrimaryKey val videoId: String,
    val title: String = "",
    val vaultPath: String = "",
    val originalPath: String = "",
    val duration: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAdded: Long = 0L,
    val resolution: String? = null,
    val folderPath: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

