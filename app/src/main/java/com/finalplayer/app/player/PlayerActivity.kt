package com.finalplayer.app.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.finalplayer.app.player.core.MpvTeardownCoordinator
import com.finalplayer.app.player.service.MediaPlaybackService
import com.finalplayer.app.utils.normalizeVideoKey
import com.finalplayer.app.ui.player.PlayerScreen
import com.finalplayer.app.ui.theme.FinalPlayerTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

private data class OrientationConfig(
    val prefKey: String,
    val aspect: Double?,
    val override: String?,
    val isShorts: Boolean
)

class PlayerActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModel()

    private var videoId: String = ""
    private var videoPath: String = ""
    private var videoTitle: String = "Video"

    private val pipBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_PIP_CONTROL) {
                when (intent.getStringExtra(EXTRA_PIP_CONTROL)) {
                    PIP_CONTROL_PLAY_PAUSE -> viewModel.pauseUnpause()
                    PIP_CONTROL_PREV -> {
                        if (viewModel.isPlaylistMode.value) {
                            viewModel.playPreviousVideo()
                        } else {
                            viewModel.seekBy(-10)
                        }
                    }
                    PIP_CONTROL_NEXT -> {
                        if (viewModel.isPlaylistMode.value) {
                            viewModel.playNextVideo()
                        } else {
                            viewModel.seekBy(10)
                        }
                    }
                }
                updatePipParams()
            }
        }
    }

    private val screenUnlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT || intent?.action == Intent.ACTION_SCREEN_ON) {
                val resumeOnUnlock = viewModel.playerPrefs?.resumeOnUnlock?.get() ?: false
                if (resumeOnUnlock) {
                    viewModel.play()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val filter = IntentFilter(ACTION_PIP_CONTROL)
            ContextCompat.registerReceiver(
                this,
                pipBroadcastReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        // Apply screen orientation preference dynamically
        lifecycleScope.launch {
            combine(
                viewModel.playerPrefs?.playerOrientation?.asFlow() ?: flowOf("video"),
                viewModel.videoAspect,
                viewModel.userOrientationOverride,
                viewModel.isShortsMode
            ) { prefKey, aspect, override, isShorts ->
                OrientationConfig(prefKey, aspect, override, isShorts)
            }.collect { cfg ->
                applyOrientation(cfg.prefKey, cfg.aspect, cfg.override, cfg.isShorts)
            }
        }

        // Apply system status & navigation bar preferences dynamically based on controls visibility
        lifecycleScope.launch {
            combine(
                viewModel.controlsShown,
                viewModel.playerPrefs?.showSystemStatusBar?.asFlow() ?: flowOf(false),
                viewModel.playerPrefs?.showSystemNavigationBar?.asFlow() ?: flowOf(false)
            ) { visible: Boolean, showStatus: Boolean, showNav: Boolean ->
                Triple(visible, showStatus, showNav)
            }.collect { (visible, showStatus, showNav) ->
                val insets = WindowInsetsControllerCompat(window, window.decorView)
                if (visible) {
                    if (showStatus) insets.show(WindowInsetsCompat.Type.statusBars()) else insets.hide(WindowInsetsCompat.Type.statusBars())
                    if (showNav) insets.show(WindowInsetsCompat.Type.navigationBars()) else insets.hide(WindowInsetsCompat.Type.navigationBars())
                } else {
                    insets.hide(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        // Keep PiP actions and aspect ratio updated
        lifecycleScope.launch {
            combine(
                viewModel.paused,
                viewModel.videoAspect
            ) { paused, aspect ->
                Pair(paused, aspect)
            }.collect {
                updatePipParams()
            }
        }

        // Listen for activity finish event (close after playback)
        lifecycleScope.launch {
            viewModel.finishActivityEvent.collect {
                finish()
            }
        }

        // Screen unlock receiver for resumeOnUnlock preference
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        try {
            registerReceiver(screenUnlockReceiver, screenFilter)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screenUnlockReceiver", e)
        }

        // Parse video/audio details from intent
        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
            ?: intent.data?.toString()
            ?: ""
        videoId = normalizeVideoKey(videoPath)
        
        val customTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        if (!customTitle.isNullOrEmpty()) {
            videoTitle = customTitle
        } else if (intent.data != null) {
            val uri = intent.data!!
            if (uri.scheme == "content") {
                var displayName: String? = null
                try {
                    contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val colIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (colIdx >= 0) displayName = cursor.getString(colIdx)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("PlayerActivity", "Could not query media display name", e)
                }
                videoTitle = displayName ?: uri.lastPathSegment ?: "Media Playback"
            } else {
                videoTitle = uri.lastPathSegment ?: "Media Playback"
            }
        } else {
            videoTitle = "Media Playback"
        }

        val isShortsMode = intent.getBooleanExtra(EXTRA_IS_SHORTS_MODE, false)
        val shortsIndex = intent.getIntExtra(EXTRA_SHORTS_INDEX, 0)
        val shortsUris = intent.getStringArrayListExtra(EXTRA_SHORTS_URIS)
        val shortsTitles = intent.getStringArrayListExtra(EXTRA_SHORTS_TITLES)
        val shortsIds = intent.getStringArrayListExtra(EXTRA_SHORTS_IDS)

        val playlistIndex = intent.getIntExtra(EXTRA_PLAYLIST_INDEX, 0)
        val playlistUris = intent.getStringArrayListExtra(EXTRA_PLAYLIST_URIS)
        val playlistTitles = intent.getStringArrayListExtra(EXTRA_PLAYLIST_TITLES)
        val playlistIds = intent.getStringArrayListExtra(EXTRA_PLAYLIST_IDS)
        val playlistDurations = intent.getLongArrayExtra(EXTRA_PLAYLIST_DURATIONS)

        val holderData = PlayerPlaylistHolder.getPlaylist()

        if (isShortsMode && !shortsUris.isNullOrEmpty()) {
            val items = shortsUris.mapIndexed { index, uri ->
                com.finalplayer.app.domain.model.VideoItem(
                    id = normalizeVideoKey(uri),
                    uri = uri,
                    title = shortsTitles?.getOrNull(index) ?: "Short",
                    duration = 0L,
                    sizeBytes = 0L,
                    dateAdded = 0L,
                    folderPath = ""
                )
            }
            viewModel.setShortsPlaylist(items, shortsIndex)
        } else if (holderData != null && holderData.first.isNotEmpty()) {
            val (items, initialIdx) = holderData
            viewModel.setCurrentVideoDetails(videoId, videoTitle, videoPath)
            viewModel.setPlaylist(items, initialIdx)
        } else if (!playlistUris.isNullOrEmpty()) {
            val items = playlistUris.mapIndexed { index, uri ->
                com.finalplayer.app.domain.model.VideoItem(
                    id = playlistIds?.getOrNull(index) ?: normalizeVideoKey(uri),
                    uri = uri,
                    title = playlistTitles?.getOrNull(index) ?: "Video",
                    duration = playlistDurations?.getOrNull(index) ?: 0L,
                    sizeBytes = 0L,
                    dateAdded = 0L,
                    folderPath = ""
                )
            }
            viewModel.setCurrentVideoDetails(videoId, videoTitle, videoPath)
            viewModel.setPlaylist(items, playlistIndex)
        } else {
            viewModel.setCurrentVideoDetails(videoId, videoTitle, videoPath)
            viewModel.autoDiscoverPlaylistForVideo(videoPath, videoId)
        }

        MpvTeardownCoordinator.markActivityCoreInitialized()

        setContent {
            FinalPlayerTheme {
                PlayerScreen(
                    videoPath = videoPath,
                    videoTitle = videoTitle,
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspect = viewModel.videoAspect.value ?: 1.7777
            val rawW = (aspect * 100).toInt()
            val clampedW = rawW.coerceIn(42, 238)
            val rational = Rational(clampedW, 100)

            val isPaused = viewModel.paused.value ?: true

            val prevPendingIntent = PendingIntent.getBroadcast(
                this, 1,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_CONTROL, PIP_CONTROL_PREV),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val playPausePendingIntent = PendingIntent.getBroadcast(
                this, 2,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_CONTROL, PIP_CONTROL_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val nextPendingIntent = PendingIntent.getBroadcast(
                this, 3,
                Intent(ACTION_PIP_CONTROL).putExtra(EXTRA_PIP_CONTROL, PIP_CONTROL_NEXT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevAction = RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_previous),
                "السابق",
                "السابق",
                prevPendingIntent
            )
            val playPauseIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
            val playPauseTitle = if (isPaused) "تشغيل" else "إيقاف"
            val playPauseAction = RemoteAction(
                Icon.createWithResource(this, playPauseIcon),
                playPauseTitle,
                playPauseTitle,
                playPausePendingIntent
            )
            val nextAction = RemoteAction(
                Icon.createWithResource(this, android.R.drawable.ic_media_next),
                "التالي",
                "التالي",
                nextPendingIntent
            )

            val actions = listOf(prevAction, playPauseAction, nextAction)
            val paramsBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(rational)
                .setActions(actions)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val autoPip = viewModel.playerPrefs?.autoPiPOnNavigation?.get() ?: true
                paramsBuilder.setAutoEnterEnabled(autoPip)
            }

            try {
                setPictureInPictureParams(paramsBuilder.build())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set PiP params", e)
            }
        }
    }

    fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                updatePipParams()
                val aspect = viewModel.videoAspect.value ?: 1.7777
                val rawW = (aspect * 100).toInt()
                val clampedW = rawW.coerceIn(42, 238)
                val rational = Rational(clampedW, 100)
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(rational)
                    .build()
                enterPictureInPictureMode(params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enter Picture-in-Picture mode", e)
            }
        }
    }

    private fun applyOrientation(prefKey: String, aspect: Double?, override: String?, isShorts: Boolean) {
        if (override != null) {
            requestedOrientation = when (override) {
                "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            }
            return
        }

        val pathTitleLower = "$videoPath $videoTitle".lowercase(java.util.Locale.ROOT)
        val isSocialVideo = isShorts ||
                pathTitleLower.contains("tiktok") ||
                pathTitleLower.contains("shorts") ||
                pathTitleLower.contains("ytshort") ||
                pathTitleLower.contains("reel") ||
                pathTitleLower.contains("instagram") ||
                pathTitleLower.contains("kwai") ||
                pathTitleLower.contains("likee")

        if (isSocialVideo) {
            // TikTok, YouTube Shorts, Reels stay strictly in vertical portrait unless aspect is explicitly wide (> 1.25)
            requestedOrientation = if (aspect != null && aspect > 1.25) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            return
        }

        requestedOrientation = when (prefKey) {
            "video" -> {
                if (aspect != null && aspect > 0.05) {
                    if (aspect < 0.95) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                } else {
                    // Default to portrait while loading aspect ratio to prevent orientation flashing
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "portrait_reverse" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "portrait_sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "landscape_reverse" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "landscape_sensor" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "smart", "auto" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            "free" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> {
                if (aspect != null && aspect > 0.05) {
                    if (aspect < 0.95) {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
        }
    }

    private var wasInPipMode = false
    private var closedFromPipMode = false

    private fun enableBackgroundAudioService() {
        viewModel.setBackgroundPlay(true)
        MpvTeardownCoordinator.markDetachedService()
        if (videoPath.isNotEmpty()) {
            MediaPlaybackService.startService(applicationContext, videoTitle, videoPath)
        }
    }

    override fun onResume() {
        super.onResume()
        closedFromPipMode = false
        viewModel.onAppResumed(applicationContext)
        val resumeOnUnlock = viewModel.playerPrefs?.resumeOnUnlock?.get() ?: true
        if (resumeOnUnlock) {
            viewModel.play()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val autoPip = viewModel.playerPrefs?.autoPiPOnNavigation?.get() ?: true
        if (autoPip && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPiPMode()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        viewModel.mpvController.getAttachedView()?.applySubtitleFontSizeForMode(isPortrait = isPortrait, isPip = isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            wasInPipMode = true
            closedFromPipMode = false
            viewModel.setControlsShown(false)
        } else {
            viewModel.setControlsShown(true)
            if (wasInPipMode) {
                wasInPipMode = false
                if (isFinishing || lifecycle.currentState < androidx.lifecycle.Lifecycle.State.RESUMED) {
                    closedFromPipMode = true
                    viewModel.pause()
                    viewModel.stopPlayback()
                    com.finalplayer.app.player.core.MPVView.stopAll()
                    viewModel.setBackgroundPlay(false)
                    MediaPlaybackService.stopService(applicationContext)
                    finishAndRemoveTask()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveCurrentProgressNow(isSynchronous = true)
        val isPipMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false
        val hasActiveSleepTimer = (viewModel.remainingTime.value > 0)
        val isBackgroundPlay = viewModel.isBackgroundPlay.value

        if (isPipMode) {
            return
        }

        if (hasActiveSleepTimer || isBackgroundPlay) {
            enableBackgroundAudioService()
        } else {
            viewModel.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveCurrentProgressNow(isSynchronous = true)
        val isPipMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false
        val hasActiveSleepTimer = (viewModel.remainingTime.value > 0)
        val isBackgroundPlay = viewModel.isBackgroundPlay.value

        if (isFinishing || closedFromPipMode) {
            viewModel.pause()
            viewModel.stopPlayback()
            com.finalplayer.app.player.core.MPVView.stopAll()
            viewModel.setBackgroundPlay(false)
            MediaPlaybackService.stopService(applicationContext)
        } else if (!isPipMode && !hasActiveSleepTimer && !isBackgroundPlay) {
            viewModel.pause()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val disableMedia = viewModel.playerPrefs?.disableMediaButtons?.get() ?: false
        if (disableMedia && isMediaKey(keyCode)) {
            return true
        }

        if (event != null) {
            val mpvView = viewModel.mpvController.getAttachedView()
            if (mpvView != null && mpvView.onKey(event)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isMediaKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND -> true
            else -> false
        }
    }

    override fun onDestroy() {
        try {
            viewModel.saveCurrentProgressNow(isSynchronous = true)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(screenUnlockReceiver)
        } catch (e: Exception) {
            // Ignored
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                unregisterReceiver(pipBroadcastReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Receiver not registered", e)
            }
        }
        val hasActiveSleepTimer = (viewModel.remainingTime.value > 0)
        if (!hasActiveSleepTimer || isFinishing || closedFromPipMode) {
            viewModel.pause()
            viewModel.stopPlayback()
            com.finalplayer.app.player.core.MPVView.stopAll()
            viewModel.setBackgroundPlay(false)
            MediaPlaybackService.stopService(applicationContext)
            val mpvView = viewModel.mpvController.getAttachedView()
            MpvTeardownCoordinator.destroyActivityCoreAsync("PlayerActivity onDestroy", mpvView)
        } else {
            MpvTeardownCoordinator.markDetachedService()
            if (videoPath.isNotEmpty()) {
                MediaPlaybackService.startService(applicationContext, videoTitle, videoPath)
            }
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_VIDEO_ID = "EXTRA_VIDEO_ID"
        const val EXTRA_VIDEO_PATH = "EXTRA_VIDEO_PATH"
        const val EXTRA_VIDEO_TITLE = "EXTRA_VIDEO_TITLE"
        const val EXTRA_PLAYLIST_ID = "EXTRA_PLAYLIST_ID"
        const val EXTRA_PLAYLIST_INDEX = "EXTRA_PLAYLIST_INDEX"
        const val EXTRA_PLAYLIST_URIS = "EXTRA_PLAYLIST_URIS"
        const val EXTRA_PLAYLIST_TITLES = "EXTRA_PLAYLIST_TITLES"
        const val EXTRA_PLAYLIST_IDS = "EXTRA_PLAYLIST_IDS"
        const val EXTRA_PLAYLIST_DURATIONS = "EXTRA_PLAYLIST_DURATIONS"

        const val EXTRA_IS_SHORTS_MODE = "EXTRA_IS_SHORTS_MODE"
        const val EXTRA_SHORTS_INDEX = "EXTRA_SHORTS_INDEX"
        const val EXTRA_SHORTS_URIS = "EXTRA_SHORTS_URIS"
        const val EXTRA_SHORTS_TITLES = "EXTRA_SHORTS_TITLES"
        const val EXTRA_SHORTS_IDS = "EXTRA_SHORTS_IDS"

        const val ACTION_PIP_CONTROL = "com.finalplayer.app.ACTION_PIP_CONTROL"
        const val EXTRA_PIP_CONTROL = "EXTRA_PIP_CONTROL"
        const val PIP_CONTROL_PLAY_PAUSE = "PIP_CONTROL_PLAY_PAUSE"
        const val PIP_CONTROL_PREV = "PIP_CONTROL_PREV"
        const val PIP_CONTROL_NEXT = "PIP_CONTROL_NEXT"
    }
}
