package com.finalplayer.app.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finalplayer.app.ui.theme.FinalPlayerTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinalPlayerTheme {
                SettingsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
