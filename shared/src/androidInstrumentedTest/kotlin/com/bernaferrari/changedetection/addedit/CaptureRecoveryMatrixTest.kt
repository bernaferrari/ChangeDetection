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

class CaptureRecoveryMatrixTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun textSetupEditFailureCancellationAndRecovery() = matrix(CaptureMode.TEXT)

    @Test fun codeSetupEditFailureCancellationAndRecovery() = matrix(CaptureMode.SOURCE)

    @Test fun screenshotSetupEditFailureCancellationAndRecovery() = matrix(CaptureMode.SCREENSHOT)

    private fun matrix(mode: CaptureMode) = untrustedTlsPage().use { tls ->
        val healthy = "<html><body><h1>Capture matrix</h1><p id='watched'>Original stable inventory</p></body></html>"
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
                val site = Site("Matrix ${mode.name}", server.url, 0L, "matrix-${mode.name}", ColorGroup(0, 0), true, false).copy(captureMode = mode, isNotificationEnabled = true)
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
                    val preview = previewer.capture(server.url, mode)
                    assertTrue(db.snapsDao().getAllSnapsForSiteId(site.id).isEmpty())
                    assertTrue(previewer.keepAsFirstVersion(site, preview))
                    repeat(2) {
                        val same = runner.check(site.id)
                        assertTrue(same is SyncResult.Success && !same.hadChanges)
                    }
                    assertEquals(0, alerts.get())
                    assertNotNull(previewer.capture(server.url, mode).firstVersion)
                    assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    server.html = healthy.replace("Original stable inventory", "A real new stock arrival is available")
                    val changed = runner.check(site.id)
                    assertTrue(changed is SyncResult.Success && changed.hadChanges)
                    assertEquals(1, alerts.get())
                    val baseline = db.snapsDao().getLastSnapForSiteId(site.id)!!
                    val bytes = files.readBytes(baseline.snapId)
                    val history = db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId }
                    suspend fun preserved() {
                        assertEquals(history, db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                        assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                        assertEquals(1, alerts.get())
                    }
                    server.status = 302
                    server.redirectTo = tls.url
                    val failedTls = runner.check(site.id)
                    assertTrue(failedTls.toString(), failedTls is SyncResult.Failed)
                    assertEquals("TLS", db.siteDao().getSiteById(site.id)!!.lastFailureCode)
                    val previewError = runCatching { previewer.capture(server.url, mode) }.exceptionOrNull()
                    assertNotNull(previewError)
                    assertEquals(CheckFailure.TLS, CheckFailure.from(ErrorHandler().handleError(previewError!!), previewError))
                    preserved()
                    server.status = 200
                    server.redirectTo = null
                    server.disconnect = true
                    assertTrue(runner.check(site.id) is SyncResult.Failed)
                    assertEquals("CONNECTION", db.siteDao().getSiteById(site.id)!!.lastFailureCode)
                    preserved()
                    server.disconnect = false
                    server.responseDelayMs = 10_000
                    coroutineScope {
                        val pending = async { runner.check(site.id) }
                        delay(300)
                        withTimeout(5_000) { pending.cancelAndJoin() }
                        assertTrue(pending.isCancelled)
                    }
                    preserved()
                    server.responseDelayMs = 0
                    val recovery = runner.check(site.id)
                    assertTrue(recovery.toString(), recovery is SyncResult.Success && !recovery.hadChanges)
                    assertNull(db.siteDao().getSiteById(site.id)!!.lastFailureCode)
                    preserved()
                    assertNotNull(previewer.capture(server.url, mode).firstVersion)
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
