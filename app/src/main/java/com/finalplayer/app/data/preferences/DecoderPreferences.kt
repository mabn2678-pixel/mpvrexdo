package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference

class DecoderPreferences(private val dataStore: DataStore<Preferences>) {
    val tryHWDecoding  = pref(booleanPreferencesKey("dec_hw"), true)
    val gpuNext        = pref(booleanPreferencesKey("dec_gpu_next"), false)
    val useVulkan      = pref(booleanPreferencesKey("dec_vulkan"), false)
    val profile        = pref(stringPreferencesKey("dec_profile"), "Fast")
    val debanding      = pref(stringPreferencesKey("dec_debanding"), "None")
    val hdrScreenOutput= pref(booleanPreferencesKey("dec_hdr"), false)
    val useYUV420P     = pref(booleanPreferencesKey("dec_yuv420p"), false)
    val anime4k        = pref(booleanPreferencesKey("dec_anime4k"), false)
    val hdrToSdr       = pref(booleanPreferencesKey("dec_hdr_to_sdr"), false)

    private fun <T> pref(key: Preferences.Key<T>, default: T): Preference<T> =
        DataStorePreference(dataStore, key, default)
}
