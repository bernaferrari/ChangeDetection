/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.source.local.MIGRATION_1_3
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.AndroidShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.sync.AndroidChangeNotifier
import com.bernaferrari.changedetection.sync.AndroidScreenshotBrowserFactory
import com.bernaferrari.changedetection.sync.AndroidScreenshotSyncer
import com.bernaferrari.changedetection.sync.SyncProgressTracker
import com.bernaferrari.changedetection.sync.ChangeNotifier
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

        importLegacyDatabaseIfNeeded(appContext)

        val path = appContext.getDatabasePath("changes.db").absolutePath
        // The callback deletes the legacy "Changes.db" only once Room has
        // actually opened (and migrated) the imported copy; if the migration
        // fails, the legacy file is still the only copy of the user's history.
        val builder = Room.databaseBuilder<ChangeDatabase>(
            context = appContext,
            name = path,
        ).addCallback(
            object : RoomDatabase.Callback() {
                override suspend fun onOpen(db: SQLiteConnection) {
                    java.io.File(appContext.getDatabasePath("Changes.db").path).delete()
                }
            },
        )
        return getRoomDatabase(
            builder,
            BundledSQLiteDriver(),
            migrations = arrayOf(MIGRATION_1_3),
        )
    }

    /**
     * App releases 2.1/2.2 stored data in a database file named "Changes.db"
     * (capital C) with schema v1. Copy the legacy file over the new name
     * ("changes.db") so existing users keep their history; [MIGRATION_1_3]
     * then migrates the imported copy from v1 to the current schema.
     * The legacy file is NOT deleted here — it is removed in a
     * RoomDatabase.Callback.onOpen, i.e. only after Room has successfully
     * opened (and migrated) the imported copy. If the migration were to
     * fail, the legacy file remains the only intact copy of the user's data.
     */
    private fun importLegacyDatabaseIfNeeded(appContext: Context) {
        try {
            val legacy = appContext.getDatabasePath("Changes.db")
            val current = appContext.getDatabasePath("changes.db")
            if (!legacy.exists()) return
            if (current.exists() && current.length() > 0) {
                // Current install already has data; leave both alone.
                return
            }
            legacy.copyTo(current, overwrite = true)
            // Also migrate WAL/SHM sidecar files when present so no committed
            // transaction in the legacy write-ahead log is lost.
            listOf("-wal", "-shm").forEach { suffix ->
                val sidecar = java.io.File(legacy.path + suffix)
                if (sidecar.exists()) {
                    sidecar.copyTo(java.io.File(current.path + suffix), overwrite = true)
                }
            }
        } catch (e: Exception) {
            // Never block startup on the import; worst case is the old behavior
            // (fresh database).
        }
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

    @Single
    fun fileStorage(@Provided context: Context): FileStorage = AndroidFileStorage(context)

    @Single
    fun changeNotifier(@Provided context: Context): ChangeNotifier = AndroidChangeNotifier(context.applicationContext)

    @Single
    fun screenshotSyncer(
        @Provided context: Context,
        syncEngine: SyncEngine,
        dataStore: DataStore<Preferences>,
        progressTracker: SyncProgressTracker,
    ): AndroidScreenshotSyncer = AndroidScreenshotSyncer(
        syncEngine,
        dataStore,
        progressTracker,
        AndroidScreenshotBrowserFactory(context),
    )
}
