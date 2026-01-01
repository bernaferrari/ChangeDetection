package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import org.koin.dsl.module
import org.koin.core.module.Module
import org.koin.android.ext.koin.androidContext

actual val platformModule: Module = module {
    single<FileStorage> { AndroidFileStorage(androidContext()) }
}
