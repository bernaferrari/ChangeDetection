/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single<FileStorage> { AndroidFileStorage(androidContext()) }
    }
