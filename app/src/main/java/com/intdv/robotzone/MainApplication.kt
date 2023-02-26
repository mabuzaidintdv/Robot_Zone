package com.intdv.robotzone

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {

            // Log Koin into Android logger
            androidLogger()

            // Reference Android context
            androidContext(this@MainApplication)

            modules(module {
                //single { MainViewModel(application = get()) }
            })
        }

        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())
    }
}
