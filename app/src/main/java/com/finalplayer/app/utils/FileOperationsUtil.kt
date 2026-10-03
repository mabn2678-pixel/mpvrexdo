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
import android.provider.MediaStore
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
            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                Uri.fromFile(file)
            }
            uris.add(uri)
        }

        val shareTitle = if (files.size == 1) files.first().name else "${files.size} ملفات"

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, shareTitle)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, shareTitle)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "مشاركة: $shareTitle")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareVideos(context: Context, items: List<VideoItem>) {
        if (items.isEmpty()) return
        val uris = ArrayList<Uri>()
        val titles = ArrayList<String>()

        for (item in items) {
            titles.add(item.title)
            val file = getVideoFile(item)
            if (file.exists()) {
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (e: Exception) {
                    if (item.uri.startsWith("content://")) Uri.parse(item.uri) else Uri.fromFile(file)
                }
                uris.add(uri)
            } else if (item.uri.startsWith("content://")) {
                uris.add(Uri.parse(item.uri))
            } else if (item.uri.startsWith("http://") || item.uri.startsWith("https://")) {
                uris.add(Uri.parse(item.uri))
            } else {
                uris.add(Uri.fromFile(file))
            }
        }

        val shareTitle = if (items.size == 1) items.first().title else "${items.size} فيديوهات"

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "video/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, shareTitle)
            }
        } else if (uris.size > 1) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, shareTitle)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, titles.joinToString("\n"))
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "مشاركة: $shareTitle")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun getVideoFile(videoItem: VideoItem, context: Context? = null): File {
        // 1. Direct path check if URI is an absolute file path
        if (videoItem.uri.startsWith("/")) {
            val f = File(videoItem.uri)
            if (f.exists()) return f
        }
        if (videoItem.uri.startsWith("file://")) {
            val path = Uri.parse(videoItem.uri).path
            if (!path.isNullOrBlank()) {
                val f = File(path)
                if (f.exists()) return f
            }
        }

        // 2. Direct check in folderPath if it's an absolute path
        if (videoItem.folderPath.isNotBlank() && videoItem.folderPath.startsWith("/")) {
            val folder = File(videoItem.folderPath)
            val direct = File(folder, videoItem.title)
            if (direct.exists()) return direct

            val withMp4 = File(folder, "${videoItem.title}.mp4")
            if (withMp4.exists()) return withMp4

            if (folder.exists() && folder.isDirectory) {
                val match = folder.listFiles()?.firstOrNull {
                    it.name.equals(videoItem.title, ignoreCase = true) ||
                    it.nameWithoutExtension.equals(videoItem.title, ignoreCase = true)
                }
                if (match != null && match.exists()) return match
            }
        }

        // 3. MediaStore lookup if content URI and context is available
        if (context != null && videoItem.uri.startsWith("content://")) {
            try {
                val uri = Uri.parse(videoItem.uri)
                val projection = arrayOf(MediaStore.Video.Media.DATA)
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                        if (idx >= 0) {
                            val path = cursor.getString(idx)
                            if (!path.isNullOrBlank()) {
                                val f = File(path)
                                if (f.exists()) return f
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 4. Check if video ID is a path
        if (videoItem.id.startsWith("/")) {
            val f = File(videoItem.id)
            if (f.exists()) return f
        }

        // 5. Look in common media directories for the file
        val candidateDirs = mutableListOf(
            File("/storage/emulated/0/Download/VideoDownloader"),
            File("/storage/emulated/0/VideoDownloader"),
            File("/storage/emulated/0/Download"),
            File("/storage/emulated/0/Downloads"),
            File("/storage/emulated/0/Movies"),
            File("/storage/emulated/0/Movies/VideoDownloader"),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        )
        if (videoItem.folderPath.isNotBlank() && !videoItem.folderPath.startsWith("/")) {
            candidateDirs.add(0, File("/storage/emulated/0", videoItem.folderPath))
            candidateDirs.add(1, File("/storage/emulated/0/Download", videoItem.folderPath))
        }

        for (dir in candidateDirs.distinct()) {
            if (dir.exists() && dir.isDirectory) {
                val direct = File(dir, videoItem.title)
                if (direct.exists()) return direct
                val match = dir.listFiles()?.firstOrNull {
                    it.name.equals(videoItem.title, ignoreCase = true) ||
                    it.nameWithoutExtension.equals(videoItem.title, ignoreCase = true)
                }
                if (match != null && match.exists()) return match
            }
        }

        // Fallback default
        return if (videoItem.folderPath.isNotBlank() && !videoItem.uri.startsWith("/")) {
            File(videoItem.folderPath, videoItem.title)
        } else if (videoItem.uri.startsWith("/")) {
            File(videoItem.uri)
        } else {
            File(videoItem.folderPath, videoItem.title)
        }
    }

    fun getVaultDir(context: Context, sourceFile: File? = null): File {
        // First determine storage root if source file is provided
        if (sourceFile != null && sourceFile.exists()) {
            val filePath = sourceFile.absolutePath

            // 1. Detect root volume directly from file path (e.g., /storage/emulated/0 or /storage/XXXX-XXXX)
            val directVolumeRoot = if (filePath.startsWith("/storage/emulated/0")) {
                File("/storage/emulated/0")
            } else if (filePath.startsWith("/storage/")) {
                val parts = filePath.split('/')
                if (parts.size >= 3) File("/storage/${parts[2]}") else null
            } else null

            if (directVolumeRoot != null && directVolumeRoot.exists()) {
                try {
                    val rootVault = File(directVolumeRoot, ".secure_vault")
                    if (!rootVault.exists()) rootVault.mkdirs()
                    if (rootVault.exists() && rootVault.canWrite()) {
                        val nomedia = File(rootVault, ".nomedia")
                        if (!nomedia.exists()) {
                            try { nomedia.createNewFile() } catch (_: Exception) {}
                        }
                        return rootVault
                    }
                } catch (_: Exception) {}
            }

            // 2. Detect via externalDirs
            val externalDirs = try {
                context.getExternalFilesDirs(null).filterNotNull()
            } catch (_: Exception) {
                emptyList()
            }

            for (extDir in externalDirs) {
                val extRootStr = extDir.absolutePath.substringBefore("/Android/")
                if (filePath.startsWith(extRootStr)) {
                    val extRoot = File(extRootStr)
                    try {
                        val rootVault = File(extRoot, ".secure_vault")
                        if (!rootVault.exists()) rootVault.mkdirs()
                        if (rootVault.exists() && rootVault.canWrite()) {
                            val nomedia = File(rootVault, ".nomedia")
                            if (!nomedia.exists()) {
                                try { nomedia.createNewFile() } catch (_: Exception) {}
                            }
                            return rootVault
                        }
                    } catch (_: Exception) {}

                    // Try Movies/.secure_vault on that volume as a writable fallback
                    try {
                        val moviesVault = File(extRoot, "Movies/.secure_vault")
                        if (!moviesVault.exists()) moviesVault.mkdirs()
                        if (moviesVault.exists() && moviesVault.canWrite()) {
                            val nomedia = File(moviesVault, ".nomedia")
                            if (!nomedia.exists()) {
                                try { nomedia.createNewFile() } catch (_: Exception) {}
                            }
                            return moviesVault
                        }
                    } catch (_: Exception) {}

                    // Fallback to app-specific external dir on that volume
                    val vDir = File(extDir, ".secure_vault")
                    if (!vDir.exists()) vDir.mkdirs()
                    val nomedia = File(vDir, ".nomedia")
                    if (!nomedia.exists()) {
                        try { nomedia.createNewFile() } catch (_: Exception) {}
                    }
                    return vDir
                }
            }
        }

        // Primary storage root default: /storage/emulated/0/.secure_vault
        try {
            val primaryRoot = Environment.getExternalStorageDirectory()
            if (primaryRoot != null && primaryRoot.exists()) {
                val rootVault = File(primaryRoot, ".secure_vault")
                if (!rootVault.exists()) rootVault.mkdirs()
                if (rootVault.exists() && rootVault.canWrite()) {
                    val nomedia = File(rootVault, ".nomedia")
                    if (!nomedia.exists()) {
                        try { nomedia.createNewFile() } catch (_: Exception) {}
                    }
                    return rootVault
                }
            }
        } catch (_: Exception) {}

        val fallbackBase = context.getExternalFilesDir(null) ?: context.filesDir
        val vDir = File(fallbackBase, ".secure_vault")
        if (!vDir.exists()) vDir.mkdirs()
        val nomedia = File(vDir, ".nomedia")
        if (!nomedia.exists()) {
            try { nomedia.createNewFile() } catch (_: Exception) {}
        }
        return vDir
    }

    fun getSongFile(song: com.finalplayer.app.music.data.model.Song): File {
        return if (song.path.isNotBlank() && !song.path.startsWith("content://")) {
            File(song.path)
        } else {
            File(song.uri.path ?: "")
        }
    }

    suspend fun deleteSongs(context: Context, songs: List<com.finalplayer.app.music.data.model.Song>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (song in songs) {
                if (song.id < 0) continue // Preview song

                try {
                    if (song.uri.toString().startsWith("content://")) {
                        context.contentResolver.delete(song.uri, null, null)
                    } else {
                        val uri = android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            song.id
                        )
                        context.contentResolver.delete(uri, null, null)
                    }
                } catch (_: Exception) {}

                if (song.path.isNotBlank() && !song.path.startsWith("content://")) {
                    val file = File(song.path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        if (deleted) {
                            scanFile(context, file)
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareSongs(context: Context, songs: List<com.finalplayer.app.music.data.model.Song>) {
        if (songs.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (song in songs) {
            if (song.uri.toString().startsWith("content://")) {
                uris.add(song.uri)
            } else if (song.path.isNotBlank()) {
                val file = File(song.path)
                if (file.exists()) {
                    val uri = try {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    } catch (e: Exception) {
                        Uri.fromFile(file)
                    }
                    uris.add(uri)
                }
            }
        }

        val shareTitle = if (songs.size == 1) songs.first().title else "${songs.size} ملفات صوتية"
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uris.first())
                putExtra(Intent.EXTRA_TEXT, "${songs.first().title} - ${songs.first().artist}")
            }
        } else if (uris.isNotEmpty()) {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, shareTitle)
                putExtra(Intent.EXTRA_TEXT, songs.joinToString("\n") { "${it.title} - ${it.artist}" })
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val chooser = Intent.createChooser(intent, "مشاركة: $shareTitle")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
