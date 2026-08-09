package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class PlayerLayoutPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOP_RIGHT_CONTROLS = stringPreferencesKey("layout_top_right_controls")
        val BOTTOM_RIGHT_CONTROLS = stringPreferencesKey("layout_bottom_right_controls")
        val BOTTOM_LEFT_CONTROLS = stringPreferencesKey("layout_bottom_left_controls")
        val PORTRAIT_BOTTOM_CONTROLS = stringPreferencesKey("layout_portrait_bottom_controls")
        val CONTROLS_TAB_BUTTONS = stringPreferencesKey("layout_controls_tab_buttons")

        val SEEKBAR_STYLE = stringPreferencesKey("layout_seekbar_style")
        val WHITE_PROGRESSBAR = booleanPreferencesKey("layout_white_progressbar")
        val CONTROLS_BELOW_SEEKBAR = booleanPreferencesKey("layout_controls_below_seekbar")

        val ELASTIC_ANIMATIONS = booleanPreferencesKey("layout_elastic_animations")
        val HIDE_BUTTON_BACKGROUND = booleanPreferencesKey("layout_hide_button_bg")
        val GLASSMORPHISM_CONTROLS = booleanPreferencesKey("layout_glassmorphism_controls")
        val GLASSMORPHISM_SEEKBAR = booleanPreferencesKey("layout_glassmorphism_seekbar")
        val ALWAYS_DARK_MODE = booleanPreferencesKey("layout_always_dark_mode")
        val SHOW_CONTROLS_ON_START = booleanPreferencesKey("layout_show_controls_on_start")
        val CONTROLS_GRADIENT_OPACITY = floatPreferencesKey("layout_controls_gradient_opacity")
        val CONTROLS_HIDE_TIMEOUT_MS = intPreferencesKey("layout_controls_hide_timeout_ms")

        const val DEFAULT_TOP_RIGHT = "screenshot,decoder,audio_track,subtitles,more"
        const val DEFAULT_BOTTOM_LEFT = "lock,rotate,aspect_ratio,speed,repeat_mode,shuffle,ab_repeat"
        const val DEFAULT_BOTTOM_RIGHT = "pip,zoom"
        const val DEFAULT_PORTRAIT_BOTTOM = "screenshot,subtitles,audio_track,lock,rotate,more"
        const val DEFAULT_CONTROLS_TAB = "screenshot,speed,decoder,aspect_ratio,pip,sleep_timer,rotate,frame_nav,chapters,repeat_mode,shuffle"
    }

    val topRightControls = pref(TOP_RIGHT_CONTROLS, DEFAULT_TOP_RIGHT)
    val bottomRightControls = pref(BOTTOM_RIGHT_CONTROLS, DEFAULT_BOTTOM_RIGHT)
    val bottomLeftControls = pref(BOTTOM_LEFT_CONTROLS, DEFAULT_BOTTOM_LEFT)
    val portraitBottomControls = pref(PORTRAIT_BOTTOM_CONTROLS, DEFAULT_PORTRAIT_BOTTOM)
    val controlsTabButtons = pref(CONTROLS_TAB_BUTTONS, DEFAULT_CONTROLS_TAB)

    val seekbarStyle = pref(SEEKBAR_STYLE, "standard")
    val whiteProgressbar = pref(WHITE_PROGRESSBAR, false)
    val controlsBelowSeekbar = pref(CONTROLS_BELOW_SEEKBAR, false)

    val elasticAnimations = pref(ELASTIC_ANIMATIONS, true)
    val hideButtonBackground = pref(HIDE_BUTTON_BACKGROUND, false)
    val glassmorphismControls = pref(GLASSMORPHISM_CONTROLS, false)
    val glassmorphismSeekbar = pref(GLASSMORPHISM_SEEKBAR, false)
    val alwaysDarkMode = pref(ALWAYS_DARK_MODE, true)
    val showControlsOnStart = pref(SHOW_CONTROLS_ON_START, true)
    val controlsGradientOpacity = pref(CONTROLS_GRADIENT_OPACITY, 0.45f)
    val controlsHideTimeoutMs = pref(CONTROLS_HIDE_TIMEOUT_MS, 3000)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)

    fun parseControlList(valStr: String): List<String> {
        if (valStr.isBlank()) return emptyList()
        return valStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun formatControlList(list: List<String>): String {
        return list.joinToString(",")
    }
}
