package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** The 90 second limit can fire after a check has already committed its result; that result must stand. */
class TimeoutAfterSavedResultTest {
    @Test fun aTimeoutAfterTheCheckSavedItsResultDoesNotMarkThePageFailed() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            // The check commits a version; fixture.site is still the row as it was when the check began.
            assertTrue(fixture.engine.processFetchResult(fixture.site, fixture.capture("Saved")) is SyncResult.Success)

            val result = fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout"))

            assertTrue("a stale timeout is skipped, got $result", result is SyncResult.Skipped)
            val stored = requireNotNull(fixture.db.siteDao().getSiteById(fixture.site.id))
            assertTrue("the saved result must stay successful", stored.isSuccessful)
            assertNull(stored.lastFailureCode)
            assertEquals(1, fixture.db.snapsDao().getAllSnapsForSiteId(fixture.site.id).size)
        }
    }

    @Test fun aTimeoutWithNoResultMarksThePageFailed() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)

            val result = fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout"))

            assertTrue("a real timeout is a failure, got $result", result is SyncResult.Failed)
            val stored = requireNotNull(fixture.db.siteDao().getSiteById(fixture.site.id))
            assertFalse(stored.isSuccessful)
            assertNotNull(stored.lastFailureCode)
        }
    }

    @Test fun aTimeoutForADeletedPageIsSkipped() = runBlocking {
        Fixture().use { fixture ->
            assertTrue(fixture.engine.recordTimeout(fixture.site, RuntimeException("timeout")) is SyncResult.Skipped)
        }
    }

    @Test fun latestQueriesChooseOneSnapshotWhenTimestampsTie() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            val dao = fixture.db.snapsDao()
            val earlierId = com.bernaferrari.changedetection.repo.Snap("a", fixture.site.id, 42L, "text/plain", "UTF-8", 1)
            val laterId = earlierId.copy(snapId = "z")
            dao.insertSnap(laterId)
            dao.insertSnap(earlierId)

            assertEquals(listOf(laterId.snapId), dao.getAllMostRecentSnaps().map { it.snapId })
            assertEquals(listOf(laterId.snapId), dao.getLatestSnapsFromSitesWithChangesList().map { it.snapId })
        }
    }

    private class Fixture : AutoCloseable {
        private val directory = Files.createTempDirectory("timeout-after-saved-result").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        private val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler())
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        fun capture(text: String) = SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())
        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }
}
