/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop (JVM)-specific Koin dependencies
 *
 * Includes:
 * - Ktor HTTP Client with OkHttp engine
 * - SQL Lite database via SQLDelight
 * - Java file system operations
 * - Desktop configuration management
 */
internal actual fun platformModule(): Module = module {
    // Desktop HTTP Client using OkHttp
    single<HttpClient> {
        HttpClient(OkHttp)
    }

    // SQL Lite database
    // single { createDatabaseDriver() }

    // Desktop preferences/config
    // single { DesktopPreferences() }

    // Platform-specific file operations
}
