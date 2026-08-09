package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class PlayerPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // General
        val PLAYER_ORIENTATION = stringPreferencesKey("player_orientation")
        val SAVE_POSITION = booleanPreferencesKey("save_position_on_quit")
        val CLOSE_AFTER_PLAYBACK = booleanPreferencesKey("close_after_playback")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val ENABLE_PREV_NEXT = booleanPreferencesKey("enable_prev_next_buttons")
        val REMEMBER_BRIGHTNESS = booleanPreferencesKey("remember_brightness")
        val DEFAULT_BRIGHTNESS = floatPreferencesKey("default_brightness")
        val AUTO_PIP = booleanPreferencesKey("auto_pip_on_navigation")
        val KEEP_SCREEN_ON_PAUSE = booleanPreferencesKey("keep_screen_on_pause")
        val RESUME_ON_UNLOCK = booleanPreferencesKey("resume_on_unlock")

        // Seek & Rewind
        val SHOW_DOUBLE_TAP_RIPPLE = booleanPreferencesKey("show_double_tap_ripple")
        val SHOW_DOUBLE_TAP_OVALS = booleanPreferencesKey("show_double_tap_ovals")
        val SHOW_SEEK_TIME = booleanPreferencesKey("show_seek_time_while_seeking")
        val USE_PRECISE_SEEKING = booleanPreferencesKey("use_precise_seeking")
        val SHOW_SEEKBAR_ON_GESTURE = booleanPreferencesKey("show_seekbar_on_gesture")
        val WHITE_SEEKBAR = booleanPreferencesKey("white_seekbar")
        val HIDE_OSD_TEXT = booleanPreferencesKey("hide_osd_text")
        val CUSTOM_SKIP_DURATION = intPreferencesKey("custom_skip_duration")

        // Gestures
        val ENABLE_BRIGHTNESS_GESTURE = booleanPreferencesKey("enable_brightness_gesture")
        val ENABLE_VOLUME_GESTURE = booleanPreferencesKey("enable_volume_gesture")
        val ENABLE_PINCH_TO_ZOOM = booleanPreferencesKey("enable_pinch_to_zoom")
        val ENABLE_PAN_AND_ZOOM = booleanPreferencesKey("enable_pan_and_zoom")
        val ENABLE_HORIZONTAL_SEEK = booleanPreferencesKey("enable_horizontal_seek")
        val ENABLE_SUBTITLE_SEEK_GESTURE = booleanPreferencesKey("enable_subtitle_seek_gesture")
        val ENABLE_SUBTITLE_DRAG = booleanPreferencesKey("enable_subtitle_drag")
        val SEEK_SENSITIVITY = intPreferencesKey("seek_sensitivity")
        val HOLD_SPEED = floatPreferencesKey("hold_for_multiple_speed")
        val SHOW_DYNAMIC_SPEED = booleanPreferencesKey("show_dynamic_speed")

        // Control
        val DISABLE_MEDIA_BUTTONS = booleanPreferencesKey("disable_media_buttons")
        val ALLOW_PANEL_GESTURES = booleanPreferencesKey("allow_panel_gestures")
        val SWAP_VOL_BRIGHTNESS = booleanPreferencesKey("swap_volume_brightness")
        val SHOW_LOADING_CIRCLE = booleanPreferencesKey("show_loading_circle")

        // Screen
        val SHOW_STATUS_BAR = booleanPreferencesKey("show_system_status_bar")
        val SHOW_NAV_BAR = booleanPreferencesKey("show_system_nav_bar")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")

        // Other existing
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val CONTROLS_TIMEOUT_MS = intPreferencesKey("controls_timeout_ms")
        val DOUBLE_TAP_SEEK_DURATION = intPreferencesKey("double_tap_seek_duration")
        val INVERT_DURATION = booleanPreferencesKey("invert_duration")
        val SHOW_BUFFERED_RANGE = booleanPreferencesKey("show_buffered_range")
        val SHOW_CHAPTER_INDICATORS = booleanPreferencesKey("show_chapter_indicators")
        val PLAYLIST_MODE = booleanPreferencesKey("playlist_mode")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val SHUFFLE = booleanPreferencesKey("shuffle_enabled")
        val VIDEO_OPEN_ANIMATION = stringPreferencesKey("video_open_animation")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val STATS_PAGE = intPreferencesKey("enabled_stats_page")
    }

    // General
    val playerOrientation       = pref(PLAYER_ORIENTATION, "video")
    val savePositionOnQuit     = pref(SAVE_POSITION, true)
    val closeAfterPlayback     = pref(CLOSE_AFTER_PLAYBACK, true)
    val autoPlayNext           = pref(AUTO_PLAY_NEXT, true)
    val enablePrevNextButtons  = pref(ENABLE_PREV_NEXT, true)
    val rememberBrightness     = pref(REMEMBER_BRIGHTNESS, false)
    val defaultBrightness      = pref(DEFAULT_BRIGHTNESS, -1f)
    val autoPiPOnNavigation    = pref(AUTO_PIP, true)
    val keepScreenOnPause      = pref(KEEP_SCREEN_ON_PAUSE, false)
    val resumeOnUnlock         = pref(RESUME_ON_UNLOCK, false)

    // Seek & Rewind
    val showDoubleTapRipple    = pref(SHOW_DOUBLE_TAP_RIPPLE, true)
    val showDoubleTapOvals     = pref(SHOW_DOUBLE_TAP_OVALS, true)
    val showSeekTimeWhileSeeking = pref(SHOW_SEEK_TIME, true)
    val usePreciseSeeking      = pref(USE_PRECISE_SEEKING, false)
    val showSeekBarOnGesture   = pref(SHOW_SEEKBAR_ON_GESTURE, false)
    val whiteSeekBar           = pref(WHITE_SEEKBAR, false)
    val hideOsdText            = pref(HIDE_OSD_TEXT, false)
    val customSkipDuration     = pref(CUSTOM_SKIP_DURATION, 90)

    // Gestures
    val enableBrightnessGesture = pref(ENABLE_BRIGHTNESS_GESTURE, true)
    val enableVolumeGesture   = pref(ENABLE_VOLUME_GESTURE, true)
    val enablePinchToZoom      = pref(ENABLE_PINCH_TO_ZOOM, true)
    val enablePanAndZoom       = pref(ENABLE_PAN_AND_ZOOM, false)
    val enableHorizontalSeek   = pref(ENABLE_HORIZONTAL_SEEK, true)
    val enableSubtitleSeekGesture = pref(ENABLE_SUBTITLE_SEEK_GESTURE, true)
    val enableSubtitleDrag     = pref(ENABLE_SUBTITLE_DRAG, true)
    val seekSensitivity        = pref(SEEK_SENSITIVITY, 50)
    val holdForMultipleSpeed   = pref(HOLD_SPEED, 2.50f)
    val showDynamicSpeed       = pref(SHOW_DYNAMIC_SPEED, true)

    // Control
    val disableMediaButtons    = pref(DISABLE_MEDIA_BUTTONS, false)
    val allowPanelGestures     = pref(ALLOW_PANEL_GESTURES, false)
    val swapVolumeAndBrightness= pref(SWAP_VOL_BRIGHTNESS, false)
    val showLoadingCircle      = pref(SHOW_LOADING_CIRCLE, true)

    // Screen
    val showSystemStatusBar    = pref(SHOW_STATUS_BAR, false)
    val showSystemNavigationBar= pref(SHOW_NAV_BAR, false)
    val reduceMotion           = pref(REDUCE_MOTION, true)

    // Other existing
    val defaultSpeed           = pref(DEFAULT_SPEED, 1.0f)
    val playerTimeToDisappear  = pref(CONTROLS_TIMEOUT_MS, 3000)
    val doubleTapToSeekDuration= pref(DOUBLE_TAP_SEEK_DURATION, 10)
    val invertDuration         = pref(INVERT_DURATION, false)
    val showBufferedRange      = pref(SHOW_BUFFERED_RANGE, true)
    val showChapterIndicators  = pref(SHOW_CHAPTER_INDICATORS, true)
    val enabledStatisticsPage  = pref(STATS_PAGE, 0)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}
