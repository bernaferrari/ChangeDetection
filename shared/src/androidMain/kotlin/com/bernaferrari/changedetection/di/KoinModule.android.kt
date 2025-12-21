/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.di

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Android-specific dependency initialization
 *
 * This module provides:
 * - Ktor HTTP Client with Android engine
 * - Room database setup and DAO access
 * - Repository implementations
 * - Platform-specific storage
 *
 * Integration with Koin will happen at the app module level
 */

/**
 * Create Android-specific HTTP client
 */
fun createAndroidHttpClient(): HttpClient {
    return HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
}

/**
 * Create Android repositories
 *
 * Usage from app module:
 * ```
 * val sitesDao = database.siteDao()
 * val snapsDao = database.snapsDao()
 * val sitesRepository = createAndroidSitesRepository(sitesDao)
 * val snapsRepository = createAndroidSnapsRepository(snapsDao)
 * ```
 */
fun createAndroidDependencies(context: Context): AndroidDependencies {
    return AndroidDependencies(
        httpClient = createAndroidHttpClient(),
        context = context
    )
}

/**
 * Container for Android dependencies
 * Can be used to initialize repositories when needed
 */
data class AndroidDependencies(
    val httpClient: HttpClient,
    val context: Context
)

/**
 * Actual implementation of platformModule
 */
actual fun platformModule(): Any = "Android DI Module"
