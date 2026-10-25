package com.bernaferrari.changedetection.addedit
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.browser.untrustedTlsPage
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.bernaferrari.changedetection.browser.WebViewCaptureHealth
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.util.AndroidFileStorage
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import io.ktor.client.HttpClient
import java.util.concurrent.atomic.AtomicReference
class TlsCaptureHealthTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun invalidMainCannotBeWatched() {
        untrustedTlsPage().use { rejects(it.url) }
    }

    @Test fun redirectToInvalidTlsCannotBeWatched() {
        untrustedTlsPage().use { tls -> TestPageServer(200, "<html><body>Redirect<script>location.href='${tls.url}'</script></body></html>").use { rejects(it.url) } }
    }

    @Test fun invalidEmbeddedResourceCannotBeWatched() {
        untrustedTlsPage().use { tls -> TestPageServer(200, "<html><body>Content<iframe src='${tls.url}'></iframe></body></html>").use { rejects(it.url) } }
    }

    @Test fun lateEmbeddedTlsRevokesReadyPreviewAndHealthyNavigationRecovers() = untrustedTlsPage().use { tls ->
        TestPageServer(200, "<html><body>Healthy document</body></html>").use { server ->
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
            assertNotNull(preview?.firstVersion)
            // The preview was ready before this frame arrived. Refusing it makes completeness
            // unproven, so even the previously returned ready capture must fail promotion.
            compose.runOnUiThread { findWeb(compose.activity.window.decorView)!!.evaluateJavascript("document.body.insertAdjacentHTML('beforeend', '<iframe src=\"${tls.url}\"></iframe>');0", null) }
            compose.waitUntil(30_000) {
                var failed = false
                compose.runOnUiThread { failed = WebViewCaptureHealth.of(findWeb(compose.activity.window.decorView)!!).hasBlockingFailure() }
                failed
            }
            compose.waitUntil(5_000) { runCatching { compose.onNodeWithText("Watch page").assertIsNotEnabled();true }.getOrDefault(false) }
            pump {
                database(server.url, CaptureMode.SCREENSHOT) { engine, site, db, _, notifications ->
                    val rejected = engine.establishFirstVersion(site, requireNotNull(preview?.firstVersion))
                    assertTrue(rejected.toString(), rejected is SyncResult.Failed)
                    assertTrue((rejected as SyncResult.Failed).exception is javax.net.ssl.SSLException)
                    assertTrue(db.snapsDao().getAllSnapsForSiteId(site.id).isEmpty())
                    assertEquals(0, notifications.get())
                }
            }
            compose.runOnUiThread { findWeb(compose.activity.window.decorView)!!.loadUrl(server.url) }
            compose.waitUntil(30_000) {
                runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            live.set(null)
            compose.onNodeWithText("Watch page").performClick()
            compose.waitUntil(5_000) { live.get() != null }
            val recovered = pump { live.get().capturePreview!!.invoke {} }
            assertNotNull(recovered?.firstVersion)
            pump {
                database(server.url, CaptureMode.SCREENSHOT) { engine, site, db, _, notifications ->
                    assertTrue(engine.establishFirstVersion(site, requireNotNull(recovered?.firstVersion)) is SyncResult.Success)
                    assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    assertEquals(0, notifications.get())
                }
            }

        }
    }

    @Test fun failedChecksPreserveHistoryAndDoNotNotifyThenRecover() = untrustedTlsPage().use { tls ->
        TestPageServer(200, "<html><body>Original healthy content</body></html>").use { server ->
            pump {
                database(server.url) { engine, site, db, files, notifications ->
                    val runner = runner(engine, db, files)
                    assertTrue(runner.check(site.id) is SyncResult.Success)
                    val baseline = db.snapsDao().getAllSnapsForSiteId(site.id).single()
                    val bytes = files.readBytes(baseline.snapId)
                    // The page now sends readers to a host with an untrusted certificate: never saved as a change.
                    server.html = "<html><body>Partial changed content<script>location.href='${tls.url}'</script></body></html>"
                    repeat(2) {
                        val result = runner.check(site.id)
                        assertTrue(result.toString(), result is SyncResult.Failed)
                        assertTrue((result as SyncResult.Failed).exception is javax.net.ssl.SSLException)
                        assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                        assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                        assertEquals(0, notifications.get())
                    }
                    server.html = "<html><body>Original healthy content</body></html>"
                    val recovery = runner.check(site.id)
                    assertTrue(recovery is SyncResult.Success && !recovery.hadChanges)
                    assertEquals(0, notifications.get())
                    server.html = "<html><body>Completely new healthy content with changed availability</body></html>"
                    assertTrue(runner.check(site.id) is SyncResult.Success)
                    assertEquals(1, notifications.get())
                }
            }
        }
    }

    @Test fun embeddedTlsFailuresPreserveScreenshotHistoryAndDoNotNotifyThenRecover() = untrustedTlsPage().use { tls ->
        val healthy = "<html><body>Original healthy content</body></html>"
        TestPageServer(200, healthy).use { server ->
            pump {
                database(server.url, CaptureMode.SCREENSHOT) { engine, site, db, files, notifications ->
                    val runner = runner(engine, db, files)
                    assertTrue(runner.check(site.id) is SyncResult.Success)
                    val baseline = db.snapsDao().getAllSnapsForSiteId(site.id).single()
                    val bytes = files.readBytes(baseline.snapId)
                    server.html = "<html><body>Partial changed content<iframe src='${tls.url}'></iframe></body></html>"
                    val attempts = java.util.Collections.synchronizedList(mutableListOf<com.bernaferrari.changedetection.diagnostics.DiagnosticAttempt>())
                    val previous = com.bernaferrari.changedetection.diagnostics.DiagnosticLog.sink
                    com.bernaferrari.changedetection.diagnostics.DiagnosticLog.sink = object : com.bernaferrari.changedetection.diagnostics.DiagnosticSink {
                        override suspend fun watchToken(watchId: String?) = "watch"
                        override suspend fun append(attempt: com.bernaferrari.changedetection.diagnostics.DiagnosticAttempt) { attempts += attempt }
                    }
                    try {
                        repeat(2) {
                            val result = runner.check(site.id)
                            assertTrue(result.toString(), result is SyncResult.Failed)
                            assertTrue((result as SyncResult.Failed).exception is javax.net.ssl.SSLException)
                            assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                            assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                            assertEquals(0, notifications.get())
                        }
                    } finally {
                        com.bernaferrari.changedetection.diagnostics.DiagnosticLog.sink = previous
                    }
                    // A strict early abort need not reach HTTP-status collection. The retained
                    // evidence must instead identify a TLS failure and its blocking classification.
                    val completed = attempts.filter { it.outcome != com.bernaferrari.changedetection.diagnostics.DiagnosticOutcome.INCOMPLETE }
                    assertEquals(2, completed.size)
                    assertTrue(completed.toString(), completed.all { it.outcome == com.bernaferrari.changedetection.diagnostics.DiagnosticOutcome.FAILURE && it.failure?.category == com.bernaferrari.changedetection.error.CheckFailure.TLS })
                    val tlsEvents = completed.flatMap { it.events }.mapNotNull { it.tls }
                    assertTrue("A refused embedded certificate must be recorded", tlsEvents.isNotEmpty())
                    assertTrue(tlsEvents.toString(), tlsEvents.all { it.blocking && it.primary in 0..5 && it.errors.all { code -> code in 0..5 } })
                    assertTrue(tlsEvents.toString(), tlsEvents.any { it.relation == com.bernaferrari.changedetection.diagnostics.TlsUrlRelation.DIFFERENT_ORIGIN && it.requestKind in setOf(com.bernaferrari.changedetection.diagnostics.TlsRequestKind.SUBRESOURCE, com.bernaferrari.changedetection.diagnostics.TlsRequestKind.UNKNOWN) })
                    assertFalse(completed.toString().contains(tls.url))
                    server.html = healthy
                    val recovery = runner.check(site.id)
                    assertTrue(recovery.toString(), recovery is SyncResult.Success && !recovery.hadChanges)
                    assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                    assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                    assertEquals(0, notifications.get())
                }
            }
        }
    }

    @Test fun freshEditPreviewsRejectInvalidMainRedirectAndEmbeddedTlsThenRecover() = untrustedTlsPage().use { tls ->
        val healthy = "<html><body style='background:white;color:black'><h1 id='watched'>Healthy content</h1></body></html>"
        TestPageServer(200, healthy).use { server ->
            pump {
                database { engine, site, db, _, notifications ->
                    val context = ApplicationProvider.getApplicationContext<Context>()
                    val prefs = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 3_000L)
                    val store = object : DataStore<Preferences> {
                        override val data = flowOf(prefs)
                        override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(prefs)
                    }
                    val previewer = AndroidCapturePreviewer(context, engine, store)
                    val region = com.bernaferrari.changedetection.detection.TextRegion(include = "#watched")
                    for (mode in listOf(CaptureMode.TEXT, CaptureMode.SCREENSHOT)) {
                        for (scenario in listOf("main", "redirect", "subresource")) {
                            server.html = when (scenario) {
                                "redirect" -> "<html><body><script>location.href='${tls.url}'</script></body></html>"
                                "subresource" -> healthy.replace("</body>", "<iframe src='${tls.url}'></iframe></body>")
                                else -> healthy
                            }
                            val target = if (scenario == "main") tls.url else server.url
                            val outcome = runCatching { previewer.capture(target, mode, region) }
                            val error = outcome.exceptionOrNull()
                            assertTrue("$mode $scenario: ${error?.javaClass?.simpleName}", error is javax.net.ssl.SSLException)
                            assertTrue(db.snapsDao().getAllSnapsForSiteId(site.id).isEmpty())
                            assertEquals(0, notifications.get())
                            server.html = healthy
                            assertNotNull(previewer.capture(server.url, mode, region).firstVersion)
                        }
                    }
                }
            }
        }
    }

    private suspend fun database(url: String = "https://example.invalid", mode: CaptureMode = CaptureMode.TEXT, block: suspend (SyncEngine, Site, ChangeDatabase, AndroidFileStorage, java.util.concurrent.atomic.AtomicInteger) -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "tls-health-${java.util.UUID.randomUUID()}.db"
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
        val site = Site("TLS", url, 0L, "tls-health", ColorGroup(0, 0), true, false).copy(captureMode = mode, isNotificationEnabled = true)
        try {
            db.siteDao().insertSite(site)
            block(engine, site, db, files, notifications)
        } finally {
            db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) }
            client.close()
            db.close()
            context.deleteDatabase(name)
        }
    }
    private fun runner(engine: SyncEngine, db: ChangeDatabase, files: AndroidFileStorage): AndroidCheckRunner {
        val prefs = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 3_000L)
        val store = object : DataStore<Preferences> {
            override val data = flowOf(prefs)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(prefs)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        return AndroidCheckRunner(repository, engine, AndroidScreenshotSyncer(engine, store, SyncProgressTracker(), browserFactory = AndroidScreenshotBrowserFactory(context)), store)
    }
    private fun <T> pump(block: suspend () -> T): T {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = scope.async { withTimeout(150_000) { block() } }
        try {
            compose.waitUntil(160_000) { job.isCompleted }
            return runBlocking { job.await() }
        } finally {
            scope.cancel()
        }
    }
    private fun rejects(url: String) {
        compose.setContent { ChangeDetectionTheme(false, false) { WatchBrowser(url, "", emptyList(), { _, _ -> }, {}, {}, {}, "Watch page") } }
        compose.waitUntil(40_000) {
            var blocked = false
            compose.runOnUiThread { blocked = findWeb(compose.activity.window.decorView)?.let { WebViewCaptureHealth.of(it).hasBlockingFailure() } == true }
            blocked && runCatching { compose.onNodeWithText("Watch page").assertIsNotEnabled();true }.getOrDefault(false)
        }
        compose.runOnUiThread {
            val observations = WebViewCaptureHealth.of(requireNotNull(findWeb(compose.activity.window.decorView))).observations()
            assertTrue(observations.toString(), observations.isNotEmpty() && observations.all { it.blocking })
        }
        compose.onNodeWithText("Watch page").assertIsNotEnabled()
    }
    private fun findWeb(view: View): WebView? = when (view) {
        is WebView -> view
        is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findWeb(view.getChildAt(it)) }
        else -> null
    }
}
