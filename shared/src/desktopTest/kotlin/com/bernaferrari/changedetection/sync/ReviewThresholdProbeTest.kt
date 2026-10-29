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

class ReviewThresholdProbeTest {
    private fun png(width: Int, height: Int, pixels: IntArray): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        return ByteArrayOutputStream().use { ImageIO.write(image, "png", it); image.flush(); it.toByteArray() }
    }
    private fun checkPair(before: ByteArray, after: ByteArray, expectedChange: Boolean = false) = runBlocking {
        val dir = Files.createTempDirectory("review-threshold").toFile()
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(name = dir.resolve("test.db").path), BundledSQLiteDriver())
        val files = DesktopFileStorage(dir.resolve("files").also { it.mkdirs() })
        val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, _ -> })
        val site = Site("Page", "https://example.test", 0L, "", "page", 0 to 0).copy(captureMode = CaptureMode.SCREENSHOT, visualChangeThresholdBps = 100)
        try {
            db.siteDao().insertSite(site)
            engine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", before, capturedWithBrowser = true))
            val result = engine.processFetchResult(requireNotNull(db.siteDao().getSiteById(site.id)), SyncEngine.FetchResult.Success("image/png", after, capturedWithBrowser = true)) as SyncResult.Success
            assertEquals("1% filter decision", expectedChange, result.hadChanges)
        } finally { client.close(); db.close(); dir.deleteRecursively() }
    }
    @Test fun blankPageWithOneExtraRowShouldNotAlert() {
        checkPair(png(100, 100, IntArray(10000) { 0xffffff }), png(100, 101, IntArray(10100) { 0xffffff }))
    }
    @Test fun blankWidthChangeObeysThreshold() {
        checkPair(png(100, 100, IntArray(10000) { 0xffffff }), png(101, 100, IntArray(10100) { 0xffffff }))
    }
    @Test fun resizedPageWithSubstantialContentChangeAlerts() {
        checkPair(png(100, 100, IntArray(10000) { 0xffffff }), png(101, 101, IntArray(10201) { 0x202020 }), expectedChange = true)
    }
    @Test fun shiftedPageWithLessThanOnePercentContentEditShouldNotAlert() {
        val width = 120
        val height = 800
        val before = IntArray(width * height) { i ->
            val y = i / width; val x = i % width; val id = y / 10 + 1
            if (y % 10 < 6 && (x + id * 7 + y % 10) % (id % 5 + 3) == 0) 0x202020 else 0xffffff
        }
        val after = IntArray(width * 3) { 0xffffff } + before.copyOf(width * (height - 3))
        for (y in 34..38) for (x in 60..67) after[y * width + x] = 0x202020
        checkPair(png(width, height, before), png(width, height, after))
    }
}
