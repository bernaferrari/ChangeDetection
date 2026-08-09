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
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.WorkerHelper
import com.bernaferrari.changedetection.background.MonitoringManager
import com.bernaferrari.changedetection.platform.AndroidShareManager
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
actual class PlatformDiModule {
    @Single
    fun database(@Provided context: Context): ChangeDatabase {
        val appContext = context.applicationContext
        val path = appContext.getDatabasePath("changes.db").absolutePath
        return getRoomDatabase(
            Room.databaseBuilder<ChangeDatabase>(
                context = appContext,
                name = path,
            ),
            BundledSQLiteDriver(),
        )
    }

    @Single
    fun dataStore(@Provided context: Context): DataStore<Preferences> = context.dataStore

    @Single
    fun workerHelper(
        @Provided context: Context,
        dataStore: DataStore<Preferences>,
    ): WorkerHelper = WorkerHelper(context, dataStore)

    @Single
    fun monitoringManager(workerHelper: WorkerHelper): MonitoringManager = workerHelper

    @Single
    fun shareManager(@Provided context: Context): ShareManager = AndroidShareManager(context)

    @Single
    fun fileStorage(@Provided context: Context): FileStorage = AndroidFileStorage(context)
}
