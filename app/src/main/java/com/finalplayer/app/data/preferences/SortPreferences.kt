package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference

class SortPreferences(dataStore: DataStore<Preferences>) {
    val sortBy = DataStorePreference(dataStore, stringPreferencesKey("sort_by"), "title")
    val sortAscending = DataStorePreference(dataStore, booleanPreferencesKey("sort_ascending"), true)
    val viewMode = DataStorePreference(dataStore, stringPreferencesKey("view_mode"), "folder")
    val layoutMode = DataStorePreference(dataStore, stringPreferencesKey("layout_mode"), "list")
    val visibleFields = DataStorePreference(dataStore, stringSetPreferencesKey("visible_fields"), setOf("Path", "Folder Size", "Total Media"))
    val onlyForFolderList = DataStorePreference(dataStore, booleanPreferencesKey("only_for_folder_list"), false)
    val showAudioFiles = DataStorePreference(dataStore, booleanPreferencesKey("show_audio_files"), false)
}
