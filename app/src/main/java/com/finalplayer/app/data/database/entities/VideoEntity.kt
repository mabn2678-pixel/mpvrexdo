package com.finalplayer.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val duration: Long,
    val sizeBytes: Long,
    val thumbnailPath: String? = null,
    val dateAdded: Long,
    val resolution: String? = null,
    val folderPath: String,
    val mimeType: String
)
