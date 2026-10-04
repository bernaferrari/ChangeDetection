/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.withWriteTransaction
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the current KMP Room schema through Android's real SQLite implementation. */
@RunWith(AndroidJUnit4::class)
class SnapsDaoTest {
    private lateinit var database: ChangeDatabase
    private lateinit var snapsDao: SnapsDao
    private lateinit var sitesDao: SitesDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = getRoomDatabase(
            Room.inMemoryDatabaseBuilder<ChangeDatabase>(context = context),
            BundledSQLiteDriver(),
        )
        snapsDao = database.snapsDao()
        sitesDao = database.siteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun watchlistIncludesEmptyPagesAndUsesTheSameLatestIdentityAsHistory() = runBlocking {
        sitesDao.insertSite(site("empty"))
        sitesDao.insertSite(site("history"))
        snapsDao.insertSnap(snap("z", "history", 100))
        snapsDao.insertSnap(snap("a", "history", 100))
        val rows = sitesDao.observeWatchlist().first().associateBy { it.site.id }
        assertEquals(2, rows.size)
        assertEquals(null, rows.getValue("empty").snap)
        assertEquals(0, rows.getValue("empty").snapshotCount)
        assertEquals(snapsDao.getLastSnapForSiteId("history"), rows.getValue("history").snap)
        assertEquals("z", rows.getValue("history").snap?.snapId)
        assertEquals(2, rows.getValue("history").snapshotCount)
    }

    @Test
    fun watchlistInvalidatesForHistoryAndStatusAndNeverMixesCommittedVersions() = runBlocking {
        sitesDao.insertSite(site("page").copy(timestamp = 0))
        val changes = Channel<List<WatchlistRow>>(Channel.UNLIMITED)
        val observer = launch { sitesDao.observeWatchlist().collect { changes.send(it) } }
        try {
            withTimeout(10_000) {
                assertEquals(0, changes.receive().single().snapshotCount)
                repeat(40) { index ->
                    val version = index + 1L
                    database.withWriteTransaction {
                        snapsDao.insertSnap(snap("version-$version", "page", version))
                        sitesDao.updateSyncMetadata("page", version, true, false, false)
                    }
                }
                do {
                    val row = changes.receive().single()
                    assertEquals(row.site.timestamp, row.snap?.timestamp)
                    assertEquals(row.site.timestamp.toInt(), row.snapshotCount)
                } while (row.site.timestamp != 40L)
                sitesDao.updateMonitoringOptions("page", false, false)
                var row: WatchlistRow
                do {
                    row = changes.receive().single()
                } while (row.site.isSyncEnabled)
                assertEquals(40, row.snapshotCount)
                snapsDao.deleteAllSnapsForSite("page")
                do {
                    row = changes.receive().single()
                } while (row.snapshotCount != 0)
                assertEquals(null, row.snap)
                sitesDao.deleteSiteById("page")
                while (changes.receive().isNotEmpty()) { }
            }
        } finally {
            observer.cancel()
            changes.cancel()
        }
    }

    @Test
    fun explicitDetectionRulesPersistWithoutRemovingHistory() = runBlocking {
        sitesDao.insertSite(site("garden"))
        snapsDao.insertSnap(snap("baseline", "garden", 1))
        listOf(com.bernaferrari.changedetection.repo.CaptureMode.TEXT, com.bernaferrari.changedetection.repo.CaptureMode.SOURCE).forEach { mode ->
            sitesDao.savePreservingStatus(requireNotNull(sitesDao.getSiteById("garden")).copy(captureMode = mode))
            assertEquals(mode, sitesDao.getSiteById("garden")?.captureMode)
            assertEquals(listOf("baseline"), snapsDao.getAllSnapsForSiteId("garden").map { it.snapId })
        }
    }

