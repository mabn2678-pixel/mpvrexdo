package com.finalplayer.app.ui.home.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finalplayer.app.data.preferences.AppearancePreferences
import com.finalplayer.app.ui.home.HomeTab
import org.koin.compose.koinInject

@Composable
fun HomeBottomBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onMusicClick: () -> Unit = {},
    appearancePrefs: AppearancePreferences = koinInject()
) {
    val showHomeTab by appearancePrefs.showHomeTab.asFlow().collectAsState(initial = true)
    val showShortsTab by appearancePrefs.showShortsTab.asFlow().collectAsState(initial = true)
    val showRecentsTab by appearancePrefs.showRecentsTab.asFlow().collectAsState(initial = true)

    NavigationBar(
        modifier = Modifier.height(56.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // 0. الموسيقى (Music)
        NavigationBarItem(
            selected = false,
            onClick = { onMusicClick() },
            icon = {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "الموسيقى"
                )
            },
            label = {
                Text(
                    text = "الموسيقى",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        // 1. الأخيرة (Recents)
        if (showRecentsTab) {
            NavigationBarItem(
                selected = selectedTab == HomeTab.RECENTS,
                onClick = { onTabSelected(HomeTab.RECENTS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "الأخيرة"
                    )
                },
                label = {
                    Text(
                        text = "الأخيرة",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == HomeTab.RECENTS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // 2. القصيرة (Shorts)
        if (showShortsTab) {
            NavigationBarItem(
                selected = selectedTab == HomeTab.SHORTS,
                onClick = { onTabSelected(HomeTab.SHORTS) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.OndemandVideo,
                        contentDescription = "القصيرة"
                    )
                },
                label = {
                    Text(
                        text = "القصيرة",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == HomeTab.SHORTS) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // 3. الرئيسية (Home)
        if (showHomeTab) {
            NavigationBarItem(
                selected = selectedTab == HomeTab.HOME,
                onClick = { onTabSelected(HomeTab.HOME) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "الرئيسية"
                    )
                },
                label = {
                    Text(
                        text = "الرئيسية",
                        fontSize = 12.sp,
                        fontWeight = if (selectedTab == HomeTab.HOME) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

