package com.bernaferrari.changedetection.di

import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.util.ApplicationScope
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.SyncEngine
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools
import org.koin.plugin.module.dsl.startKoin

private var koinApplication: KoinApplication? = null

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication {
    Napier.base(DebugAntilog())

    return koinApplication ?: run {
        // Configure once, before any composable can request an avatar. setSafe must
        // never run again after Coil has instantiated the process-wide loader.
        coil3.SingletonImageLoader.setSafe { context ->
            coil3.ImageLoader.Builder(context)
                .components { add(coil3.network.ktor3.KtorNetworkFetcherFactory()) }
                .build()
        }
        startKoin<ChangeDetectionKoinApp> { appDeclaration() }
    }.also { application ->
        koinApplication = application
        application.koin.get<ApplicationScope>().scope.launch { application.koin.get<SnapshotFileCleanup>().run() }
    }
}

// Helper for iOS to get SyncEngine
fun KoinApplication.getSyncEngine(): SyncEngine = koin.get()

/**
 * Helper object for iOS to access common functionality.
 * This is exported to Swift as `KoinHelper.shared`.
 */
object KoinHelper {
    fun getSyncEngine(): SyncEngine = KoinPlatformTools.defaultContext().get().get()
}
