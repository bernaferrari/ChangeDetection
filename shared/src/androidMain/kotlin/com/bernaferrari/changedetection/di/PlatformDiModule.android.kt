package com.bernaferrari.changedetection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import androidx.datastore.preferences.preferencesDataStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.importLegacyDatabase
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.AndroidShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.sync.AndroidChangeNotifier
import com.bernaferrari.changedetection.sync.AndroidFailureNotifier
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.AndroidScreenshotSyncer
import com.bernaferrari.changedetection.sync.SyncProgressTracker
import com.bernaferrari.changedetection.sync.ChangeNotifier
import com.bernaferrari.changedetection.sync.FailureNotifier
import com.bernaferrari.changedetection.sync.SyncEngine
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
actual class PlatformDiModule {
    @Single
    fun capturePreviewer(@Provided context: Context, engine: SyncEngine, preferences: DataStore<Preferences>): com.bernaferrari.changedetection.addedit.CapturePreviewer = com.bernaferrari.changedetection.addedit.AndroidCapturePreviewer(context, engine, preferences)

    @Single
    fun database(@Provided context: Context): ChangeDatabase {
        val appContext = context.applicationContext

        // A failed import must stop startup before Room can open a fresh or partial copy.
        // Keep the original database and its sidecars as an upgrade backup.
        importLegacyDatabase(appContext.getDatabasePath("Changes.db"), appContext.getDatabasePath("changes.db"))

        val path = appContext.getDatabasePath("changes.db").absolutePath
        val builder = Room.databaseBuilder<ChangeDatabase>(
            context = appContext,
            name = path,
        )
        return getRoomDatabase(builder, BundledSQLiteDriver())
    }

    @Single
    fun dataStore(@Provided context: Context): DataStore<Preferences> = context.dataStore

    @Single
    fun workerHelper(
        @Provided context: Context,
        dataStore: DataStore<Preferences>,
        session: com.bernaferrari.changedetection.background.TurboSession,
        applicationScope: com.bernaferrari.changedetection.util.ApplicationScope,
    ): WorkerHelper = WorkerHelper(context, dataStore, session, applicationScope)

    @Single
    fun checkRunner(
        sites: com.bernaferrari.changedetection.repo.source.SitesRepository,
        engine: SyncEngine,
        screenshots: AndroidScreenshotSyncer,
        preferences: DataStore<Preferences>,
    ): com.bernaferrari.changedetection.sync.CheckRunner = com.bernaferrari.changedetection.sync.AndroidCheckRunner(sites, engine, screenshots, preferences)

    @Single
    fun turboSession(
        applicationScope: com.bernaferrari.changedetection.util.ApplicationScope,
        runner: com.bernaferrari.changedetection.sync.CheckRunner,
    ): com.bernaferrari.changedetection.background.TurboSession = com.bernaferrari.changedetection.background.TurboSession(
        kotlinx.coroutines.CoroutineScope(applicationScope.scope.coroutineContext + kotlinx.coroutines.Dispatchers.Main.immediate),
        runner,
    )

    @Single
    fun monitoringManager(workerHelper: WorkerHelper): MonitoringManager = workerHelper

    @Single
    fun shareManager(@Provided context: Context): ShareManager = AndroidShareManager(context)

    @Single(binds = [FileStorage::class])
    fun fileStorage(@Provided context: Context): AndroidFileStorage = AndroidFileStorage(context)

    @Single
    fun backupService(
        database: ChangeDatabase,
        sitesDao: SitesDao,
        snapsDao: SnapsDao,
        fileStorage: AndroidFileStorage,
        cleanup: com.bernaferrari.changedetection.storage.SnapshotFileCleanup,
        preferences: DataStore<Preferences>,
    ): com.bernaferrari.changedetection.backup.BackupService = com.bernaferrari.changedetection.backup.BackupService(
        database,
        sitesDao,
        snapsDao,
        fileStorage,
        cleanup,
        readSettings = { com.bernaferrari.changedetection.backup.BackupSettings.read(preferences.data.first()) },
        writeSettings = { values -> preferences.edit { com.bernaferrari.changedetection.backup.BackupSettings.apply(values, it) } },
    )

    @Single
    fun changeNotifier(@Provided context: Context): ChangeNotifier = AndroidChangeNotifier(context.applicationContext)

    @Single
    fun failureNotifier(@Provided context: Context): FailureNotifier = AndroidFailureNotifier(context.applicationContext)

    @Single
    fun screenshotSyncer(
        @Provided context: Context,
        syncEngine: SyncEngine,
        dataStore: DataStore<Preferences>,
        progressTracker: SyncProgressTracker,
        sites: com.bernaferrari.changedetection.repo.source.SitesRepository,
    ): AndroidScreenshotSyncer = AndroidScreenshotSyncer(
        syncEngine,
        dataStore,
        progressTracker,
        onIcon = sites::updateIcon,
        browserFactory = AndroidScreenshotBrowserFactory(context),
    )
}
