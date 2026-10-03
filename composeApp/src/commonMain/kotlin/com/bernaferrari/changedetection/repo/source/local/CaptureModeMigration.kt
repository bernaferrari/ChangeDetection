/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/** Adds page-specific capture choice without changing existing behavior or history. */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.prepare("ALTER TABLE sites ADD COLUMN captureMode TEXT NOT NULL DEFAULT 'DEFAULT'").use { it.step() }
    }
}
