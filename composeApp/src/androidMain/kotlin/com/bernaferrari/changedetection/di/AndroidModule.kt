/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.bernaferrari.changedetection.SyncWorker
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.addedit.AddEditViewModel
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.localBrowser.LocalBrowserViewModel
import com.bernaferrari.changedetection.notifications.NotificationHelper
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

// DataStore extension
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val androidModule: Module =
    module {
        // Database
        single<ChangeDatabase> {
            Room
                .databaseBuilder(
                    androidContext(),
                    ChangeDatabase::class.java,
                    "changes.db",
                ).build()
        }

        // DataStore
        single<DataStore<Preferences>> { androidContext().dataStore }

        // OkHttpClient for SyncWorker
        single {
            OkHttpClient
                .Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }

        // NotificationHelper
        single { NotificationHelper(androidContext()) }

        // ErrorHandler
        single { ErrorHandler() }

        // WorkerHelper (implements MonitoringManager)
        single { WorkerHelper(androidContext(), get()) }
        single<MonitoringManager> { get<WorkerHelper>() }
        single<com.bernaferrari.changedetection.platform.ShareManager> {
            com.bernaferrari.changedetection.platform
                .AndroidShareManager(androidContext())
        }

        // Android-specific ViewModels
        viewModel { ThemeViewModel(get()) }
        viewModel { MainViewModel(get(), get(), get(), get(), get()) }
        viewModelOf(::LocalBrowserViewModel)
        viewModel { (siteId: String?) -> AddEditViewModel(siteId, get(), get()) }

        // SettingsViewModel with dependencies
        viewModel { SettingsViewModel(get(), get()) }

        // Register SyncWorker with Koin WorkManager
        workerOf(::SyncWorker)
    }
