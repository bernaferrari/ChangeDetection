package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.webkit.WebView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.browser.*
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.util.AndroidFileStorage
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test
import kotlin.coroutines.resume

/** Controlled WebView fixtures, not a reproduction on the customer's device. */
class RenderedTextWatchTest {
    @Test fun selectedRenderedRegionSurvivesImmediateCheck() = journey("#dynamic", false)

    @Test fun selectedRenderedRegionCanBePreviewedWhenEditing() = journey("#dynamic", true)

    @Test fun wholeRenderedPageDoesNotProduceAFalseChange() = journey("", false)

    private fun journey(selector: String, edit: Boolean) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "rendered-text-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val files = AndroidFileStorage(context)
        val sites = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        val client = HttpClient()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler())
        val settings = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 100L)
        val preferences = object : DataStore<Preferences> {
            override val data = flowOf(settings)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(settings)
        }
        val previewer = AndroidCapturePreviewer(context, engine, preferences)
        try {
            TestPageServer(200, """<!doctype html><title>Rendered fixture</title><body><p>Static introduction</p><script>let p=document.createElement('p');p.id='dynamic';p.textContent='Rendered Greek theatre tickets';document.body.appendChild(p);</script></body>""").use { server ->
                val region = TextRegion(include = selector)
                val site = Site("Fixture", server.url, 0L, "rendered", ColorGroup(0, 0), true, false).copy(captureMode = CaptureMode.TEXT, textIncludeSelector = selector)
                val initial = withContext(Dispatchers.Main) {
                    val web = WebView(context)
                    try {
                        web.layout(0, 0, 1080, 1920)
                        web.configureChangeDetectionWebView(overviewMode = true, acceptThirdPartyCookies = true)
                        // Use the same load readiness as background captures and the exact live text script.
                        AndroidPageCapture(web).capture(server.url, 100, 1000)
                        val raw = suspendCancellableCoroutine<String> { c -> web.evaluateJavascript(liveTextCaptureScript(region)) { if (c.isActive) c.resume(it) } }
                        previewFromCapture(requireNotNull(decodeLiveTextCapture(raw, server.url)), server.url, CaptureMode.TEXT, region)
                    } finally {
                        web.destroySafely(clearCache = false)
                    }
                }
                assertTrue((initial as CapturePreview.Html).text.contains("Rendered Greek theatre tickets"))
                sites.saveSite(site, resetHistory = false, requireExisting = false)
                assertTrue(previewer.keepAsFirstVersion(site, initial))
                if (edit) {
                    val edited = previewer.capture(site.url, CaptureMode.TEXT, region) as CapturePreview.Html
                    assertEquals(initial.text, edited.text)
                } else {
                    val syncer = AndroidScreenshotSyncer(engine, preferences, SyncProgressTracker(), browserFactory = AndroidScreenshotBrowserFactory(context))
                    val result = AndroidCheckRunner(sites, engine, syncer, preferences).check(site.id)
                    assertTrue("Expected successful unchanged rendered text, got $result", result is SyncResult.Success && !result.hadChanges)
                    assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                }
            }
        } finally {
            db.snapsDao().getAllSnapsForSiteId("rendered").forEach { files.deleteFile(it.snapId) }
            client.close()
            db.close()
            context.deleteDatabase(name)
        }
    }
}
