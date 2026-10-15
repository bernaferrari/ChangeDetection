package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.ChangeDatabase
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.uuid.ExperimentalUuidApi

/**
 * Comprehensive unit tests for SyncEngine.
 *
 * Tests cover:
 * - Content fetching and processing
 * - Change detection logic
 * - Error handling and recovery
 * - Concurrency and mutex behavior
 * - Rollback on failures
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class SyncEngineTest {

    private lateinit var syncEngine: SyncEngine
    private lateinit var mockHttpClient: HttpClient
    private lateinit var mockDatabase: ChangeDatabase
    private lateinit var mockSitesDao: SitesDao
    private lateinit var mockSnapsDao: SnapsDao
    private lateinit var mockFileStorage: FileStorage
    private lateinit var mockChangeNotifier: ChangeNotifier
    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setup() {
        mockDatabase = mock()
        mockSitesDao = mock()
        mockSnapsDao = mock()
        mockFileStorage = mock()
        mockChangeNotifier = mock()
        errorHandler = ErrorHandler()

        // Will be overridden in specific tests
        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respondOk("<html><body>Test content</body></html>")
                }
            }
        }
    }

    // ========== Content Fetching Tests ==========

    @Test
    fun `oversized stream is rejected before the server closes its response`() = runTest {
        val channel = ByteChannel(autoFlush = true)
        val producer = backgroundScope.launch(Dispatchers.Default) {
            val chunk = ByteArray(8192) { 65 }
            try {
                repeat(1500) { channel.writeFully(chunk) }
                // The server never sends EOF. A buffering GET would wait here rather than enforce the cap.
                awaitCancellation()
            } catch (_: ClosedByteChannelException) {
                // Rejecting an oversized response closes the reader while the server is still writing.
            }
        }
        mockHttpClient = HttpClient(MockEngine { respond(channel, headers = headersOf(HttpHeaders.ContentType, "application/octet-stream")) })
        syncEngine = makeSyncEngine()
        try {
            val result = withContext(Dispatchers.Default) {
                withTimeout(5000) { syncEngine.fetchFromServer(createTestSite()) }
            }
            assertTrue(result is SyncEngine.FetchResult.Error)
            assertEquals(SyncError.Reason.CONTENT_TOO_LARGE, ((result as SyncEngine.FetchResult.Error).throwable as SyncError).reason)
        } finally {
            channel.cancel()
            producer.cancelAndJoin()
            mockHttpClient.close()
        }
    }

    @Test
    fun `fetchFromServer returns success for valid HTML response`() = runTest {
        // Arrange
        val site = createTestSite()
        val htmlContent = "<html><body>Hello World</body></html>"

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = ByteReadChannel(htmlContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            }
        }

        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.fetchFromServer(site)

        // Assert
        assertTrue(result is SyncEngine.FetchResult.Success)
        val success = result as SyncEngine.FetchResult.Success
        assertTrue(success.contentType.contains("html", ignoreCase = true))
        assertEquals(htmlContent, success.data.decodeToString())
    }

    @Test
    fun `fetchFromServer handles HTTP 404 error correctly`() = runTest {
        // Arrange
        val site = createTestSite()

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respondError(HttpStatusCode.NotFound)
                }
            }
        }

        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.fetchFromServer(site)

        // Assert
        assertTrue(result is SyncEngine.FetchResult.Error)
        val error = result as SyncEngine.FetchResult.Error
        assertTrue(error.message.contains("404"))
        assertNotNull(error.errorResult)
        assertEquals(ErrorType.NETWORK, error.errorResult?.type)
    }

    @Test
    fun `fetchFromServer handles HTTP 403 forbidden correctly`() = runTest {
        // Arrange
        val site = createTestSite()

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respondError(HttpStatusCode.Forbidden)
                }
            }
        }

        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.fetchFromServer(site)

        // Assert
        assertTrue(result is SyncEngine.FetchResult.Error)
        val error = result as SyncEngine.FetchResult.Error
        assertEquals(ErrorType.NETWORK, error.errorResult?.type)
        assertEquals(RecoveryAction.CHECK_URL, error.errorResult?.recoveryAction)
    }

    @Test
    fun `fetchFromServer handles HTTP 429 rate limiting correctly`() = runTest {
        // Arrange
        val site = createTestSite()

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respondError(HttpStatusCode.TooManyRequests)
                }
            }
        }

        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.fetchFromServer(site)

        // Assert
        assertTrue(result is SyncEngine.FetchResult.Error)
        val error = result as SyncEngine.FetchResult.Error
        assertEquals(ErrorType.RATE_LIMITED, error.errorResult?.type)
        assertTrue(error.errorResult?.isRetryable == true)
    }

    // ========== Change Detection Tests ==========

    @Test
    fun `syncSite returns Skipped when site not found`() = runTest {
        // Arrange
        whenever(mockSitesDao.getSiteById(any())).thenReturn(null)
        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.syncSite("non-existent-site")

        // Assert
        assertTrue(result is SyncResult.Skipped)
        val skipped = result as SyncResult.Skipped
        assertEquals("Site not found", skipped.reason)
    }

    @Test
    fun `syncSite returns Skipped when sync is disabled`() = runTest {
        // Arrange
        val site = createTestSite(isSyncEnabled = false)
        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Skipped)
        val skipped = result as SyncResult.Skipped
        assertEquals("Sync disabled", skipped.reason)
    }

    @Test
    fun `syncSite creates baseline without reporting or notifying a change`() = runTest {
        // Arrange
        val site = createTestSite()
        val htmlContent = "<html><body>New content</body></html>"

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = ByteReadChannel(htmlContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            }
        }

        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(any())).thenReturn(null) // No previous snap

        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertFalse(success.hadChanges)
        assertNotNull(success.snapId)

        // Verify interactions
        verify(mockFileStorage).writeBytes(any(), any())
        verify(mockSnapsDao).insertSnap(any())
        verify(mockSitesDao).updateSyncMetadata(any(), any(), any(), any(), any(), anyOrNull())
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `syncSite does not save when content unchanged`() = runTest {
        // Arrange
        val site = createTestSite()
        val htmlContent = "<html><body>Same content</body></html>"
        val contentBytes = htmlContent.toByteArray()

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = ByteReadChannel(htmlContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            }
        }

        val previousSnap = createTestSnap(site.id).copy(contentHash = ContentHasher.hashHtmlContent(contentBytes))

        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(any())).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(any())).thenReturn(contentBytes) // Same content
        whenever(mockFileStorage.fileExists(any())).thenReturn(true) // Snap file present

        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertFalse(success.hadChanges)
        assertNull(success.snapId)
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `syncSite notifies exactly once after a changed snapshot is saved`() = runTest {
        val site = createTestSite()
        val previousContent = "<html><body>Old content</body></html>".toByteArray()
        val changedContent = "<html><body>Meaningfully changed content</body></html>"
        val previousSnap = createTestSnap(site.id).copy(contentSize = previousContent.size)

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    respond(
                        content = ByteReadChannel(changedContent),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/html"),
                    )
                }
            }
        }
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(previousSnap.snapId)).thenReturn(previousContent)
        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        val result = syncEngine.syncSite(site.id)

        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertTrue(success.hadChanges)
        verify(mockSnapsDao).insertSnap(
            check { snap -> assertEquals(success.snapId, snap.snapId) },
        )
        verify(mockChangeNotifier, times(1)).notifyChange(site, requireNotNull(success.snapId))
        verifyNoMoreInteractions(mockChangeNotifier)
    }

    @Test
    fun `zero threshold detects same-size content changes`() = runTest {
        val site = createTestSite()
        val previousContent = "<html><body>AAAA</body></html>".toByteArray()
        val newContent = "<html><body>BBBB</body></html>".toByteArray()
        val previousSnap = createTestSnap(site.id).copy(contentSize = previousContent.size)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(previousSnap.snapId)).thenReturn(previousContent)
        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val result = syncEngine.processFetchResult(
            site = site,
            result = SyncEngine.FetchResult.Success("text/html", newContent),
            threshold = 0,
        )

        assertTrue(result is SyncResult.Success && result.hadChanges)
        verify(mockChangeNotifier).notifyChange(eq(site), any())
    }

    @Test
    fun `syncSite does not notify when site alerts are disabled`() = runTest {
        val site = createTestSite(isNotificationEnabled = false)
        val previousContent = "<html><body>Before</body></html>".toByteArray()
        val previousSnap = createTestSnap(site.id).copy(contentSize = previousContent.size)
        val result = SyncEngine.FetchResult.Success(
            contentType = "text/html",
            data = "<html><body>After, with a substantial update</body></html>".toByteArray(),
        )
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(previousSnap.snapId)).thenReturn(previousContent)
        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val syncResult = syncEngine.processFetchResult(site, result)

        assertTrue(syncResult is SyncResult.Success && syncResult.hadChanges)
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `notification failure does not undo a saved change`() = runTest {
        val site = createTestSite()
        val previousContent = "<html><body>Before</body></html>".toByteArray()
        val previousSnap = createTestSnap(site.id).copy(contentSize = previousContent.size)
        val result = SyncEngine.FetchResult.Success(
            contentType = "text/html",
            data = "<html><body>After, with a substantial update</body></html>".toByteArray(),
        )
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(previousSnap.snapId)).thenReturn(previousContent)
        doThrow(IllegalStateException("notifications unavailable"))
            .whenever(mockChangeNotifier)
            .notifyChange(any(), any())
        syncEngine = makeSyncEngine(notifier = mockChangeNotifier)

        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val syncResult = syncEngine.processFetchResult(site, result)

        assertTrue(syncResult is SyncResult.Success && syncResult.hadChanges)
        verify(mockSnapsDao).insertSnap(any())
        verify(mockFileStorage, never()).deleteFile(any())
    }

    @Test
    fun `positive threshold detects replacements of the same byte length`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val previous = "AAAA".toByteArray()
        val snap = createTestSnap(site.id).copy(contentHash = ContentHasher.hashContent(previous), contentSize = 4)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(snap)
        whenever(mockFileStorage.readBytes(snap.snapId)).thenReturn(previous)
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/plain", "BBBB".toByteArray()), 3)
        assertTrue(result is SyncResult.Success && result.hadChanges)
    }

    @Test
    fun `region capture saves only selected markup and suppresses outside changes`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT, textIncludeSelector = "#price", textExcludeSelector = ".timer")
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        var latest: Snap? = null
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenAnswer { latest }
        doAnswer {
            latest = it.arguments[0] as Snap
            Unit
        }.whenever(mockSnapsDao).insertSnap(any())
        syncEngine = makeSyncEngine()
        val first = "<nav>Ad A</nav><div id='price'>100<span class='timer'>10s</span></div>"
        val baseline = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", first.toByteArray()))
        assertTrue(baseline is SyncResult.Success && !baseline.hadChanges)
        val bytes = argumentCaptor<ByteArray>()
        verify(mockFileStorage).writeBytes(any(), bytes.capture())
        assertEquals("100", com.bernaferrari.changedetection.util.readableSnapshotText(bytes.firstValue.decodeToString(), true))
        assertFalse(bytes.firstValue.decodeToString().contains("timer"))
        assertFalse(bytes.firstValue.decodeToString().contains("Ad A"))
        val unchanged = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", first.replace("Ad A", "Ad B").replace("10s", "9s").toByteArray()))
        assertTrue(unchanged is SyncResult.Success && !unchanged.hadChanges)
        val changed = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", first.replace("100", "200").toByteArray()))
        assertTrue(changed is SyncResult.Success && changed.hadChanges)
        verify(mockSnapsDao, times(2)).insertSnap(any())
    }

    @Test
    fun `missing selected region reports durable failure without erasing history`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT, textIncludeSelector = "#missing")
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "<p>Other page</p>".toByteArray()))
        assertTrue(result is SyncResult.Failed)
        verify(mockSitesDao).updateSyncMetadata(eq(site.id), any(), eq(false), any(), isNull(), eq("REGION"))
        // Reading baseline metadata is allowed; failed extraction must never write history or touch its files.
        verify(mockSnapsDao).getLastSnapForSiteId(site.id)
        verifyNoMoreInteractions(mockSnapsDao)
        verifyNoInteractions(mockFileStorage, mockChangeNotifier)
    }

    @Test
    fun `changing region invalidates an in flight capture`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT, textIncludeSelector = "#price")
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site.copy(textIncludeSelector = "#stock"))
        syncEngine = makeSyncEngine()
        assertTrue(syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "<p id='price'>100</p>".toByteArray())) is SyncResult.Skipped)
        verifyNoInteractions(mockSnapsDao, mockFileStorage, mockChangeNotifier)
    }

    @Test
    fun `readable text ignores markup without reading or replacing the saved capture`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val before = "<p class='old'>Price 100</p>".toByteArray()
        val snap = createTestSnap(site.id).copy(contentType = "text/html", contentHash = ContentHasher.detectionHash(before, "text/html", site.captureMode))
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(snap)
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "<p class='new'>Price 100</p>".toByteArray()))
        assertTrue(result is SyncResult.Success && !result.hadChanges)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockFileStorage, never()).readBytes(any())
    }

    @Test
    fun `new detection policy establishes baseline without an alert or history deletion`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT)
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id).copy(contentType = "text/html", contentHash = "legacy-hash"))
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "<p>Current price</p>".toByteArray()))
        assertTrue(result is SyncResult.Success && !result.hadChanges && result.snapId != null)
        verify(mockSnapsDao).insertSnap(argThat { contentHash.startsWith("text-v1:") })
        verifyNoInteractions(mockChangeNotifier)
        verify(mockFileStorage, never()).deleteFile(any())
    }

    @Test
    fun `corrected region extraction establishes a baseline without alerting or deleting history`() = runTest {
        val site = createTestSite().copy(
            captureMode = com.bernaferrari.changedetection.repo.CaptureMode.TEXT,
            textIncludeSelector = "main > p:nth-child(2)",
            textExcludeSelector = "#noise",
        )
        val page = "<main><p id='noise'>Ad</p><p>Watched stock</p><p>Other product</p></main>".toByteArray()
        val corrected = com.bernaferrari.changedetection.detection.prepareTextCapture(page, "text/html", site.textRegion())
        val oldHash = corrected.contentHash.replace("text-region-v2:", "text-region-v1:").substringBeforeLast(':') + ":old-wrong-content"
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id).copy(contentType = "text/html", contentHash = oldHash))
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", page))
        assertTrue(result is SyncResult.Success && !result.hadChanges && result.snapId != null)
        verify(mockSnapsDao).insertSnap(argThat { contentHash == corrected.contentHash })
        verifyNoInteractions(mockChangeNotifier)
        verify(mockFileStorage, never()).deleteFile(any())
    }

    @Test
    fun `explicit source rule cannot be suppressed by an old byte threshold`() = runTest {
        val site = createTestSite().copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.SOURCE, threshold = 100_000)
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        val before = "<script>v=1</script>".toByteArray()
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id).copy(contentType = "text/html", contentHash = ContentHasher.detectionHash(before, "text/html", site.captureMode)))
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "<script>v=2</script>".toByteArray()), site.threshold)
        assertTrue(result is SyncResult.Success && result.hadChanges)
        verify(mockSnapsDao).insertSnap(any())
    }

    @Test
    fun `empty response records a failure without creating a healthy baseline`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        syncEngine = makeSyncEngine()
        assertTrue(syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", byteArrayOf())) is SyncResult.Failed)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockSitesDao).updateSyncMetadata(eq(site.id), any(), eq(false), any(), isNull(), anyOrNull())
    }

    @Test
    fun `removing a threshold invalidates a capture started with the old filter`() = runTest {
        val original = createTestSite().copy(threshold = 512)
        whenever(mockSitesDao.getSiteById(original.id)).thenReturn(original.copy(threshold = 0))
        syncEngine = makeSyncEngine()
        val result = syncEngine.processFetchResult(original, SyncEngine.FetchResult.Success("text/html", "Changed price".toByteArray()), original.threshold)
        assertTrue(result is SyncResult.Skipped)
        verifyNoInteractions(mockSnapsDao, mockFileStorage, mockChangeNotifier)
    }

    @Test
    fun `capture of an edited page cannot become its new baseline`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site.copy(url = "https://example.org/new"))
        syncEngine = makeSyncEngine()
        assertTrue(syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "old page".toByteArray())) is SyncResult.Skipped)
        verifyNoInteractions(mockSnapsDao, mockFileStorage, mockChangeNotifier)
    }

    // ========== Concurrency Tests ==========

    @Test
    fun `trySyncSite returns Skipped when already syncing`() = runTest {
        // Arrange
        val site = createTestSite()
        var callCount = 0

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    callCount++
                    Thread.sleep(100) // Simulate slow network
                    respondOk("<html>Slow content</html>")
                }
            }
        }

        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(any())).thenReturn(null)

        syncEngine = makeSyncEngine()

        // Act: Start two concurrent syncs
        val results = listOf(
            async(Dispatchers.Default) { syncEngine.trySyncSite(site.id) },
            async(Dispatchers.Default) { syncEngine.trySyncSite(site.id) },
        ).awaitAll()

        // Assert: One should be skipped
        val skippedCount = results.count { it is SyncResult.Skipped }
        assertTrue("At least one sync should be skipped", skippedCount >= 1)
    }

    // ========== Rollback Tests ==========

    @Test
    fun `switching from screenshot to page content saves a baseline without an alert`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id).copy(contentType = "image/png"))
        syncEngine = makeSyncEngine()

        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "new content baseline".toByteArray()))

        assertTrue(result is SyncResult.Success)
        assertFalse((result as SyncResult.Success).hadChanges)
        verify(mockSnapsDao).insertSnap(any())
        verify(mockSnapsDao, never()).deleteSnapById(any())
        verify(mockSitesDao).updateSyncMetadata(eq(site.id), any(), eq(true), eq(false), isNull(), anyOrNull())
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `switching from content to screenshot starts a baseline even with an equal hash`() = runTest {
        val site = createTestSite()
        val bytes = byteArrayOf(1, 2, 3)
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(
            createTestSnap(site.id).copy(
                contentHash = com.bernaferrari.changedetection.util.ContentHasher.hashContent(bytes),
            ),
        )
        syncEngine = makeSyncEngine()

        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", bytes))

        assertTrue(result is SyncResult.Success)
        assertNotNull((result as SyncResult.Success).snapId)
        assertFalse(result.hadChanges)
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `changing capture mode during write rejects the previous mode result`() = runTest {
        val site = createTestSite()
        var current = site
        whenever(mockSitesDao.getSiteById(site.id)).thenAnswer { current }
        doAnswer {
            current = site.copy(captureMode = com.bernaferrari.changedetection.repo.CaptureMode.HTML)
            null
        }
            .whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()

        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("image/png", byteArrayOf(1, 2, 3)))

        assertTrue(result is SyncResult.Skipped)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockFileStorage).deleteFile(any())
    }

    @Test
    fun `low storage reports an actionable failure without replacing history`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id))
        doThrow(SnapshotStorageError(lowSpace = true)).whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()

        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "new content".toByteArray()))

        assertTrue(result is SyncResult.Failed)
        assertEquals("Not enough storage", (result as SyncResult.Failed).error.userMessage)
        assertFalse(result.error.isRetryable)
        verify(mockSitesDao).updateSyncMetadata(eq(site.id), any(), eq(false), any(), isNull(), anyOrNull())
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockSnapsDao, never()).deleteSnapById(any())
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `fresh start during unchanged comparison stays waiting for its new baseline`() = runTest {
        val site = createTestSite()
        val content = "same page".toByteArray()
        val previous = createTestSnap(site.id).copy(contentHash = com.bernaferrari.changedetection.util.ContentHasher.hashContent(content))
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(previous, null)
        syncEngine = makeSyncEngine()

        assertTrue(syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/plain", content)) is SyncResult.Skipped)
        verify(mockSitesDao, never()).updateSyncMetadata(any(), any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `stop during write cancels the check and removes the uncommitted file`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        lateinit var check: kotlinx.coroutines.Deferred<SyncResult>
        doAnswer {
            check.cancel()
            null
        }.whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()
        check = async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "page".toByteArray()))
        }
        check.start()
        check.join()

        assertTrue(check.isCancelled)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockFileStorage).deleteFile(any())
    }

    @Test
    fun `stop during commit never removes committed content`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        lateinit var check: kotlinx.coroutines.Deferred<SyncResult>
        whenever(mockSnapsDao.insertSnap(any())).thenAnswer {
            check.cancel()
            Unit
        }
        syncEngine = makeSyncEngine()
        check = async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "page".toByteArray()))
        }
        check.start()
        check.join()

        assertTrue(check.isCancelled)
        verify(mockSitesDao).updateSyncMetadata(any(), any(), eq(true), any(), eq(true), anyOrNull())
        verify(mockFileStorage, never()).deleteFile(any())
        verify(mockSnapsDao, never()).deleteSnapById(any())
    }

    @Test
    fun `editing a URL during file write cannot attach old content to the new page`() = runTest {
        val site = createTestSite()
        var current = site
        whenever(mockSitesDao.getSiteById(site.id)).thenAnswer { current }
        doAnswer {
            current = site.copy(url = "https://example.com/edited")
            null
        }
            .whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()

        val result = syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "old page".toByteArray()))

        assertTrue(result is SyncResult.Skipped)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockSitesDao, never()).updateSyncMetadata(any(), any(), any(), any(), any(), anyOrNull())
        verify(mockFileStorage).deleteFile(any())
    }

    @Test
    fun `cancelled partial write is cleaned up without committing a snapshot`() = runTest {
        val site = createTestSite()
        whenever(mockSitesDao.getSiteById(site.id)).thenReturn(site)
        doThrow(kotlinx.coroutines.CancellationException("Stopped during write"))
            .whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()

        val error = runCatching {
            syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "page".toByteArray()))
        }.exceptionOrNull()

        assertTrue(error is kotlinx.coroutines.CancellationException)
        verify(mockSnapsDao, never()).insertSnap(any())
        verify(mockFileStorage).deleteFile(any())
    }

    @Test
    fun `notification uses preference saved while content was being written`() = runTest {
        val site = createTestSite()
        var current = site
        whenever(mockSitesDao.getSiteById(site.id)).thenAnswer { current }
        whenever(mockSnapsDao.getLastSnapForSiteId(site.id)).thenReturn(createTestSnap(site.id))
        doAnswer {
            current = site.copy(isNotificationEnabled = false)
            null
        }
            .whenever(mockFileStorage).writeBytes(any(), any())
        syncEngine = makeSyncEngine()

        assertTrue(syncEngine.processFetchResult(site, SyncEngine.FetchResult.Success("text/html", "new page".toByteArray())) is SyncResult.Success)
        verify(mockSnapsDao).insertSnap(any())
        verifyNoInteractions(mockChangeNotifier)
    }

    @Test
    fun `syncSite rolls back file on database failure`() = runTest {
        // Arrange
        val site = createTestSite()

        mockHttpClient = HttpClient(MockEngine) {
            engine {
                addHandler { respondOk("<html>Content</html>") }
            }
        }

        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(any())).thenReturn(null)
        whenever(mockSnapsDao.insertSnap(any())).thenThrow(RuntimeException("Database error"))

        syncEngine = makeSyncEngine()

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Failed)

        // Verify rollback was attempted
        verify(mockFileStorage).deleteFile(any())
    }

    // ========== Helper Methods ==========

    /** Builds the engine under test with a no-op write transaction (Room's
     *  withWriteTransaction extension can't be stubbed on a mocked database). */
    private fun makeSyncEngine(notifier: ChangeNotifier = mockChangeNotifier): SyncEngine = object : SyncEngine(
        mockHttpClient,
        mockDatabase,
        mockSitesDao,
        mockSnapsDao,
        mockFileStorage,
        errorHandler,
        notifier,
    ) {
        override suspend fun <T> inWriteTransaction(block: suspend () -> T): T = block()
    }

    private fun createTestSite(
        id: String = "test-site-id",
        url: String = "https://example.com",
        isSyncEnabled: Boolean = true,
        isNotificationEnabled: Boolean = true,
    ): Site = Site(
        id = id,
        title = "Test Site",
        url = url,
        timestamp = System.currentTimeMillis(),
        isSuccessful = true,
        isSyncEnabled = isSyncEnabled,
        isBrowser = false,
        threshold = 0,
        isRead = false,
        isNotificationEnabled = isNotificationEnabled,
        notes = "",
        colors = com.bernaferrari.changedetection.repo.ColorGroup(0xFF6200EE.toInt(), 0xFF03DAC5.toInt()),
        constraints = com.bernaferrari.changedetection.repo.ConstraintsRequired(
            wifi = false,
            charging = false,
            batteryNotLow = false,
            deviceIdle = false,
        ),
    )

    private fun createTestSnap(
        siteId: String,
        snapId: String = "test-snap-id",
    ): Snap = Snap(
        snapId = snapId,
        siteId = siteId,
        timestamp = System.currentTimeMillis(),
        contentType = "text/html",
        contentCharset = "UTF-8",
        contentSize = 1024,
        contentHash = "test-hash",
        processedContentHash = "test-hash",
    )
}
