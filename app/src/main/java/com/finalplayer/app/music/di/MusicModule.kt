package com.finalplayer.app.music.di

import com.finalplayer.app.music.data.db.MusicDatabase
import com.finalplayer.app.music.data.local.LrcParser
import com.finalplayer.app.music.data.local.MediaStoreScanner
import com.finalplayer.app.music.data.repository.MusicRepository
import com.finalplayer.app.music.data.repository.MusicRepositoryImpl
import com.finalplayer.app.music.player.MusicController
import com.finalplayer.app.music.ui.MusicLibraryViewModel
import com.finalplayer.app.music.ui.MusicPlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val musicModule = module {
    single { MediaStoreScanner(androidContext()) }
    single { LrcParser() }
    single<MusicRepository> { MusicRepositoryImpl(androidContext(), get(), get()) }
    single { MusicController(androidContext()) }
    single { MusicDatabase.getInstance(androidContext()) }
    single { get<MusicDatabase>().playlistDao() }
    viewModel { MusicLibraryViewModel(get(), get()) }
    viewModel { MusicPlayerViewModel(get(), get()) }
}
