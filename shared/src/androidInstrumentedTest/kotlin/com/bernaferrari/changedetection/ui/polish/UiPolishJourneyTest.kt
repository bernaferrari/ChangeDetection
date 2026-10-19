package com.bernaferrari.changedetection.ui.polish

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.lifecycle.viewModelScope
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.bernaferrari.changedetection.*
import com.bernaferrari.changedetection.background.*
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.preview.*
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.*
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonViewModel
import com.bernaferrari.changedetection.ui.comparison.ImageComparisonContent
import com.bernaferrari.changedetection.ui.comparison.ComparisonMode
import com.bernaferrari.changedetection.ui.logs.*
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.util.*
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

class UiPolishJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val phase = InstrumentationRegistry.getArguments().getString("phase") ?: "after"

    @Test fun activityLoadingEmptyAndReadySubtitlesHaveHonestStableLayout() {
        var preview by mutableStateOf<ChangePreview?>(null)
        val snap = Snap("subtitle", "page", 1L, "text/html", "UTF-8", 10)
        val site = Site("Theatre tickets", "https://example.test", 1L, "", "page", 0 to 0)
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                Surface {
                    ActivityRow(RoundedCornerShape(16.dp), snap, site, {}, {}, true, preview)
                }
            }
        }
        compose.onNodeWithText("Loading…").assertIsDisplayed()
        screenshot("activity-loading")
        compose.runOnUiThread { preview = ChangePreview.Words(" ", emptyList(), 0, false, false) }
        compose.onNodeWithText("Loading…").assertDoesNotExist()
        val row = compose.onNodeWithTag("activity-row-subtitle").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithText("Theatre tickets").fetchSemanticsNode().boundsInRoot
        assertEquals("Title alone should be vertically centered", row.center.y, title.center.y, 2f)
        screenshot("activity-no-subtitle")
        compose.runOnUiThread { preview = ChangePreview.Large(10, 20, added = 10) }
        compose.onNodeWithText("10 new", substring = true).assertIsDisplayed()
        screenshot("activity-ready")
    }

    @Test fun pendingImageComparisonKeepsBothCapturesVisible() {
        val before = image(false)
        val after = image(true)
        compose.setContent {
            ChangeDetectionTheme(false, false) {
                Box(Modifier.fillMaxSize()) {
                    ImageComparisonContent(before, after, null, ComparisonMode.ImageSwipe, Modifier.fillMaxSize(), isPreparing = true)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        }
        compose.onNodeWithText("Before").assertIsDisplayed()
        compose.onNodeWithText("After").assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Before").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithContentDescription("After").fetchSemanticsNodes().isNotEmpty()
        }
        screenshot("comparison-preparing")
    }

    @Test fun realHomeActivityNavigationAndRepeatedPullsUseSavedCapturesOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "polish-${java.util.UUID.randomUUID()}.db"
        val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        val reads = AtomicInteger()
        val mainReads = AtomicInteger()
        val checks = AtomicInteger()
        val actualFiles = AndroidFileStorage(context)
        val files = object : FileStorage by actualFiles {
            override fun readBytes(fileName: String): ByteArray {
                reads.incrementAndGet()
                if (Looper.myLooper() == Looper.getMainLooper()) mainReads.incrementAndGet()
                return actualFiles.readBytes(fileName)
            }
        }
        val cleanup = SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)
        val sites = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), cleanup))
        val snaps = SnapsRepository(SnapsLocalDataSource(AppExecutors(), db.snapsDao(), db.siteDao(), files, cleanup))
        val state = MutableStateFlow(emptyList<SyncJobInfo>())
        val scope = ApplicationScope()
        val preferences = object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(emptyPreferences())
        }
        val monitoring = object : MonitoringManager {
            override val quickMonitorState = MutableStateFlow(TurboState())
            override fun getSyncStatusFlow() = state
            override fun reloadSite(site: Site) {
                checks.incrementAndGet()
                state.value = listOf(SyncJobInfo(SyncState.RUNNING, setOf(site.id)))
                scope.scope.launch {
                    delay(1_500)
                    state.value = emptyList()
                }
            }
            override fun startQuickMonitor(siteIds: List<String>, intervalSeconds: Int, durationMinutes: Int) = Unit
            override fun stopQuickMonitor() = Unit
            override suspend fun updateSchedule() = Unit
            override fun cancelWork() = Unit
        }
        val client = HttpClient()
        val errors = ErrorHandler()
        val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, errors)
        val previews = ChangePreviewRepository(snaps) { db.snapsDao().getPrecedingSnap(it.siteId, it.timestamp, it.snapId) }
        lateinit var main: MainViewModel
        lateinit var logs: LogsViewModel
        var about by mutableStateOf(false)
        var created = false
        try {
            runBlocking {
                val before = image(false)
                val after = image(true)
                for (n in 1..3) {
                    val site = Site("Theatre $n", "https://fixture.invalid/$n", System.currentTimeMillis(), "", "page-$n", 0 to 0).copy(isRead = false, iconUrl = "data:image/png;base64,")
                    sites.saveSite(site, false, false)
                    for ((id, time, bytes) in listOf(Triple("old-$n", System.currentTimeMillis() - 3_600_000, before), Triple("new-$n", System.currentTimeMillis() - 60_000, after))) {
                        files.writeBytes(id, bytes)
                        db.snapsDao().insertSnap(Snap(id, site.id, time, "image/png", "UTF-8", bytes.size))
                    }
                }
            }
            compose.runOnUiThread {
                main = MainViewModel(snaps, sites, monitoring, preferences, errors, scope, SyncProgressTracker(), engine)
                logs = LogsViewModel(db.siteDao(), db.snapsDao(), snaps, files, cleanup)
                created = true
                startKoin {
                    modules(
                        module {
                            single { main }
                            single { logs }
                            single { snaps }
                            single { previews }
                            factory {
                                ComparisonViewModel(
                                    sites,
                                    snaps,
                                    monitoring,
                                    object : ShareManager {
                                        override fun shareText(text: String, title: String) = Unit
                                        override fun shareFile(fileName: String, text: String, title: String) = Unit
                                    },
                                    errors,
                                    scope,
                                )
                            }
                        },
                    )
                }
            }
            compose.setContent {
                ChangeDetectionTheme(false, false) {
                    if (about) {
                        AboutScreen({ about = false }, {}, {}, {}, {}, "Review")
                    } else {
                        ChangeDetectionApp(PlatformActions())
                    }
                }
            }
            compose.waitUntil(10_000) { compose.onAllNodesWithTag("home-list").fetchSemanticsNodes().isNotEmpty() }
            Thread.sleep(1_500)
            screenshot("home")
            val timings = mutableListOf<Long>()
            repeat(3) {
                val start = SystemClock.elapsedRealtime()
                compose.onNode(hasText("Activity") and hasClickAction()).performClick()
                compose.waitUntil(5_000) { compose.onAllNodesWithTag("activity-list").fetchSemanticsNodes().isNotEmpty() }
                timings += SystemClock.elapsedRealtime() - start
                if (it == 0) screenshot("activity")
                compose.onNode(hasText("Home") and hasClickAction()).performClick()
                compose.onNodeWithTag("home-list").assertIsDisplayed()
            }
            println("UI REVIEW $phase Home→Activity milliseconds=$timings reads=${reads.get()} mainThreadFileReads=${mainReads.get()}")
            assertEquals("Snapshot I/O must stay off the main thread", 0, mainReads.get())
            repeat(3) {
                if (phase == "before") {
                    compose.onNodeWithTag("home-list").performTouchInput { swipeDown(durationMillis = 700) }
                } else {
                    compose.onNodeWithTag("home-list").performTouchInput {
                        down(topCenter + Offset(0f, 40f))
                        moveBy(Offset(0f, 80f), 200)
                    }
                    if (it == 0) screenshot("pulling")
                    compose.onNodeWithTag("home-list").performTouchInput { moveBy(Offset(0f, 650f), 300) }
                    if (it == 0) screenshot("release")
                    compose.onNodeWithTag("home-list").performTouchInput { up() }
                }
                Thread.sleep(150)
                screenshot("pull-$it")
                Thread.sleep(1_600)
            }
            val checksBefore = checks.get()
            compose.onNodeWithTag("home-list").performTouchInput {
                down(topCenter + Offset(0f, 40f))
                moveBy(Offset(0f, 60f), 150)
                up()
            }
            Thread.sleep(250)
            assertEquals("An interrupted short pull must not start another check", checksBefore, checks.get())
            compose.runOnUiThread { about = true }
            compose.onNodeWithTag("about-list").performScrollToKey("note")
            screenshot("about")
        } finally {
            compose.runOnUiThread {
                if (created) main.viewModelScope.cancel()
                if (created) logs.viewModelScope.cancel()
                stopKoin()
            }
            scope.scope.cancel()
            client.close()
            runBlocking { db.siteDao().getSitesList().forEach { site -> db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) } } }
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun screenshot(label: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "cd-ui-$phase-$label.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ChangeDetectionReview")
        }
        val uri = requireNotNull(context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
        context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun image(changed: Boolean): ByteArray {
        val bitmap = Bitmap.createBitmap(720, 5000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
        }
        canvas.drawText(if (changed) "National Theatre — tickets available" else "National Theatre — sold out", 24f, 80f, paint)
        repeat(75) { canvas.drawText("Performance ${it + 1} · Athens · 19:00", 24f, 140f + it * 60, paint) }
        return ByteArrayOutputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
            it.toByteArray()
        }
    }
}
