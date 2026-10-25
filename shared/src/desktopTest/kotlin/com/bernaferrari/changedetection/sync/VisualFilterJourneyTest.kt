package com.bernaferrari.changedetection.sync

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.util.DesktopFileStorage
import io.ktor.client.HttpClient
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Test

class VisualFilterJourneyTest {
    @Test fun aCommittedChangeDoesNotFinishItsOwningCheckBeforeAlertSubmission() = runBlocking {
        val directory = Files.createTempDirectory("awaited-alert").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val notices = mutableListOf<String>()
        val delivery = ChangeAlertDelivery(prepare = { _, _ ->
            entered.complete(Unit)
            release.await()
            null
        }, publish = { _, id, _, _ -> notices.add(id) })
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { site, id -> delivery.deliver(site, id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT)
        try {
            db.siteDao().insertSite(site)
            engine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", coloredPng(0xffffff)))
            val owner = async { engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", coloredPng(0x000000))) }
            entered.await()
            assertEquals(2, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            assertFalse(owner.isCompleted)
            assertTrue(notices.isEmpty())
            release.complete(Unit)
            assertTrue(owner.await() is SyncResult.Success)
            assertEquals(1, notices.size)
        } finally {
            release.complete(Unit)
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun defaultCapturePolicyDistinguishesBrowserRenderingFromTheFetchedImage() = runBlocking {
        val directory = Files.createTempDirectory("default-image-policy").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test/image.png", 0L, "", "page", 0 to 0)
        try {
            db.siteDao().insertSite(site)
            engine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", coloredPng(0xffffff), capturedWithBrowser = true))
            val source = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", coloredPng(0x000000))) as SyncResult.Success
            assertFalse(source.hadChanges)
            assertTrue(notices.isEmpty())
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test(timeout = 20_000)
    fun thresholdAccumulatesAgainstSavedBaselineAndNeverSwallowsComparisonErrors() = runBlocking {
        val directory = Files.createTempDirectory("visual-filter").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("snapshots").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Schedule", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT, visualChangeThresholdBps = 100)
        suspend fun capture(changedPixels: Int, width: Int = 100): SyncResult = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", png(width, changedPixels)))
        try {
            db.siteDao().insertSite(site)
            val first = capture(0) as SyncResult.Success
            assertFalse(first.hadChanges)
            assertTrue(notices.isEmpty())
            val baseline = requireNotNull(db.snapsDao().getLastSnapForSiteId(site.id))
            assertFalse((capture(99) as SyncResult.Success).hadChanges)
            assertEquals(baseline, db.snapsDao().getLastSnapForSiteId(site.id))
            assertTrue(notices.isEmpty())
            assertTrue((capture(100) as SyncResult.Success).hadChanges)
            assertEquals(1, notices.size)
            assertFalse((capture(150) as SyncResult.Success).hadChanges)
            assertTrue((capture(200) as SyncResult.Success).hadChanges)
            assertEquals(2, notices.size)
            // A resize is meaningful even if the filter would otherwise suppress the area.
            assertTrue((capture(200, 101) as SyncResult.Success).hadChanges)
            assertEquals(3, notices.size)
            val saved = requireNotNull(db.snapsDao().getLastSnapForSiteId(site.id))
            val healthy = requireNotNull(db.siteDao().getSiteById(site.id))
            val failure = engine.processFetchResult(healthy, SyncEngine.FetchResult.Success("image/png", byteArrayOf(1, 2, 3)))
            assertTrue(failure is SyncResult.Failed)
            assertEquals(saved, db.snapsDao().getLastSnapForSiteId(site.id))
            assertEquals(healthy.lastSuccessfulCheck, db.siteDao().getSiteById(site.id)?.lastSuccessfulCheck)
            assertArrayEquals(png(101, 200), files.readBytes(saved.snapId))
            assertEquals(3, notices.size)
            val updated = requireNotNull(db.siteDao().getSiteById(site.id)).copy(visualChangeThresholdBps = 500)
            db.siteDao().updateSite(updated)
            assertTrue(engine.processFetchResult(healthy, SyncEngine.FetchResult.Success("image/png", png(101, 1000))) is SyncResult.Skipped)
            assertEquals(saved, db.snapsDao().getLastSnapForSiteId(site.id))
            db.siteDao().updateSite(updated.copy(visualChangeThresholdBps = 0))
            // With filtering disabled, even a tiny visible icon or digit change is retained.
            assertTrue((capture(201, 101) as SyncResult.Success).hadChanges)
            assertEquals(4, notices.size)
            // Detailed regions remain useful for illustrating the saved change.
            assertTrue((engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", png(101, 201, detail = true))) as SyncResult.Success).hadChanges)
            assertEquals(5, notices.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test(timeout = 20_000)
    fun unfilteredDetectionRetainsFlatColorSmallGlyphAndIconChanges() = runBlocking {
        val directory = Files.createTempDirectory("visual-evidence").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        suspend fun capture(site: Site, bytes: ByteArray) = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", bytes))
        val repository = com.bernaferrari.changedetection.repo.source.SnapsRepository(SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, com.bernaferrari.changedetection.storage.SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        val previews = com.bernaferrari.changedetection.preview.ChangePreviewRepository(repository) { db.snapsDao().getPrecedingSnap(it.siteId, it.timestamp, it.snapId) }
        try {
            for ((index, pair) in listOf(
                coloredPng(0x000000) to coloredPng(0xffffff),
                coloredPng(0xff0000) to coloredPng(0x00ff00),
                coloredPng(0xffffff) to coloredPng(0xffffff, patch = true),
                coloredPng(0xffffff) to coloredPng(0xe6e6e6),
            ).withIndex()) {
                val site = Site("Page", "https://example.test/$index", 0L, "", "page-$index", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT)
                db.siteDao().insertSite(site)
                assertFalse((capture(site, pair.first) as SyncResult.Success).hadChanges)
                assertFalse((capture(site, pair.first) as SyncResult.Success).hadChanges)
                assertTrue("Visible evidence $index must survive presentation filtering", (capture(site, pair.second) as SyncResult.Success).hadChanges)
                assertArrayEquals(pair.second, files.readBytes(requireNotNull(db.snapsDao().getLastSnapForSiteId(site.id)).snapId))
                assertTrue("Home/activity must not deny real pixel evidence", previews.latest(site.id) is com.bernaferrari.changedetection.preview.ChangePreview.Crop)
                assertTrue(previews.forVersion(requireNotNull(db.snapsDao().getLastSnapForSiteId(site.id))) is com.bernaferrari.changedetection.preview.ChangePreview.Crop)
            }
            assertEquals(4, notices.size)
            val quiet = Site("Quiet", "https://example.test/noise", 0L, "", "noise", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT)
            db.siteDao().insertSite(quiet)
            capture(quiet, coloredPng(0xffffff))
            assertFalse((capture(quiet, coloredPng(0xfbfbfb)) as SyncResult.Success).hadChanges)
            assertEquals(4, notices.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun switchingToKmpwrightEstablishesASilentBaselineWithoutLosingHistory() = runBlocking {
        val directory = Files.createTempDirectory("browser-renderer-policy").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT)
        suspend fun capture(bytes: ByteArray) = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", bytes, capturedWithBrowser = true)) as SyncResult.Success
        try {
            db.siteDao().insertSite(site)
            val oldBytes = coloredPng(0xff0000)
            val oldHash = "image-v2:${site.capturePolicy().imageIdentity(true)}:${com.bernaferrari.changedetection.util.ContentHasher.hashContent(oldBytes)}"
            val old = Snap(site.id, 1L, "image/png", "", oldBytes.size, oldHash)
            files.writeBytes(old.snapId, oldBytes)
            db.snapsDao().insertSnap(old)
            assertFalse(capture(coloredPng(0x00ff00)).hadChanges)
            assertTrue(notices.isEmpty())
            assertTrue(db.snapsDao().getAllSnapsForSiteId(site.id).any { it.snapId == old.snapId })
            assertTrue(capture(coloredPng(0x0000ff)).hadChanges)
            assertEquals(1, notices.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun sourceImageToScreenshotEstablishesASilentBaselineThenAlertsForContent() = runBlocking {
        val directory = Files.createTempDirectory("image-mode-policy").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val source = Site("Image", "https://example.test/image.png", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SOURCE)
        suspend fun capture(bytes: ByteArray) = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(source.id)), SyncEngine.FetchResult.Success("image/png", bytes)) as SyncResult.Success
        try {
            db.siteDao().insertSite(source)
            assertFalse(capture(coloredPng(0xff0000)).hadChanges)
            db.siteDao().updateSite(requireNotNull(db.siteDao().getSiteById(source.id)).copy(captureMode = CaptureMode.SCREENSHOT))
            assertFalse(capture(coloredPng(0x00ff00)).hadChanges)
            assertTrue(notices.isEmpty())
            assertTrue(capture(coloredPng(0x0000ff)).hadChanges)
            assertEquals(1, notices.size)
            assertEquals(3, db.snapsDao().getAllSnapsForSiteId(source.id).size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun screenshotSelectorEditsRejectStaleCapturesAndEstablishANewBaseline() = runBlocking {
        val directory = Files.createTempDirectory("screenshot-policy").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT, textIncludeSelector = "#price")
        try {
            db.siteDao().insertSite(site)
            engine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", coloredPng(0xff0000)))
            val old = requireNotNull(db.siteDao().getSiteById(site.id))
            val updated = old.copy(textIncludeSelector = "#status", textExcludeSelector = ".clock")
            db.siteDao().updateSite(updated)
            assertTrue(engine.processFetchResult(old, SyncEngine.FetchResult.Success("image/png", coloredPng(0xffffff))) is SyncResult.Skipped)
            assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            val baseline = engine.processFetchResult(updated, SyncEngine.FetchResult.Success("image/png", coloredPng(0x00ff00))) as SyncResult.Success
            assertFalse(baseline.hadChanges)
            assertEquals(2, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            assertTrue(notices.isEmpty())
            val current = requireNotNull(db.siteDao().getSiteById(site.id))
            assertTrue((engine.processFetchResult(current, SyncEngine.FetchResult.Success("image/png", coloredPng(0x0000ff))) as SyncResult.Success).hadChanges)
            assertEquals(1, notices.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    @Test fun aMissingLegacyBaselineRecordsAFailureWithoutStoppingLaterPages() = runBlocking {
        val directory = Files.createTempDirectory("missing-baseline-batch").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = directory.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(directory.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val notices = mutableListOf<String>()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, id -> notices.add(id) })
        val broken = Site("Broken", "https://example.test/broken", 1L, "", "broken", 0 to 0).copy(captureMode = CaptureMode.HTML)
        val healthy = broken.copy(id = "healthy", url = "https://example.test/healthy")
        try {
            db.siteDao().insertSite(broken)
            db.siteDao().insertSite(healthy)
            db.snapsDao().insertSnap(Snap("missing", broken.id, 1L, "text/html", "UTF-8", 20))
            val results = listOf(broken, healthy).map { engine.processFetchResult(it, SyncEngine.FetchResult.Success("text/html", "<p>Available</p>".encodeToByteArray())) }
            assertTrue(results[0] is SyncResult.Failed)
            assertTrue(results[1] is SyncResult.Success)
            assertEquals("SAVED_CONTENT_UNAVAILABLE", db.siteDao().getSiteById(broken.id)?.lastFailureCode)
            assertEquals("missing", db.snapsDao().getLastSnapForSiteId(broken.id)?.snapId)
            assertNotNull(db.snapsDao().getLastSnapForSiteId(healthy.id))
            assertTrue(notices.isEmpty())
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    private fun coloredPng(color: Int, patch: Boolean = false): ByteArray {
        val image = BufferedImage(300, 180, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 180) for (x in 0 until 300) image.setRGB(x, y, color)
        if (patch) for (y in 80 until 87) for (x in 140 until 147) image.setRGB(x, y, 0x000000)
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            image.flush()
            output.toByteArray()
        }
    }

    private fun png(width: Int, changedPixels: Int, detail: Boolean = false): ByteArray {
        val image = BufferedImage(width, 100, BufferedImage.TYPE_INT_RGB)
        return try {
            repeat(changedPixels) { index -> image.setRGB(index % width, index / width, 0xffffff) }
            // A small checkered patch, like a few glyphs of text.
            if (detail) for (y in 40 until 52) for (x in 30 until 60) if ((x + y) % 3 == 0) image.setRGB(x, y, 0xffffff)
            ByteArrayOutputStream().use { output ->
                check(ImageIO.write(image, "png", output))
                output.toByteArray()
            }
        } finally {
            image.flush()
        }
    }
}
