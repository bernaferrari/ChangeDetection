/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.platform.IosShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.util.IosFileStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS platform-specific Koin module.
 * Provides file storage and sharing capabilities.
 * Note: Database, DataStore, MonitoringManager, and ViewModels are in iosModule.
 */
actual val platformModule: Module =
    module {
        single<FileStorage> { IosFileStorage() }
        single<ShareManager> { IosShareManager() }
    }
