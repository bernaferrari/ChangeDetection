/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * The one upgrade path: from the last public release of the original app to today's schema.
 *
 * Every public release (2.1/2.2 in 2019 through the 2022 and 2024 updates) used database version 1 in a file
 * named "Changes.db". Version 1 exists in two shapes, because the August 2019 update added `isBrowser` and
 * `threshold` to `sites` without bumping the version:
 *
 * - sites: title, url, timestamp, siteId, isSuccessful, [isBrowser, threshold], isRead, isSyncEnabled,
 *   isNotificationEnabled, notes, colors, constraints; unique index on siteId.
 * - snaps: snapId, siteId, timestamp, contentType, contentCharset, contentSize; unique index on
 *   (siteId, snapId); foreign key to sites with cascade delete.
 *
 * Snapshot bodies live in files named by snapId, exactly as they do now, so they carry over untouched.
 * Columns are added in place (never by rebuilding tables, which would cascade-delete history), and only
 * when missing, so both shapes of version 1 upgrade the same way.
 */
val MIGRATION_1_5: Migration = object : Migration(1, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        val siteColumns = connection.columns("sites")
        val snapColumns = connection.columns("snaps")
        suspend fun addSite(column: String, definition: String) {
            if (column !in siteColumns) connection.exec("ALTER TABLE sites ADD COLUMN `$column` $definition")
        }
        suspend fun addSnap(column: String, definition: String) {
            if (column !in snapColumns) connection.exec("ALTER TABLE snaps ADD COLUMN `$column` $definition")
        }

        // Columns the 2019 update added (missing only in 2.1/2.2 databases).
        addSite("isBrowser", "INTEGER NOT NULL DEFAULT 0")
        addSite("threshold", "INTEGER NOT NULL DEFAULT 0")
        // Everything added since. Defaults keep each page's old behavior: the app's capture setting, whole
        // page, no visual filter, no icon yet.
        addSite("captureMode", "TEXT NOT NULL DEFAULT 'DEFAULT'")
        addSite("lastSuccessfulCheck", "INTEGER DEFAULT NULL")
        addSite("lastFailureCode", "TEXT DEFAULT NULL")
        addSite("textIncludeSelector", "TEXT NOT NULL DEFAULT ''")
        addSite("textExcludeSelector", "TEXT NOT NULL DEFAULT ''")
        addSite("visualChangeThresholdBps", "INTEGER NOT NULL DEFAULT 0")
        addSite("iconUrl", "TEXT NOT NULL DEFAULT ''")
        // Hashes are filled lazily on the next comparison.
        addSnap("contentHash", "TEXT NOT NULL DEFAULT ''")
        addSnap("processedContentHash", "TEXT NOT NULL DEFAULT ''")

        // The old app never used isRead (always false); today false means "a change waits for review",
        // which would greet every upgraded user with all pages marked new.
        connection.exec("UPDATE sites SET isRead = 1")
        // Only a successful latest check can stand in for the last success; failures stay unknown.
        connection.exec("UPDATE sites SET lastSuccessfulCheck = timestamp WHERE lastSuccessfulCheck IS NULL AND isSuccessful = 1 AND timestamp > 0")

        // Version 1 indexes duplicated the primary keys; today's indexes match the queries that run.
        connection.exec("DROP INDEX IF EXISTS index_sites_siteId")
        connection.exec("DROP INDEX IF EXISTS index_snaps_siteId_snapId")
        connection.exec("CREATE INDEX IF NOT EXISTS index_snaps_siteId_timestamp_snapId ON snaps (siteId, timestamp, snapId)")
        connection.exec("CREATE INDEX IF NOT EXISTS index_snaps_composite ON snaps (siteId, contentType, timestamp)")
        connection.exec("CREATE INDEX IF NOT EXISTS index_snaps_siteId_contentHash ON snaps (siteId, contentHash)")
        connection.exec("CREATE INDEX IF NOT EXISTS index_snaps_timestamp ON snaps (timestamp)")

        // Files of deleted snapshots are removed in the background, even after the app restarts.
        connection.exec("CREATE TABLE IF NOT EXISTS pendingFileDeletions (snapId TEXT NOT NULL PRIMARY KEY)")
    }
}

private suspend fun SQLiteConnection.exec(sql: String) = prepare(sql).use { it.step() }

private suspend fun SQLiteConnection.columns(table: String): Set<String> = prepare("PRAGMA table_info(`$table`)").use { statement ->
    buildSet { while (statement.step()) add(statement.getText(1)) }
}
