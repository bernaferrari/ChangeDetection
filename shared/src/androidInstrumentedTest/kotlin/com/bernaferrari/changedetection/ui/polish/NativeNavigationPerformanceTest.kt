package com.bernaferrari.changedetection.ui.polish

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.os.*
import android.view.FrameMetrics
import android.view.Window
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.preview.ChangePreviewRepository
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.ui.logs.LogsViewModel
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

/** Real Android frame clock: no Compose test rule, automatic clock advancement, or idling wait. */
class PerfNavigationActivity : ComponentActivity()

class NativeNavigationPerformanceTest {
    @Test fun nativeClockNavigationFrames() {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 33)
        val args = InstrumentationRegistry.getArguments()
        val label = args.getString("phase") ?: "native"
        val pages = args.getString("pages")?.toIntOrNull() ?: 3
        val versions = args.getString("versions")?.toIntOrNull() ?: 2
        val capture = args.getString("real_capture") == "true"
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "native-nav-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val actualFiles = AndroidFileStorage(context)
        val mainReads = AtomicInteger()
        val files = object : FileStorage by actualFiles {
            override fun readBytes(fileName: String): ByteArray {
                if (Looper.myLooper() == Looper.getMainLooper()) mainReads.incrementAndGet()
                return actualFiles.readBytes(fileName)
            }
        }
        val cleanup = SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)
        val sites = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), cleanup))
        val snaps = SnapsRepository(SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, cleanup))
        val scope = ApplicationScope()
        val client = HttpClient()
        val errors = ErrorHandler()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, errors)
        val preferences = object : DataStoreShim() {}
        val monitoring = object : MonitoringManager {
            override val quickMonitorState = MutableStateFlow(TurboState())
            override fun getSyncStatusFlow() = flowOf(emptyList<SyncJobInfo>())
            override fun reloadSite(site: Site) = Unit
            override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
            override fun stopQuickMonitor() = Unit
            override suspend fun updateSchedule() = Unit
            override fun cancelWork() = Unit
        }
        val frameThread = HandlerThread("native-frame-metrics").apply { start() }
        val frames = java.util.Collections.synchronizedList(mutableListOf<List<Long>>())
        val listener = Window.OnFrameMetricsAvailableListener { _, m, _ ->
            frames += listOf(m.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP), m.getMetric(FrameMetrics.TOTAL_DURATION), m.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION), m.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION), m.getMetric(FrameMetrics.DRAW_DURATION), m.getMetric(FrameMetrics.SYNC_DURATION), m.getMetric(FrameMetrics.GPU_DURATION))
        }
        lateinit var main: MainViewModel
        lateinit var logs: LogsViewModel
        var scenario: ActivityScenario<PerfNavigationActivity>? = null
        var server: TestPageServer? = null
        val captures = AtomicInteger()
        var captureJob: Job? = null
        try {
            runBlocking {
                val old = png(Color.WHITE)
                val new = png(Color.LTGRAY)
                repeat(pages) { page ->
                    val site = Site("Native page $page", "https://fixture.invalid/$page", System.currentTimeMillis(), "", "native-$page", 0 to 0).copy(isRead = false, iconUrl = "data:image/png;base64,")
                    sites.saveSite(site, false, false)
                    repeat(versions) { version ->
                        val id = "native-$page-$version"
                        val bytes = if (version % 2 == 0) old else new
                        files.writeBytes(id, bytes)
                        db.snapsDao().insertSnap(Snap(id, site.id, System.currentTimeMillis() - (versions - version) * 60_000, "image/png", "", bytes.size))
                    }
                }
            }
            scenario = ActivityScenario.launch(Intent(context, PerfNavigationActivity::class.java))
            scenario.onActivity { activity ->
                main = MainViewModel(snaps, sites, monitoring, preferences, errors, scope, SyncProgressTracker(), engine)
                logs = LogsViewModel(db.siteDao(), db.snapsDao(), snaps, files, cleanup)
                startKoin {
                    modules(
                        module {
                            single { main }
                            single { logs }
                            single { snaps }
                            single { ChangePreviewRepository(snaps) { db.snapsDao().getPrecedingSnap(it.siteId, it.timestamp, it.snapId) } }
                        },
                    )
                }
                activity.window.addOnFrameMetricsAvailableListener(listener, Handler(frameThread.looper))
                activity.setContent { ChangeDetectionTheme(false, false) { ChangeDetectionApp(PlatformActions()) } }
            }
            fun waitNode(text: String, clickable: Boolean?): AccessibilityNodeInfo {
                val deadline = SystemClock.uptimeMillis() + 15_000
                while (SystemClock.uptimeMillis() < deadline) {
                    automation.clearCache()
                    val nodes = mutableListOf<AccessibilityNodeInfo>()
                    fun visit(node: AccessibilityNodeInfo?) {
                        if (node == null || nodes.size > 1000) return
                        nodes += node
                        repeat(node.childCount) { visit(node.getChild(it)) }
                    }
                    visit(automation.rootInActiveWindow)
                    nodes.filter { ((it.text?.toString() ?: "") + " " + (it.contentDescription?.toString() ?: "")).contains(text, ignoreCase = true) }.map { candidate ->
                        if (clickable != true) {
                            candidate
                        } else {
                            var parent = candidate
                            while (!parent.isClickable && parent.parent != null) parent = parent.parent
                            parent
                        }
                    }.firstOrNull { node ->
                        val bounds = Rect()
                        node.getBoundsInScreen(bounds)
                        node.isVisibleToUser && (clickable == null || node.isClickable == clickable) && (clickable != false || bounds.top < context.resources.displayMetrics.heightPixels / 2)
                    }?.let { return it }
                    Thread.sleep(10)
                }
                error("Visible $text node not found")
            }
            waitNode("new changes", false)
            if (capture) {
                server = TestPageServer(200, "<body><h1>Concurrent check</h1><div style='height:5000px;background:#ddd'>Page content</div></body>")
                val url = requireNotNull(server).url
                captureJob = scope.scope.launch(Dispatchers.Main) {
                    repeat(3) {
                        val browser = AndroidScreenshotBrowserFactory(context).create()
                        try {
                            check(browser.capture(Site("Capture", url, 0L, "capture", ColorGroup(0, 0), true, false), 0) is ScreenshotCaptureOutcome.Success)
                            captures.incrementAndGet()
                        } finally {
                            browser.close()
                        }
                    }
                }
            }
            val activityBounds = Rect().also { waitNode("Activity", null).getBoundsInScreen(it) }
            val homeBounds = Rect().also { waitNode("Home", null).getBoundsInScreen(it) }
            fun tap(bounds: Rect) {
                val now = SystemClock.uptimeMillis()
                for ((action, time) in listOf(android.view.MotionEvent.ACTION_DOWN to now, android.view.MotionEvent.ACTION_UP to now + 20)) {
                    val event = android.view.MotionEvent.obtain(now, time, action, bounds.centerX().toFloat(), bounds.centerY().toFloat(), 0)
                    try {
                        instrumentation.sendPointerSync(event)
                    } finally {
                        event.recycle()
                    }
                }
            }
            repeat(10) { index ->
                val start = System.nanoTime()
                Trace.beginAsyncSection("CD-native-nav-window", index)
                tap(activityBounds)
                waitNode("Activity", false)
                val accessibleAt = System.nanoTime()
                Thread.sleep(400) // Fixed observation window, explicitly not a 'settled' timing.
                val end = System.nanoTime()
                Trace.endAsyncSection("CD-native-nav-window", index)
                Thread.sleep(30)
                val window = synchronized(frames) { frames.filter { it[0] >= start && it[0] <= end } }
                val totals = window.map { it[1] / 1_000_000.0 }.sorted()
                println("NATIVE_NAV $label index=$index accessibilityTitleMs=${(accessibleAt - start) / 1_000_000.0} firstFrameMs=${window.firstOrNull()?.let { (it[0] - start) / 1_000_000.0 }} frames=${window.size} p95Ms=${totals.getOrNull((totals.size * 0.95).toInt().coerceAtMost(totals.lastIndex.coerceAtLeast(0)))} maxMs=${totals.lastOrNull()} maxQueueMs=${window.maxOfOrNull { it[2] / 1_000_000.0 }} maxLayoutMs=${window.maxOfOrNull { it[3] / 1_000_000.0 }} maxDrawMs=${window.maxOfOrNull { it[4] / 1_000_000.0 }} maxSyncMs=${window.maxOfOrNull { it[5] / 1_000_000.0 }} maxGpuMs=${window.maxOfOrNull { it[6] / 1_000_000.0 }}")
                tap(homeBounds)
                waitNode("new changes", false)
                Thread.sleep(400)
            }
            captureJob?.let {
                runBlocking { it.join() }
                assertEquals(3, captures.get())
            }
            assertEquals(0, mainReads.get())
            println("NATIVE_META $label pages=$pages versions=$versions realCaptures=${captures.get()} mainReads=${mainReads.get()} debuggable=${context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0}")
        } finally {
            captureJob?.cancel()
            server?.close()
            scenario?.onActivity { activity ->
                activity.window.removeOnFrameMetricsAvailableListener(listener)
                main.viewModelScope.cancel()
                logs.viewModelScope.cancel()
                stopKoin()
            }
            scenario?.close()
            frameThread.quitSafely()
            scope.scope.cancel()
            client.close()
            runBlocking { db.siteDao().getSitesList().forEach { site -> db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) } } }
            db.close()
            context.deleteDatabase(name)
        }
    }
    private open class DataStoreShim : androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        override val data = flowOf(androidx.datastore.preferences.core.emptyPreferences())
        override suspend fun updateData(transform: suspend (androidx.datastore.preferences.core.Preferences) -> androidx.datastore.preferences.core.Preferences) = transform(androidx.datastore.preferences.core.emptyPreferences())
    }
    private fun png(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(720, 5000, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }
}
