/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Core shared configuration for all platforms
 *
 * This module contains:
 * - HTTP Client configuration
 * - JSON serializer setup
 * - Repository interfaces (implemented per platform)
 */

/**
 * Create a multiplatform HTTP client with JSON serialization
 * Used by all platforms (Android, iOS, Desktop)
 */
fun createHttpClient(): HttpClient {
    return HttpClient {
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
 * Create a JSON serializer configuration
 * Used for serializing/deserializing domain models
 */
fun createJsonSerializer(): Json {
    return Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
}

/**
 * Platform-specific DI module registration
 * Implemented in androidMain, iosMain, desktopMain
 *
 * Each platform should:
 * 1. Create HTTPClient with platform-specific engine
 * 2. Create and register repositories
 * 3. Register platform-specific services (storage, etc)
 */
expect fun platformModule(): Any

/**
 * Initialize dependency injection
 * Call this once at application startup
 *
 * For Android:
 * ```
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         initDependencies()
 *     }
 * }
 * ```
 */
fun initDependencies() {
    // This will be implemented with Koin once dependency resolution is fixed
    // For now, repositories can be initialized directly in the app module
}
