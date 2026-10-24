package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.lifecycle.ViewModelStore
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.AndroidFileStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class LiveBrowserWatchJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun constantTitlePushStateNavigationUsesTheLiveUrlForPreviewAndSave() = verifyOpenBrowserReuse(ComparisonMethod.HTML)

    @Test fun screenshotReusesTheOpenBrowserInsteadOfLoadingAnotherPage() = verifyOpenBrowserReuse(ComparisonMethod.SCREENSHOT)

    @Test fun longPageDoesNotPrepareContentOutsideTheScreenshot() = verifyOpenBrowserReuse(ComparisonMethod.SCREENSHOT, longPage = true)

    @Test fun selectedPartDoesNotPrepareTheRestOfALongPage() = verifyOpenBrowserReuse(ComparisonMethod.SCREENSHOT, longPage = true, selectedPart = true)

    private fun verifyOpenBrowserReuse(method: ComparisonMethod, longPage: Boolean = false, selectedPart: Boolean = false) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "spa-watch-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val files = AndroidFileStorage(context)
        val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        val initial = preferencesOf(SettingsViewModel.COMPARISON_METHOD_KEY to method.name)
        val preferences = object : DataStore<Preferences> {
            override val data = flowOf(initial)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(initial)
        }
        val monitoring = object : MonitoringManager {
            override val quickMonitorState = MutableStateFlow(TurboState())
            override fun getSyncStatusFlow() = flowOf(emptyList<SyncJobInfo>())
            override fun reloadSite(site: Site) = Unit
            override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
            override fun stopQuickMonitor() = Unit
            override suspend fun updateSchedule() = Unit
            override fun cancelWork() = Unit
        }
        val keptPreview = AtomicReference<CapturePreview>()
        val saved = CompletableDeferred<String>()
        val store = ViewModelStore()
        val html = """<img src="/missing-image.png" width="16" height="16"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Constant title</title><button id="product" onclick="history.pushState({},'', '/product/b');document.getElementById('details').textContent='Only in the open browser';document.body.style.background='#0000ff' ">Product B</button><p id="details">Product details</p><span id="age">2 hours ago</span><div id="footer" style="position:fixed;bottom:0;height:30px;background:red">Page controls</div>"""
        try {
            TestPageServer(200, html, resourceDelayMs = if (longPage) 45_000 else 0).use { server ->
                val draftUrl = server.url + "catalog"
                val expected = server.url + "product/b"
                lateinit var model: AddEditViewModel
                compose.runOnUiThread {
                    model = AddEditViewModel(
                        null,
                        repository,
                        object : CapturePreviewer {
                            override suspend fun capture(url: String, mode: CaptureMode): CapturePreview = error("The loaded browser document must be reused")
                            override suspend fun keepAsFirstVersion(site: Site, preview: CapturePreview): Boolean {
                                keptPreview.set(preview)
                                return true
                            }
                        },
                        monitoring,
                        preferences,
                    )
                    store.put("draft", model)
                }
                compose.setContent {
                    ChangeDetectionTheme(darkTheme = false, dynamicColor = false) {
                        AddEditFlow(viewModel = model, onNavigateUp = {}, onSaveComplete = {}, onOpenSite = {}, onPageAdded = { id, _ -> saved.complete(id) })
                    }
                }
                compose.onNodeWithText("Page address").performTextReplacement(draftUrl)
                compose.onNodeWithText("Open page").performClick()
                val ready = AtomicBoolean(false)
                val web = AtomicReference<WebView>()
                compose.waitUntil(30_000) {
                    compose.runOnUiThread {
                        findWeb(compose.activity.window.decorView)?.let {
                            web.set(it)
                            it.evaluateJavascript("!!document.getElementById('product')") { value -> ready.set(value == "true") }
                        }
                    }
                    ready.get()
                }
                val current = AtomicReference<String>()
                compose.runOnUiThread {
                    val outsideCapture = if (longPage) "document.body.insertAdjacentHTML('beforeend','<div style=\"height:20000px\"></div><img src=\"/slow-image\" width=\"20\" height=\"20\">');" else ""
                    web.get().evaluateJavascript("document.getElementById('product').click();${outsideCapture}window.location.href") { current.set(decodeBrowserUrl(it)) }
                }
                compose.waitUntil(5_000) { current.get() == expected }
                if (selectedPart) {
                    compose.runOnUiThread {
                        model.setTextRegion(com.bernaferrari.changedetection.detection.TextRegion(include = "#details"))
                    }
                }
                val startedAt = android.os.SystemClock.elapsedRealtime()
                compose.onNode(hasText("Watch this page") and hasClickAction()).performClick()
                compose.waitUntil(5_000) {
                    compose.onAllNodes(hasText("Start watching") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                }
                println("Preview review opened in ${android.os.SystemClock.elapsedRealtime() - startedAt} ms")
                if (method == ComparisonMethod.SCREENSHOT && !selectedPart) {
                    compose.waitUntil(5_000) {
                        compose.onAllNodesWithTag("browser-preview").fetchSemanticsNodes().isNotEmpty()
                    }
                    println("Visible browser preview in ${android.os.SystemClock.elapsedRealtime() - startedAt} ms")
                    val visible = (model.uiState.value.preview as CapturePreviewState.Ready).content as CapturePreview.Image
                    if (longPage) assertTrue("Show the viewport before the full-page capture finishes", visible.isPreparing)
                    if (visible.isPreparing) {
                        assertNull("A viewport thumbnail must never become the baseline", visible.firstVersion)
                        val thumbnail = android.graphics.BitmapFactory.decodeByteArray(visible.bytes, 0, visible.bytes.size)
                        try {
                            val pixel = thumbnail.getPixel(thumbnail.width / 2, thumbnail.height / 2)
                            assertTrue("Reuse the blue browser document, not a blank placeholder", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
                        } finally {
                            thumbnail.recycle()
                        }
                    }
                }
                compose.waitUntil(30_000) {
                    val content = (model.uiState.value.preview as? CapturePreviewState.Ready)?.content
                    content != null && (content !is CapturePreview.Image || !content.isPreparing)
                }
                val elapsed = android.os.SystemClock.elapsedRealtime() - startedAt
                println("Live preview ready in $elapsed ms")
                // The 30s readiness timeout is shorter than the resource's 45s response.
                // Assert the resource is still pending, rather than a hardware-dependent
                // deadline: the unchanged control takes 8s on the software renderer.
                if (longPage) {
                    val ignoredResourceStillPending = AtomicBoolean(false)
                    compose.runOnUiThread {
                        web.get().evaluateJavascript("!document.querySelector('img[src=\"/slow-image\"]').complete") { value -> ignoredResourceStillPending.set(value == "true") }
                    }
                    compose.waitUntil(2_000) { ignoredResourceStillPending.get() }
                    assertTrue("The preview completed while the out-of-capture image was still pending", ignoredResourceStillPending.get())
                }
                val reviewed = (model.uiState.value.preview as CapturePreviewState.Ready).content
                if (method == ComparisonMethod.HTML) {
                    assertTrue("Reuse client-side state that a new request would lose", (reviewed as CapturePreview.Html).text.contains("Only in the open browser"))
                    assertTrue(requireNotNull(reviewed.firstVersion).data.decodeToString().contains("Only in the open browser"))
                } else {
                    val image = reviewed as CapturePreview.Image
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size)
                    try {
                        if (selectedPart) assertTrue("Capture only the selected paragraph", bitmap.height < 200)
                        val pixel = if (selectedPart) bitmap.getPixel(bitmap.width - 2, bitmap.height - 2) else bitmap.getPixel(bitmap.width / 2, bitmap.height - 10)
                        assertTrue("Capture the blue browser state, not the original response", android.graphics.Color.blue(pixel) > 200 && android.graphics.Color.red(pixel) < 40)
                    } finally {
                        bitmap.recycle()
                    }
                    assertTrue(requireNotNull(image.firstVersion).capturedWithBrowser)
                }
                assertEquals(expected, model.uiState.value.url)
                val restored = AtomicBoolean(false)
                compose.runOnUiThread {
                    web.get().evaluateJavascript("document.getElementById('age').textContent==='2 hours ago' && document.getElementById('footer').style.visibility===''") { restored.set(it == "true") }
                }
                compose.waitUntil(5_000) { restored.get() }
                compose.onNode(hasText("Start watching") and hasClickAction()).performClick()
                compose.waitUntil(5_000) { saved.isCompleted }
                assertEquals(expected, runBlocking { db.siteDao().getSiteById(saved.await())?.url })
                assertSame("The exact reviewed payload must become the baseline", reviewed, keptPreview.get())
            }
        } finally {
            compose.runOnUiThread { store.clear() }
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun findWeb(view: View): WebView? {
        if (view is WebView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) findWeb(view.getChildAt(index))?.let { return it }
        return null
    }
}
