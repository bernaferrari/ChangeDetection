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
    // Preserve the original public release. Retired development schemas may start fresh.
    .addMigrations(MIGRATION_1_4, *migrations)
    .fallbackToDestructiveMigration(true)
    .addCallback(snapshotCleanupCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
