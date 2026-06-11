/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.sync.SyncEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

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

/**
 * Helper object for iOS to access common functionality.
 * This is exported to Swift as `KoinHelper.shared`.
 */
object KoinHelper {
    fun getSyncEngine(): SyncEngine = KoinPlatformTools.defaultContext().get().get()
}
