package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class AppearancePreferences(private val dataStore: DataStore<Preferences>) {
    // Theme & Styling
    val themeMode                   = pref(stringPreferencesKey("app_theme_mode"), "system") // "system", "light", "dark"
    val themePreset                 = pref(stringPreferencesKey("app_theme_preset"), "default") // "default", "kanagawa", "catppuccin", "cloudflare", "doom", "cotton_candy", "green_apple", "gruvbox", "lavender"
    val amoledMode                  = pref(booleanPreferencesKey("app_amoled_mode"), true)
    val useSystemFont               = pref(booleanPreferencesKey("app_use_system_font"), false)
    val hidePlayerButtonsBackground = pref(booleanPreferencesKey("app_hide_btn_bg"), false)
    val seekbarStyle                = pref(stringPreferencesKey("app_seekbar_style"), "thin") // "thin", "default", "thick"
    val useSpringAnimations         = pref(booleanPreferencesKey("app_spring_anim"), true)
    val matchControlsToTheme        = pref(booleanPreferencesKey("app_match_theme"), false)
    val playerAlwaysDark            = pref(booleanPreferencesKey("app_always_dark"), true)
    val glassmorphismControls       = pref(booleanPreferencesKey("app_glass_ctrl"), false)
    val glassmorphismSeekbar        = pref(booleanPreferencesKey("app_glass_seek"), false)

    // Navigation Tabs
    val showHomeTab                 = pref(booleanPreferencesKey("tab_home"), true)
    val showShortsTab               = pref(booleanPreferencesKey("tab_shorts"), true)
    val showRecentsTab              = pref(booleanPreferencesKey("tab_recents"), true)

    // File Browser
    val showFullFileNames           = pref(booleanPreferencesKey("browser_full_names"), false)
    val showNewVideoTag             = pref(booleanPreferencesKey("browser_new_tag"), true)
    val newVideoDaysThreshold       = pref(intPreferencesKey("browser_new_days"), 7)
    val autoScrollToLastVideo       = pref(booleanPreferencesKey("browser_auto_scroll"), false)
    val watchThreshold              = pref(intPreferencesKey("browser_watch_threshold"), 90)
    val showAudioFiles              = pref(booleanPreferencesKey("browser_show_audio"), true)
    val showDetailedBreadcrumbs     = pref(booleanPreferencesKey("browser_breadcrumbs"), false)

    // Thumbnails
    val clickThumbnailToSelect      = pref(booleanPreferencesKey("thumb_click_select"), false)
    val enableGridThumbnails        = pref(booleanPreferencesKey("thumb_grid_experimental"), false)
    val thumbnailStrategy           = pref(stringPreferencesKey("thumb_strategy"), "first_frame")
    val thumbnailPositionPercent    = pref(intPreferencesKey("thumb_position_pct"), 30)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}

