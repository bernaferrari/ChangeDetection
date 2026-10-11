/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteConnection

/** No foreign key: cleanup must survive deletion of the owning page and snapshot. */
@Entity(tableName = "pendingFileDeletions")
data class PendingFileDeletion(@PrimaryKey val snapId: String)

@Dao
interface PendingFileDeletionDao {
    @Query("SELECT MAX(snapId) FROM pendingFileDeletions")
    suspend fun lastId(): String?

    @Query("SELECT snapId FROM pendingFileDeletions WHERE snapId <= :through ORDER BY snapId LIMIT 500")
    suspend fun firstBatch(through: String): List<String>

    @Query("SELECT snapId FROM pendingFileDeletions WHERE snapId > :after AND snapId <= :through ORDER BY snapId LIMIT 500")
    suspend fun batch(after: String, through: String): List<String>

    @Query("DELETE FROM pendingFileDeletions WHERE snapId = :id")
    suspend fun complete(id: String)
}

/** The trigger participates in the deleting transaction, including foreign-key cascades. */
private suspend fun installCleanupTrigger(connection: SQLiteConnection) {
    connection.prepare(
        "CREATE TRIGGER IF NOT EXISTS queue_deleted_snapshot AFTER DELETE ON snaps BEGIN " +
            "INSERT OR IGNORE INTO pendingFileDeletions(snapId) VALUES (OLD.snapId); END",
    ).use { it.step() }
}

internal val snapshotCleanupCallback = object : RoomDatabase.Callback() {
    override suspend fun onOpen(connection: SQLiteConnection) = installCleanupTrigger(connection)
}
