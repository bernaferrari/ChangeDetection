package com.bernaferrari.changedetection.sync

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidScreenshotSyncerTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sync remains active until capture and persistence complete`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val captureStarted = CompletableDeferred<Unit>()
        val browser = FakeBrowser {
            captureStarted.complete(Unit)
            gate.await()
            ScreenshotCaptureOutcome.Success("image/png", PNG_BYTES)
        }
        val tracker = SyncProgressTracker()
        val engine = successfulEngine()
        val syncer = AndroidScreenshotSyncer(engine, dataStore(), tracker) { browser }
        val site = site("page-1")

        val result = async { syncer.syncSites(listOf(site)) }
        runCurrent()
        captureStarted.await()

        assertFalse("Work must await the browser", result.isCompleted)
        assertEquals(setOf(site.id), tracker.activeSiteIds.value)

        gate.complete(Unit)
        runCurrent()

        assertTrue(result.await().single() is SyncResult.Success)
        assertTrue(tracker.activeSiteIds.value.isEmpty())
        assertTrue(browser.closed)
        verify(engine).processFetchResult(
            argThat { id == site.id },
            argThat { this is SyncEngine.FetchResult.Success && capturedWithBrowser && data.contentEquals(PNG_BYTES) },
            any(),
        )
    }

    @Test
    fun `navigation failure is persisted as a failed check`() = runTest(dispatcher) {
        val browser = FakeBrowser {
            ScreenshotCaptureOutcome.Failure("navigation failed")
        }
        val engine = successfulEngine()
        val syncer = AndroidScreenshotSyncer(engine, dataStore(), SyncProgressTracker()) { browser }

        syncer.syncSites(listOf(site("page-2")))

        verify(engine).processFetchResult(
            any(),
            argThat { this is SyncEngine.FetchResult.Error && message == "navigation failed" },
            any(),
        )
        assertTrue(browser.closed)
    }

    @Test
    fun `cancellation is rethrown and browser is always closed`() = runTest(dispatcher) {
        val browser = FakeBrowser { throw CancellationException("cancelled") }
        val tracker = SyncProgressTracker()
        val syncer = AndroidScreenshotSyncer(successfulEngine(), dataStore(), tracker) { browser }

        val result = runCatching { syncer.syncSites(listOf(site("page-3"))) }

        assertTrue(result.exceptionOrNull() is CancellationException)
        assertTrue(tracker.activeSiteIds.value.isEmpty())
        assertTrue(browser.closed)
    }

    @Test
    fun `dead renderer is closed before the next page gets a fresh browser`() = runTest(dispatcher) {
        val dead = FakeBrowser { throw java.io.IOException("Renderer stopped") }
        val healthy = FakeBrowser { ScreenshotCaptureOutcome.Success("image/png", PNG_BYTES) }
        var created = 0
        val engine = successfulEngine()
        val syncer = AndroidScreenshotSyncer(engine, dataStore(), SyncProgressTracker()) {
            if (created++ == 0) {
                dead
            } else {
                assertTrue("Failed browser must be released before the next is allocated", dead.closed)
                healthy
            }
        }
        val results = syncer.syncSites(listOf(site("broken"), site("healthy")))
        assertEquals(2, created)
        assertEquals(2, results.size)
        assertTrue(healthy.closed)
        verify(engine).processFetchResult(argThat { id == "broken" }, argThat { this is SyncEngine.FetchResult.Error }, any())
        verify(engine).processFetchResult(argThat { id == "healthy" }, argThat { this is SyncEngine.FetchResult.Success }, any())
    }

    @Test
    fun `browser creation failure does not prevent later pages or allocate for paused pages`() = runTest(dispatcher) {
        val healthy = FakeBrowser { ScreenshotCaptureOutcome.Success("image/png", PNG_BYTES) }
        var created = 0
        val engine = successfulEngine()
        val syncer = AndroidScreenshotSyncer(engine, dataStore(), SyncProgressTracker()) {
            if (created++ == 0) throw IllegalStateException("Browser unavailable")
            healthy
        }
        val results = syncer.syncSites(listOf(site("paused").copy(isSyncEnabled = false), site("broken"), site("healthy")))
        assertEquals(2, created)
        assertTrue(results.first() is SyncResult.Skipped)
        assertTrue(healthy.closed)
        verify(engine).processFetchResult(argThat { id == "broken" }, argThat { this is SyncEngine.FetchResult.Error }, any())
        verify(engine).processFetchResult(argThat { id == "healthy" }, argThat { this is SyncEngine.FetchResult.Success }, any())
    }

    private fun successfulEngine(): SyncEngine {
        val engine = mock<SyncEngine>()
        runTest {
            whenever(engine.processFetchResult(any(), any(), any())).thenAnswer { invocation ->
                val site = invocation.getArgument<Site>(0)
                SyncResult.Success(site.id, hadChanges = false)
            }
        }
        return engine
    }

    private fun dataStore(): DataStore<Preferences> = mock<DataStore<Preferences>>().also {
        whenever(it.data).thenReturn(flowOf(emptyPreferences()))
    }

    private fun site(id: String) = Site(
        title = "Test page",
        url = "https://example.com",
        timestamp = 0L,
        id = id,
        isSuccessful = true,
        isBrowser = true,
        threshold = 0,
        isRead = false,
        isSyncEnabled = true,
        isNotificationEnabled = false,
        notes = "",
        colors = 0 to 0,
        constraints = ConstraintsRequired(false, false, false, false),
    )

    private class FakeBrowser(
        private val captureBlock: suspend () -> ScreenshotCaptureOutcome,
    ) : ScreenshotBrowser {
        var closed = false

        override suspend fun capture(site: Site, delayMs: Long): ScreenshotCaptureOutcome = captureBlock()

        override fun close() {
            closed = true
        }
    }

    private companion object {
        val PNG_BYTES = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)
    }
}
