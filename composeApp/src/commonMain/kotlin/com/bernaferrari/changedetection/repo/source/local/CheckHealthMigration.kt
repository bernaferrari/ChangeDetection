/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/** Only a successful latest attempt can establish the legacy last-success timestamp. */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE sites ADD COLUMN lastSuccessfulCheck INTEGER DEFAULT NULL").use { it.step() }
        connection.prepare("UPDATE sites SET lastSuccessfulCheck = timestamp WHERE isSuccessful = 1 AND timestamp > 0").use { it.step() }
    }
}

/** Legacy failures remain unknown; an old error must never be guessed from a timestamp. */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE sites ADD COLUMN lastFailureCode TEXT DEFAULT NULL").use { it.step() }
    }
}

/** Empty region rules preserve every existing monitor's whole-page behavior. */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE sites ADD COLUMN textIncludeSelector TEXT NOT NULL DEFAULT ''").use { it.step() }
        connection.prepare("ALTER TABLE sites ADD COLUMN textExcludeSelector TEXT NOT NULL DEFAULT ''").use { it.step() }
    }
}

/** Visual filtering is opt-in; upgrading must not suppress existing alerts. */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE sites ADD COLUMN visualChangeThresholdBps INTEGER NOT NULL DEFAULT 0").use { it.step() }
    }
}
