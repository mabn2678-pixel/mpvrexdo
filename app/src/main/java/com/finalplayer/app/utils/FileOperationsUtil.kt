package com.finalplayer.app.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.finalplayer.app.domain.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileInfo(
    val name: String,
    val path: String,
    val size: Long,            // bytes
    val sizeFormatted: String, // e.g. "128 MB"
    val duration: Long,        // milliseconds
    val durationFormatted: String, // e.g. "01:24:35"
    val resolution: String,    // e.g. "1920x1080"
    val format: String,        // e.g. "MP4"
    val lastModified: String   // formatted date
)

object FileOperationsUtil {

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    suspend fun renameFile(context: Context, file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext Result.failure(SecurityException("يلزم منح إذن الوصول للتخزين"))
            }
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("الملف الأصلي غير موجود على التخزين"))
            }
            val parent = file.parentFile ?: return@withContext Result.failure(IllegalArgumentException("مسار المجلد غير صالح"))
            val targetFile = File(parent, newName)
            if (targetFile.exists()) {
                return@withContext Result.failure(IllegalArgumentException("يوجد ملف بنفس الاسم بالفعل"))
            }

            val renamed = file.renameTo(targetFile)
            if (renamed && targetFile.exists()) {
                scanFile(context, file)
                scanFile(context, targetFile)
                Result.success(targetFile)
            } else {
                Result.failure(Exception("فشلت عملية إعادة التسمية"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFiles(context: Context, files: List<File>, destination: File): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext Result.failure(SecurityException("يلزم منح إذن الوصول للتخزين"))
            }
            if (!destination.exists()) {
                destination.mkdirs()
            }

            val movedFiles = mutableListOf<File>()
            for (file in files) {
                if (!file.exists()) continue
                val targetFile = File(destination, file.name)
                val success = if (file.renameTo(targetFile)) {
                    true
                } else {
                    copySingleFile(file, targetFile) && file.delete()
                }

                if (success) {
                    movedFiles.add(targetFile)
                    scanFile(context, file)
                    scanFile(context, targetFile)
                }
            }
            Result.success(movedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFiles(context: Context, files: List<File>, destination: File): Result<List<File>> = withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext Result.failure(SecurityException("يلزم منح إذن الوصول للتخزين"))
            }
            if (!destination.exists()) {
                destination.mkdirs()
            }

            val copiedFiles = mutableListOf<File>()
            for (file in files) {
                if (!file.exists()) continue
                val targetFile = File(destination, file.name)
                if (copySingleFile(file, targetFile)) {
                    copiedFiles.add(targetFile)
                    scanFile(context, targetFile)
                }
            }
            Result.success(copiedFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copySingleFile(src: File, dst: File): Boolean {
        return try {
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun hideFiles(context: Context, files: List<File>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext Result.failure(SecurityException("يلزم منح إذن الوصول للتخزين"))
            }

            for (file in files) {
                if (!file.exists()) continue
                val parent = file.parentFile ?: continue

                val nomediaFile = File(parent, ".nomedia")
                if (!nomediaFile.exists()) {
                    try { nomediaFile.createNewFile() } catch (_: Exception) {}
                }

                if (!file.name.startsWith(".")) {
                    val hiddenFile = File(parent, ".${file.name}")
                    if (file.renameTo(hiddenFile)) {
                        scanFile(context, file)
                        scanFile(context, hiddenFile)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFiles(context: Context, files: List<File>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!hasStoragePermission(context)) {
                return@withContext Result.failure(SecurityException("يلزم منح إذن الوصول للتخزين"))
            }

            for (file in files) {
                if (!file.exists()) continue
                val deleted = file.delete()
                if (deleted) {
                    scanFile(context, file)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileInfo(file: File, fallbackVideoItem: VideoItem? = null): FileInfo {
        val size = if (file.exists()) file.length() else (fallbackVideoItem?.sizeBytes ?: 0L)
        val name = file.name.ifBlank { fallbackVideoItem?.title ?: "ملف غير معروف" }
        val path = file.absolutePath
        val lastModifiedDate = if (file.exists()) file.lastModified() else (fallbackVideoItem?.dateAdded?.times(1000L) ?: 0L)

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        val formattedDate = if (lastModifiedDate > 0) dateFormat.format(Date(lastModifiedDate)) else "غير معروف"

        val ext = file.extension.uppercase(Locale.getDefault()).ifBlank { "فيديو" }

        var durationMs = fallbackVideoItem?.duration ?: 0L
        var resolution = fallbackVideoItem?.resolution ?: "غير معروف"

        if (file.exists()) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(file.absolutePath)
                val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!durStr.isNullOrBlank()) {
                    durationMs = durStr.toLongOrNull() ?: durationMs
                }
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (!width.isNullOrBlank() && !height.isNullOrBlank()) {
                    resolution = "${width}x${height}"
                }
            } catch (e: Exception) {
                // Keep fallbacks
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
        }

        return FileInfo(
            name = name,
            path = path,
            size = size,
            sizeFormatted = formatFileSize(size),
            duration = durationMs,
            durationFormatted = formatDurationMs(durationMs),
            resolution = resolution,
            format = ext,
            lastModified = formattedDate
        )
    }

    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (file in files) {
            if (file.exists()) {
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    Uri.fromFile(file)
                }
                uris.add(uri)
            }
        }
        if (uris.isEmpty()) return

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "مشاركة الفيديو")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    suspend fun scanFile(context: Context, file: File) = withContext(Dispatchers.IO) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("video/*"),
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatDurationMs(durationMs: Long): String {
        if (durationMs <= 0) return "00:00"
        val totalSeconds = durationMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    fun getVideoFile(videoItem: VideoItem): File {
        return if (videoItem.folderPath.isNotBlank() && !videoItem.uri.startsWith("/")) {
            File(videoItem.folderPath, videoItem.title)
        } else if (videoItem.uri.startsWith("/")) {
            File(videoItem.uri)
        } else {
            File(videoItem.folderPath, videoItem.title)
        }
    }
}
