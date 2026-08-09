package com.finalplayer.app

import android.app.Application
import com.finalplayer.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FinalPlayerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FinalPlayerApp)
            modules(appModule)
        }
    }
}
