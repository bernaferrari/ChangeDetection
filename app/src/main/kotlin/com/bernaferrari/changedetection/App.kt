/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import android.app.Application
import androidx.work.Configuration
import com.bernaferrari.changedetection.di.androidModule
import com.bernaferrari.changedetection.di.appModule
import com.bernaferrari.changedetection.di.initKoin
import com.bernaferrari.changedetection.di.platformModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class App :
    Application(),
    Configuration.Provider,
    KoinComponent {
    private val workerHelper: WorkerHelper by inject()

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()

    override fun onCreate() {
        super.onCreate()

        // Register MainActivity with the library's provider
        MainActivityProvider.register(MainActivity::class.java)

        // Initialize Napier for logging
        Napier.base(DebugAntilog())

        // Initialize Koin for all dependencies
        initKoin {
            androidLogger()
            androidContext(this@App)
            workManagerFactory()
            modules(
                appModule,
                platformModule,
                androidModule,
            )
        }
    }
}
