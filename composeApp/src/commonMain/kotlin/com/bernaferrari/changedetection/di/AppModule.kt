/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.source.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.comparison.ComparisonViewModel
import com.bernaferrari.changedetection.ui.logs.LogsViewModel
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel

val appModule: Module = module {
    // Ktor HttpClient with default timeout configuration
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
            install(HttpTimeout) {
                // Default timeouts - can be overridden per-request
                requestTimeoutMillis = 30_000L // 30 seconds
                connectTimeoutMillis = 15_000L // 15 seconds
                socketTimeoutMillis = 30_000L // 30 seconds
            }
        }
    }

    // Error Handler - centralized error management
    single { ErrorHandler() }

    // DAOs (provided by platform-specific modules that inject ChangeDatabase)
    single<SitesDao> { get<ChangeDatabase>().siteDao() }
    single<SnapsDao> { get<ChangeDatabase>().snapsDao() }

    // App Executors
    single { AppExecutors() }

    // Data Sources
    single<SitesDataSource> { SitesLocalDataSource(get(), get()) }
    single<SnapsDataSource> { SnapsLocalDataSource(get(), get(), get()) }

    // Repositories
    single { SitesRepository(get()) }
    single { SnapsRepository(get()) }

    // Sync Engine with all dependencies
    single { SyncEngine(get<HttpClient>(), get<SitesDao>(), get<SnapsDao>(), get<FileStorage>(), get<ErrorHandler>()) }

    // ViewModels
    viewModelOf(::MainViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ComparisonViewModel)
    viewModelOf(::LogsViewModel)
    viewModelOf(::ThemeViewModel)
}
