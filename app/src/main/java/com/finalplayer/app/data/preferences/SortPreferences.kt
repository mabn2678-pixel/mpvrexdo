package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference

val DEFAULT_VISIBLE_FIELDS: Set<String> = setOf(
    "Folder Size",
    "Total Media",
    "Full Name",
    "Total Duration",
    "Resolution",
    "File Size",
    "Date",
    "Progress Bar"
)

class SortPreferences(dataStore: DataStore<Preferences>) {
    val sortBy = DataStorePreference(dataStore, stringPreferencesKey("sort_by"), "title")
    val sortAscending = DataStorePreference(dataStore, booleanPreferencesKey("sort_ascending"), true)
    val viewMode = DataStorePreference(dataStore, stringPreferencesKey("view_mode"), "folder")
    val layoutMode = DataStorePreference(dataStore, stringPreferencesKey("layout_mode"), "list")
    val visibleFields = DataStorePreference(dataStore, stringSetPreferencesKey("visible_fields"), DEFAULT_VISIBLE_FIELDS)
    val onlyForFolderList = DataStorePreference(dataStore, booleanPreferencesKey("only_for_folder_list"), false)
    val showAudioFiles = DataStorePreference(dataStore, booleanPreferencesKey("show_audio_files"), false)

    // Dedicated independent preferences for Shorts Tab
    val shortsSortBy = DataStorePreference(dataStore, stringPreferencesKey("shorts_sort_by"), "date")
    val shortsSortAscending = DataStorePreference(dataStore, booleanPreferencesKey("shorts_sort_ascending"), false)
    val shortsLayoutMode = DataStorePreference(dataStore, stringPreferencesKey("shorts_layout_mode"), "grid")
    val shortsVisibleFields = DataStorePreference(dataStore, stringSetPreferencesKey("shorts_visible_fields"), DEFAULT_VISIBLE_FIELDS)
}
