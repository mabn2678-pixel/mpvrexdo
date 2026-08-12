package com.finalplayer.app.player.core

import android.content.Context
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import dev.jdtech.mpv.MPVLib
import com.finalplayer.app.ui.player.controls.components.sheets.TrackNode
import com.finalplayer.app.ui.player.controls.components.sheets.ChapterNode
import java.io.File

class MPVView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var mpvLib: MPVLib? = null
    var isInitialized = false
        private set

    var isSurfaceReady = false
        private set

    var onSurfaceReady: (() -> Unit)? = null

    // Playback state properties
    var isPaused: Boolean = true
        private set
    var positionMs: Long = 0L
        private set
    var durationMs: Long = 0L
        private set
    var isPausedForCache: Boolean = false
        private set
    var cacheTimeSeconds: Double = 0.0
        private set
    var isEofReached: Boolean = false
        private set
    var videoAspect: Double = 1.7777777777777777 // 16:9 default
        private set

    init {
        holder.addCallback(this)
    }

    fun initialize(context: Context, configDir: File) {
        synchronized(this) {
            if (isInitialized) return
            try {
                val lib = MPVLib.create(context)
                if (lib != null) {
                    mpvLib = lib
                    initOptions(configDir, lib)
                    lib.init()
                    isInitialized = true
                    Log.d(TAG, "MPVLib initialized successfully")

                    if (isSurfaceReady) {
                        lib.attachSurface(holder.surface)
                    }
                } else {
                    Log.e(TAG, "MPVLib.create returned null")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error initializing MPVLib", e)
            }
        }
    }

    private fun initOptions(configDir: File, lib: MPVLib) {
        try {
            lib.setOptionString("config", "yes")
            lib.setOptionString("config-dir", configDir.absolutePath)

            // Hardware decoding setup (mediacodec direct zero-copy for maximum hardware efficiency)
            lib.setOptionString("hwdec", "mediacodec")
            lib.setOptionString("hwdec-codecs", "all")

            // Video output setup
            lib.setOptionString("vo", "gpu")
            lib.setOptionString("gpu-context", "android")

            // Optimization for performance & thermal efficiency
            lib.setOptionString("vd-lavc-dr", "yes")
            lib.setOptionString("hr-seek", "no")
            lib.setOptionString("hr-seek-framedrop", "yes")
            lib.setOptionString("video-sync", "audio")
            lib.setOptionString("framedrop", "vo")

            // Buffer & caching options for long video playback
            lib.setOptionString("demuxer-max-bytes", "32MiB")
            lib.setOptionString("demuxer-max-back-bytes", "16MiB")

            // Screenshot directory setup
            val screenshotDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: File(context.filesDir, "screenshots")
            if (!screenshotDir.exists()) screenshotDir.mkdirs()
            lib.setOptionString("screenshot-directory", screenshotDir.absolutePath)

            applySubtitleOptions(lib)

            Log.d(TAG, "MPV options initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting MPV options", e)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        synchronized(this) {
            if (isInitialized) {
                try {
                    mpvLib?.attachSurface(holder.surface)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error attaching surface", e)
                }
            }
        }
        onSurfaceReady?.invoke()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(this) {
            if (isInitialized) {
                try {
                    mpvLib?.setPropertyString("android-surface-size", "${width}x${height}")
                } catch (e: Throwable) {
                    Log.e(TAG, "Error updating surface size", e)
                }
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        synchronized(this) {
            if (isInitialized) {
                try {
                    mpvLib?.detachSurface()
                } catch (e: Throwable) {
                    Log.e(TAG, "Error detaching surface", e)
                }
            }
        }
    }

    private fun getActiveLib(): MPVLib? {
        return synchronized(this) {
            if (isInitialized) mpvLib else null
        }
    }

    fun playFile(path: String) {
        val lib = getActiveLib() ?: return
        try {
            lib.command(arrayOf("loadfile", path))
            isPaused = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error playing file: $path", e)
        }
    }

    fun pause() {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyBoolean("pause", true)
            isPaused = true
        } catch (e: Throwable) {
            Log.e(TAG, "Error pausing MPV", e)
        }
    }

    fun unpause() {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyBoolean("pause", false)
            isPaused = false
        } catch (e: Throwable) {
            Log.e(TAG, "Error unpausing MPV", e)
        }
    }

    fun togglePause() {
        if (isPaused) unpause() else pause()
    }

    fun seekTo(positionSeconds: Double, mode: String = "absolute+exact") {
        val lib = getActiveLib() ?: return
        try {
            lib.command(arrayOf("seek", positionSeconds.toString(), mode))
        } catch (e: Throwable) {
            Log.e(TAG, "Error seeking to $positionSeconds ($mode)", e)
        }
    }

    fun seekBy(offsetSeconds: Int) {
        val lib = getActiveLib() ?: return
        try {
            lib.command(arrayOf("seek", offsetSeconds.toString(), "relative"))
        } catch (e: Throwable) {
            Log.e(TAG, "Error relative seek $offsetSeconds", e)
        }
    }

    fun setOptionString(name: String, value: String) {
        val lib = getActiveLib() ?: return
        try {
            lib.setOptionString(name, value)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting option string $name=$value", e)
        }
    }

    fun applySubtitleOptions(libParam: MPVLib? = null) {
        val lib = libParam ?: getActiveLib() ?: return
        try {
            lib.setOptionString("sub-auto", "no")
            lib.setOptionString("embeddedfonts", "yes")
            lib.setOptionString("sub-ass-override", "scale")
            lib.setOptionString("sub-use-margins", "yes")
            lib.setOptionString("blend-subtitles", "no")
            lib.setOptionString("sub-margin-y", "5")
            lib.setOptionString("sub-scale-by-window", "no")
            lib.setOptionString("sub-scale-with-window", "no")
            lib.setOptionString("sub-font-size", "55")
            lib.setOptionString("sub-pos", "100")
            lib.setOptionString("sub-border-size", "3.0")
            lib.setOptionString("sub-color", "#FFFFFFFF")
            lib.setOptionString("sub-border-color", "#FF000000")
            lib.setOptionString("sub-shadow-offset", "1")
            lib.setOptionString("sub-shadow-color", "#80000000")
        } catch (e: Throwable) {
            Log.e(TAG, "Error applying subtitle options", e)
        }
    }

    fun isIdle(): Boolean {
        val lib = getActiveLib() ?: return true
        return try {
            lib.getPropertyBoolean("idle-active") ?: true
        } catch (e: Throwable) {
            true
        }
    }

    fun getPropertyString(name: String): String? {
        val lib = getActiveLib() ?: return null
        if (name != "idle-active" && isIdle()) return null
        return try {
            lib.getPropertyString(name)
        } catch (e: Throwable) {
            null
        }
    }

    fun setPropertyString(name: String, value: String) {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyString(name, value)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting property string $name=$value", e)
        }
    }

    fun setPropertyInt(name: String, value: Int) {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyInt(name, value)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting property int $name=$value", e)
        }
    }

    fun setPropertyBoolean(name: String, value: Boolean) {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyBoolean(name, value)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting property boolean $name=$value", e)
        }
    }

    fun getPropertyInt(name: String): Int? {
        val lib = getActiveLib() ?: return null
        if (name != "idle-active" && isIdle()) return null
        return try {
            lib.getPropertyInt(name)
        } catch (e: Throwable) {
            null
        }
    }

    fun getPropertyBoolean(name: String): Boolean? {
        val lib = getActiveLib() ?: return null
        return try {
            lib.getPropertyBoolean(name)
        } catch (e: Throwable) {
            null
        }
    }

    fun setPropertyFloat(name: String, value: Float) {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyDouble(name, value.toDouble())
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting property float $name=$value", e)
        }
    }

    fun setPropertyDouble(name: String, value: Double) {
        val lib = getActiveLib() ?: return
        try {
            lib.setPropertyDouble(name, value)
        } catch (e: Throwable) {
            Log.e(TAG, "Error setting property double $name=$value", e)
        }
    }

    fun getPropertyDouble(name: String): Double? {
        val lib = getActiveLib() ?: return null
        if (name != "idle-active" && isIdle()) return null
        return try {
            lib.getPropertyDouble(name)
        } catch (e: Throwable) {
            null
        }
    }

    fun getChapterList(): List<ChapterNode> {
        val lib = getActiveLib() ?: return emptyList()
        if (lib.getPropertyBoolean("idle-active") == true) return emptyList()
        val list = mutableListOf<ChapterNode>()
        try {
            val count = lib.getPropertyInt("chapter-list/count") ?: 0
            for (i in 0 until count) {
                val title = lib.getPropertyString("chapter-list/$i/title") ?: ""
                val time = lib.getPropertyDouble("chapter-list/$i/time") ?: 0.0
                list.add(ChapterNode(title = title, time = time))
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting chapter list", e)
        }
        return list
    }

    fun getCurrentChapter(): Int? {
        val lib = getActiveLib() ?: return null
        if (lib.getPropertyBoolean("idle-active") == true) return null
        return try {
            lib.getPropertyInt("chapter")
        } catch (e: Throwable) {
            null
        }
    }

    fun getSubtitleText(): String? {
        val lib = getActiveLib() ?: return null
        if (lib.getPropertyBoolean("idle-active") == true) return null
        return getPropertyString("sub-text")
    }

    fun getTrackList(): List<TrackNode> {
        val lib = getActiveLib() ?: return emptyList()
        if (lib.getPropertyBoolean("idle-active") == true) return emptyList()
        val list = mutableListOf<TrackNode>()
        try {
            val count = lib.getPropertyInt("track-list/count") ?: 0
            for (i in 0 until count) {
                val type = lib.getPropertyString("track-list/$i/type") ?: continue
                if (type != "sub" && type != "audio") continue
                val id = lib.getPropertyInt("track-list/$i/id") ?: (i + 1)
                val lang = lib.getPropertyString("track-list/$i/lang") ?: ""
                val title = lib.getPropertyString("track-list/$i/title") ?: ""
                val isDefault = lib.getPropertyBoolean("track-list/$i/default") ?: false
                val forced = lib.getPropertyBoolean("track-list/$i/forced") ?: false
                val external = lib.getPropertyBoolean("track-list/$i/external") ?: false
                val extFilename = lib.getPropertyString("track-list/$i/external-filename")

                list.add(
                    TrackNode(
                        id = id,
                        type = type,
                        lang = lang,
                        title = title,
                        isDefault = isDefault,
                        forced = forced,
                        external = external,
                        externalFilename = extFilename
                    )
                )
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error getting track list", e)
        }
        return list
    }

    fun detachSurface() {
        val lib = synchronized(this) { mpvLib } ?: return
        try {
            lib.detachSurface()
        } catch (e: Throwable) {
            Log.e(TAG, "Error detaching surface manually", e)
        }
    }

    fun command(args: Array<String>) {
        val lib = getActiveLib() ?: return
        try {
            lib.command(args)
        } catch (e: Throwable) {
            Log.e(TAG, "Error running command: ${args.joinToString()}", e)
        }
    }

    fun updatePlaybackState() {
        val lib = getActiveLib() ?: return
        try {
            val isIdle = lib.getPropertyBoolean("idle-active") ?: true
            if (isIdle) {
                positionMs = 0L
                durationMs = 0L
                isPaused = true
                isPausedForCache = false
                cacheTimeSeconds = 0.0
                isEofReached = false
                return
            }

            val posSec = lib.getPropertyDouble("time-pos") ?: 0.0
            val durSec = lib.getPropertyDouble("duration") ?: 0.0
            val paused = lib.getPropertyBoolean("pause") ?: true
            val pausedForCache = lib.getPropertyBoolean("paused-for-cache") ?: false
            val cacheSec = lib.getPropertyDouble("demuxer-cache-time") ?: 0.0
            val eof = lib.getPropertyBoolean("eof-reached") ?: false
            val rawAspect = lib.getPropertyDouble("video-params/aspect") ?: 0.0
            val dw = lib.getPropertyInt("video-params/dw") ?: 0
            val dh = lib.getPropertyInt("video-params/dh") ?: 0
            val w = lib.getPropertyInt("video-params/w") ?: 0
            val h = lib.getPropertyInt("video-params/h") ?: 0
            val rotate = lib.getPropertyInt("video-params/rotate") ?: 0

            val effectiveAspect = if (dw > 0 && dh > 0) {
                if (rotate == 90 || rotate == 270) dh.toDouble() / dw.toDouble()
                else dw.toDouble() / dh.toDouble()
            } else if (w > 0 && h > 0) {
                if (rotate == 90 || rotate == 270) h.toDouble() / w.toDouble()
                else w.toDouble() / h.toDouble()
            } else if (rawAspect > 0) {
                if (rotate == 90 || rotate == 270) 1.0 / rawAspect
                else rawAspect
            } else 0.0

            positionMs = (posSec * 1000).toLong().coerceAtLeast(0L)
            durationMs = (durSec * 1000).toLong().coerceAtLeast(0L)
            isPaused = paused
            isPausedForCache = pausedForCache
            cacheTimeSeconds = cacheSec
            isEofReached = eof
            if (effectiveAspect > 0.05) videoAspect = effectiveAspect
        } catch (e: Throwable) {
            // Property query exception ignored during initialization/no file
        }
    }

    fun stop() {
        val lib = getActiveLib() ?: return
        try {
            lib.command(arrayOf("stop"))
            isPaused = true
            positionMs = 0L
            durationMs = 0L
        } catch (e: Throwable) {
            Log.e(TAG, "Error stopping MPV", e)
        }
    }

    fun destroy() {
        val libToDestroy = synchronized(this) {
            if (!isInitialized) return
            isInitialized = false
            val lib = mpvLib
            mpvLib = null
            lib
        } ?: return

        try {
            libToDestroy.detachSurface()
            libToDestroy.destroy()
            Log.d(TAG, "MPVLib destroyed")
        } catch (e: Throwable) {
            Log.e(TAG, "Error destroying MPVLib", e)
        }
    }

    fun onKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                togglePause()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekBy(-10)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                seekBy(10)
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekBy(10)
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBy(-10)
                true
            }
            else -> false
        }
    }

    companion object {
        private const val TAG = "MPVView"
    }
}
