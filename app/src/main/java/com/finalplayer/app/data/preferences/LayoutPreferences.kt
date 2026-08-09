package com.finalplayer.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.finalplayer.app.data.preferences.base.DataStorePreference
import com.finalplayer.app.data.preferences.base.Preference
import com.finalplayer.app.domain.model.PlayerButtonType

/**
 * Manages Dynamic Player Layout preferences using DataStore.
 * Saves controls lists for top_right_controls, bottom_right_controls, bottom_left_controls, overflow_menu_controls.
 */
class LayoutPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val TOP_RIGHT_CONTROLS = stringPreferencesKey("top_right_controls")
        val BOTTOM_RIGHT_CONTROLS = stringPreferencesKey("bottom_right_controls")
        val BOTTOM_LEFT_CONTROLS = stringPreferencesKey("bottom_left_controls")
        val OVERFLOW_MENU_CONTROLS = stringPreferencesKey("overflow_menu_controls")
        val PORTRAIT_BOTTOM_CONTROLS = stringPreferencesKey("portrait_bottom_controls")

        const val DEFAULT_TOP_RIGHT = "decoder,audio_track,subtitles,more"
        const val DEFAULT_BOTTOM_LEFT = "lock,rotate,aspect_ratio,speed,repeat_mode,shuffle,ab_repeat"
        const val DEFAULT_BOTTOM_RIGHT = "pip,zoom"
        const val DEFAULT_OVERFLOW_MENU = "speed,decoder,aspect_ratio,pip,sleep_timer,rotate,frame_nav,chapters,repeat_mode,shuffle"
        const val DEFAULT_PORTRAIT_BOTTOM = "subtitles,audio_track,lock,rotate,more"
    }

    val topRightControls: Preference<String> =
        DataStorePreference(dataStore, TOP_RIGHT_CONTROLS, DEFAULT_TOP_RIGHT)

    val bottomRightControls: Preference<String> =
        DataStorePreference(dataStore, BOTTOM_RIGHT_CONTROLS, DEFAULT_BOTTOM_RIGHT)

    val bottomLeftControls: Preference<String> =
        DataStorePreference(dataStore, BOTTOM_LEFT_CONTROLS, DEFAULT_BOTTOM_LEFT)

    val overflowMenuControls: Preference<String> =
        DataStorePreference(dataStore, OVERFLOW_MENU_CONTROLS, DEFAULT_OVERFLOW_MENU)

    val portraitBottomControls: Preference<String> =
        DataStorePreference(dataStore, PORTRAIT_BOTTOM_CONTROLS, DEFAULT_PORTRAIT_BOTTOM)

    fun parseControlList(valStr: String?): List<String> {
        if (valStr.isNullOrBlank()) return emptyList()
        return valStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun parseButtonTypeList(valStr: String?): List<PlayerButtonType> {
        return parseControlList(valStr).mapNotNull { PlayerButtonType.fromId(it) }
    }

    fun formatControlList(list: List<String>): String {
        return list.joinToString(",")
    }

    fun formatButtonTypeList(list: List<PlayerButtonType>): String {
        return list.joinToString(",") { it.id }
    }
}
