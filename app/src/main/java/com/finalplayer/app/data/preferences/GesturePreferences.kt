package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class GesturePreferences(private val dataStore: DataStore<Preferences>) {
    val brightnessGestureEnabled  = pref(booleanPreferencesKey("gest_brightness"), true)
    val volumeGestureEnabled      = pref(booleanPreferencesKey("gest_volume"), true)
    val seekGestureEnabled        = pref(booleanPreferencesKey("gest_seek"), true)
    val pinchToZoom               = pref(booleanPreferencesKey("gest_pinch_zoom"), true)
    val subtitleScrollSeek        = pref(booleanPreferencesKey("gest_sub_scroll"), true)
    val subtitleDrag              = pref(booleanPreferencesKey("gest_sub_drag"), true)
    val panAndZoom                = pref(booleanPreferencesKey("gest_pan_zoom"), false)
    val preventAccidentalSeek     = pref(booleanPreferencesKey("gest_prevent_accidental"), false)
    val gestureSensitivity        = pref(floatPreferencesKey("gest_sensitivity"), 1.0f)
    val swipeSeekSpeed            = pref(floatPreferencesKey("gest_swipe_speed"), 1.0f)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}
