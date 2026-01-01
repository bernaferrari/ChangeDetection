package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.sync.SyncEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    // Initialize Napier
    Napier.base(DebugAntilog())

    return startKoin {
        appDeclaration()
        modules(appModule, platformModule)
    }
}

// Helper for iOS to get SyncEngine
fun KoinApplication.getSyncEngine(): SyncEngine = koin.get()
