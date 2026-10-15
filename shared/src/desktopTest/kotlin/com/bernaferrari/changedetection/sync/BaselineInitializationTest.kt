package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.addedit.CapturePreview
import com.bernaferrari.changedetection.addedit.HtmlCapturePreviewer
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.getRoomDatabase
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.nio.file.Files
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BaselineInitializationTest {
    @Test fun aDelayedPreviewCannotReplaceANewerBaseline() = runBlocking {
        Fixture().use { fixture ->
            val site = fixture.site
            fixture.db.siteDao().insertSite(site)
            val release = CompletableDeferred<Unit>()
            val oldPreview = async {
                release.await()
                HtmlCapturePreviewer(fixture.engine).keepAsFirstVersion(site, fixture.preview("Old preview"))
            }
            fixture.engine.processFetchResult(site, fixture.capture("Fresh capture"))
            release.complete(Unit)
            assertTrue("An existing baseline completes initialization without scheduling another check", oldPreview.await())
            val history = fixture.db.snapsDao().getAllSnapsForSiteId(site.id)
            assertEquals("The late preview must never become a second version", 1, history.size)
            assertEquals("Fresh capture", fixture.files.readBytes(history.single().snapId).decodeToString())
            assertTrue(fixture.notices.isEmpty())
        }
    }

    @Test fun aPreviewInitializesOnceAndLaterRealChangesStillAlert() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            assertTrue(HtmlCapturePreviewer(fixture.engine).keepAsFirstVersion(fixture.site, fixture.preview("Baseline")))
            assertEquals(1, fixture.db.snapsDao().getAllSnapsForSiteId(fixture.site.id).size)
            assertTrue(fixture.notices.isEmpty())
            val changed = fixture.engine.processFetchResult(fixture.site, fixture.capture("Changed")) as SyncResult.Success
            assertTrue(changed.hadChanges)
            assertEquals(1, fixture.notices.size)
        }
    }

    @Test fun aBackwardClockCorrectionDoesNotRepeatChangesOrRewindTheBaseline() = runBlocking {
        Fixture().use { fixture ->
            fixture.db.siteDao().insertSite(fixture.site)
            fixture.engine.processFetchResult(fixture.site, fixture.capture("Original"))
            val old = requireNotNull(fixture.db.snapsDao().getLastSnapForSiteId(fixture.site.id))
            val future = kotlin.time.Clock.System.now().toEpochMilliseconds() + 86_400_000
            fixture.db.snapsDao().deleteSnapById(old.snapId)
            fixture.db.snapsDao().insertSnap(old.copy(timestamp = future))
            assertTrue((fixture.engine.processFetchResult(fixture.site, fixture.capture("Changed")) as SyncResult.Success).hadChanges)
            val checked = requireNotNull(fixture.db.siteDao().getSiteById(fixture.site.id))
            assertTrue("Scheduling must use the corrected clock, not the monotonic history order", checked.timestamp < future)
            assertTrue(requireNotNull(checked.lastSuccessfulCheck) < future)
            repeat(3) {
                assertFalse("An unchanged capture must use the newest saved content even after clock correction", (fixture.engine.processFetchResult(fixture.site, fixture.capture("Changed")) as SyncResult.Success).hadChanges)
            }
            val history = fixture.db.snapsDao().getAllSnapsForSiteId(fixture.site.id)
            assertEquals(2, history.size)
            assertTrue(history.first().timestamp > future)
            assertEquals("Changed", fixture.files.readBytes(history.first().snapId).decodeToString())
            assertEquals(1, fixture.notices.size)
        }
    }

    private class Fixture : AutoCloseable {
        private val directory = Files.createTempDirectory("baseline-initialization").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        private val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        fun capture(text: String) = SyncEngine.FetchResult.Success("text/plain", text.encodeToByteArray())
        fun preview(text: String) = CapturePreview.Html(text, text.length, firstVersion = capture(text))
        override fun close() {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }
}
