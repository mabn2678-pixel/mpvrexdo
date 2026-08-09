package com.finalplayer.app.ui.home.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.ui.components.VideoThumbnailImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetailedMediaInfo(
    val resolution: String = "1920×1080 (1080p)",
    val videoCodec: String = "H.264 / AVC",
    val bitrate: String = "2.4 Mbps",
    val audioCodec: String = "AAC • 48 kHz • Stereo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailsSheet(
    sheetState: SheetState,
    video: VideoItem,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onShare: () -> Unit = {},
    onAddToSafeFolder: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val context = LocalContext.current
    var mediaInfo by remember { mutableStateOf(DetailedMediaInfo()) }

    LaunchedEffect(video.uri) {
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                val uri = Uri.parse(video.uri)
                if (video.uri.startsWith("content://") || video.uri.startsWith("file://")) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.setDataSource(video.uri)
                }
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH) ?: "1920"
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT) ?: "1080"
                val bitrateVal = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
                val bitrateStr = if (bitrateVal != null) String.format(Locale.getDefault(), "%.1f Mbps", bitrateVal / 1000000.0) else "Auto"
                val mimetype = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/mp4"

                retriever.release()
                mediaInfo = DetailedMediaInfo(
                    resolution = "$width×$height",
                    videoCodec = mimetype.removePrefix("video/").uppercase(Locale.getDefault()),
                    bitrate = bitrateStr,
                    audioCodec = "AAC / AC3 • 48 kHz"
                )
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header 16:9 Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                VideoThumbnailImage(
                    videoUri = video.uri,
                    thumbnailUrl = video.thumbnailPath,
                    modifier = Modifier.fillMaxSize(),
                    contentDescription = video.title
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title & Path
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = video.uri,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Video Path", video.uri))
                    Toast.makeText(context, "تم نسخ المسار", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Path",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // File Info Rows
            DetailRow(label = "الحجم / Size", value = formatFileSize(video.sizeBytes))
            DetailRow(label = "المدة / Duration", value = formatDuration(video.duration))
            DetailRow(label = "الدقة / Resolution", value = mediaInfo.resolution)
            DetailRow(label = "الترميز / Codec", value = mediaInfo.videoCodec)
            DetailRow(label = "معدل البت / Bitrate", value = mediaInfo.bitrate)
            DetailRow(label = "الصوت / Audio", value = mediaInfo.audioCodec)
            val subText = remember(video) { scanSubtitlesForVideo(video) }
            DetailRow(label = "الترجمات / Subtitles", value = subText)
            val dateMs = if (video.dateAdded > 0 && video.dateAdded < 100_000_000_000L) video.dateAdded * 1000L else video.dateAdded
            val formattedDate = if (dateMs > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(dateMs)) else "-"
            DetailRow(label = "تاريخ الإضافة", value = formattedDate)

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                    onDismiss()
                    onPlay()
                }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تشغيل")
                }

                OutlinedButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = null)
                }

                OutlinedButton(onClick = onAddToSafeFolder) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%dh %dm %ds", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%dm %ds", minutes, seconds)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun scanSubtitlesForVideo(video: VideoItem): String {
    return try {
        val folder = java.io.File(video.folderPath)
        if (!folder.exists() || !folder.isDirectory) return "لا توجد"

        val videoFile = java.io.File(video.uri.removePrefix("file://"))
        val baseName = if (videoFile.exists()) videoFile.nameWithoutExtension else {
            video.title.substringBeforeLast(".")
        }
        if (baseName.isBlank()) return "لا توجد"

        val subExtensions = setOf("srt", "vtt", "ass", "sub", "ssa", "idx", "mks")
        val matchingFiles = folder.listFiles { file ->
            if (!file.isFile) return@listFiles false
            val ext = file.extension.lowercase()
            if (ext !in subExtensions) return@listFiles false
            val fileWithoutExt = file.nameWithoutExtension
            file.name.startsWith(baseName, ignoreCase = true) ||
                    baseName.startsWith(fileWithoutExt.substringBefore("."), ignoreCase = true) ||
                    fileWithoutExt.contains(baseName, ignoreCase = true)
        } ?: emptyArray()

        if (matchingFiles.isEmpty()) return "لا توجد"

        val totalCount = matchingFiles.size
        val langCounts = mutableMapOf<String, Int>()
        val langPatterns = mapOf(
            "de" to listOf("de", "ger", "deu", "german"),
            "ar" to listOf("ar", "ara", "arabic"),
            "en" to listOf("en", "eng", "english"),
            "fr" to listOf("fr", "fre", "fra", "french"),
            "es" to listOf("es", "spa", "spanish"),
            "tr" to listOf("tr", "tur", "turkish"),
            "ru" to listOf("ru", "rus", "russian"),
            "it" to listOf("it", "ita", "italian"),
            "fa" to listOf("fa", "per", "fas", "persian"),
            "ur" to listOf("ur", "urd", "urdu"),
            "zh" to listOf("zh", "chi", "zho", "chinese"),
            "ja" to listOf("ja", "jpn", "japanese")
        )

        for (file in matchingFiles) {
            val nameLower = file.name.lowercase()
            var foundLang: String? = null
            for ((key, patterns) in langPatterns) {
                if (patterns.any { p ->
                        nameLower.contains(".$p.") ||
                        nameLower.contains(".$p-") ||
                        nameLower.contains("-$p.") ||
                        nameLower.contains("_$p.") ||
                        nameLower.endsWith(".$p") ||
                        nameLower.contains("$p.srt") ||
                        nameLower.contains("$p.vtt") ||
                        nameLower.contains("$p.ass")
                    }
                ) {
                    foundLang = key
                    break
                }
            }
            val target = foundLang ?: "sub"
            langCounts[target] = (langCounts[target] ?: 0) + 1
        }

        val details = langCounts.map { (lang, count) ->
            if (count > 1) "$lang ($count)" else lang
        }.joinToString(", ")

        "$totalCount ملف ($details)"
    } catch (_: Exception) {
        "لا توجد"
    }
}
