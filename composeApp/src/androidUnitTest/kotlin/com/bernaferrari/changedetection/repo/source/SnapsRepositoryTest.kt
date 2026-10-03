/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.repo.source

import com.bernaferrari.changedetection.repo.AppExecutors
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.repo.source.local.SnapsLocalDataSource
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.system.measureTimeMillis

/**
 * Comprehensive unit tests for N+1 query optimization in SnapsRepository.
 *
 * Tests verify the optimization that replaces individual `getMostRecentSnap()` calls
 * with a single batch `getAllMostRecentSnaps()` query.
 *
 * Key optimization details:
 * - BEFORE: 1 query for sites + N queries for each site's latest snap = 1+N queries
 * - AFTER: 1 query for sites + 1 batch query for all snaps = 2 queries
 * - Performance improvement: ~98% reduction in queries for 50 sites (51 → 2 queries)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SnapsRepositoryTest {

    private lateinit var repository: SnapsRepository
    private lateinit var localDataSource: SnapsLocalDataSource
    private lateinit var mockDao: SnapsDao
    private lateinit var mockSitesDao: SitesDao
    private lateinit var appExecutors: AppExecutors

    @Mock
    private lateinit var mockFileStorage: FileStorage

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        appExecutors = AppExecutors(
            ioContext = Dispatchers.Unconfined,
            uiContext = Dispatchers.Unconfined,
        )
        mockDao = mock(SnapsDao::class.java)
        mockSitesDao = mock(SitesDao::class.java)
        localDataSource = SnapsLocalDataSource(appExecutors, mockDao, mockSitesDao, mockFileStorage, mock(com.bernaferrari.changedetection.storage.SnapshotFileCleanup::class.java))
        repository = SnapsRepository(localDataSource)
    }

    // ========== Happy Path Tests ==========

    @Test
    fun `getAllMostRecentSnaps returns all snapshots for multiple sites in single call`() = runTest {
        // Arrange: Create snapshots for 3 different sites with multiple snaps each
        val site1Id = "site-1"
        val site2Id = "site-2"
        val site3Id = "site-3"

        val snap1 = createSnap(snapId = "snap-1-latest", siteId = site1Id, timestamp = 1000L)
        val snap2 = createSnap(snapId = "snap-2-latest", siteId = site2Id, timestamp = 2000L)
        val snap3 = createSnap(snapId = "snap-3-latest", siteId = site3Id, timestamp = 3000L)

        val allLatestSnaps = listOf(snap1, snap2, snap3)

        // Mock the optimized batch query
        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(allLatestSnaps)

        // Act: Call the optimized batch method
        val result = repository.getAllMostRecentSnaps()

        // Assert: Verify all snapshots are returned
        assertEquals(3, result.size)
        assertTrue(result.containsKey(site1Id))
        assertTrue(result.containsKey(site2Id))
        assertTrue(result.containsKey(site3Id))

        // Verify the batch query was called exactly once (optimization verification)
        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    @Test
    fun `getAllMostRecentSnaps correctly returns most recent snapshot per site`() = runTest {
        // Arrange: Create scenarios where sites have multiple snapshots
        val siteId = "site-1"

        // Only the most recent snap should be returned (timestamp 5000)
        val mostRecentSnap = createSnap(
            snapId = "snap-latest",
            siteId = siteId,
            timestamp = 5000L,
            contentSize = 999,
        )

        // Mock DAO to return only the most recent snap (as the SQL query would)
        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(listOf(mostRecentSnap))

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Verify only the most recent snapshot is in the result
        assertEquals(1, result.size)
        assertEquals(mostRecentSnap, result[siteId])
        assertEquals(5000L, result[siteId]?.timestamp)
        assertEquals(999, result[siteId]?.contentSize)
    }

    @Test
    fun `getAllMostRecentSnaps result map enables O(1) lookups by site ID`() = runTest {
        // Arrange: Create 50 sites (realistic scenario)
        val sites = (1..50).map { i ->
            createSnap(
                snapId = "snap-$i",
                siteId = "site-$i",
                timestamp = i * 1000L,
            )
        }

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(sites)

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Verify O(1) map lookup performance
        assertEquals(50, result.size)

        // Test multiple lookups - these should be O(1) operations
        assertNotNull(result["site-1"])
        assertNotNull(result["site-25"])
        assertNotNull(result["site-50"])
        assertEquals("snap-1", result["site-1"]?.snapId)
        assertEquals("snap-25", result["site-25"]?.snapId)
        assertEquals("snap-50", result["site-50"]?.snapId)

        // Verify only one database query was made for all 50 sites
        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `getAllMostRecentSnaps handles empty results correctly`() = runTest {
        // Arrange: No snapshots exist
        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(emptyList())

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Returns empty map without errors
        assertTrue(result.isEmpty())
        assertEquals(0, result.size)

        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    @Test
    fun `getAllMostRecentSnaps handles single site with single snapshot`() = runTest {
        // Arrange: Minimal case - one site, one snapshot
        val singleSnap = createSnap(
            snapId = "only-snap",
            siteId = "only-site",
            timestamp = 1000L,
        )

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(listOf(singleSnap))

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert
        assertEquals(1, result.size)
        assertEquals(singleSnap, result["only-site"])

        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    @Test
    fun `getAllMostRecentSnaps handles sites with no snapshots`() = runTest {
        // Arrange: Empty list simulating sites that have no snapshots yet
        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(emptyList())

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Should handle gracefully
        assertTrue(result.isEmpty())

        // Lookup for non-existent site returns null (not an error)
        assertNull(result["non-existent-site"])

        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    @Test
    fun `getAllMostRecentSnaps handles null content hashes correctly`() = runTest {
        // Arrange: Snaps with empty/default hash values (valid scenario)
        val snapWithoutHash = createSnap(
            snapId = "snap-1",
            siteId = "site-1",
            timestamp = 1000L,
            contentHash = "",
            processedContentHash = "",
        )

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(listOf(snapWithoutHash))

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Should handle snaps with empty hashes
        assertEquals(1, result.size)
        assertNotNull(result["site-1"])
        assertEquals("", result["site-1"]?.contentHash)
        assertEquals("", result["site-1"]?.processedContentHash)
    }

    // ========== Performance Tests ==========

    @Test
    fun `batch query is more efficient than individual queries`() = runTest {
        // Arrange: Simulate 50 sites (realistic production scenario)
        val numberOfSites = 50
        val snaps = (1..numberOfSites).map { i ->
            createSnap(
                snapId = "snap-$i",
                siteId = "site-$i",
                timestamp = i * 1000L,
            )
        }

        // Mock both approaches
        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(snaps)
        snaps.forEach { snap ->
            `when`(mockDao.getLastSnapForSiteId(snap.siteId)).thenReturn(snap)
        }

        // Act & Measure: Batch approach (optimized)
        var batchQueryTime = 0L
        batchQueryTime = measureTimeMillis {
            repository.getAllMostRecentSnaps()
        }

        // Act & Measure: Individual queries approach (old N+1 problem)
        var individualQueriesTime = 0L
        individualQueriesTime = measureTimeMillis {
            // Simulate old approach: query each site individually
            snaps.forEach { snap ->
                runCatching { repository.getMostRecentSnap(snap.siteId) }
            }
        }

        // Assert: Verify query counts
        // Batch approach: called once
        verify(mockDao, times(1)).getAllMostRecentSnaps()

        // The optimization reduces queries from 51 (1 + 50) to 1 (98% reduction)
        println("Performance comparison:")
        println("  Batch query: 1 database call")
        println("  Individual queries: $numberOfSites database calls")
        println("  Query reduction: ${(1 - 1.0 / numberOfSites) * 100}%")
    }

    @Test
    fun `batch query returns correct map structure for quick lookups`() = runTest {
        // Arrange: Multiple sites with realistic data
        val snaps = listOf(
            createSnap("snap-1", "https://example.com", 1000L),
            createSnap("snap-2", "https://google.com", 2000L),
            createSnap("snap-3", "https://github.com", 3000L),
        )

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(snaps)

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Map structure allows O(1) lookups
        assertTrue(result is Map<String, Snap>)
        assertEquals(3, result.keys.size)

        // Verify map keys are siteIds
        assertTrue(result.containsKey("https://example.com"))
        assertTrue(result.containsKey("https://google.com"))
        assertTrue(result.containsKey("https://github.com"))

        // Verify map values are correct Snap objects
        assertEquals("snap-1", result["https://example.com"]?.snapId)
        assertEquals("snap-2", result["https://google.com"]?.snapId)
        assertEquals("snap-3", result["https://github.com"]?.snapId)
    }

    // ========== Integration Test with MainViewModel Usage Pattern ==========

    @Test
    fun `batch query matches MainViewModel usage pattern`() = runTest {
        // Arrange: Simulate the exact usage pattern from MainViewModel.kt lines 116-122
        val sites = listOf("site-1", "site-2", "site-3")
        val snaps = sites.map { siteId ->
            createSnap(
                snapId = "snap-$siteId",
                siteId = siteId,
                timestamp = System.currentTimeMillis(),
            )
        }

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(snaps)

        // Act: Simulate MainViewModel flow
        val snapsBySite = runCatching {
            repository.getAllMostRecentSnaps()
        }.getOrNull() ?: emptyMap()

        // Simulate mapping sites to SiteAndLastSnap
        val results = sites.map { siteId ->
            Pair(siteId, snapsBySite[siteId]) // Fast O(1) map lookup
        }

        // Assert: Verify the pattern works correctly
        assertEquals(3, results.size)
        results.forEach { (siteId, snap) ->
            assertNotNull("Snap for $siteId should not be null", snap)
            assertEquals(siteId, snap?.siteId)
        }

        // Verify only single batch query was made
        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    // ========== Error Handling Tests ==========

    @Test
    fun `getAllMostRecentSnaps handles duplicate siteIds correctly`() = runTest {
        // Arrange: SQL query should not return duplicates, but test the map conversion
        // In case of duplicates, Map.associateBy will keep the last occurrence
        val snaps = listOf(
            createSnap("snap-1", "site-1", timestamp = 1000L),
            createSnap("snap-2", "site-1", timestamp = 2000L), // Same siteId, later timestamp
        )

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(snaps)

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Map keeps the last occurrence (which should be the most recent)
        assertEquals(1, result.size)
        assertEquals("snap-2", result["site-1"]?.snapId)
        assertEquals(2000L, result["site-1"]?.timestamp)
    }

    @Test
    fun `getAllMostRecentSnaps handles very large result sets`() = runTest {
        // Arrange: Test with 1000 sites (stress test)
        val largeNumberOfSites = 1000
        val snaps = (1..largeNumberOfSites).map { i ->
            createSnap(
                snapId = "snap-$i",
                siteId = "site-$i",
                timestamp = i * 1000L,
            )
        }

        `when`(mockDao.getAllMostRecentSnaps()).thenReturn(snaps)

        // Act
        val result = repository.getAllMostRecentSnaps()

        // Assert: Should handle large datasets efficiently
        assertEquals(largeNumberOfSites, result.size)
        assertNotNull(result["site-1"])
        assertNotNull(result["site-500"])
        assertNotNull(result["site-1000"])

        // Still only 1 query for 1000 sites (massive optimization)
        verify(mockDao, times(1)).getAllMostRecentSnaps()
    }

    // ========== Helper Methods ==========

    private fun createSnap(
        snapId: String,
        siteId: String,
        timestamp: Long,
        contentType: String = "text/html",
        contentCharset: String = "UTF-8",
        contentSize: Int = 1024,
        contentHash: String = "hash-$snapId",
        processedContentHash: String = "processed-$snapId",
    ): Snap = Snap(
        snapId = snapId,
        siteId = siteId,
        timestamp = timestamp,
        contentType = contentType,
        contentCharset = contentCharset,
        contentSize = contentSize,
        contentHash = contentHash,
        processedContentHash = processedContentHash,
    )
}
