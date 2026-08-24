package com.finalplayer.app.data.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.finalplayer.app.MainActivity
import com.finalplayer.app.R
import com.finalplayer.app.data.database.dao.SecureMediaDao
import com.finalplayer.app.data.database.dao.VideoDao
import com.finalplayer.app.data.database.entities.SecureMediaEntity
import com.finalplayer.app.data.database.entities.VideoEntity
import com.finalplayer.app.data.mapper.toEntity
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.utils.FileOperationsUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileTransferManager(
    private val context: Context,
    private val videoDao: VideoDao,
    private val secureMediaDao: SecureMediaDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var transferJob: Job? = null

    private val _transferState = MutableStateFlow<TransferProgress?>(null)
    val transferState: StateFlow<TransferProgress?> = _transferState.asStateFlow()

    private val _transferCompletionEvents = MutableSharedFlow<Pair<Boolean, String>>()
    val transferCompletionEvents: SharedFlow<Pair<Boolean, String>> = _transferCompletionEvents.asSharedFlow()

    fun startTransfer(
        videos: List<VideoItem>,
        destination: File? = null,
        type: TransferType,
        runInBackground: Boolean = false,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (_transferState.value?.isRunning == true) {
            onComplete?.invoke(false, "توجد عملية جارية حالياً")
            return
        }

        if (videos.isEmpty()) {
            onComplete?.invoke(false, "لم يتم تحديد أي ملفات")
            return
        }

        val totalBytes = videos.sumOf {
            val f = FileOperationsUtil.getVideoFile(it)
            if (f.exists()) f.length() else it.sizeBytes
        }.coerceAtLeast(1L)

        _transferState.value = TransferProgress(
            type = type,
            isRunning = true,
            isBackground = runInBackground,
            currentFileIndex = 0,
            totalFileCount = videos.size,
            currentFileName = videos.firstOrNull()?.title ?: "",
            transferredBytes = 0L,
            totalBytes = totalBytes,
            percentage = 0,
            transferredSizeFormatted = "0 B",
            totalSizeFormatted = FileOperationsUtil.formatFileSize(totalBytes),
            destinationPath = destination?.absolutePath ?: ""
        )

        if (runInBackground) {
            startForegroundService()
        }

        transferJob = scope.launch {
            try {
                if (!FileOperationsUtil.hasStoragePermission(context)) {
                    val errorMsg = "يلزم منح إذن الوصول للتخزين"
                    _transferState.value = null
                    stopForegroundService()
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, errorMsg)
                        _transferCompletionEvents.emit(false to errorMsg)
                    }
                    return@launch
                }

                if (destination != null && !destination.exists()) {
                    destination.mkdirs()
                }

                var totalBytesProcessed = 0L
                val successFiles = mutableListOf<VideoItem>()

                when (type) {
                    TransferType.MOVE, TransferType.COPY -> {
                        val targetDest = destination ?: throw IllegalArgumentException("Destination folder required")
                        for ((index, video) in videos.withIndex()) {
                            if (!isActive) throw CancellationException("Transfer cancelled by user")

                            val sourceFile = FileOperationsUtil.getVideoFile(video)
                            val targetFile = File(targetDest, sourceFile.name.ifBlank { video.title })

                            _transferState.value = _transferState.value?.copy(
                                currentFileIndex = index + 1,
                                currentFileName = video.title
                            )
                            updateNotification()

                            val fileCopied = copyStreamWithProgress(
                                src = sourceFile,
                                dst = targetFile,
                                onBytesChunk = { bytesRead ->
                                    totalBytesProcessed += bytesRead
                                    val pct = ((totalBytesProcessed.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                                    _transferState.value = _transferState.value?.copy(
                                        transferredBytes = totalBytesProcessed,
                                        percentage = pct,
                                        transferredSizeFormatted = FileOperationsUtil.formatFileSize(totalBytesProcessed)
                                    )
                                    updateNotification()
                                }
                            )

                            if (!fileCopied) {
                                throw Exception("فشل نقل/نسخ الملف: ${video.title}")
                            }

                            if (type == TransferType.MOVE) {
                                sourceFile.delete()
                                FileOperationsUtil.scanFile(context, sourceFile)
                            }

                            FileOperationsUtil.scanFile(context, targetFile)
                            successFiles.add(video)

                            if (type == TransferType.MOVE) {
                                val updated = video.toEntity().copy(
                                    folderPath = targetDest.absolutePath,
                                    uri = if (targetFile.exists()) targetFile.absolutePath else "${targetDest.absolutePath}/${video.title}"
                                )
                                videoDao.insertVideos(listOf(updated))
                            } else {
                                val copied = video.toEntity().copy(
                                    id = "${video.id}_copy_${System.currentTimeMillis()}_$index",
                                    folderPath = targetDest.absolutePath,
                                    uri = if (targetFile.exists()) targetFile.absolutePath else "${targetDest.absolutePath}/${video.title}",
                                    dateAdded = System.currentTimeMillis() / 1000L
                                )
                                videoDao.insertVideos(listOf(copied))
                            }
                        }
                    }

                    TransferType.HIDE_TO_SECURE -> {
                        val vaultDir = File(context.filesDir, "secure_vault").apply { if (!exists()) mkdirs() }
                        for ((index, video) in videos.withIndex()) {
                            if (!isActive) throw CancellationException("Transfer cancelled by user")

                            val sourceFile = FileOperationsUtil.getVideoFile(video)
                            val originalPathStr = sourceFile.absolutePath.ifBlank { video.uri }
                            val vaultTargetFile = File(vaultDir, ".sec_${video.id}_${System.currentTimeMillis()}_$index.dat")

                            _transferState.value = _transferState.value?.copy(
                                currentFileIndex = index + 1,
                                currentFileName = video.title
                            )
                            updateNotification()

                            val fileCopied = copyStreamWithProgress(
                                src = sourceFile,
                                dst = vaultTargetFile,
                                onBytesChunk = { bytesRead ->
                                    totalBytesProcessed += bytesRead
                                    val pct = ((totalBytesProcessed.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                                    _transferState.value = _transferState.value?.copy(
                                        transferredBytes = totalBytesProcessed,
                                        percentage = pct,
                                        transferredSizeFormatted = FileOperationsUtil.formatFileSize(totalBytesProcessed)
                                    )
                                    updateNotification()
                                }
                            )

                            if (!fileCopied) {
                                throw Exception("فشل تشفير ونقل الملف إلى المجلد الآمن: ${video.title}")
                            }

                            sourceFile.delete()
                            FileOperationsUtil.scanFile(context, sourceFile)
                            FileOperationsUtil.scanFile(context, vaultTargetFile)

                            val actualSize = if (vaultTargetFile.exists()) vaultTargetFile.length() else video.sizeBytes

                            secureMediaDao.insert(
                                SecureMediaEntity(
                                    videoId = video.id,
                                    title = video.title,
                                    vaultPath = vaultTargetFile.absolutePath,
                                    originalPath = originalPathStr,
                                    duration = video.duration,
                                    sizeBytes = actualSize,
                                    dateAdded = video.dateAdded,
                                    resolution = video.resolution,
                                    folderPath = video.folderPath,
                                    addedAt = System.currentTimeMillis()
                                )
                            )
                            videoDao.deleteVideo(video.id)
                            successFiles.add(video)
                        }
                    }

                    TransferType.RESTORE_FROM_SECURE -> {
                        for ((index, video) in videos.withIndex()) {
                            if (!isActive) throw CancellationException("Transfer cancelled by user")

                            val entity = secureMediaDao.getByVideoId(video.id)
                            val vaultFile = if (entity != null && entity.vaultPath.isNotBlank()) {
                                File(entity.vaultPath)
                            } else {
                                File(video.uri)
                            }

                            val originalPathStr = entity?.originalPath ?: video.uri
                            val originalTargetFile = File(originalPathStr)
                            val parent = originalTargetFile.parentFile ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                            if (!parent.exists()) {
                                parent.mkdirs()
                            }
                            val destinationFile = File(parent, originalTargetFile.name.removePrefix("."))

                            _transferState.value = _transferState.value?.copy(
                                currentFileIndex = index + 1,
                                currentFileName = video.title
                            )
                            updateNotification()

                            val fileRestored = copyStreamWithProgress(
                                src = vaultFile,
                                dst = destinationFile,
                                onBytesChunk = { bytesRead ->
                                    totalBytesProcessed += bytesRead
                                    val pct = ((totalBytesProcessed.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
                                    _transferState.value = _transferState.value?.copy(
                                        transferredBytes = totalBytesProcessed,
                                        percentage = pct,
                                        transferredSizeFormatted = FileOperationsUtil.formatFileSize(totalBytesProcessed)
                                    )
                                    updateNotification()
                                }
                            )

                            if (!fileRestored) {
                                throw Exception("فشل استعادة الملف: ${video.title}")
                            }

                            vaultFile.delete()
                            FileOperationsUtil.scanFile(context, destinationFile)
                            FileOperationsUtil.scanFile(context, vaultFile)

                            val videoEntity = VideoEntity(
                                id = entity?.videoId ?: video.id,
                                title = entity?.title?.ifBlank { destinationFile.name } ?: video.title,
                                uri = destinationFile.absolutePath,
                                duration = entity?.duration ?: video.duration,
                                sizeBytes = destinationFile.length(),
                                dateAdded = if ((entity?.dateAdded ?: 0L) > 0) entity!!.dateAdded else (System.currentTimeMillis() / 1000L),
                                resolution = entity?.resolution ?: video.resolution,
                                folderPath = parent.absolutePath,
                                mimeType = "video/*"
                            )
                            videoDao.insertVideos(listOf(videoEntity))
                            secureMediaDao.remove(video.id)
                            successFiles.add(video)
                        }
                    }
                }

                val successMessage = when (type) {
                    TransferType.MOVE -> "تم نقل ${videos.size} ملف بنجاح"
                    TransferType.COPY -> "تم نسخ ${videos.size} ملف بنجاح"
                    TransferType.HIDE_TO_SECURE -> "تم إخفاء وتأمين ${videos.size} ملف بنجاح في المجلد الآمن"
                    TransferType.RESTORE_FROM_SECURE -> "تمت استعادة ${videos.size} ملف بنجاح إلى هاتفك"
                }

                _transferState.value = null
                stopForegroundService()

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, successMessage)
                    _transferCompletionEvents.emit(true to successMessage)
                }

            } catch (e: CancellationException) {
                val actionName = when (type) {
                    TransferType.MOVE -> "النقل"
                    TransferType.COPY -> "النسخ"
                    TransferType.HIDE_TO_SECURE -> "الإخفاء والتأمين"
                    TransferType.RESTORE_FROM_SECURE -> "الاستعادة"
                }
                val cancelMsg = "تم إلغاء عملية $actionName"
                _transferState.value = null
                stopForegroundService()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, cancelMsg)
                    _transferCompletionEvents.emit(false to cancelMsg)
                }
            } catch (e: Exception) {
                val actionName = when (type) {
                    TransferType.MOVE -> "النقل"
                    TransferType.COPY -> "النسخ"
                    TransferType.HIDE_TO_SECURE -> "الإخفاء والتأمين"
                    TransferType.RESTORE_FROM_SECURE -> "الاستعادة"
                }
                val errMsg = "فشلت عملية $actionName: ${e.message ?: "خطأ غير معروف"}"
                _transferState.value = null
                stopForegroundService()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, errMsg)
                    _transferCompletionEvents.emit(false to errMsg)
                }
            }
        }
    }


    private suspend fun copyStreamWithProgress(
        src: File,
        dst: File,
        onBytesChunk: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        if (!src.exists()) return@withContext false
        try {
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) throw CancellationException("Cancelled")
                        output.write(buffer, 0, bytesRead)
                        onBytesChunk(bytesRead)
                    }
                    output.flush()
                }
            }
            true
        } catch (e: CancellationException) {
            dst.delete()
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            dst.delete()
            false
        }
    }

    fun moveToBackground() {
        val current = _transferState.value ?: return
        if (!current.isRunning) return
        _transferState.value = current.copy(isBackground = true)
        startForegroundService()
    }

    fun showInForeground() {
        val current = _transferState.value ?: return
        _transferState.value = current.copy(isBackground = false)
    }

    fun cancelTransfer() {
        transferJob?.cancel()
        transferJob = null
        _transferState.value = null
        stopForegroundService()
    }

    private fun startForegroundService() {
        try {
            val intent = Intent(context, FileTransferForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopForegroundService() {
        try {
            val intent = Intent(context, FileTransferForegroundService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNotification() {
        val current = _transferState.value ?: return
        if (current.isBackground) {
            FileTransferForegroundService.updateProgressNotification(context, current)
        }
    }

    companion object {
        @Volatile
        private var instance: FileTransferManager? = null

        fun getInstance(context: Context, videoDao: VideoDao, secureMediaDao: SecureMediaDao): FileTransferManager {
            return instance ?: synchronized(this) {
                instance ?: FileTransferManager(context.applicationContext, videoDao, secureMediaDao).also { instance = it }
            }
        }
    }
}

class FileTransferForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            // Cancel through manager
            currentTransferManager?.cancelTransfer()
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel(this)
        val initialNotification = buildNotification(this, currentTransferProgress)
        startForeground(NOTIFICATION_ID, initialNotification)
        return START_NOT_STICKY
    }

    companion object {
        const val CHANNEL_ID = "file_transfer_channel"
        const val NOTIFICATION_ID = 2004
        const val ACTION_CANCEL = "com.finalplayer.app.action.CANCEL_TRANSFER"

        var currentTransferManager: FileTransferManager? = null
        var currentTransferProgress: TransferProgress? = null

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "نقل ونسخ الملفات"
                val descriptionText = "إشعارات تقدم عمليات نقل ونسخ وتأمين الفيديوهات في الخلفية"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun updateProgressNotification(context: Context, progress: TransferProgress) {
            currentTransferProgress = progress
            val notification = buildNotification(context, progress)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun buildNotification(context: Context, progress: TransferProgress?): android.app.Notification {
            createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val cancelIntent = Intent(context, FileTransferForegroundService::class.java).apply {
                action = ACTION_CANCEL
            }
            val cancelPendingIntent = PendingIntent.getService(
                context,
                1,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = when (progress?.type) {
                TransferType.MOVE -> "جاري نقل الملفات..."
                TransferType.COPY -> "جاري نسخ الملفات..."
                TransferType.HIDE_TO_SECURE -> "جاري التأمين والنقل إلى المجلد الآمن..."
                TransferType.RESTORE_FROM_SECURE -> "جاري استعادة الملفات إلى الهاتف..."
                else -> "جاري معالجة الملفات..."
            }
            val currentIdx = progress?.currentFileIndex ?: 0
            val totalCount = progress?.totalFileCount ?: 0
            val pct = progress?.percentage ?: 0
            val transferredSize = progress?.transferredSizeFormatted ?: "0 B"
            val totalSize = progress?.totalSizeFormatted ?: "0 B"

            val contentText = "$currentIdx/$totalCount ($pct%) - $transferredSize من $totalSize"

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(title)
                .setContentText(contentText)
                .setProgress(100, pct, false)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", cancelPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }
}
