package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.browser.untrustedTlsPage
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.detection.TextRegionException
import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import io.ktor.client.HttpClient
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class CaptureRepeatJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun wholeScreenshotRepeatsChangesAndReturns() = matrix(CaptureMode.SCREENSHOT, false)

    @Test fun selectedScreenshotRepeatsChangesAndReturns() = matrix(CaptureMode.SCREENSHOT, true)

    @Test fun selectedTextRepeatsChangesAndReturns() = matrix(CaptureMode.TEXT, true)

    @Test fun sourceRepeatsChangesAndReturns() = matrix(CaptureMode.SOURCE, false)

    private fun matrix(mode: CaptureMode, selected: Boolean) = untrustedTlsPage().use { tls ->
        val healthy = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body><div id='watched' style='width:280px;height:240px;background:red'>Original stable inventory</div></body></html>"
        TestPageServer(200, healthy).use { server ->
            compose.setContent { Text("Capture matrix") }
            pump {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val name = "matrix-${java.util.UUID.randomUUID()}.db"
                val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
                val files = AndroidFileStorage(context)
                val client = HttpClient()
                val alerts = java.util.concurrent.atomic.AtomicInteger()
                val engine = SyncEngine(
                    client,
                    db,
                    db.siteDao(),
                    db.snapsDao(),
                    files,
                    ErrorHandler(),
                    ChangeNotifier { _, _ ->
                        alerts.incrementAndGet()
                        Unit
                    },
                )
                val site = Site("Matrix ${mode.name}", server.url, 0L, "matrix-${mode.name}", ColorGroup(0, 0), true, false).copy(captureMode = mode, isNotificationEnabled = true, textIncludeSelector = if (selected) "#watched" else "", visualChangeThresholdBps = 100)
                val prefs = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 1_000L)
                val store = object : DataStore<Preferences> {
                    override val data = flowOf(prefs)
                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(prefs)
                }
                val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
                val runner = AndroidCheckRunner(repository, engine, AndroidScreenshotSyncer(engine, store, SyncProgressTracker(), browserFactory = AndroidScreenshotBrowserFactory(context)), store)
                val previewer = AndroidCapturePreviewer(context, engine, store)
                try {
                    db.siteDao().insertSite(site)
                    val region = TextRegion(site.textIncludeSelector)
                    val preview = previewer.capture(server.url, mode, region)
                    assertTrue(previewer.keepAsFirstVersion(site, preview))
                    val changed = healthy.replace("Original stable inventory", "Genuine new inventory").replace("background:red", "background:blue")
                    suspend fun checkSame(expectedAlerts: Int, expectedVersions: Int) {
                        val same = runner.check(site.id)
                        assertTrue(same.toString(), same is SyncResult.Success && !same.hadChanges)
                        assertEquals(expectedAlerts, alerts.get())
                        assertEquals(expectedVersions, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    }
                    repeat(2) { checkSame(0, 1) }
                    server.html = changed
                    val b = runner.check(site.id)
                    assertTrue(b is SyncResult.Success && b.hadChanges)
                    repeat(2) { checkSame(1, 2) } // A -> B -> B must not notify twice.
                    server.html = healthy
                    val a = runner.check(site.id)
                    assertTrue("A genuine return B -> A must remain a change", a is SyncResult.Success && a.hadChanges)
                    checkSame(2, 3)
                    val before = db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId }
                    val baseline = db.snapsDao().getLastSnapForSiteId(site.id)!!
                    val bytes = files.readBytes(baseline.snapId)
                    server.disconnect = true
                    assertTrue(runner.check(site.id) is SyncResult.Failed)
                    assertEquals(before, db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                    assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                    assertEquals(2, alerts.get())
                    server.disconnect = false
                    checkSame(2, 3)
                    if (selected) {
                        server.html = healthy.replace("id='watched'", "id='removed'")
                        assertTrue(runner.check(site.id) is SyncResult.Failed)
                        assertEquals("REGION", db.siteDao().getSiteById(site.id)!!.lastFailureCode)
                        assertTrue(runCatching { previewer.capture(server.url, mode, region) }.exceptionOrNull() is TextRegionException)
                        assertEquals(before, db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                        assertEquals(2, alerts.get())
                        server.html = healthy
                        checkSame(2, 3)
                    }
                    assertNotNull(previewer.capture(server.url, mode, region).firstVersion)
                } finally {
                    db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) }
                    client.close()
                    db.close()
                    context.deleteDatabase(name)
                }
            }
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
