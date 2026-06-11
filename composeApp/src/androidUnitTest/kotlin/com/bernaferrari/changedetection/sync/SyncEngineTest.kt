/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.sync

import com.bernaferrari.changedetection.error.*
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.util.ContentHasher
import com.bernaferrari.changedetection.util.FileStorage
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
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
    private lateinit var mockSitesDao: SitesDao
    private lateinit var mockSnapsDao: SnapsDao
    private lateinit var mockFileStorage: FileStorage
    private lateinit var errorHandler: ErrorHandler

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        mockSitesDao = mock()
        mockSnapsDao = mock()
        mockFileStorage = mock()
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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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
        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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
        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Skipped)
        val skipped = result as SyncResult.Skipped
        assertEquals("Sync disabled", skipped.reason)
    }

    @Test
    fun `syncSite saves new snap when content changes`() = runTest {
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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertTrue(success.hadChanges)
        assertNotNull(success.snapId)

        // Verify interactions
        verify(mockFileStorage).writeBytes(any(), any())
        verify(mockSnapsDao).insertSnap(any())
        verify(mockSitesDao).updateSite(any())
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

        val previousSnap = createTestSnap(site.id)

        whenever(mockSitesDao.getSiteById(any())).thenReturn(site)
        whenever(mockSnapsDao.getLastSnapForSiteId(any())).thenReturn(previousSnap)
        whenever(mockFileStorage.readBytes(any())).thenReturn(contentBytes) // Same content

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Success)
        val success = result as SyncResult.Success
        assertFalse(success.hadChanges)
        assertNull(success.snapId)
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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

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

        syncEngine = SyncEngine(mockHttpClient, mockSitesDao, mockSnapsDao, mockFileStorage, errorHandler)

        // Act
        val result = syncEngine.syncSite(site.id)

        // Assert
        assertTrue(result is SyncResult.Failed)

        // Verify rollback was attempted
        verify(mockFileStorage).deleteFile(any())
    }

    // ========== Helper Methods ==========

    private fun createTestSite(
        id: String = "test-site-id",
        url: String = "https://example.com",
        isSyncEnabled: Boolean = true,
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
        isNotificationEnabled = true,
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
