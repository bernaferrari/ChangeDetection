package com.bernaferrari.changedetection.addedit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.bernaferrari.changedetection.browser.TestPageServer
import com.bernaferrari.changedetection.error.ErrorHandler
import com.bernaferrari.changedetection.repo.*
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.local.*
import com.bernaferrari.changedetection.storage.SnapshotFileCleanup
import com.bernaferrari.changedetection.sync.*
import com.bernaferrari.changedetection.ui.settings.SettingsViewModel
import com.bernaferrari.changedetection.util.AndroidFileStorage
import io.ktor.client.HttpClient
import java.io.ByteArrayOutputStream
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Same HTML and CSS URI: a failed background dependency is not a visual page change. */
class KnownBackgroundResourceFailureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun failedVisibleCssBackgroundMustNotCreateHistoryOrNotifyAndHealthyRecoveryIsUnchanged() = BackgroundImageServer().use { image ->
        val html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head><body><p>Unchanged page inventory</p><div id='watched' style=\"width:280px;height:240px;background-image:url('${image.url}');background-size:280px 240px\"></div></body></html>"
        TestPageServer(200, html).use { server ->
            compose.setContent { Text("HTTP background fixture") }
            pump {
                val context = ApplicationProvider.getApplicationContext<Context>()
                val name = "background-resource-${java.util.UUID.randomUUID()}.db"
                val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
                val files = AndroidFileStorage(context)
                val client = HttpClient()
                val alerts = AtomicInteger()
                val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, _ -> alerts.incrementAndGet();Unit })
                val site = Site("HTTP background", server.url, 0L, "background-resource", ColorGroup(0, 0), true, false)
                    .copy(captureMode = CaptureMode.SCREENSHOT, isNotificationEnabled = true, visualChangeThresholdBps = 100)
                val preferences = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to 1_000L)
                val store = object : DataStore<Preferences> {
                    override val data = flowOf(preferences)
                    override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(preferences)
                }
                val repository = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
                val runner = AndroidCheckRunner(repository, engine, AndroidScreenshotSyncer(engine, store, SyncProgressTracker(), browserFactory = AndroidScreenshotBrowserFactory(context)), store)
                val previewer = AndroidCapturePreviewer(context, engine, store)
                try {
                    db.siteDao().insertSite(site)
                    val preview = previewer.capture(server.url, CaptureMode.SCREENSHOT)
                    assertTrue(previewer.keepAsFirstVersion(site, preview))
                    assertTrue("The CSS background must actually be fetched", image.successfulResponses.get() > 0)
                    val baseline = db.snapsDao().getLastSnapForSiteId(site.id)!!
                    val bytes = files.readBytes(baseline.snapId)
                    val same = runner.check(site.id)
                    assertTrue(same.toString(), same is SyncResult.Success && !same.hadChanges)

                    // No main-document, stylesheet or resource-URI change. Only its HTTP response changes.
                    image.status = 404
                    val failed = runner.check(site.id)
                    val versions = db.snapsDao().getAllSnapsForSiteId(site.id).size
                    println("REVIEW_HTTP_BACKGROUND result=$failed versions=$versions alerts=${alerts.get()} baselineIntact=${bytes.contentEquals(files.readBytes(baseline.snapId))}")
                    assertTrue("A visible CSS resource must be requested even when unavailable", image.notFoundResponses.get() > 0)
                    assertTrue("HTTP 404 background must fail rather than become a page change: $failed", failed is SyncResult.Failed)
                    assertEquals(1, versions)
                    assertEquals(0, alerts.get())
                    assertArrayEquals(bytes, files.readBytes(baseline.snapId))

                    image.status = 200
                    val recovered = runner.check(site.id)
                    assertTrue(recovered.toString(), recovered is SyncResult.Success && !recovered.hadChanges)
                    assertEquals(1, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    assertEquals(0, alerts.get())

                    server.html = html.replace("<body>", "<body style='background:yellow'>").replace("Unchanged page inventory", "Genuine updated inventory")
                    val changed = runner.check(site.id)
                    assertTrue(changed.toString(), changed is SyncResult.Success && changed.hadChanges)
                    val repeated = runner.check(site.id)
                    assertTrue(repeated.toString(), repeated is SyncResult.Success && !repeated.hadChanges)
                    assertEquals(1, alerts.get())
                    server.html = html
                    val returned = runner.check(site.id)
                    assertTrue(returned.toString(), returned is SyncResult.Success && returned.hadChanges)
                    val unchangedReturn = runner.check(site.id)
                    assertTrue(unchangedReturn.toString(), unchangedReturn is SyncResult.Success && !unchangedReturn.hadChanges)
                    assertEquals(2, alerts.get())
                    assertEquals(3, db.snapsDao().getAllSnapsForSiteId(site.id).size)
                    assertArrayEquals(bytes, files.readBytes(baseline.snapId))
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
        val task = scope.async { withTimeout(150_000) { block() } }
        try {
            compose.waitUntil(160_000) { task.isCompleted }
            return runBlocking { task.await() }
        } finally {
            scope.cancel()
        }
    }

    private class BackgroundImageServer : AutoCloseable {
        private val socket = ServerSocket(0)
        private val pool = Executors.newCachedThreadPool()
        @Volatile var status = 200
        val successfulResponses = AtomicInteger()
        val notFoundResponses = AtomicInteger()
        val url = "http://127.0.0.1:${socket.localPort}/background.png"
        private val bytes = Bitmap.createBitmap(280, 240, Bitmap.Config.ARGB_8888).let { bitmap ->
            try {
                bitmap.eraseColor(Color.BLUE)
                ByteArrayOutputStream().use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));output.toByteArray() }
            } finally { bitmap.recycle() }
        }
        init {
            pool.submit {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    pool.submit {
                        client.use {
                            client.soTimeout = 5_000
                            val reader = client.getInputStream().bufferedReader()
                            reader.readLine() ?: return@use
                            while (!reader.readLine().isNullOrEmpty()) { }
                            val response = status
                            val body = if (response == 200) bytes else byteArrayOf()
                            if (response == 200) successfulResponses.incrementAndGet() else if (response == 404) notFoundResponses.incrementAndGet()
                            val header = "HTTP/1.1 $response ${if (response == 200) "OK" else "Not Found"}\r\nContent-Type: image/png\r\nCache-Control: no-store\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n"
                            client.getOutputStream().apply { write(header.toByteArray());write(body);flush() }
                        }
                    }
                }
            }
        }
        override fun close() { socket.close();pool.shutdownNow() }
    }
}
