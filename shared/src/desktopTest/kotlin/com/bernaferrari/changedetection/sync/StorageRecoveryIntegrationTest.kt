package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.util.DesktopFileStorage
import com.bernaferrari.changedetection.error.*
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class StorageRecoveryIntegrationTest {
    @Test fun lowSpacePreservesBaselineAndRetryCommitsExactlyOneChangedVersion() = runBlocking {
        val directory = Files.createTempDirectory("snapshot-write-recovery").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val captures = directory.resolve("snapshots").apply { mkdirs() }
        var available = Long.MAX_VALUE
        val files = DesktopFileStorage(captures) { available }
        val notices = mutableListOf<String>()
        val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        try {
            val site = Site("Schedule", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.TEXT)
            db.siteDao().insertSite(site)
            fun fetched(text: String) = SyncEngine.FetchResult.Success("text/html", "<p>$text</p>".encodeToByteArray())
            val baseline = engine.processFetchResult(site, fetched("Opening 10:00")) as SyncResult.Success
            val baselineId = requireNotNull(baseline.snapId)
            val baselineBytes = files.readBytes(baselineId)
            val healthy = requireNotNull(db.siteDao().getSiteById(site.id))
            assertFalse(baseline.hadChanges)
            assertTrue(notices.isEmpty())
            available = 0
            val failed = engine.processFetchResult(healthy, fetched("Opening 10:30")) as SyncResult.Failed
            assertTrue(failed.exception is SnapshotStorageError)
            assertEquals("Not enough storage", failed.error.userMessage)
            val unhealthy = requireNotNull(db.siteDao().getSiteById(site.id))
            assertFalse(unhealthy.isSuccessful)
            assertEquals(healthy.lastSuccessfulCheck, unhealthy.lastSuccessfulCheck)
            assertEquals(setOf(baselineId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId }.toSet())
            assertEquals(setOf(baselineId), captures.listFiles()!!.map { it.name }.toSet())
            assertArrayEquals(baselineBytes, files.readBytes(baselineId))
            assertTrue(notices.isEmpty())
            available = Long.MAX_VALUE
            val recovered = engine.processFetchResult(unhealthy, fetched("Opening 10:30")) as SyncResult.Success
            assertTrue(recovered.hadChanges)
            assertEquals(listOf(recovered.snapId), notices)
            assertEquals(2, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            val saved = requireNotNull(db.siteDao().getSiteById(site.id))
            assertTrue(saved.isSuccessful)
            assertFalse(saved.isRead)
            val unchanged = engine.processFetchResult(saved, fetched("Opening 10:30")) as SyncResult.Success
            assertFalse(unchanged.hadChanges)
            assertEquals(1, notices.size)
            assertEquals(2, captures.listFiles()!!.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun failedAtomicReplacementPreservesExistingFileAndRemovesTemporaryFile() {
        val directory = Files.createTempDirectory("snapshot-atomic-failure").toFile()
        try {
            val target = directory.resolve("capture").apply { mkdirs() }
            target.resolve("keep").writeText("Existing data")
            val files = DesktopFileStorage(directory) { Long.MAX_VALUE }
            val failure = runCatching { files.writeBytes("capture", "replacement".encodeToByteArray()) }.exceptionOrNull()
            assertTrue(failure is SnapshotStorageError)
            assertFalse((failure as SnapshotStorageError).lowSpace)
            assertEquals("Existing data", target.resolve("keep").readText())
            assertEquals(listOf("capture"), directory.listFiles()!!.map { it.name })
        } finally {
            directory.deleteRecursively()
        }
    }
}
