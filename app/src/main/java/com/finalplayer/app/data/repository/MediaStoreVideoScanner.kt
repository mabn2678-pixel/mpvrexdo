package com.finalplayer.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.finalplayer.app.data.database.entities.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreVideoScanner(private val context: Context) {

    suspend fun scanDeviceVideos(): List<VideoEntity> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoEntity>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE
        )

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val widthColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeTypeColumn = c.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(nameColumn) ?: "Unknown Video"
                    val duration = c.getLong(durationColumn)
                    val sizeBytes = c.getLong(sizeColumn)
                    val dateAdded = c.getLong(dateAddedColumn)
                    val fullPath = c.getString(dataColumn) ?: ""
                    val width = c.getInt(widthColumn)
                    val height = c.getInt(heightColumn)
                    val mimeType = c.getString(mimeTypeColumn) ?: "video/*"

                    val folderPath = if (fullPath.isNotEmpty()) {
                        File(fullPath).parent ?: "/storage/emulated/0"
                    } else {
                        "/storage/emulated/0"
                    }

                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else null
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id).toString()

                    videoList.add(
                        VideoEntity(
                            id = id.toString(),
                            uri = uri,
                            title = title,
                            duration = duration,
                            sizeBytes = sizeBytes,
                            thumbnailPath = null,
                            dateAdded = dateAdded,
                            resolution = resolution,
                            folderPath = folderPath,
                            mimeType = mimeType
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList.addAll(getMockVideos())
        videoList
    }

    private fun getMockVideos(): List<VideoEntity> {
        val currentTime = System.currentTimeMillis() / 1000L
        return listOf(
            VideoEntity(
                id = "mock_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny 4K.mp4",
                duration = 596000L,
                sizeBytes = 158000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop",
                dateAdded = currentTime,
                resolution = "3840x2160",
                folderPath = "/storage/emulated/0/Movies/Sample Videos",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_2",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantDream.mp4",
                title = "Elephant's Dream.mp4",
                duration = 653000L,
                sizeBytes = 182000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 3600,
                resolution = "1920x1080",
                folderPath = "/storage/emulated/0/Movies/Sample Videos",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_3",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "For Bigger Blazes.mp4",
                duration = 15000L,
                sizeBytes = 15000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 7200,
                resolution = "1280x720",
                folderPath = "/storage/emulated/0/Movies/Sample Videos",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_4",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                title = "For Bigger Escapes.mp4",
                duration = 15000L,
                sizeBytes = 14000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 10800,
                resolution = "1280x720",
                folderPath = "/storage/emulated/0/Movies/Sample Videos",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_5",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                title = "For Bigger Fun.mp4",
                duration = 60000L,
                sizeBytes = 28000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 14400,
                resolution = "1280x720",
                folderPath = "/storage/emulated/0/Movies/Sample Videos",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_6",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                title = "Tears of Steel 1080p.mp4",
                duration = 734000L,
                sizeBytes = 220000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 18000,
                resolution = "1920x1080",
                folderPath = "/storage/emulated/0/DCIM/Camera",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_7",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                title = "WhatsApp Video 2026.mp4",
                duration = 15000L,
                sizeBytes = 12000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 21600,
                resolution = "1280x720",
                folderPath = "/storage/emulated/0/WhatsApp/WhatsApp Video",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_tiktok_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                title = "TikTok_Dance_Trend_Vertical.mp4",
                duration = 28000L,
                sizeBytes = 15000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 1000,
                resolution = "720x1280",
                folderPath = "/storage/emulated/0/Download/TikTok",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_shorts_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                title = "YouTube_Shorts_Highlights_Vertical.mp4",
                duration = 45000L,
                sizeBytes = 18000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 2000,
                resolution = "1080x1920",
                folderPath = "/storage/emulated/0/Movies/Shorts",
                mimeType = "video/mp4"
            ),
            VideoEntity(
                id = "mock_reels_1",
                uri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                title = "Instagram_Reels_Clip_Vertical.mp4",
                duration = 20000L,
                sizeBytes = 12000000L,
                thumbnailPath = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop",
                dateAdded = currentTime - 3000,
                resolution = "1080x1920",
                folderPath = "/storage/emulated/0/DCIM/Instagram",
                mimeType = "video/mp4"
            )
        )
    }
}
