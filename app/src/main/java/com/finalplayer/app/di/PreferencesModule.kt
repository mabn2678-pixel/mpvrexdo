package com.finalplayer.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.finalplayer.app.data.preferences.AppearancePreferences
import com.finalplayer.app.data.preferences.AudioPreferences
import com.finalplayer.app.data.preferences.DecoderPreferences
import com.finalplayer.app.data.preferences.GesturePreferences
import com.finalplayer.app.data.preferences.LayoutPreferences
import com.finalplayer.app.data.preferences.OnboardingPreferences
import com.finalplayer.app.data.preferences.PlayerLayoutPreferences
import com.finalplayer.app.data.preferences.PlayerPreferences
import com.finalplayer.app.data.preferences.SecurePinPreferences
import com.finalplayer.app.data.preferences.SortPreferences
import com.finalplayer.app.data.preferences.SubtitlesPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")
private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_prefs")

val preferencesModule = module {
    single<DataStore<Preferences>> { androidContext().appDataStore }
    single<DataStore<Preferences>>(named("secure_prefs")) { androidContext().secureDataStore }

    single { OnboardingPreferences(get()) }
    single { PlayerPreferences(get()) }
    single { LayoutPreferences(get()) }
    single { PlayerLayoutPreferences(get()) }
    single { SubtitlesPreferences(get()) }
    single { AudioPreferences(get()) }
    single { DecoderPreferences(get()) }
    single { AppearancePreferences(get()) }
    single { GesturePreferences(get()) }
    single { SortPreferences(get()) }
    single { SecurePinPreferences(get(named("secure_prefs"))) }
}
