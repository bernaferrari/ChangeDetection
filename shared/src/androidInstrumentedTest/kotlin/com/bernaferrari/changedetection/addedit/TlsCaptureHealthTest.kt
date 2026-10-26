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

    @Test fun optionalTlsScriptDoesNotBlockLivePreviewOrSaving() = untrustedTlsPage().use { tls ->
        TestPageServer(200, "<html><body>Healthy document<script async src='${tls.url}'></script></body></html>").use { server ->
            val live = AtomicReference<LivePage>()
            compose.setContent { ChangeDetectionTheme(false, false) { WatchBrowser(server.url, "", emptyList(), { _, _ -> }, {}, { live.set(it) }, {}, "Watch page") } }
            compose.waitUntil(40_000) {
                var refused = false
                compose.runOnUiThread { refused = findWeb(compose.activity.window.decorView)?.let { WebViewCaptureHealth.of(it).blockedResources() > 0 } == true }
                refused && runCatching {
                    compose.onNodeWithText("Watch page").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithText("Watch page").performClick()
            compose.waitUntil(5_000) { live.get() != null }
            val preview = pump { live.get().capturePreview!!.invoke {} }
            assertNotNull(preview?.firstVersion)
            pump {
                database(server.url, CaptureMode.SCREENSHOT) { engine, site, db, files, notifications ->
                    assertTrue(engine.establishFirstVersion(site, requireNotNull(preview?.firstVersion)) is SyncResult.Success)
                    assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    val checked = runner(engine, db, files).check(site.id)
                    assertTrue(checked.toString(), checked is SyncResult.Success && !checked.hadChanges)
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
                        override suspend fun append(attempt: com.bernaferrari.changedetection.diagnostics.DiagnosticAttempt) {
                            attempts += attempt
                        }
                    }
                    try {
                        repeat(2) {
                            val result = runner.check(site.id)
                            assertTrue(result.toString(), result is SyncResult.Failed)
                            assertTrue((result as SyncResult.Failed).exception is com.bernaferrari.changedetection.browser.CaptureNotReadyException)
                            assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                            assertArrayEquals(bytes, files.readBytes(baseline.snapId))
                            assertEquals(0, notifications.get())
                        }
                    } finally {
                        com.bernaferrari.changedetection.diagnostics.DiagnosticLog.sink = previous
                    }
                    // The page loaded; the watched iframe was refused and capture validation rejected its missing content.
                    val completed = attempts.filter { it.outcome != com.bernaferrari.changedetection.diagnostics.DiagnosticOutcome.INCOMPLETE }
                    assertEquals(2, completed.size)
                    assertTrue(completed.toString(), completed.all { it.outcome == com.bernaferrari.changedetection.diagnostics.DiagnosticOutcome.FAILURE && it.failure?.category == com.bernaferrari.changedetection.error.CheckFailure.CAPTURE_INCOMPLETE })
                    val tlsEvents = completed.flatMap { it.events }.mapNotNull { it.tls }
                    assertTrue("A refused embedded certificate must be recorded", tlsEvents.isNotEmpty())
                    assertTrue(tlsEvents.toString(), tlsEvents.all { !it.blocking && it.primary in 0..5 && it.errors.all { code -> code in 0..5 } })
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

    @Test fun selectedEditPreviewsRejectInvalidDocumentsButIgnoreUnrelatedFrames() = untrustedTlsPage().use { tls ->
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
                            if (scenario == "subresource") {
                                assertNotNull("$mode must ignore a frame outside #watched", outcome.getOrThrow().firstVersion)
                            } else {
                                assertTrue("$mode $scenario: ${error?.javaClass?.simpleName}", error is javax.net.ssl.SSLException)
                            }
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

    @Test fun staleDocumentCannotSaveOrChangeHistoryAndHealthyCaptureRecovers() = pump {
        val healthy = "<html><body style='background:white;color:black'><h1>Stable watched content</h1></body></html>"
        TestPageServer(200, healthy).use { server ->
            for (mode in listOf(CaptureMode.SCREENSHOT, CaptureMode.TEXT)) {
                database(server.url, mode) { engine, site, db, files, notifications ->
                    val context = ApplicationProvider.getApplicationContext<Context>()
                    val views = mutableListOf<WebView>()
                    data class Captured(val view: WebView, val session: com.bernaferrari.changedetection.browser.AndroidPageCapture, val value: com.bernaferrari.changedetection.browser.WebViewScreenshot)
                    suspend fun capture(): Captured = withContext(Dispatchers.Main.immediate) {
                        val view = WebView(android.view.ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault_Light))
                        views += view
                        view.layout(0, 0, 390, 844)
                        view.settings.javaScriptEnabled = true
                        view.settings.domStorageEnabled = true
                        val session = com.bernaferrari.changedetection.browser.AndroidPageCapture(view)
                        val value = if (mode == CaptureMode.TEXT) session.captureText(server.url, 0) else session.capture(server.url, 0, 10_000)
                        println("STALE_FIXTURE captured mode=$mode")
                        Captured(view, session, value)
                    }
                    suspend fun invalidate(captured: Captured) = withContext(Dispatchers.Main.immediate) {
                        val health = WebViewCaptureHealth.of(captured.view)
                        val before = health.epoch()
                        captured.view.loadUrl(server.url + "next")
                        withTimeout(5_000) { while (health.epoch() == before) delay(50) }
                        println("STALE_FIXTURE navigated mode=$mode")
                    }
                    fun payload(c: Captured, validation: suspend () -> Unit) = SyncEngine.FetchResult.Success(c.value.contentType, c.value.bytes, capturedWithBrowser = true, validateCapture = validation)
                    try {
                        val old = capture()
                        val oldValidation = old.session.captureValidator()
                        invalidate(old)
                        val current = capture()
                        val currentValidation = current.session.captureValidator()
                        val rejected = engine.establishFirstVersion(site, payload(old, oldValidation))
                        assertTrue(rejected.toString(), rejected is SyncResult.Failed && rejected.exception is com.bernaferrari.changedetection.browser.CaptureNotReadyException)
                        assertEquals(com.bernaferrari.changedetection.browser.CaptureIssue.DOCUMENT_CHANGED, ((rejected as SyncResult.Failed).exception as com.bernaferrari.changedetection.browser.CaptureNotReadyException).issue)
                        assertTrue(db.snapsDao().getAllSnapsForSiteId(site.id).isEmpty())
                        assertEquals(0, notifications.get())
                        assertTrue(engine.establishFirstVersion(site, payload(current, currentValidation)) is SyncResult.Success)
                        val baseline = db.snapsDao().getAllSnapsForSiteId(site.id).single()
                        invalidate(current)
                        val fresh = capture()
                        val freshValidation = fresh.session.captureValidator()
                        val failed = engine.processFetchResult(site, payload(current, currentValidation), site.threshold)
                        assertTrue(failed.toString(), failed is SyncResult.Failed)
                        assertArrayEquals(current.value.bytes, files.readBytes(baseline.snapId))
                        assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                        assertEquals(0, notifications.get())
                        val recovered = engine.processFetchResult(site, payload(fresh, freshValidation), site.threshold)
                        assertTrue(recovered.toString(), recovered is SyncResult.Success && !recovered.hadChanges)
                        assertEquals(listOf(baseline.snapId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
                        assertEquals(0, notifications.get())
                    } finally {
                        withContext(Dispatchers.Main.immediate) {
                            views.forEach { view ->
                                view.stopLoading()
                                view.webViewClient = android.webkit.WebViewClient()
                                view.destroy()
                            }
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
            blocked && runCatching {
                compose.onNodeWithText("Watch page").assertIsNotEnabled()
                true
            }.getOrDefault(false)
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
