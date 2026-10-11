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
    // Preserve the public version-1 release and both version-5 development schemas.
    // Older unsupported development downgrades still start fresh.
    .addMigrations(MIGRATION_1_5, MIGRATION_5_6, *migrations)
    .fallbackToDestructiveMigrationOnDowngrade(true)
    .addCallback(snapshotCleanupCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
