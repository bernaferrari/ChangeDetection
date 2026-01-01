package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.util.IosFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import org.koin.dsl.module
import org.koin.core.module.Module

actual val platformModule: Module = module {
    single<FileStorage> { IosFileStorage() }
}
