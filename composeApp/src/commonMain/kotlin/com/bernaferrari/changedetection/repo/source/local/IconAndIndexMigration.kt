/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * v10: pages remember their icon, and the snapshot indexes match the queries that actually run.
 * Every history read is "one page, newest first" (siteId, timestamp, snapId); indexes that only
 * duplicated the primary key or a prefix of another index cost a write on every check and are dropped.
 */
val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override suspend fun migrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE sites ADD COLUMN iconUrl TEXT NOT NULL DEFAULT ''",
            "DROP INDEX IF EXISTS index_sites_siteId",
            "DROP INDEX IF EXISTS index_snaps_siteId_snapId",
            "DROP INDEX IF EXISTS index_snaps_siteId_timestamp",
            "DROP INDEX IF EXISTS index_snaps_siteId_contentType",
            "DROP INDEX IF EXISTS index_snaps_contentType",
            "DROP INDEX IF EXISTS index_snaps_contentHash",
            "DROP INDEX IF EXISTS index_snaps_processedContentHash",
            "CREATE INDEX IF NOT EXISTS index_snaps_siteId_timestamp_snapId ON snaps (siteId, timestamp, snapId)",
        ).forEach { sql -> connection.prepare(sql).use { it.step() } }
    }
}
