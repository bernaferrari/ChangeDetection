/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
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
import org.junit.Assert.*
import org.junit.Test

class VisualFilterJourneyTest {
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
            assertTrue((capture(201, 101) as SyncResult.Success).hadChanges)
            assertEquals(4, notices.size)
        } finally {
            client.close()
            db.close()
            directory.deleteRecursively()
        }
    }

    private fun png(width: Int, changedPixels: Int): ByteArray {
        val image = BufferedImage(width, 100, BufferedImage.TYPE_INT_RGB)
        return try {
            repeat(changedPixels) { index -> image.setRGB(index % width, index / width, 0xffffff) }
            ByteArrayOutputStream().use { output ->
                check(ImageIO.write(image, "png", output))
                output.toByteArray()
            }
        } finally {
            image.flush()
        }
    }
}
