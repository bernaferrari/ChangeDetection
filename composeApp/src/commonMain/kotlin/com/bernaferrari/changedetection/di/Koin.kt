/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.util.ApplicationScope
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.SyncEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin

private var koinApplication: KoinApplication? = null

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    Napier.base(DebugAntilog())

    return koinApplication ?: startKoin<ChangeDetectionKoinApp> { appDeclaration() }.also { application ->
        koinApplication = application
        application.koin.get<ApplicationScope>().scope.launch { application.koin.get<SnapshotFileCleanup>().run() }
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
