package com.finalplayer.app.ui.settings.layout

import androidx.compose.runtime.Composable

@Composable
fun PlayerLayoutEditSectionScreen(
    sectionId: String,
    onBack: () -> Unit
) {
    EditLayoutScreen(
        region = sectionId,
        onBack = onBack
    )
}
