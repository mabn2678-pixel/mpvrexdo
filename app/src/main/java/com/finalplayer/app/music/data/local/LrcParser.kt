package com.finalplayer.app.music.data.local

import com.finalplayer.app.music.data.model.LrcLine
import com.finalplayer.app.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

class LrcParser {

    data class ParsedLrc(
        val lines: List<LrcLine>,
        val title: String? = null,
        val artist: String? = null,
        val offset: Long = 0L
    )

    suspend fun parseFile(filePath: String): ParsedLrc? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists() || !file.canRead()) return@withContext null
            val content = file.readText(Charsets.UTF_8)
            parseContent(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseContent(content: String): ParsedLrc {
        var title: String? = null
        var artist: String? = null
        var offset: Long = 0L
        val rawLinesList = mutableListOf<LrcLine>()

        val timestampPattern = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{2,3}))?\\]")
        val tagPattern = Pattern.compile("\\[([a-zA-Z]+):(.*)\\]")

        content.lines().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            val matcher = timestampPattern.matcher(line)
            var lastMatchEnd = 0
            val timestamps = mutableListOf<Long>()

            while (matcher.find()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val msStr = matcher.group(3)

                val min = minStr.toLongOrNull() ?: 0L
                val sec = secStr.toLongOrNull() ?: 0L
                var ms = 0L
                if (msStr != null) {
                    ms = msStr.toLongOrNull() ?: 0L
                    if (msStr.length == 2) {
                        ms *= 10
                    }
                }

                val timeMs = min * 60000L + sec * 1000L + ms
                timestamps.add(timeMs)
                lastMatchEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                val text = line.substring(lastMatchEnd).trim()
                for (ts in timestamps) {
                    rawLinesList.add(LrcLine(timeMs = ts, text = text))
                }
            } else {
                val tagMatcher = tagPattern.matcher(line)
                if (tagMatcher.matches()) {
                    val key = tagMatcher.group(1)?.lowercase()
                    val value = tagMatcher.group(2)?.trim()
                    when (key) {
                        "ti" -> title = value
                        "ar" -> artist = value
                        "offset" -> offset = value?.toLongOrNull() ?: 0L
                    }
                }
            }
        }

        val finalLines = rawLinesList
            .map { it.copy(timeMs = (it.timeMs + offset).coerceAtLeast(0L)) }
            .sortedBy { it.timeMs }

        return ParsedLrc(
            lines = finalLines,
            title = title,
            artist = artist,
            offset = offset
        )
    }

    suspend fun findLrcForSong(song: Song): ParsedLrc = withContext(Dispatchers.IO) {
        if (song.path.isNotBlank()) {
            val audioFile = File(song.path)
            val parent = audioFile.parentFile
            if (parent != null) {
                val baseName = audioFile.nameWithoutExtension

                val lrcFile = File(parent, "$baseName.lrc")
                if (lrcFile.exists()) {
                    val parsed = parseFile(lrcFile.absolutePath)
                    if (parsed != null && parsed.lines.isNotEmpty()) return@withContext parsed
                }

                val altLrcFile = File("${song.path}.lrc")
                if (altLrcFile.exists()) {
                    val parsed = parseFile(altLrcFile.absolutePath)
                    if (parsed != null && parsed.lines.isNotEmpty()) return@withContext parsed
                }
            }
        }

        generateFallbackLyrics(song)
    }

    private fun generateFallbackLyrics(song: Song): ParsedLrc {
        val totalMs = if (song.duration > 0) song.duration else 180000L
        val lines = mutableListOf<LrcLine>()

        val sampleLyrics = listOf(
            "♪ (مقدمة موسيقية رائعة)",
            "أهلاً بك في تطبيق Final Player للموسيقى",
            "أغنية: ${song.title}",
            "بصوت الفنان: ${song.artist}",
            "من ألبوم: ${song.album}",
            "♪ الألحان تعزف بأجمل النغمات...",
            "كلمات الأغنية تنساب بروعة وسلاسة",
            "يمكنك الضغط على أي سطر للانتقال المباشر",
            "♪ استمتع بتجربة استماع فريدة وبجودة عالية",
            "الموسيقى تُثري الروح وتمنحك الهدوء",
            "شريط التمرير يساعدك على التنقل بسهولة",
            "♪ ختام المقطع الموسيقي..."
        )

        val intervalMs = (totalMs / sampleLyrics.size.coerceAtLeast(1)).coerceAtLeast(5000L)
        sampleLyrics.forEachIndexed { index, text ->
            val timeMs = (index * intervalMs).coerceAtMost(totalMs - 1000L)
            lines.add(LrcLine(timeMs = timeMs, text = text))
        }

        return ParsedLrc(
            lines = lines,
            title = song.title,
            artist = song.artist
        )
    }

    fun getCurrentLineIndex(lines: List<LrcLine>, positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (positionMs >= lines[i].timeMs) {
                return i
            }
        }
        return -1
    }
}
