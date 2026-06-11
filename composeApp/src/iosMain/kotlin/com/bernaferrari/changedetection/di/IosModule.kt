/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bernaferrari.changedetection.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.bernaferrari.changedetection.addedit.AddEditViewModel
import com.bernaferrari.changedetection.background.IosMonitoringManager
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.localBrowser.LocalBrowserViewModel
import com.bernaferrari.changedetection.platform.IosShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getDatabaseBuilder
import com.bernaferrari.changedetection.ui.comparison.ComparisonViewModel
import com.bernaferrari.changedetection.ui.logs.LogsViewModel
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific Koin module.
 * Provides all iOS-specific dependencies including database, datastore,
 * and platform-specific implementations.
 *
 * Note: ErrorHandler, SyncEngine, and repositories are provided by appModule.
 * FileStorage is provided by platformModule.
 */
val iosModule: Module =
    module {
        // Database - using Room multiplatform
        single<ChangeDatabase> {
            getDatabaseBuilder().build()
        }

        // DataStore - using multiplatform preferences
        single<DataStore<Preferences>> {
            PreferenceDataStoreFactory.createWithPath(
                corruptionHandler = null,
                migrations = emptyList(),
                produceFile = {
                    // Store in app's documents directory
                    val documentDirectory =
                        NSFileManager.defaultManager
                            .URLForDirectory(
                                NSDocumentDirectory,
                                NSUserDomainMask,
                                null,
                                false,
                                null,
                            )?.path ?: ""
                    "$documentDirectory/settings.preferences_pb".toPath()
                },
            )
        }

        // MonitoringManager - iOS implementation
        single<MonitoringManager> {
            IosMonitoringManager(
                syncEngine = get(),
                dataStore = get(),
            )
        }

        // ShareManager - iOS implementation (also provided by platformModule, this overrides)
        single<ShareManager> { IosShareManager() }

        // ViewModels - iOS needs to declare these since they depend on MonitoringManager
        viewModel { ThemeViewModel(get()) }
        viewModel { MainViewModel(get(), get(), get(), get(), get()) }
        viewModel { LocalBrowserViewModel() } // Uses KoinComponent
        viewModel { (siteId: String?) -> AddEditViewModel(siteId, get(), get()) }
        viewModel { SettingsViewModel(get(), get()) }
        viewModel { LogsViewModel(get(), get()) }
        viewModel { ComparisonViewModel(get(), get(), get(), get()) }
    }

/**
 * Initialize Koin for iOS with all required modules.
 */
fun initKoinIos(): org.koin.core.KoinApplication = initKoin {
    modules(appModule, platformModule, iosModule)
}
