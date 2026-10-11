/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteDriver
import kotlinx.coroutines.Dispatchers

fun getRoomDatabase(
    builder: RoomDatabase.Builder<ChangeDatabase>,
    driver: SQLiteDriver,
    migrations: Array<out Migration> = emptyArray(),
): ChangeDatabase = builder
    .setDriver(driver)
    // One upgrade path: the last public release (version 1, 2019–2024) straight to today. Unpublished
    // pre-release versions start fresh, and so does a downgrade.
    .addMigrations(MIGRATION_1_10, *migrations)
    .fallbackToDestructiveMigrationFrom(true, *UNPUBLISHED_VERSIONS)
    .fallbackToDestructiveMigrationOnDowngrade(true)
    .addCallback(snapshotCleanupCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
