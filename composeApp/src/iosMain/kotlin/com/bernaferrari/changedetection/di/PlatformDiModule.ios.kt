/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.background.IosMonitoringManager
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.IosShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getDatabaseBuilder
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.sync.ChangeNotifier
import com.bernaferrari.changedetection.sync.NoOpChangeNotifier
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.util.IosFileStorage
import okio.Path.Companion.toPath
import org.koin.core.KoinApplication
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@Module
actual class PlatformDiModule {
    @Single
    fun capturePreviewer(engine: com.bernaferrari.changedetection.sync.SyncEngine): com.bernaferrari.changedetection.addedit.CapturePreviewer = com.bernaferrari.changedetection.addedit.HtmlCapturePreviewer(engine)

    @Single
    fun database(): ChangeDatabase = getRoomDatabase(getDatabaseBuilder(), BundledSQLiteDriver())

    @Single
    fun dataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = {
            val documentDirectory =
                NSFileManager.defaultManager
                    .URLForDirectory(
                        NSDocumentDirectory,
                        NSUserDomainMask,
                        null,
                        false,
                        null,
                    )?.path.orEmpty()
            "$documentDirectory/settings.preferences_pb".toPath()
        },
    )

    @Single
    fun monitoringManager(
        syncEngine: SyncEngine,
        dataStore: DataStore<Preferences>,
    ): MonitoringManager = IosMonitoringManager(syncEngine, dataStore)

    @Single
    fun shareManager(): ShareManager = IosShareManager()

    @Single
    fun fileStorage(): FileStorage = IosFileStorage()

    @Single
    fun changeNotifier(): ChangeNotifier = NoOpChangeNotifier
}

fun initKoinIos(): KoinApplication = initKoin()
