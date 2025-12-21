/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */

package com.bernaferrari.changedetection.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.ios.iOS
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific Koin dependencies
 *
 * Includes:
 * - Ktor HTTP Client with iOS native engine
 * - iOS KeyChain for secure storage
 * - iOS UserDefaults for preferences
 * - iOS FileManager for file operations
 */
internal actual fun platformModule(): Module = module {
    // iOS-specific HTTP Client using native URLSession
    single<HttpClient> {
        HttpClient(iOS)
    }

    // iOS KeyChain for secure credential storage
    // single { KeyChainStorage() }

    // iOS UserDefaults wrapper
    // single { UserDefaultsPreferences() }

    // iOS-specific file management
}
