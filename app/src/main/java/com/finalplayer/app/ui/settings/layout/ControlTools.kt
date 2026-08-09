package com.finalplayer.app.ui.settings.layout

import androidx.compose.ui.graphics.vector.ImageVector
import com.finalplayer.app.domain.model.PlayerButtonType

data class ControlToolItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val buttonType: PlayerButtonType
)

object ControlTools {
    val ALL_TOOLS: List<ControlToolItem> = PlayerButtonType.entries.map {
        ControlToolItem(
            id = it.id,
            title = it.title,
            icon = it.icon,
            buttonType = it
        )
    }

    private val toolsMap = ALL_TOOLS.associateBy { it.id }

    fun getById(id: String): ControlToolItem? {
        val tool = toolsMap[id]
        if (tool != null) return tool
        val type = PlayerButtonType.fromId(id) ?: return null
        return ControlToolItem(type.id, type.title, type.icon, type)
    }
}
