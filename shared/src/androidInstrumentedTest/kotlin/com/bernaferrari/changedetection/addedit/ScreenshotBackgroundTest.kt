package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.browser.untrustedTlsPage
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import io.ktor.client.HttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Screenshot backdrop and immutable legacy-history regressions. */
class ScreenshotBackgroundTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun transparentHealthyRepeatedChecks() = compare(false, false)

    @Test fun whiteHealthyRepeatedChecks() = compare(true, false)

    @Test fun transparentRecoveryAfterTls() = compare(false, true)

    @Test fun whiteRecoveryAfterTls() = compare(true, true)

    @Test fun legacyTransparentHealthyChecks() = compare(true, false, true)

    @Test fun legacyTransparentRecoveryAfterTls() = compare(true, true, true)

    private fun compare(white: Boolean, tlsRecovery: Boolean, legacy: Boolean = false) = untrustedTlsPage().use { tls ->
        val label = (if (legacy) "legacy-" else "") + (if (white) "white" else "transparent") + (if (tlsRecovery) "-recovery" else "-healthy")
        val style = if (white) " style='background:white;color:black'" else ""
        val healthy = "<html><head><meta name='viewport' content='width=device-width'></head><body$style><h1>TLS capture fixture</h1><p>Healthy stable page</p></body></html>"
        TestPageServer(200, healthy).use { server ->
            val live = AtomicReference<LivePage>()
            compose.setContent { ChangeDetectionTheme(false, false) { WatchBrowser(server.url, "", emptyList(), { _, _ -> }, {}, { live.set(it) }, {}, "Watch page") } }
            compose.waitUntil(40_000) {
                runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithText("Watch page").performClick()
            compose.waitUntil(5_000) { live.get() != null }
            val preview = pump { live.get().capturePreview!!.invoke {} }
            val liveCapture = requireNotNull(preview?.firstVersion)
            val bitmap = BitmapFactory.decodeByteArray(liveCapture.data, 0, liveCapture.data.size)
            try {
                assertEquals("Live Screenshot must have an opaque backdrop", 255, android.graphics.Color.alpha(bitmap.getPixel(0, 0)))
            } finally {
                bitmap.recycle()
            }
            val capture = if (legacy) liveCapture.copy(data = legacyTransparent(liveCapture.data)) else liveCapture
            File(context.filesDir, "tls-render-$label-baseline.png").writeBytes(capture.data)
            val name = "render-${java.util.UUID.randomUUID()}.db"
            val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
            val files = AndroidFileStorage(context)
            val client = HttpClient()
            val notifications = java.util.concurrent.atomic.AtomicInteger()
            val engine = SyncEngine(
                client,
                db,
                db.siteDao(),
                db.snapsDao(),
                files,
                ErrorHandler(),
                ChangeNotifier { _, _ ->
                    notifications.incrementAndGet()
                    Unit
                },
            )
            val site = Site(label, server.url, 0L, "render", ColorGroup(0, 0), true, false).copy(captureMode = CaptureMode.SCREENSHOT, isNotificationEnabled = true)
            try {
                pump {
                    db.siteDao().insertSite(site)
                    assertTrue(engine.establishFirstVersion(site, capture) is SyncResult.Success)
                    val baseline = db.snapsDao().getAllSnapsForSiteId(site.id).single()
                    val prefs = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 3_000L)
                    val store = object : DataStore<Preferences> {
                        override val data = flowOf(prefs)
                        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(prefs)
                    }
                    val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
                    val runner = AndroidCheckRunner(repository, engine, AndroidScreenshotSyncer(engine, store, SyncProgressTracker(), browserFactory = AndroidScreenshotBrowserFactory(context)), store)
                    if (tlsRecovery) {
                        // The page sends readers to a host with an untrusted certificate: those checks fail and save nothing.
                        // (An embedded frame with a bad certificate is only left out, so it no longer fails a check.)
                        server.html = healthy.replace("</body>", "<script>location.href='${tls.url}'</script></body>")
                        repeat(2) {
                            val failed = runner.check(site.id)
                            assertTrue(failed.toString(), failed is SyncResult.Failed && failed.exception is javax.net.ssl.SSLException)
                            assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                            assertArrayEquals(capture.data, files.readBytes(baseline.snapId))
                            assertEquals(0, notifications.get())
                        }
                        server.html = healthy
                    }
                    repeat(2) { index ->
                        val result = runner.check(site.id)
                        assertTrue(result.toString(), result is SyncResult.Success)
                        assertFalse("$label must not generate a background-only change", (result as SyncResult.Success).hadChanges)
                        assertEquals(0, notifications.get())
                        assertArrayEquals(capture.data, files.readBytes(baseline.snapId))
                        val snapshots = db.snapsDao().getAllSnapsForSiteId(site.id)
                        val latest = db.snapsDao().getLastSnapForSiteId(site.id)!!
                        val latestBytes = files.readBytes(latest.snapId)
                        File(context.filesDir, "tls-render-$label-check-$index.png").writeBytes(latestBytes)
                        val record = JSONObject().put("scenario", label).put("check", index).put("hadChanges", (result as SyncResult.Success).hadChanges).put("snapshots", snapshots.size).put("notifications", notifications.get()).put("baselinePixel", pixel(capture.data)).put("latestPixel", pixel(latestBytes))
                        synchronized(ScreenshotBackgroundTest::class.java) { File(context.filesDir, "tls-render-comparison.jsonl").appendText(record.toString() + "\n") }
                        println("TLS_RENDER $record")
                    }
                    server.html = healthy.replace("Healthy stable page", "Actual new stock is available").replace("<h1>", "<h1 style='color:red'>")
                    val changed = runner.check(site.id)
                    assertTrue(changed is SyncResult.Success && changed.hadChanges)
                    assertEquals(1, notifications.get())
                    assertArrayEquals(capture.data, files.readBytes(baseline.snapId))
                }
            } finally {
                runBlocking { db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) } }
                client.close()
                db.close()
                context.deleteDatabase(name)
            }
        }
    }
    private fun legacyTransparent(bytes: ByteArray): ByteArray {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val image = source.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
        source.recycle()
        return try {
            val pixels = IntArray(image.width * image.height)
            image.getPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            for (i in pixels.indices) if (pixels[i] == android.graphics.Color.WHITE) pixels[i] = android.graphics.Color.TRANSPARENT
            image.setHasAlpha(true)
            image.setPixels(pixels, 0, image.width, 0, 0, image.width, image.height)
            java.io.ByteArrayOutputStream().use { output ->
                check(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                output.toByteArray()
            }
        } finally {
            image.recycle()
        }
    }
    private fun pixel(bytes: ByteArray): String {
        val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return try {
            "${image.width}x${image.height}:" + Integer.toHexString(image.getPixel(0, 0))
        } finally {
            image.recycle()
        }
    }
    private fun <T> pump(block: suspend () -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val task = scope.async { withTimeout(180_000) { block() } }
        try {
            compose.waitUntil(190_000) { task.isCompleted }
            return runBlocking { task.await() }
        } finally {
            scope.cancel()
        }
    }
}
