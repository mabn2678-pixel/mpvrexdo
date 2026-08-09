package com.finalplayer.app.ui.securefolder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finalplayer.app.data.database.dao.SecureMediaDao
import com.finalplayer.app.data.database.entities.SecureMediaEntity
import com.finalplayer.app.data.preferences.SecurePinPreferences
import com.finalplayer.app.domain.model.VideoItem
import com.finalplayer.app.domain.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SecureFolderViewModel(
    private val secureMediaDao: SecureMediaDao,
    private val videoRepository: VideoRepository,
    private val pinPreferences: SecurePinPreferences
) : ViewModel() {

    // حالة القفل — لا تُحفظ بين الجلسات
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet: StateFlow<Boolean> = _isPinSet.asStateFlow()

    private val _sortBy = MutableStateFlow("title")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _layoutMode = MutableStateFlow("list")
    val layoutMode: StateFlow<String> = _layoutMode.asStateFlow()

    init {
        viewModelScope.launch {
            pinPreferences.hasPin.asFlow().collect { hasPin ->
                _isPinSet.value = hasPin
            }
        }
        viewModelScope.launch {
            pinPreferences.sortBy.asFlow().collect {
                _sortBy.value = it
            }
        }
        viewModelScope.launch {
            pinPreferences.sortAscending.asFlow().collect {
                _sortAscending.value = it
            }
        }
        viewModelScope.launch {
            pinPreferences.layoutMode.asFlow().collect {
                _layoutMode.value = it
            }
        }
    }

    val secureVideoCount: StateFlow<Int> =
        secureMediaDao.getCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val secureVideos: StateFlow<List<VideoItem>> =
        combine(
            videoRepository.getSecureVideos(),
            _sortBy,
            _sortAscending
        ) { videos, sort, asc ->
            val sorted = when (sort) {
                "date" -> videos.sortedBy { it.dateAdded }
                "size" -> videos.sortedBy { it.sizeBytes }
                "duration" -> videos.sortedBy { it.duration }
                else -> videos.sortedBy { it.title.lowercase(java.util.Locale.ROOT) }
            }
            if (asc) sorted else sorted.reversed()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortBy(value: String) {
        _sortBy.value = value
        pinPreferences.sortBy.set(value)
    }

    fun setSortAscending(value: Boolean) {
        _sortAscending.value = value
        pinPreferences.sortAscending.set(value)
    }

    fun setLayoutMode(value: String) {
        _layoutMode.value = value
        pinPreferences.layoutMode.set(value)
    }

    val pinLength: Int
        get() = pinPreferences.pinLength.get().let { if (it in 4..6) it else 4 }

    private var autoLockJob: Job? = null

    // ════ PIN Setup ════

    fun setupPin(pin: String) {
        pinPreferences.setPin(pin)
        _isPinSet.value = true
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!pinPreferences.verifyPin(oldPin)) return false
        pinPreferences.setPin(newPin)
        return true
    }

    fun removePin() {
        pinPreferences.hasPin.set(false)
        pinPreferences.pinHash.set("")
        _isUnlocked.value = false
        _isPinSet.value = false
    }

    // ════ Unlock ════

    fun verifyPinAndUnlock(pin: String): UnlockResult {
        if (pinPreferences.isLocked()) {
            val remaining = pinPreferences.remainingLockSeconds()
            return UnlockResult.Locked(remaining)
        }
        return if (pinPreferences.verifyPin(pin)) {
            pinPreferences.resetFailedAttempts()
            unlock()
            UnlockResult.Success
        } else {
            val locked = pinPreferences.recordFailedAttempt()
            val attemptsLeft = 5 - pinPreferences.failedAttempts.get()
            if (locked) UnlockResult.Locked(30L)
            else UnlockResult.WrongPin(attemptsLeft.coerceAtLeast(0))
        }
    }

    fun unlock() {
        _isUnlocked.value = true
        startAutoLockTimer()
    }

    fun lock() {
        _isUnlocked.value = false
        autoLockJob?.cancel()
    }

    private fun startAutoLockTimer() {
        autoLockJob?.cancel()
        val minutes = pinPreferences.autoLockMinutes.get()
        if (minutes <= 0) return  // 0 = لا يقفل تلقائياً
        autoLockJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            lock()
        }
    }

    // ════ Media Management ════

    fun addToSecureFolder(videoId: String, originalPath: String) {
        viewModelScope.launch {
            secureMediaDao.insert(
                SecureMediaEntity(
                    videoId = videoId,
                    originalPath = originalPath
                )
            )
        }
    }

    fun removeFromSecureFolder(videoId: String) {
        viewModelScope.launch {
            secureMediaDao.remove(videoId)
        }
    }

    suspend fun isSecure(videoId: String): Boolean =
        secureMediaDao.isSecure(videoId)

    // ════ Biometric ════

    fun enableBiometric(enabled: Boolean) {
        pinPreferences.biometricEnabled.set(enabled)
    }

    val isBiometricEnabled get() = pinPreferences.biometricEnabled.get()

    sealed class UnlockResult {
        object Success : UnlockResult()
        data class WrongPin(val attemptsLeft: Int) : UnlockResult()
        data class Locked(val secondsRemaining: Long) : UnlockResult()
    }
}
