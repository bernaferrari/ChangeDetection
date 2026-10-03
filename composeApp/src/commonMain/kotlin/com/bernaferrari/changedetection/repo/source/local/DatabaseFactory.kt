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
    // Schema v1 (app releases 2.1/2.2, file "Changes.db") jumps straight to
    // v3 via MIGRATION_1_3; the Android platform module registers it. A
    // version *downgrade* still falls back to a rebuild.
    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, *migrations)
    .fallbackToDestructiveMigrationOnDowngrade(true)
    .addCallback(snapshotCleanupCallback)
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()
