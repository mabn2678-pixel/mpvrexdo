package com.finalplayer.app.data.mapper

import com.finalplayer.app.data.database.entities.PlaybackProgressEntity
import com.finalplayer.app.data.database.entities.VideoEntity
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.model.VideoFolder
import com.finalplayer.app.domain.model.VideoItem
import java.io.File

fun VideoEntity.toDomainModel(): VideoItem {
    return VideoItem(
        id = id,
        uri = uri,
        title = title,
        duration = duration,
        sizeBytes = sizeBytes,
        thumbnailPath = thumbnailPath,
        dateAdded = dateAdded,
        resolution = resolution,
        folderPath = folderPath
    )
}

fun VideoItem.toEntity(mimeType: String = "video/*"): VideoEntity {
    return VideoEntity(
        id = id,
        uri = uri,
        title = title,
        duration = duration,
        sizeBytes = sizeBytes,
        thumbnailPath = thumbnailPath,
        dateAdded = dateAdded,
        resolution = resolution,
        folderPath = folderPath,
        mimeType = mimeType
    )
}

fun PlaybackProgressEntity.toDomainModel(): PlaybackProgress {
    return PlaybackProgress(
        videoId = videoId,
        positionMs = positionMs,
        durationMs = durationMs,
        lastPlayedTimestamp = lastPlayedTimestamp,
        isCompleted = isCompleted
    )
}

fun PlaybackProgress.toEntity(): PlaybackProgressEntity {
    return PlaybackProgressEntity(
        videoId = videoId,
        positionMs = positionMs,
        durationMs = durationMs,
        lastPlayedTimestamp = lastPlayedTimestamp,
        isCompleted = isCompleted
    )
}

fun List<VideoEntity>.toVideoFolders(): List<VideoFolder> {
    return this.groupBy { it.folderPath }
        .map { (folderPath, videos) ->
            val folderFile = File(folderPath)
            val folderName = folderFile.name.ifEmpty { "Root" }
            val totalDuration = videos.sumOf { it.duration }
            val totalSizeBytes = videos.sumOf { it.sizeBytes }
            val folderDate = if (folderFile.exists() && folderFile.lastModified() > 0) {
                folderFile.lastModified()
            } else {
                val maxDate = videos.maxOfOrNull { it.dateAdded } ?: 0L
                if (maxDate > 0 && maxDate < 100_000_000_000L) maxDate * 1000L else maxDate
            }
            VideoFolder(
                path = folderPath,
                name = folderName,
                videoCount = videos.size,
                totalDuration = totalDuration,
                totalSizeBytes = totalSizeBytes,
                lastModified = folderDate,
                coverThumbnail = videos.firstOrNull { it.thumbnailPath != null }?.thumbnailPath,
                previewThumbnails = videos.take(3).map { it.thumbnailPath ?: it.uri }
            )
        }
        .sortedBy { it.name }
}
