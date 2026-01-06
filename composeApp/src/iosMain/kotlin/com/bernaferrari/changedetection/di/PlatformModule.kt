package com.bernaferrari.changedetection.di

import com.bernaferrari.changedetection.util.IosFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.platform.IosShareManager
import org.koin.dsl.module
import org.koin.core.module.Module

actual val platformModule: Module = module {
    single<FileStorage> { IosFileStorage() }
    single<ShareManager> { IosShareManager() }
}
