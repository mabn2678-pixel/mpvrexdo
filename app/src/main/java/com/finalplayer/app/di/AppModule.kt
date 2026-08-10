package com.finalplayer.app.di

import androidx.room.Room
import com.finalplayer.app.data.database.FinalPlayerDatabase
import com.finalplayer.app.data.preferences.OnboardingPreferences
import com.finalplayer.app.data.repository.MediaStoreVideoScanner
import com.finalplayer.app.data.repository.PlaybackRepositoryImpl
import com.finalplayer.app.data.repository.VideoRepositoryImpl
import com.finalplayer.app.domain.repository.PlaybackRepository
import com.finalplayer.app.domain.repository.VideoRepository
import com.finalplayer.app.domain.usecase.GetRecentlyPlayedUseCase
import com.finalplayer.app.domain.usecase.GetVideoLibraryUseCase
import com.finalplayer.app.domain.usecase.GetVideosByFolderUseCase
import com.finalplayer.app.domain.usecase.SavePlaybackProgressUseCase
import com.finalplayer.app.domain.usecase.ScanForVideosUseCase
import com.finalplayer.app.player.PlayerViewModel
import com.finalplayer.app.player.core.MPVController
import com.finalplayer.app.ui.home.HomeViewModel
import com.finalplayer.app.ui.onboarding.OnboardingViewModel
import com.finalplayer.app.ui.recents.RecentsViewModel
import com.finalplayer.app.ui.search.SearchViewModel
import com.finalplayer.app.ui.securefolder.SecureFolderViewModel
import com.finalplayer.app.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    includes(preferencesModule)

    // Database & DAOs
    single {
        Room.databaseBuilder(
            androidContext(),
            FinalPlayerDatabase::class.java,
            "finalplayer.db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<FinalPlayerDatabase>().videoDao() }
    single { get<FinalPlayerDatabase>().playbackProgressDao() }
    single { get<FinalPlayerDatabase>().secureMediaDao() }

    // Scanner & Repositories
    single { MediaStoreVideoScanner(androidContext()) }
    single<VideoRepository> { VideoRepositoryImpl(get(), get(), get()) }
    single<PlaybackRepository> { PlaybackRepositoryImpl(get()) }

    // MPV Player Controller
    single { MPVController(androidContext()) }

    // Domain Use Cases
    factory { GetVideoLibraryUseCase(get()) }
    factory { GetVideosByFolderUseCase(get()) }
    factory { ScanForVideosUseCase(get()) }
    factory { SavePlaybackProgressUseCase(get()) }
    factory { GetRecentlyPlayedUseCase(get()) }

    // Music Player
    single { com.finalplayer.app.music.data.local.MediaStoreScanner(androidContext()) }
    single { com.finalplayer.app.music.data.local.LrcParser() }
    single<com.finalplayer.app.music.data.repository.MusicRepository> {
        com.finalplayer.app.music.data.repository.MusicRepositoryImpl(androidContext(), get(), get())
    }
    single { com.finalplayer.app.music.player.MusicController(androidContext()) }
    viewModel { com.finalplayer.app.music.ui.MusicViewModel(get(), get()) }

    // ViewModels
    viewModel { OnboardingViewModel(get()) }
    viewModel { HomeViewModel(androidContext(), get(), get(), get(), get(), get(), get()) }
    viewModel { PlayerViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { RecentsViewModel(get(), get(), get()) }
    viewModel { SecureFolderViewModel(get(), get(), get()) }
}

