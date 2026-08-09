package com.finalplayer.app.ui.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.data.database.dao.SecureMediaDao
import com.finalplayer.app.data.database.dao.VideoDao
import com.finalplayer.app.data.mapper.toDomainModel
import com.finalplayer.app.domain.model.PlaybackProgress
import com.finalplayer.app.domain.model.RecentVideoItem
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.PlaybackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecentsViewModel(
    private val playbackRepository: PlaybackRepository,
    private val videoDao: VideoDao,
    private val secureMediaDao: SecureMediaDao
) : ViewModel() {

    init {
        pruneOldHistory()
    }

    private fun pruneOldHistory() {
        viewModelScope.launch {
            val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
            playbackRepository.deleteOlderThan(threeDaysAgo)
            playbackRepository.trimExcessHistory()
        }
    }

    val recentlyPlayed: StateFlow<List<RecentVideoItem>> = combine(
        playbackRepository.getRecentlyPlayed(50),
        videoDao.getAllVideos(),
        secureMediaDao.getAllSecureVideoIds()
    ) { progressList, allVideos, secureIds ->
        val secureSet = secureIds.toSet()
        val threeDaysAgo = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
        val validProgress = progressList
            .filter { it.lastPlayedTimestamp >= threeDaysAgo && it.videoId !in secureSet }
            .distinctBy { it.videoId }
            .take(50)

        val videoMap = allVideos.filter { it.id !in secureSet }.associateBy { it.id }
        validProgress.mapNotNull { progress ->
            val videoEntity = videoMap[progress.videoId]
            val video = videoEntity?.toDomainModel() ?: VideoItem(
                id = progress.videoId,
                uri = progress.videoId,
                title = progress.videoId.substringAfterLast('/'),
                duration = progress.durationMs,
                sizeBytes = 0L,
                dateAdded = progress.lastPlayedTimestamp,
                folderPath = ""
            )

            val percent = if (progress.durationMs > 0) {
                (progress.positionMs.toFloat() / progress.durationMs.toFloat() * 100f).coerceIn(0f, 100f)
            } else 0f

            val completed = percent > 95f
            val timeFormatted = formatRelativeDate(progress.lastPlayedTimestamp)

            RecentVideoItem(
                video = video,
                progress = progress,
                progressPercent = percent,
                isCompleted = completed,
                lastPlayedFormatted = timeFormatted
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearHistory() {
        viewModelScope.launch {
            playbackRepository.clearHistory()
        }
    }

    fun removeFromHistory(videoId: String) {
        viewModelScope.launch {
            playbackRepository.removeFromHistory(videoId)
        }
    }

    private fun formatRelativeDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val played = Calendar.getInstance().apply { timeInMillis = timestamp }

        val diffMillis = now.timeInMillis - timestamp
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

        return when {
            diffDays == 0 && now.get(Calendar.DAY_OF_YEAR) == played.get(Calendar.DAY_OF_YEAR) -> "اليوم"
            diffDays == 1 || (diffDays == 0 && now.get(Calendar.DAY_OF_YEAR) != played.get(Calendar.DAY_OF_YEAR)) -> "أمس"
            diffDays in 2..10 -> "منذ $diffDays أيام"
            else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
