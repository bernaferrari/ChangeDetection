/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.storage

import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.bernaferrari.changedetection.util.FileStorage
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class DurableSnapshotCleanupTest {
    @Test fun largeCascadeSurvivesRestartAndFailuresDoNotStarveLaterFiles() = runBlocking {
        val directory = Files.createTempDirectory("durable-cleanup").toFile()
        fun open() = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        var db = open()
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        try {
            db.withWriteTransaction {
                db.siteDao().insertSite(page("page"))
                db.siteDao().insertSite(page("other"))
                repeat(1203) { index ->
                    val id = "capture-${index.toString().padStart(5, '0')}"
                    db.snapsDao().insertSnap(snap(id, "page"))
                    if (index != 2) files.writeBytes(id, byteArrayOf(1)) // already missing is success
                }
                db.snapsDao().insertSnap(snap("protected", "other"))
                files.writeBytes("protected", byteArrayOf(9))
            }
            db.siteDao().deleteSiteById("page")
            assertEquals(500, db.pendingFileDeletionDao().firstBatch("z").size)
            // Simulate termination after the database commit, before any file cleanup.
            db.close()
            db = open()
            val failingFiles = object : FileStorage by files {
                override fun deleteFile(fileName: String): Boolean = when (fileName) {
                    "capture-00000" -> false
                    "capture-00001" -> throw java.io.IOException("Injected filesystem failure")
                    else -> files.deleteFile(fileName)
                }
            }
            SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), failingFiles).drain()
            assertEquals(listOf("capture-00000", "capture-00001"), db.pendingFileDeletionDao().firstBatch("z"))
            assertFalse(files.fileExists("capture-01202"))
            assertTrue(files.fileExists("protected"))
            db.close()
            db = open()
            // The process-start worker recovers pending work without any check.
            val worker = launch { SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files).run() }
            try {
                withTimeout(5000) {
                    while (db.pendingFileDeletionDao().lastId() != null) delay(10)
                }
            } finally {
                worker.cancelAndJoin()
            }
            assertFalse(files.fileExists("capture-00000"))
            assertFalse(files.fileExists("capture-00001"))
            assertNotNull(db.snapsDao().getSnapById("protected"))
            assertTrue(files.fileExists("protected"))
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun rollbackDoesNotQueueLiveFilesAndInterruptedAcknowledgementIsIdempotent() = runBlocking {
        val directory = Files.createTempDirectory("cleanup-rollback").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        try {
            db.siteDao().insertSite(page("page"))
            db.snapsDao().insertSnap(snap("saved", "page"))
            files.writeBytes("saved", byteArrayOf(1))
            assertTrue(
                runCatching {
                    db.withWriteTransaction {
                        db.siteDao().deleteSiteById("page")
                        error("Injected transaction failure")
                    }
                }.isFailure,
            )
            assertNotNull(db.snapsDao().getSnapById("saved"))
            assertNull(db.pendingFileDeletionDao().lastId())
            db.snapsDao().deleteSnapById("saved")
            val interrupted = object : FileStorage by files {
                override fun deleteFile(fileName: String): Boolean {
                    files.deleteFile(fileName)
                    throw CancellationException("Stopped after deleting file, before queue acknowledgement")
                }
            }
            assertTrue(runCatching { SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), interrupted).drain() }.exceptionOrNull() is CancellationException)
            assertEquals("saved", db.pendingFileDeletionDao().lastId())
            assertFalse(files.fileExists("saved"))
            SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files).drain()
            assertNull(db.pendingFileDeletionDao().lastId())
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun legacyUpgradePreservesHistoryAndInstallsTransactionalCleanup() = runBlocking {
        val directory = Files.createTempDirectory("cleanup-migration").toFile()
        val path = directory.resolve("test.db").path
        val driver = BundledSQLiteDriver()
        try {
            driver.open(path).use { connection ->
                fun execute(sql: String) {
                    connection.prepare(sql).use { it.step() }
                }
                // The last public release's tables (database version 1).
                execute("CREATE TABLE sites (title TEXT, url TEXT NOT NULL, timestamp INTEGER NOT NULL, siteId TEXT NOT NULL, isSuccessful INTEGER NOT NULL, isBrowser INTEGER NOT NULL, threshold INTEGER NOT NULL, isRead INTEGER NOT NULL, isSyncEnabled INTEGER NOT NULL, isNotificationEnabled INTEGER NOT NULL, notes TEXT NOT NULL, colors TEXT NOT NULL, constraints TEXT NOT NULL, PRIMARY KEY(siteId))")
                execute("CREATE TABLE snaps (snapId TEXT NOT NULL, siteId TEXT NOT NULL, timestamp INTEGER NOT NULL, contentType TEXT NOT NULL, contentCharset TEXT NOT NULL, contentSize INTEGER NOT NULL, PRIMARY KEY(snapId), FOREIGN KEY(siteId) REFERENCES sites(siteId) ON UPDATE NO ACTION ON DELETE CASCADE)")
                execute("INSERT INTO sites VALUES ('Saved','https://example.test',123,'page',1,0,0,0,1,1,'','0,0','false, false, false, false')")
                execute("INSERT INTO snaps VALUES ('saved','page',123,'text/html','utf-8',42)")
                execute("PRAGMA user_version = 1")
            }
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = path), driver)
            try {
                assertNotNull(db.snapsDao().getSnapById("saved"))
                assertNull(db.pendingFileDeletionDao().lastId())
                db.siteDao().saveWithHistoryReset(requireNotNull(db.siteDao().getSiteById("page")))
                assertNull(db.snapsDao().getSnapById("saved"))
                assertEquals(listOf("saved"), db.pendingFileDeletionDao().firstBatch("z"))
                assertNotNull(db.siteDao().getSiteById("page"))
            } finally {
                db.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun page(id: String) = Site("Page", "https://example.test/$id", 0L, "", id, 0 to 0)
    private fun snap(id: String, page: String) = Snap(id, page, 1L, "text/html", "UTF-8", 1)
}
