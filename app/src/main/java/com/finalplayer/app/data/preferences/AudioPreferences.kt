package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class AudioPreferences(private val dataStore: DataStore<Preferences>) {
    val preferredLanguages    = pref(stringPreferencesKey("audio_preferred_langs"), "")
    val defaultAudioDelay     = pref(intPreferencesKey("audio_default_delay_ms"), 0)
    val audioPitchCorrection  = pref(booleanPreferencesKey("audio_pitch_correction"), true)
    val volumeBoostCap        = pref(intPreferencesKey("audio_volume_boost_cap"), 30)
    val volumeNormalization   = pref(booleanPreferencesKey("audio_volume_normalization"), false)
    val backgroundPlayEnabled = pref(booleanPreferencesKey("audio_background_play"), false)
    val audioChannels         = pref(stringPreferencesKey("audio_channels"), "auto-safe")
    val drcEnabled            = pref(booleanPreferencesKey("audio_drc"), false)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}
