package com.finalplayer.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_media")
data class SecureMediaEntity(
    @PrimaryKey val videoId: String,
    val originalPath: String,
    val addedAt: Long = System.currentTimeMillis()
)
