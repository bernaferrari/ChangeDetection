/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.source.SitesDataSource
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SitesLocalDataSource
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.repo.source.local.SnapsLocalDataSource
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/** Common services shared by every supported platform. */
@Module
class CoreDiModule {
    @Single
    fun errorHandler(): ErrorHandler = ErrorHandler()

    @Single
    fun httpClient(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000L
            connectTimeoutMillis = 15_000L
            socketTimeoutMillis = 30_000L
        }
    }

    @Single
    fun sitesDao(database: ChangeDatabase): SitesDao = database.siteDao()

    @Single
    fun snapsDao(database: ChangeDatabase): SnapsDao = database.snapsDao()

    @Single
    fun appExecutors(): AppExecutors = AppExecutors()

    @Single(binds = [SitesDataSource::class])
    fun sitesDataSource(
        appExecutors: AppExecutors,
        sitesDao: SitesDao,
    ): SitesDataSource = SitesLocalDataSource(appExecutors, sitesDao)

    @Single(binds = [SnapsDataSource::class])
    fun snapsDataSource(
        appExecutors: AppExecutors,
        snapsDao: SnapsDao,
        fileStorage: FileStorage,
    ): SnapsDataSource = SnapsLocalDataSource(appExecutors, snapsDao, fileStorage)

    @Single
    fun sitesRepository(dataSource: SitesDataSource): SitesRepository = SitesRepository(dataSource)

    @Single
    fun snapsRepository(dataSource: SnapsDataSource): SnapsRepository = SnapsRepository(dataSource)

    @Single
    fun syncEngine(
        httpClient: HttpClient,
        sitesDao: SitesDao,
        snapsDao: SnapsDao,
        fileStorage: FileStorage,
        errorHandler: ErrorHandler,
    ): SyncEngine = SyncEngine(httpClient, sitesDao, snapsDao, fileStorage, errorHandler)
}
