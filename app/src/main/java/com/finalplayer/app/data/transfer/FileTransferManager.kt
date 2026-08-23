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
import com.finalplayer.app.data.database.dao.VideoDao
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.data.mapper.toEntity
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
    private val videoDao: VideoDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var transferJob: Job? = null

    private val _transferState = MutableStateFlow<TransferProgress?>(null)
    val transferState: StateFlow<TransferProgress?> = _transferState.asStateFlow()

    private val _transferCompletionEvents = MutableSharedFlow<Pair<Boolean, String>>()
    val transferCompletionEvents: SharedFlow<Pair<Boolean, String>> = _transferCompletionEvents.asSharedFlow()

    fun startTransfer(
        videos: List<VideoItem>,
        destination: File,
        type: TransferType,
        runInBackground: Boolean = false,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        if (_transferState.value?.isRunning == true) {
            onComplete?.invoke(false, "توجد عملية نقل أو نسخ أخرى قيد التنفيذ حالياً")
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
            destinationPath = destination.absolutePath
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

                if (!destination.exists()) {
                    destination.mkdirs()
                }

                var totalBytesProcessed = 0L
                val successFiles = mutableListOf<VideoItem>()

                for ((index, video) in videos.withIndex()) {
                    if (!isActive) throw CancellationException("Transfer cancelled by user")

                    val sourceFile = FileOperationsUtil.getVideoFile(video)
                    val targetFile = File(destination, sourceFile.name.ifBlank { video.title })

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

                    // Update DB for this item
                    if (type == TransferType.MOVE) {
                        val updated = video.toEntity().copy(
                            folderPath = destination.absolutePath,
                            uri = if (targetFile.exists()) targetFile.absolutePath else "${destination.absolutePath}/${video.title}"
                        )
                        videoDao.insertVideos(listOf(updated))
                    } else {
                        val copied = video.toEntity().copy(
                            id = "${video.id}_copy_${System.currentTimeMillis()}_$index",
                            folderPath = destination.absolutePath,
                            uri = if (targetFile.exists()) targetFile.absolutePath else "${destination.absolutePath}/${video.title}",
                            dateAdded = System.currentTimeMillis() / 1000L
                        )
                        videoDao.insertVideos(listOf(copied))
                    }
                }

                val actionName = if (type == TransferType.MOVE) "نقل" else "نسخ"
                val successMessage = "تم $actionName ${videos.size} ملف بنجاح"
                _transferState.value = null
                stopForegroundService()

                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, successMessage)
                    _transferCompletionEvents.emit(true to successMessage)
                }

            } catch (e: CancellationException) {
                val actionName = if (type == TransferType.MOVE) "النقل" else "النسخ"
                val cancelMsg = "تم إلغاء عملية $actionName"
                _transferState.value = null
                stopForegroundService()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, cancelMsg)
                    _transferCompletionEvents.emit(false to cancelMsg)
                }
            } catch (e: Exception) {
                val actionName = if (type == TransferType.MOVE) "النقل" else "النسخ"
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

        fun getInstance(context: Context, videoDao: VideoDao): FileTransferManager {
            return instance ?: synchronized(this) {
                instance ?: FileTransferManager(context.applicationContext, videoDao).also { instance = it }
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
                val descriptionText = "إشعارات تقدم عمليات نقل ونسخ الفيديوهات في الخلفية"
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

            val title = if (progress?.type == TransferType.MOVE) "جاري نقل الملفات..." else "جاري نسخ الملفات..."
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