    @Test
    fun cleanupPreservesLatestPairEvenWhenContentRepeatsAndIsPageScoped() = runBlocking {
        sitesDao.insertSite(site("garden"))
        sitesDao.insertSite(site("other"))
        listOf("a", "b", "c", "d").forEachIndexed { index, id ->
            snapsDao.insertSnap(snap(id, "garden", index.toLong()).copy(contentHash = "repeated"))
        }
        snapsDao.insertSnap(snap("unrelated", "other", 0))
        assertEquals(setOf("a", "b"), snapsDao.removeOlderVersionsBatch("garden").toSet())
        assertEquals(listOf("d", "c"), snapsDao.getAllSnapsForSiteId("garden").map { it.snapId })
        assertEquals(1, snapsDao.getAllSnapsForSiteId("other").size)
        assertTrue(snapsDao.removeOlderVersionsBatch("garden").isEmpty())
        assertTrue(snapsDao.removeOlderVersionsBatch("other").isEmpty())
    }

    @Test
    fun versionThreeUpgradePreservesHistoryAndUsesTheExistingDefault() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "capture-migration-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name).absolutePath
        fun open() = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, path), BundledSQLiteDriver())
        try {
            val original = open()
            try {
                original.siteDao().insertSite(site("garden"))
                original.snapsDao().insertSnap(snap("old-version", "garden", 1_000))
            } finally {
                original.close()
            }
            // Reconstruct schema 3 by removing columns and cleanup objects added later.
            BundledSQLiteDriver().open(path).use { connection ->
                connection.prepare("DROP TRIGGER queue_deleted_snapshot").use { it.step() }
                connection.prepare("DROP TABLE pendingFileDeletions").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN visualChangeThresholdBps").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textIncludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textExcludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN lastFailureCode").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN lastSuccessfulCheck").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN captureMode").use { it.step() }
                connection.prepare("PRAGMA user_version = 3").use { it.step() }
            }
            val migrated = open()
            try {
                val restored = requireNotNull(migrated.siteDao().getSiteById("garden"))
                assertEquals("Synthetic garden", restored.title)
                assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.DEFAULT, restored.captureMode)
                assertEquals(listOf("old-version"), migrated.snapsDao().getAllSnapsForSiteId("garden").map { it.snapId })
                migrated.siteDao().savePreservingStatus(restored.copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.HTML))
                assertEquals(com.bernaferrari.changedetection.repo.CaptureMode.HTML, migrated.siteDao().getSiteById("garden")?.captureMode)
                assertEquals(1, migrated.snapsDao().getAllSnapsForSiteId("garden").size)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun latestSnapshotsReturnsOneNewestSnapshotPerSite() = runBlocking {
        sitesDao.insertSite(site("garden"))
        sitesDao.insertSite(site("library"))
        snapsDao.insertSnap(snap("garden-old", "garden", 1_000))
        snapsDao.insertSnap(snap("garden-new", "garden", 3_000))
        snapsDao.insertSnap(snap("library-only", "library", 2_000))

        val latest = snapsDao.getAllMostRecentSnaps()

        assertEquals(listOf("garden-new", "library-only"), latest.map(Snap::snapId))
    }

    @Test
    fun latestSnapshotsIsEmptyForAnEmptyDatabase() = runBlocking {
        assertTrue(snapsDao.getAllMostRecentSnaps().isEmpty())
    }

    @Test
    fun failedSnapshotCommitRollsBackBothRowsAndRemovesItsFile() = runBlocking {
        val monitor = site("garden")
        sitesDao.insertSite(monitor)
        val files = mutableMapOf<String, ByteArray>()
        val storage = object : com.bernaferrari.changedetection.util.FileStorage {
            override fun writeBytes(fileName: String, bytes: ByteArray) {
                files[fileName] = bytes
            }
            override fun readBytes(fileName: String) = files.getValue(fileName)
            override fun deleteFile(fileName: String) = files.remove(fileName) != null
            override fun fileExists(fileName: String) = files.containsKey(fileName)
        }
        val client = io.ktor.client.HttpClient()
        try {
            val engine = object : com.bernaferrari.changedetection.sync.SyncEngine(
                client,
                database,
                sitesDao,
                snapsDao,
                storage,
                com.bernaferrari.changedetection.error.ErrorHandler(),
            ) {
                override suspend fun <T> inWriteTransaction(block: suspend () -> T): T = database.withWriteTransaction {
                    block()
                    // Fail after both SQL writes, before Room commits.
                    throw IllegalStateException("Simulated commit failure")
                }
            }

            val result = engine.processFetchResult(
                monitor,
                com.bernaferrari.changedetection.sync.SyncEngine.FetchResult.Success("text/html", "baseline".toByteArray()),
            )

            assertTrue(result is com.bernaferrari.changedetection.sync.SyncResult.Failed)
            assertTrue(snapsDao.getAllSnapsForSiteId(monitor.id).isEmpty())
            val restored = requireNotNull(sitesDao.getSiteById(monitor.id))
            assertEquals(monitor.timestamp, restored.timestamp)
            assertEquals(monitor.isSuccessful, restored.isSuccessful)
            assertEquals(monitor.isRead, restored.isRead)
            assertEquals(monitor.isBrowser, restored.isBrowser)
            assertTrue(files.isEmpty())
        } finally {
            client.close()
        }
    }

    @Test
    fun savingRenamedMonitorPreservesItsHistory() = runBlocking {
        val monitor = site("garden")
        sitesDao.insertSite(monitor)
        snapsDao.insertSnap(snap("before", monitor.id, 1_000))
        snapsDao.insertSnap(snap("after", monitor.id, 2_000))

        sitesDao.insertSite(monitor.copy(title = "Renamed garden"))

        assertEquals("Renamed garden", sitesDao.getSiteById(monitor.id)?.title)
        assertEquals(listOf("after", "before"), snapsDao.getAllSnapsForSiteId(monitor.id).map(Snap::snapId))
    }

    @Test
    fun unknownLegacyHashesNeverEstablishDuplicateContent() = runBlocking {
        sitesDao.insertSite(site("garden"))
        snapsDao.insertSnap(snap("one", "garden", 1_000))
        snapsDao.insertSnap(snap("two", "garden", 2_000))
        snapsDao.insertSnap(snap("three", "garden", 3_000))

        assertEquals(3, snapsDao.getUniqueContentSnaps("garden").size)
    }

    @Test
    fun freshStartCommitsTheEditedPageAndQueuesOnlyItsObsoleteFiles() = runBlocking {
        val monitor = site("garden")
        sitesDao.insertSite(monitor)
        sitesDao.insertSite(site("library"))
        snapsDao.insertSnap(snap("old", "garden", 1_000))
        snapsDao.insertSnap(snap("keep", "library", 1_000))

        sitesDao.saveWithHistoryReset(monitor.copy(url = "https://garden.example/new"))

        assertEquals(listOf("old"), database.pendingFileDeletionDao().batch("", "old"))
        assertEquals("https://garden.example/new", sitesDao.getSiteById("garden")?.url)
        assertTrue(snapsDao.getAllSnapsForSiteId("garden").isEmpty())
        assertEquals(listOf("keep"), snapsDao.getAllSnapsForSiteId("library").map(Snap::snapId))
    }

    @Test
    fun lastSuccessfulCheckSurvivesFailureAndStaleFormThenResetsWithHistory() = runBlocking {
        val form = site("garden")
        sitesDao.insertSite(form)
        assertEquals(null, sitesDao.getSiteById("garden")?.lastSuccessfulCheck)
        sitesDao.updateSyncMetadata("garden", 100L, true, false, true)
        sitesDao.updateSyncMetadata("garden", 200L, false, false, null, "NOT_FOUND")
        sitesDao.savePreservingStatus(form.copy(title = "Renamed"))
        val failed = requireNotNull(sitesDao.getSiteById("garden"))
        assertEquals("NOT_FOUND", failed.lastFailureCode)
        assertEquals(200L, failed.timestamp)
        assertEquals(100L, failed.lastSuccessfulCheck)
        sitesDao.updateSyncMetadata("garden", 300L, true, false, null)
        assertEquals(null, sitesDao.getSiteById("garden")?.lastFailureCode)
        assertEquals(300L, sitesDao.getSiteById("garden")?.lastSuccessfulCheck)
        sitesDao.saveWithHistoryReset(requireNotNull(sitesDao.getSiteById("garden")))
        assertEquals(null, sitesDao.getSiteById("garden")?.lastSuccessfulCheck)
        assertEquals(0L, sitesDao.getSiteById("garden")?.timestamp)
    }

    @Test
    fun versionSixUpgradePreservesHistoryAndDefaultsToWholePage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath("region-migration-${java.util.UUID.randomUUID()}.db").absolutePath
        fun open() = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, path), BundledSQLiteDriver())
        try {
            val original = open()
            original.siteDao().insertSite(site("garden").copy(lastSuccessfulCheck = 100, lastFailureCode = "TIMEOUT", isRead = false))
            original.snapsDao().insertSnap(snap("saved", "garden", 100))
            original.close()
            BundledSQLiteDriver().open(path).use { connection ->
                connection.prepare("DROP TRIGGER queue_deleted_snapshot").use { it.step() }
                connection.prepare("DROP TABLE pendingFileDeletions").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN visualChangeThresholdBps").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textIncludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textExcludeSelector").use { it.step() }
                connection.prepare("PRAGMA user_version = 6").use { it.step() }
            }
            val migrated = open()
            try {
                val restored = requireNotNull(migrated.siteDao().getSiteById("garden"))
                assertEquals("", restored.textIncludeSelector)
                assertEquals("", restored.textExcludeSelector)
                assertEquals(100L, restored.lastSuccessfulCheck)
                assertEquals("TIMEOUT", restored.lastFailureCode)
                assertEquals(false, restored.isRead)
                migrated.siteDao().savePreservingStatus(restored.copy(textIncludeSelector = "#price", textExcludeSelector = ".timer"))
                assertEquals("#price", migrated.siteDao().getSiteById("garden")?.textIncludeSelector)
                assertEquals(".timer", migrated.siteDao().getSiteById("garden")?.textExcludeSelector)
                assertEquals(listOf("saved"), migrated.snapsDao().getAllSnapsForSiteId("garden").map { it.snapId })
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(path)
        }
    }

    @Test
    fun versionFiveUpgradeKeepsHistoryAndDoesNotInventFailureReasons() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath("failure-migration-${java.util.UUID.randomUUID()}.db").absolutePath
        fun open() = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, path), BundledSQLiteDriver())
        try {
            val original = open()
            try {
                original.siteDao().insertSite(site("garden").copy(timestamp = 200, lastSuccessfulCheck = 100, isSuccessful = false, isRead = false))
                original.snapsDao().insertSnap(snap("saved", "garden", 100))
            } finally {
                original.close()
            }
            BundledSQLiteDriver().open(path).use { connection ->
                connection.prepare("DROP TRIGGER queue_deleted_snapshot").use { it.step() }
                connection.prepare("DROP TABLE pendingFileDeletions").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN visualChangeThresholdBps").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textIncludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textExcludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN lastFailureCode").use { it.step() }
                connection.prepare("PRAGMA user_version = 5").use { it.step() }
            }
            val migrated = open()
            try {
                val restored = requireNotNull(migrated.siteDao().getSiteById("garden"))
                assertEquals(null, restored.lastFailureCode)
                assertEquals(100L, restored.lastSuccessfulCheck)
                assertEquals(false, restored.isSuccessful)
                assertEquals(false, restored.isRead)
                assertEquals(listOf("saved"), migrated.snapsDao().getAllSnapsForSiteId("garden").map { it.snapId })
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(path)
        }
    }

    @Test
    fun versionFourUpgradeBackfillsOnlyKnownSuccessfulChecks() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val path = context.getDatabasePath("health-migration-${java.util.UUID.randomUUID()}.db").absolutePath
        fun open() = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, path), BundledSQLiteDriver())
        try {
            val original = open()
            try {
                original.siteDao().insertSite(site("success").copy(timestamp = 100, isSuccessful = true))
                original.siteDao().insertSite(site("failure").copy(timestamp = 200, isSuccessful = false))
                original.siteDao().insertSite(site("unobserved").copy(timestamp = 0, isSuccessful = true))
                original.snapsDao().insertSnap(snap("saved", "failure", 50))
            } finally {
                original.close()
            }
            BundledSQLiteDriver().open(path).use { connection ->
                connection.prepare("DROP TRIGGER queue_deleted_snapshot").use { it.step() }
                connection.prepare("DROP TABLE pendingFileDeletions").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN visualChangeThresholdBps").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textIncludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN textExcludeSelector").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN lastFailureCode").use { it.step() }
                connection.prepare("ALTER TABLE sites DROP COLUMN lastSuccessfulCheck").use { it.step() }
                connection.prepare("PRAGMA user_version = 4").use { it.step() }
            }
            val migrated = open()
            try {
                assertEquals(100L, migrated.siteDao().getSiteById("success")?.lastSuccessfulCheck)
                assertEquals(null, migrated.siteDao().getSiteById("failure")?.lastSuccessfulCheck)
                assertEquals(null, migrated.siteDao().getSiteById("unobserved")?.lastSuccessfulCheck)
                assertEquals(listOf("saved"), migrated.snapsDao().getAllSnapsForSiteId("failure").map { it.snapId })
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(path)
        }
    }

    @Test
    fun openingAnOlderSnapshotDoesNotAcknowledgeANewerChange() = runBlocking {
        sitesDao.insertSite(site("garden").copy(isRead = false))
        snapsDao.insertSnap(snap("old", "garden", 1))
        snapsDao.insertSnap(snap("new", "garden", 2))
        assertEquals(0, sitesDao.markSiteRead("garden", "old"))
        assertEquals(false, sitesDao.getSiteById("garden")?.isRead)
        assertEquals(0, sitesDao.markSiteRead("garden", "missing"))
        assertEquals(1, sitesDao.markSiteRead("garden", "new"))
        assertEquals(true, sitesDao.getSiteById("garden")?.isRead)
    }

    @Test
    fun unchangedCheckDoesNotUndoAConcurrentReadAction() = runBlocking {
        sitesDao.insertSite(site("garden").copy(isRead = false))
        snapsDao.insertSnap(snap("reviewed", "garden", 1))
        sitesDao.markSiteRead("garden", "reviewed")

        sitesDao.updateSyncMetadata("garden", 2_000, true, false, null)

        assertEquals(true, sitesDao.getSiteById("garden")?.isRead)
    }

    @Test
    fun savingAnOpenFormPreservesACompletedCheck() = runBlocking {
        val form = site("garden").copy(timestamp = 100L, isRead = false)
        sitesDao.insertSite(form)
        sitesDao.updateSyncMetadata("garden", 200L, false, true, null)
        snapsDao.insertSnap(snap("reviewed", "garden", 1))
        sitesDao.markSiteRead("garden", "reviewed")
        sitesDao.savePreservingStatus(form.copy(title = "Edited while checking"))
        val saved = requireNotNull(sitesDao.getSiteById("garden"))
        assertEquals("Edited while checking", saved.title)
        assertEquals(200L, saved.timestamp)
        assertEquals(false, saved.isSuccessful)
        assertEquals(true, saved.isRead)
        assertEquals(true, saved.isBrowser)
    }

    @Test
    fun togglingMonitoringPreservesCheckResults() = runBlocking {
        sitesDao.insertSite(site("garden"))
        sitesDao.updateSyncMetadata("garden", 300L, false, true, false)
        sitesDao.updateMonitoringOptions("garden", false, false)
        val saved = requireNotNull(sitesDao.getSiteById("garden"))
        assertEquals(300L, saved.timestamp)
        assertEquals(false, saved.isSuccessful)
        assertEquals(false, saved.isRead)
        assertEquals(false, saved.isSyncEnabled)
        assertEquals(false, saved.isNotificationEnabled)
    }

    @Test
    fun retentionDeletesObsoleteFilesOnlyAfterTheirRows() = runBlocking {
        sitesDao.insertSite(site("garden"))
        (1..4).forEach { snapsDao.insertSnap(snap("version-$it", "garden", it.toLong())) }
        val stored = mutableSetOf("version-1", "version-2", "version-3", "version-4", "old-orphan")
        val files = object : com.bernaferrari.changedetection.util.FileStorage {
            override fun writeBytes(fileName: String, bytes: ByteArray) = Unit
            override fun readBytes(fileName: String) = byteArrayOf()
            override fun fileExists(fileName: String) = fileName in stored
            override fun deleteFile(fileName: String) = stored.remove(fileName)
            override fun orphanCandidates(olderThan: Long) = stored.toList()
        }
        com.bernaferrari.changedetection.storage.SnapshotMaintenance(
            database,
            snapsDao,
            files,
            com.bernaferrari.changedetection.storage.SnapshotRetention(versionsPerPage = 2),
        ).afterSavedVersion()
        assertEquals(setOf("version-3", "version-4"), stored)
        assertEquals(stored, snapsDao.getAllSnapsForSiteId("garden").map { it.snapId }.toSet())
    }

    @Test
    fun staleOrphansCanBeReclaimedBeforeTheFirstSuccessfulWrite() = runBlocking {
        val monitor = site("garden")
        sitesDao.insertSite(monitor)
        val stored = mutableSetOf("stale-partial")
        val files = object : com.bernaferrari.changedetection.util.FileStorage {
            override fun writeBytes(fileName: String, bytes: ByteArray) {
                if ("stale-partial" in stored) throw com.bernaferrari.changedetection.error.SnapshotStorageError(lowSpace = true)
                stored.add(fileName)
            }
            override fun readBytes(fileName: String) = byteArrayOf()
            override fun fileExists(fileName: String) = fileName in stored
            override fun deleteFile(fileName: String) = stored.remove(fileName)
            override fun orphanCandidates(olderThan: Long) = listOf("stale-partial").filter { it in stored }
        }
        val client = io.ktor.client.HttpClient()
        try {
            val engine = com.bernaferrari.changedetection.sync.SyncEngine(
                client,
                database,
                sitesDao,
                snapsDao,
                files,
                com.bernaferrari.changedetection.error.ErrorHandler(),
                maintenance = com.bernaferrari.changedetection.storage.SnapshotMaintenance(database, snapsDao, files),
            )
            val result = engine.processFetchResult(monitor, com.bernaferrari.changedetection.sync.SyncEngine.FetchResult.Success("text/html", "baseline".toByteArray()))
            assertTrue("Cleanup must not depend on a successful snapshot write", result is com.bernaferrari.changedetection.sync.SyncResult.Success)
            assertEquals(snapsDao.getAllSnapsForSiteId(monitor.id).map { it.snapId }.toSet(), stored)
            assertEquals(1, stored.size)
        } finally {
            client.close()
        }
    }

    private fun site(id: String) = Site(
        title = "Synthetic $id",
        url = "https://$id.example",
        timestamp = 0,
        id = id,
        isSuccessful = true,
        isBrowser = false,
        threshold = 0,
        isRead = false,
        isSyncEnabled = true,
        isNotificationEnabled = true,
        notes = "",
        colors = 0xFF00897B.toInt() to 0xFF26A69A.toInt(),
        constraints = ConstraintsRequired(false, false, false, false),
    )

    private fun snap(id: String, siteId: String, timestamp: Long) = Snap(
        snapId = id,
        siteId = siteId,
        timestamp = timestamp,
        contentType = "text/html",
        contentCharset = "UTF-8",
        contentSize = 1,
    )
}
