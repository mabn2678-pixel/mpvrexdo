package com.finalplayer.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finalplayer.app.player.PlayerActivity
import com.finalplayer.app.ui.about.AboutScreen
import com.finalplayer.app.ui.about.LibrariesScreen
import com.finalplayer.app.ui.home.FolderDetailScreen
import com.finalplayer.app.ui.home.HomeScreen
import com.finalplayer.app.ui.home.HomeViewModel
import com.finalplayer.app.ui.onboarding.OnboardingScreen
import com.finalplayer.app.ui.onboarding.OnboardingViewModel
import com.finalplayer.app.ui.search.SearchScreen
import com.finalplayer.app.ui.securefolder.SecureFolderScreen
import com.finalplayer.app.ui.settings.SettingsScreen
import com.finalplayer.app.ui.theme.FinalPlayerTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope
import com.finalplayer.app.music.player.MusicController
import com.finalplayer.app.music.ui.musicNavGraph
import com.finalplayer.app.domain.repository.PlaybackRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import androidx.compose.runtime.LaunchedEffect
import com.finalplayer.app.music.player.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : FragmentActivity() {

    private val musicController: MusicController by inject()
    private val pendingDestination = MutableStateFlow<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkIntentForNavigation(intent)
    }

    private fun checkIntentForNavigation(intent: Intent?) {
        if (intent?.action == MusicPlayerService.ACTION_OPEN_MUSIC_PLAYER ||
            intent?.getStringExtra("EXTRA_OPEN_DESTINATION") == "music_player") {
            pendingDestination.value = "music_player"
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            musicController.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        musicController.disconnect()
    }

    private fun encodeNavPath(path: String): String {
        if (path.isEmpty()) return "empty"
        return try {
            path.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        } catch (_: Exception) {
            "empty"
        }
    }

    private fun decodeNavPath(encoded: String): String {
        if (encoded.isEmpty() || encoded == "empty") return ""
        return try {
            val clean = encoded.trim()
            val isHex = clean.length % 2 == 0 && clean.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            if (isHex && clean.isNotEmpty()) {
                val bytes = ByteArray(clean.length / 2)
                for (i in clean.indices step 2) {
                    bytes[i / 2] = clean.substring(i, i + 2).toInt(16).toByte()
                }
                String(bytes, Charsets.UTF_8)
            } else {
                try {
                    val bytes = Base64.decode(clean, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                    String(bytes, Charsets.UTF_8)
                } catch (_: Exception) {
                    Uri.decode(clean)
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun openPlayer(
        path: String,
        title: String,
        videoId: String? = null,
        playlist: List<com.finalplayer.app.domain.model.VideoItem> = emptyList(),
        index: Int = 0
    ) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_VIDEO_PATH, path)
            putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, title)
            if (!videoId.isNullOrEmpty()) {
                putExtra(PlayerActivity.EXTRA_VIDEO_ID, videoId)
            }
            if (playlist.isNotEmpty()) {
                putExtra(PlayerActivity.EXTRA_PLAYLIST_INDEX, index)
                putStringArrayListExtra(PlayerActivity.EXTRA_PLAYLIST_URIS, ArrayList(playlist.map { it.uri }))
                putStringArrayListExtra(PlayerActivity.EXTRA_PLAYLIST_TITLES, ArrayList(playlist.map { it.title }))
                putStringArrayListExtra(PlayerActivity.EXTRA_PLAYLIST_IDS, ArrayList(playlist.map { it.id }))
                putExtra(PlayerActivity.EXTRA_PLAYLIST_DURATIONS, playlist.map { it.duration }.toLongArray())
            }
        }
        startActivity(intent)
    }

    private fun openShortsPlayer(shortsList: List<com.finalplayer.app.domain.model.VideoItem>, initialIndex: Int = 0) {
        if (shortsList.isEmpty()) return
        val initialVideo = shortsList.getOrNull(initialIndex) ?: shortsList[0]
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_VIDEO_PATH, initialVideo.uri)
            putExtra(PlayerActivity.EXTRA_VIDEO_TITLE, initialVideo.title)
            putExtra(PlayerActivity.EXTRA_VIDEO_ID, initialVideo.id)
            putExtra(PlayerActivity.EXTRA_IS_SHORTS_MODE, true)
            putExtra(PlayerActivity.EXTRA_SHORTS_INDEX, initialIndex)
            putStringArrayListExtra(PlayerActivity.EXTRA_SHORTS_URIS, ArrayList(shortsList.map { it.uri }))
            putStringArrayListExtra(PlayerActivity.EXTRA_SHORTS_TITLES, ArrayList(shortsList.map { it.title }))
            putStringArrayListExtra(PlayerActivity.EXTRA_SHORTS_IDS, ArrayList(shortsList.map { it.id }))
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkIntentForNavigation(intent)
        enableEdgeToEdge()
        setContent {
            FinalPlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val onboardingViewModel: OnboardingViewModel = koinViewModel()
                    val homeViewModel: HomeViewModel = koinViewModel()
                    val playbackRepository: PlaybackRepository = koinInject()
                    val mainScope = rememberCoroutineScope()

                    val hasCompletedOnboardingState by onboardingViewModel.hasCompletedOnboarding.collectAsState()

                    if (hasCompletedOnboardingState == null) {
                        return@Surface
                    }

                    val hasCompletedOnboarding = hasCompletedOnboardingState == true
                    val navController = rememberNavController()

                    val pendingDest by pendingDestination.collectAsState()

                    LaunchedEffect(pendingDest) {
                        if (pendingDest == "music_player") {
                            pendingDestination.value = null
                            navController.navigate("music_library") {
                                launchSingleTop = true
                            }
                            navController.navigate("music_player") {
                                launchSingleTop = true
                            }
                        }
                    }

                    val startDestination = if (hasCompletedOnboarding) "home" else "onboarding"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("onboarding") {
                            OnboardingScreen(
                                viewModel = onboardingViewModel,
                                onPermissionGranted = {
                                    navController.navigate("home") {
                                        popUpTo("onboarding") { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable("home") {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onFolderClick = { folderPath ->
                                    val encoded = encodeNavPath(folderPath)
                                    navController.navigate("folder_detail/$encoded") {
                                        launchSingleTop = true
                                    }
                                },
                                onVideoClick = { video, list, index ->
                                    openPlayer(video.uri, video.title, video.id, list, index)
                                },
                                onRecentVideoClick = { path, title ->
                                    openPlayer(path, title)
                                },
                                onShortsVideoClick = { list, index ->
                                    openShortsPlayer(list, index)
                                },
                                onPlayButtonClick = {
                                    mainScope.launch {
                                        val allVideos = homeViewModel.uiState.value.allVideos
                                        val progressList = playbackRepository.getAllProgress().firstOrNull() ?: emptyList()
                                        val lastPlayed = allVideos.mapNotNull { v ->
                                            val prog = progressList.find { p -> p.videoId == v.id || p.videoId == v.uri }
                                            if (prog != null && prog.lastPlayedTimestamp > 0) {
                                                v to prog.lastPlayedTimestamp
                                            } else null
                                        }.maxByOrNull { it.second }?.first ?: allVideos.firstOrNull()

                                        if (lastPlayed != null) {
                                            val idx = allVideos.indexOf(lastPlayed).coerceAtLeast(0)
                                            openPlayer(lastPlayed.uri, lastPlayed.title, lastPlayed.id, allVideos, idx)
                                        } else {
                                            val firstFolder = homeViewModel.uiState.value.folders.firstOrNull()
                                            if (firstFolder != null) {
                                                val encoded = encodeNavPath(firstFolder.path)
                                                navController.navigate("folder_detail/$encoded")
                                            } else {
                                                openPlayer("", "Demo Video")
                                            }
                                        }
                                    }
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                },
                                onSecureFolderClick = {
                                    navController.navigate("secure_folder")
                                },
                                onMusicClick = {
                                    navController.navigate("music_library")
                                }
                            )
                        }

                        // Music navigation graph
                        musicNavGraph(
                            navController = navController,
                            onBack = { navController.popBackStack() }
                        )

                        composable("secure_folder") {
                            SecureFolderScreen(
                                onVideoClick = { video, list, index ->
                                    openPlayer(video.uri, video.title, video.id, list, index)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = "folder_detail/{folderPath}",
                            arguments = listOf(navArgument("folderPath") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val rawArg = backStackEntry.arguments?.getString("folderPath") ?: ""
                            val folderPath = decodeNavPath(rawArg)
                            FolderDetailScreen(
                                folderPath = folderPath,
                                viewModel = homeViewModel,
                                onVideoClick = { video, list, index ->
                                    openPlayer(video.uri, video.title, video.id, list, index)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("search") {
                            SearchScreen(
                                onBack = { navController.popBackStack() },
                                onVideoClick = { video, list, index ->
                                    openPlayer(video.uri, video.title, video.id, list, index)
                                },
                                onFolderClick = { folderPath ->
                                    val encoded = encodeNavPath(folderPath)
                                    navController.navigate("folder_detail/$encoded")
                                }
                            )
                        }

                        composable(
                            route = "settings?sub={sub}",
                            arguments = listOf(navArgument("sub") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { backStackEntry ->
                            val sub = backStackEntry.arguments?.getString("sub")
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                initialSubScreen = sub
                            )
                        }

                        composable("about") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                initialSubScreen = "about"
                            )
                        }

                        composable("libraries") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() },
                                initialSubScreen = "libraries"
                            )
                        }

                        composable(
                            route = "edit_layout/{region}",
                            arguments = listOf(navArgument("region") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val region = backStackEntry.arguments?.getString("region") ?: "top_right"
                            com.finalplayer.app.ui.settings.layout.EditLayoutScreen(
                                region = region,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
