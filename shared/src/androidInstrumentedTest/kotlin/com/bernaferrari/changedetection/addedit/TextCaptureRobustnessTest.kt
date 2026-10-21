package com.bernaferrari.changedetection.addedit

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
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
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class TextCaptureRobustnessTest {
    @Test fun delayedSelectedRegionCanBeCheckedAndPreviewed() = watch("#target") {
        server.response = { _, _ -> Response("<body><p>Loading</p><script>setTimeout(()=>{document.body.innerHTML='<p id=target>$TEXT</p>'},600)</script>") }
        unchanged(runner.check(site.id))
        assertEquals(TEXT, (preview.capture(site.url, CaptureMode.TEXT, site.textRegion()) as CapturePreview.Html).text)
    }

    @Test fun emptyHtmlCannotReplaceTheLastGoodVersion() = watch("") {
        server.response = { _, _ -> Response("<html><head><title>Empty response</title></head><body></body></html>") }
        failedWithoutVersion(runner.check(site.id))
        assertTrue(runCatching { preview.capture(site.url, CaptureMode.TEXT) }.isFailure)
        server.response = { _, _ -> Response(PAGE) }
        unchanged(runner.check(site.id))
    }

    @Test fun lateRedirectToHttpErrorCannotBecomeAChange() = watch("", delayMs = 500) {
        server.response = { path, _ -> if (path == "/down") Response("Service unavailable", 503) else Response("<body>Redirecting<script>setTimeout(()=>location.href='/down',100)</script>") }
        failedWithoutVersion(runner.check(site.id))
    }

    @Test fun removedRegionFailsAndThenRecoversWithoutLosingTheBaseline() = watch("#target") {
        server.response = { _, _ -> Response("<body><p>Different content outside selection</p></body>") }
        failedWithoutVersion(runner.check(site.id))
        assertTrue(runCatching { preview.capture(site.url, CaptureMode.TEXT, TextRegion("#target")) }.isFailure)
        server.response = { _, _ -> Response(PAGE) }
        unchanged(runner.check(site.id))
    }

    @Test fun renderedHttpErrorsKeepTheirFailureCategory() = watch("") {
        for ((status, expected) in listOf(
            403 to com.bernaferrari.changedetection.error.CheckFailure.ACCESS_DENIED,
            404 to com.bernaferrari.changedetection.error.CheckFailure.NOT_FOUND,
            429 to com.bernaferrari.changedetection.error.CheckFailure.RATE_LIMITED,
            503 to com.bernaferrari.changedetection.error.CheckFailure.SERVER,
        )) {
            server.response = { _, _ -> Response("Unavailable", status) }
            val result = runner.check(site.id)
            failedWithoutVersion(result)
            val failure = result as SyncResult.Failed
            assertEquals(
                "Preserve the main-document HTTP status",
                expected,
                com.bernaferrari.changedetection.error.CheckFailure.from(failure.error, failure.exception),
            )
            val context = ApplicationProvider.getApplicationContext<Context>()
            val report = com.bernaferrari.changedetection.diagnostics.AndroidDiagnostics.install(context).report(site.id)
            val attempts = kotlinx.serialization.json.Json.parseToJsonElement(report.json)
                .let { it as kotlinx.serialization.json.JsonObject }.getValue("attempts") as kotlinx.serialization.json.JsonArray
            assertTrue("Actual rendered check must retain its HTTP callback and failure stage", attempts.any { row ->
                val encoded = row.toString()
                encoded.contains("\"category\":\"${expected.name}\"") &&
                    encoded.contains("\"stage\":\"NAVIGATE\"") && encoded.contains("\"metric\":\"HTTP_STATUS\",\"code\":$status")
            })
            assertFalse(report.json.contains(server.url))
        }
    }

    @Test fun httpErrorPreservesBaselineAndRetryWorks() = watch("") {
        server.response = { _, _ -> Response("Unavailable", 503) }
        failedWithoutVersion(runner.check(site.id))
        server.response = { _, _ -> Response(PAGE) }
        unchanged(runner.check(site.id))
    }

    @Test fun redirectAndSessionCookieKeepGreekWindows1253Text() = watch("") {
        val token = java.util.UUID.randomUUID().toString()
        server.response = { _, _ -> Response(PAGE, headers = mapOf("Set-Cookie" to "watch_session=$token; Path=/")) }
        preview.capture(site.url, CaptureMode.TEXT)
        server.response = { path, cookie ->
            when {
                path == "/" -> Response("", 302, mapOf("Location" to "/session"))
                "watch_session=$token" in cookie -> Response(PAGE, charset = "windows-1253")
                else -> Response("Login required", 401)
            }
        }
        unchanged(runner.check(site.id))
        assertEquals(TEXT, (preview.capture(site.url, CaptureMode.TEXT) as CapturePreview.Html).text)
    }

    @Test fun cancelledCaptureClosesBrowserAndAllowsRetry() = watch("", delayMs = 1_000) {
        val requests = server.requests.get()
        coroutineScope {
            val check = launch { runner.check(site.id) }
            withTimeout(5_000) { while (server.requests.get() <= requests) delay(20) }
            check.cancelAndJoin()
        }
        assertEquals(0, openBrowsers.get())
        assertBaseline()
        unchanged(runner.check(site.id))
    }

    @Test fun samePageConcurrentChecksDoNotCreateDuplicateVersions() = watch("#target") {
        coroutineScope {
            listOf(async { runner.check(site.id) }, async { runner.check(site.id) }).awaitAll().forEach { unchanged(it) }
        }
        assertEquals(0, openBrowsers.get())
    }

    @Test fun networkDisconnectPreservesBaselineAndRetryWorks() = watch("") {
        server.dropConnections = true
        failedWithoutVersion(runner.check(site.id))
        server.dropConnections = false
        unchanged(runner.check(site.id))
    }

    @Test fun emptyShellWaitsForWholePageJavaScriptText() = watch("") {
        server.response = { _, _ -> Response("<body><script>setTimeout(()=>{document.body.innerHTML='<p>$TEXT</p>'},600)</script>") }
        unchanged(runner.check(site.id))
    }

    @Test fun parentTimeoutReleasesBrowserAndAllowsRetry() = watch("", delayMs = 1_000) {
        val result = withTimeoutOrNull(200) { runner.check(site.id) }
        assertNull(result)
        assertEquals(0, openBrowsers.get())
        assertBaseline()
        unchanged(runner.check(site.id))
    }

    @Test fun realTextChangeStillSavesAndNotifiesOnce() = watch("#target") {
        server.response = { _, _ -> Response("<body><p id=target>$TEXT νέα</p></body>") }
        changed(runner.check(site.id))
    }

    private fun watch(selector: String, delayMs: Long = 0, block: suspend Fixture.() -> Unit) = runBlocking {
        val f = Fixture(selector, delayMs)
        try {
            f.start()
            f.block()
        } finally {
            f.close()
        }
    }

    private class Fixture(selector: String, delayMs: Long) {
        private val context = ApplicationProvider.getApplicationContext<Context>()
        private val name = "text-edge-${java.util.UUID.randomUUID()}.db"
        private val db = getRoomDatabase(Room.databaseBuilder<ChangeDatabase>(context, context.getDatabasePath(name).absolutePath), BundledSQLiteDriver())
        private val files = AndroidFileStorage(context)
        private val sites = SitesRepository(SitesLocalDataSource(AppExecutors(), db.siteDao(), SnapshotFileCleanup(db.pendingFileDeletionDao(), db.snapsDao(), files)))
        private val client = HttpClient()
        val server = Server()
        val site = Site("Edge fixture", server.url, 0L, "edge", ColorGroup(0, 0), true, true).copy(captureMode = CaptureMode.TEXT, textIncludeSelector = selector)
        private val alerts = AtomicInteger()
        private val engine = SyncEngine(client, db, db.siteDao(), db.snapsDao(), files, ErrorHandler(), ChangeNotifier { _, _ -> alerts.incrementAndGet() })
        private val settings = preferencesOf(SettingsViewModel.BROWSER_DELAY_KEY to delayMs)
        private val preferences = object : DataStore<Preferences> {
            override val data = flowOf(settings)
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(settings)
        }
        val preview = AndroidCapturePreviewer(context, engine, preferences)
        val openBrowsers = AtomicInteger()
        private val browsers = AndroidScreenshotBrowserFactory(context)
        private val syncer = AndroidScreenshotSyncer(
            engine,
            preferences,
            SyncProgressTracker(),
            browserFactory = ScreenshotBrowserFactory {
                val delegate = browsers.create()
                openBrowsers.incrementAndGet()
                object : ScreenshotBrowser {
                    override suspend fun capture(site: Site, delayMs: Long) = delegate.capture(site, delayMs)
                    override fun close() {
                        delegate.close()
                        openBrowsers.decrementAndGet()
                    }
                }
            },
        )
        val runner = AndroidCheckRunner(sites, engine, syncer, preferences)
        private var baselineId: String? = null
        private var baselineBytes = byteArrayOf()
        suspend fun start() {
            sites.saveSite(site, false, false)
            val initial = preview.capture(site.url, CaptureMode.TEXT, site.textRegion()) as CapturePreview.Html
            assertEquals(TEXT, initial.text)
            assertTrue(preview.keepAsFirstVersion(site, initial))
            baselineId = db.snapsDao().getAllSnapsForSiteId(site.id).single().snapId
            baselineBytes = files.readBytes(requireNotNull(baselineId))
            assertEquals(0, alerts.get())
        }
        suspend fun assertBaseline() {
            assertEquals(listOf(baselineId), db.snapsDao().getAllSnapsForSiteId(site.id).map { it.snapId })
            assertArrayEquals(baselineBytes, files.readBytes(requireNotNull(baselineId)))
            assertEquals("Failures must not notify", 0, alerts.get())
        }
        suspend fun unchanged(result: SyncResult) {
            assertTrue("Expected unchanged success, got $result", result is SyncResult.Success && !result.hadChanges)
            assertBaseline()
        }
        suspend fun changed(result: SyncResult) {
            assertTrue("Expected a real change, got $result", result is SyncResult.Success && result.hadChanges)
            assertEquals(2, db.snapsDao().getAllSnapsForSiteId(site.id).size)
            assertArrayEquals(baselineBytes, files.readBytes(requireNotNull(baselineId)))
            assertEquals(1, alerts.get())
        }
        suspend fun failedWithoutVersion(result: SyncResult) {
            assertTrue("Expected failure, got $result", result is SyncResult.Failed)
            assertBaseline()
        }
        suspend fun close() {
            server.close()
            db.snapsDao().getAllSnapsForSiteId(site.id).forEach { files.deleteFile(it.snapId) }
            client.close()
            db.close()
            context.deleteDatabase(name)
        }
    }

    private data class Response(val html: String, val status: Int = 200, val headers: Map<String, String> = emptyMap(), val charset: String = "utf-8")
    private class Server : AutoCloseable {
        private val socket = ServerSocket(0)
        private val executor = Executors.newCachedThreadPool()
        val requests = AtomicInteger()

        @Volatile var dropConnections = false
        val url = "http://127.0.0.1:${socket.localPort}/"

        @Volatile var response: (String, String) -> Response = { _, _ -> Response(PAGE) }
        init {
            executor.submit {
                while (!socket.isClosed) {
                    val client = runCatching { socket.accept() }.getOrNull() ?: break
                    executor.submit { respond(client) }
                }
            }
        }
        private fun respond(client: Socket) = client.use {
            val reader = it.getInputStream().bufferedReader()
            val path = reader.readLine()?.split(' ')?.getOrNull(1) ?: return@use
            var cookie = ""
            while (true) {
                val line = reader.readLine() ?: return@use
                if (line.isEmpty()) break
                if (line.startsWith("Cookie:", true)) cookie = line.substringAfter(':')
            }
            requests.incrementAndGet()
            if (dropConnections) return@use
            val result = response(path, cookie)
            val bytes = result.html.toByteArray(Charset.forName(result.charset))
            val head = "HTTP/1.1 ${result.status} Fixture\r\nContent-Type: text/html; charset=${result.charset}\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n" + result.headers.entries.joinToString("") { h -> "${h.key}: ${h.value}\r\n" } + "\r\n"
            it.getOutputStream().apply {
                write(head.toByteArray())
                write(bytes)
                flush()
            }
        }
        override fun close() {
            socket.close()
            executor.shutdownNow()
        }
    }
    private companion object {
        const val TEXT = "Ελληνικό θέατρο και εισιτήρια"
        const val PAGE = "<!doctype html><body><p id=target>$TEXT</p></body>"
    }
}
