/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.app.Application
import androidx.work.Configuration
import com.bernaferrari.changedetection.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androidx.workmanager.factory.KoinWorkerFactory

class App :
    Application(),
    Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(KoinWorkerFactory())
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()

    override fun onCreate() {
        super.onCreate()

        // Register MainActivity with the library's provider
        MainActivityProvider.register(MainActivity::class.java)

        initKoin {
            androidLogger()
            androidContext(this@App)
            workManagerFactory()
        }
    }
}
