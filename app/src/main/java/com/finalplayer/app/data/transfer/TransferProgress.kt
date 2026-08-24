package com.finalplayer.app.data.transfer

enum class TransferType {
    COPY,
    MOVE,
    HIDE_TO_SECURE,
    RESTORE_FROM_SECURE
}

data class TransferProgress(
    val type: TransferType = TransferType.COPY,
    val isRunning: Boolean = false,
    val isBackground: Boolean = false,
    val currentFileIndex: Int = 0,
    val totalFileCount: Int = 0,
    val currentFileName: String = "",
    val transferredBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val transferredSizeFormatted: String = "0 B",
    val totalSizeFormatted: String = "0 B",
    val destinationPath: String = "",
    val error: String? = null
)
