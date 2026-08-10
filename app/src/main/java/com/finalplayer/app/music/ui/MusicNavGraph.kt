package com.finalplayer.app.music.ui

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation

fun NavGraphBuilder.musicNavGraph(
    navController: NavHostController,
    onBack: () -> Unit
) {
    navigation(startDestination = "music_library", route = "music") {
        composable("music_library") {
            MusicLibraryScreen(
                onAlbumClick = { albumId -> navController.navigate("music_album/$albumId") },
                onArtistClick = { name -> navController.navigate("music_artist/$name") },
                onOpenPlayer = { navController.navigate("music_player") },
                onBack = onBack
            )
        }

        composable("music_player") {
            MusicPlayerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            "music_album/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId") ?: return@composable
            MusicAlbumDetailScreen(
                albumId = albumId,
                onBack = { navController.popBackStack() },
                onOpenPlayer = { navController.navigate("music_player") }
            )
        }

        composable(
            "music_artist/{artistName}",
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("artistName") ?: return@composable
            MusicArtistDetailScreen(
                artistName = name,
                onBack = { navController.popBackStack() },
                onAlbumClick = { id -> navController.navigate("music_album/$id") },
                onOpenPlayer = { navController.navigate("music_player") }
            )
        }
    }
}
