/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.storage

import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.util.FileStorage
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class SnapshotMaintenanceDatabaseTest {
    @Test fun pageDeletionIncludesAConcurrentCommittedCaptureAndRejectsLaterCommits() = runBlocking {
        val directory = Files.createTempDirectory("page-delete-race").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = com.bernaferrari.changedetection.util.DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val captureReady = CompletableDeferred<Unit>()
        val releaseCapture = CompletableDeferred<Unit>()
        try {
            db.siteDao().insertSite(Site("Page", "https://example.test", 0L, "", "page", 0 to 0))
            db.siteDao().insertSite(Site("Other", "https://other.test", 0L, "", "other", 0 to 0))
            files.writeBytes("unrelated", byteArrayOf(1))
            db.snapsDao().insertSnap(Snap("unrelated", "other", 0L, "text/html", "UTF-8", 1))
            val capture = async(Dispatchers.Default) {
                db.withWriteTransaction {
                    files.writeBytes("concurrent", byteArrayOf(2))
                    db.snapsDao().insertSnap(Snap("concurrent", "page", 1L, "text/html", "UTF-8", 1))
                    captureReady.complete(Unit)
                    releaseCapture.await()
                }
            }
            withTimeout(5000) { captureReady.await() }
            val deletionStarted = CompletableDeferred<Unit>()
            val deletion = async(Dispatchers.Default) {
                deletionStarted.complete(Unit)
                SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)).deleteSiteWithSnaps("page")
            }
            deletionStarted.await()
            releaseCapture.complete(Unit)
            withTimeout(5000) {
                capture.await()
                deletion.await()
            }
            assertNull(db.siteDao().getSiteById("page"))
            assertNull(db.snapsDao().getSnapById("concurrent"))
            assertFalse(files.fileExists("concurrent"))
            assertTrue(files.fileExists("unrelated"))
            assertNotNull(db.snapsDao().getSnapById("unrelated"))
            // The foreign key also prevents an obsolete producer from committing after deletion.
            assertTrue(
                runCatching {
                    db.snapsDao().insertSnap(Snap("late", "page", 2L, "text/html", "UTF-8", 1))
                }.isFailure,
            )
            db.siteDao().deleteSiteById("page")
            assertNull(db.pendingFileDeletionDao().lastId())
        } finally {
            releaseCapture.complete(Unit)
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun explicitCleanupBatchesMetadataAndRemovesOnlyObsoletePageFiles() = runBlocking {
        val directory = Files.createTempDirectory("manual-cleanup").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val present = mutableSetOf<String>()
        var deleted = 0
        val files = object : FileStorage {
            override fun writeBytes(fileName: String, bytes: ByteArray) {
                present.add(fileName)
            }
            override fun readBytes(fileName: String) = byteArrayOf()
            override fun fileExists(fileName: String) = fileName in present
            override fun deleteFile(fileName: String): Boolean = present.remove(fileName).also { if (it) deleted++ }
        }
        try {
            val archive = (0 until 1202).map { index ->
                Snap("capture-${index.toString().padStart(5, '0')}", "page", (index / 3).toLong(), "text/html", "UTF-8", 1, contentHash = "same-content")
            }
            db.withWriteTransaction {
                db.siteDao().insertSite(Site("Page", "https://example.test", 0L, "", "page", 0 to 0))
                db.siteDao().insertSite(Site("Other", "https://other.test", 0L, "", "other", 0 to 0))
                (archive + Snap("unrelated", "other", 0L, "text/html", "UTF-8", 1)).forEach {
                    db.snapsDao().insertSnap(it)
                    present.add(it.snapId)
                }
            }
            val batch = db.snapsDao().removeOlderVersionsBatch("page", 37)
            assertEquals(37, batch.size)
            batch.forEach { files.deleteFile(it) }
            val source = SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files))
            source.pruneSnaps("page")
            val protected = archive.sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId }).take(2).map { it.snapId }
            assertEquals(protected, db.snapsDao().getAllSnapsForSiteId("page").map { it.snapId })
            assertEquals((protected + "unrelated").toSet(), present)
            assertEquals(1200, deleted)
            assertNotNull(db.snapsDao().getSnapById("unrelated"))
            source.pruneSnaps("page")
            assertEquals(1200, deleted)
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun sqlBatchesMatchPolicyIncludingTiesAndProtectedPairs() = runBlocking {
        val directory = Files.createTempDirectory("retention-policy").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        try {
            val random = Random(72)
            val archive = (0 until 400).map { index ->
                Snap("snap-${index.toString().padStart(4, '0')}", "site-${index % 6}", random.nextLong(20), "text/html", "UTF-8", random.nextInt(-10, 1000))
            }
            db.withWriteTransaction {
                repeat(6) { db.siteDao().insertSite(Site("Page", "https://example.test", 0L, "", "site-$it", 0 to 0)) }
                archive.forEach { db.snapsDao().insertSnap(it) }
            }
            val policy = SnapshotRetention(versionsPerPage = 7, byteBudget = 8000)
            val expected = policy.obsoleteVersions(archive).map { it.snapId }.toSet()
            val deleted = mutableSetOf<String>()
            while (true) {
                val batch = db.withWriteTransaction {
                    db.snapsDao().retentionBatch(7, 8000, 17).also {
                        assertTrue(it.size <= 17)
                        if (it.isNotEmpty()) db.snapsDao().deleteSnapsByIds(it)
                    }
                }
                if (batch.isEmpty()) break
                assertTrue(deleted.addAll(batch))
            }
            assertEquals(expected, deleted)
            assertEquals(archive.map { it.snapId }.toSet() - expected, db.snapsDao().getSnapsWithChange2().map { it.snapId }.toSet())
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun cancellationReleasesQueuedCheckAndOrphanRecoveryFinishesInterruptedCleanup() = runBlocking {
        val directory = Files.createTempDirectory("retention-cancel").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = BlockingFiles()
        var owner: Job? = null
        try {
            db.withWriteTransaction {
                db.siteDao().insertSite(Site("Page", "https://example.test", 0L, "", "page", 0 to 0))
                repeat(602) {
                    val id = "snap-${it.toString().padStart(4, '0')}"
                    db.snapsDao().insertSnap(Snap(id, "page", it.toLong(), "text/html", "UTF-8", 1))
                    files.present.add(id)
                }
            }
            val maintenance = SnapshotMaintenance(db, db.snapsDao(), files, SnapshotRetention(2, 1000))
            owner = launch(Dispatchers.Default) { maintenance.afterSavedVersion() }
            assertTrue(withContext(Dispatchers.IO) { files.entered.await(5, TimeUnit.SECONDS) })
            val queued = launch(Dispatchers.Default) { maintenance.beforeCheck() }
            queued.cancel()
            withTimeout(2000) { queued.join() }
            owner.cancel()
            files.release.countDown()
            withTimeout(2000) { owner.join() }
            assertEquals(1, files.deletions.get())
            assertEquals(102, db.snapsDao().getSnapsWithChange2().size)
            maintenance.afterSavedVersion()
            assertEquals(setOf("snap-0600", "snap-0601"), files.present.toSet())
            assertEquals(files.present.toSet(), db.snapsDao().getSnapsWithChange2().map { it.snapId }.toSet())
        } finally {
            files.release.countDown()
            owner?.cancelAndJoin()
            db.close()
            directory.deleteRecursively()
        }
    }

    private class BlockingFiles : FileStorage {
        val present = ConcurrentHashMap.newKeySet<String>()
        val deletions = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        override fun orphanCandidates(olderThan: Long) = present.toList()
        override fun writeBytes(fileName: String, bytes: ByteArray) {
            present.add(fileName)
        }
        override fun readBytes(fileName: String) = byteArrayOf()
        override fun fileExists(fileName: String) = fileName in present
        override fun deleteFile(fileName: String): Boolean {
            if (deletions.incrementAndGet() == 1) {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
            return present.remove(fileName)
        }
    }
}
