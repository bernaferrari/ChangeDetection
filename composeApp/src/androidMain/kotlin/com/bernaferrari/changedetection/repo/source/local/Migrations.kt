/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/**
 * Hand-written migration from the schema shipped by app releases 2.1/2.2
 * (database version 1, file "Changes.db") straight to the current version 3.
 *
 * v1 -> v3 mapping (reconstructed from the entities at git tag `2.1`; the
 * schema never changed between 2.1 and 2.2, both were version 1):
 *
 * - `sites`: identical to today except it lacked `isBrowser` and `threshold`.
 *   Both are added as NOT NULL with default 0 (`isBrowser = false`,
 *   `threshold = 0`). The `colors` and `constraints` converter formats are
 *   unchanged, so those TEXT columns carry over as-is.
 * - `snaps`: identical except it lacked `contentHash` and
 *   `processedContentHash`. Both are added as NOT NULL TEXT with default ''
 *   so existing rows hash lazily on their next comparison instead of being
 *   wiped.
 * - Table names, column names/types and both primary keys are unchanged;
 *   only the indices introduced since then need to be created.
 */
val MIGRATION_1_3: Migration = object : Migration(1, 3) {
    override suspend fun migrate(db: SQLiteConnection) {
        // androidx.sqlite's execSQL extension lives in nonWebMain/webMain, not
        // visible from androidMain's source set — use the driver primitive.
        fun SQLiteConnection.exec(sql: String) {
            prepare(sql).use { it.step() }
        }

        db.exec("ALTER TABLE sites ADD COLUMN isBrowser INTEGER NOT NULL DEFAULT 0")
        db.exec("ALTER TABLE sites ADD COLUMN threshold INTEGER NOT NULL DEFAULT 0")

        db.exec("ALTER TABLE snaps ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
        db.exec("ALTER TABLE snaps ADD COLUMN processedContentHash TEXT NOT NULL DEFAULT ''")

        db.exec("CREATE UNIQUE INDEX IF NOT EXISTS `index_sites_siteId` ON `sites` (`siteId`)")
        db.exec(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_snaps_siteId_snapId` ON `snaps` (`siteId`, `snapId`)",
        )
        db.exec(
            "CREATE INDEX IF NOT EXISTS `index_snaps_siteId_timestamp` ON `snaps` (`siteId`, `timestamp`)",
        )
        db.exec(
            "CREATE INDEX IF NOT EXISTS `index_snaps_siteId_contentType` ON `snaps` (`siteId`, `contentType`)",
        )
        db.exec("CREATE INDEX IF NOT EXISTS `index_snaps_timestamp` ON `snaps` (`timestamp`)")
        db.exec("CREATE INDEX IF NOT EXISTS `index_snaps_contentType` ON `snaps` (`contentType`)")
        db.exec(
            "CREATE INDEX IF NOT EXISTS `index_snaps_composite` ON `snaps` (`siteId`, `contentType`, `timestamp`)",
        )
        db.exec("CREATE INDEX IF NOT EXISTS `index_snaps_contentHash` ON `snaps` (`contentHash`)")
        db.exec(
            "CREATE INDEX IF NOT EXISTS `index_snaps_processedContentHash` ON `snaps` (`processedContentHash`)",
        )
        db.exec(
            "CREATE INDEX IF NOT EXISTS `index_snaps_siteId_contentHash` ON `snaps` (`siteId`, `contentHash`)",
        )
    }
}
