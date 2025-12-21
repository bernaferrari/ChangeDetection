package com.bernaferrari.changedetection.repo.source.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.Site
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SnapsDao N+1 optimization.
 * Tests the actual SQL query execution against a real Room database.
 *
 * Verifies that the optimized getAllMostRecentSnaps() query correctly:
 * - Fetches the most recent snapshot for each site in a single query
 * - Uses SQL window functions for efficiency
 * - Handles edge cases properly
 */
@RunWith(AndroidJUnit4::class)
class SnapsDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var snapsDao: SnapsDao
    private lateinit var sitesDao: SitesDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        snapsDao = database.snapsDao()
        sitesDao = database.sitesDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun getAllMostRecentSnaps_returnsCorrectSnapsForMultipleSites() = runBlocking {
        // Arrange: Create 3 sites
        val site1 = createSite("site-1", "https://example.com")
        val site2 = createSite("site-2", "https://google.com")
        val site3 = createSite("site-3", "https://github.com")

        sitesDao.insertSite(site1)
        sitesDao.insertSite(site2)
        sitesDao.insertSite(site3)

        // Create multiple snapshots for each site
        // Site 1: 3 snapshots
        snapsDao.insertSnap(createSnap("snap-1-old", "site-1", 1000L))
        snapsDao.insertSnap(createSnap("snap-1-mid", "site-1", 2000L))
        val site1Latest = createSnap("snap-1-latest", "site-1", 3000L)
        snapsDao.insertSnap(site1Latest)

        // Site 2: 2 snapshots
        snapsDao.insertSnap(createSnap("snap-2-old", "site-2", 1500L))
        val site2Latest = createSnap("snap-2-latest", "site-2", 2500L)
        snapsDao.insertSnap(site2Latest)

        // Site 3: 1 snapshot
        val site3Latest = createSnap("snap-3-only", "site-3", 4000L)
        snapsDao.insertSnap(site3Latest)

        // Act: Call the optimized batch query
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Should return exactly 3 snaps (one per site, the most recent)
        assertEquals(3, results.size)

        // Verify correct snaps are returned
        val resultMap = results.associateBy { it.siteId }
        assertEquals("snap-1-latest", resultMap["site-1"]?.snapId)
        assertEquals(3000L, resultMap["site-1"]?.timestamp)

        assertEquals("snap-2-latest", resultMap["site-2"]?.snapId)
        assertEquals(2500L, resultMap["site-2"]?.timestamp)

        assertEquals("snap-3-only", resultMap["site-3"]?.snapId)
        assertEquals(4000L, resultMap["site-3"]?.timestamp)
    }

    @Test
    fun getAllMostRecentSnaps_returnsEmptyListWhenNoSnaps() = runBlocking {
        // Arrange: Database is empty

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert
        assertTrue(results.isEmpty())
    }

    @Test
    fun getAllMostRecentSnaps_orderedByTimestampDescending() = runBlocking {
        // Arrange: Create sites with different latest timestamps
        sitesDao.insertSite(createSite("site-1", "https://example.com"))
        sitesDao.insertSite(createSite("site-2", "https://google.com"))
        sitesDao.insertSite(createSite("site-3", "https://github.com"))

        // Insert in non-chronological order
        snapsDao.insertSnap(createSnap("snap-2", "site-2", 2000L))  // Middle
        snapsDao.insertSnap(createSnap("snap-3", "site-3", 3000L))  // Latest
        snapsDao.insertSnap(createSnap("snap-1", "site-1", 1000L))  // Oldest

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Results should be ordered by timestamp DESC
        assertEquals(3, results.size)
        assertEquals("snap-3", results[0].snapId)  // 3000L (latest)
        assertEquals("snap-2", results[1].snapId)  // 2000L
        assertEquals("snap-1", results[2].snapId)  // 1000L (oldest)
    }

    @Test
    fun getAllMostRecentSnaps_handlesIdenticalTimestamps() = runBlocking {
        // Arrange: Two sites with same timestamp
        sitesDao.insertSite(createSite("site-1", "https://example.com"))
        sitesDao.insertSite(createSite("site-2", "https://google.com"))

        val sameTimestamp = 5000L
        snapsDao.insertSnap(createSnap("snap-1", "site-1", sameTimestamp))
        snapsDao.insertSnap(createSnap("snap-2", "site-2", sameTimestamp))

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Both snaps returned (one per site)
        assertEquals(2, results.size)
        val siteIds = results.map { it.siteId }
        assertTrue(siteIds.contains("site-1"))
        assertTrue(siteIds.contains("site-2"))
    }

    @Test
    fun getAllMostRecentSnaps_correctlyHandles50Sites() = runBlocking {
        // Arrange: Create 50 sites with multiple snaps each
        val numberOfSites = 50
        val snapsPerSite = 3

        repeat(numberOfSites) { siteIndex ->
            val siteId = "site-$siteIndex"
            sitesDao.insertSite(createSite(siteId, "https://example-$siteIndex.com"))

            repeat(snapsPerSite) { snapIndex ->
                val timestamp = (siteIndex * 1000L) + (snapIndex * 100L)
                snapsDao.insertSnap(createSnap("snap-$siteIndex-$snapIndex", siteId, timestamp))
            }
        }

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Should return exactly 50 snaps (one per site, the most recent)
        assertEquals(numberOfSites, results.size)

        // Verify each site has only its latest snap returned
        val resultMap = results.associateBy { it.siteId }
        repeat(numberOfSites) { siteIndex ->
            val siteId = "site-$siteIndex"
            val latestSnap = resultMap[siteId]
            assertEquals("snap-$siteIndex-${snapsPerSite - 1}", latestSnap?.snapId)
            // Latest timestamp = siteIndex * 1000 + (snapsPerSite - 1) * 100
            val expectedTimestamp = (siteIndex * 1000L) + ((snapsPerSite - 1) * 100L)
            assertEquals(expectedTimestamp, latestSnap?.timestamp)
        }
    }

    @Test
    fun getAllMostRecentSnaps_handlesLargeTimestampValues() = runBlocking {
        // Arrange: Create sites with very large timestamp values
        sitesDao.insertSite(createSite("site-1", "https://example.com"))
        sitesDao.insertSite(createSite("site-2", "https://google.com"))

        val largeTimestamp1 = Long.MAX_VALUE - 1000
        val largeTimestamp2 = Long.MAX_VALUE - 500

        snapsDao.insertSnap(createSnap("snap-1-old", "site-1", largeTimestamp1 - 1000))
        snapsDao.insertSnap(createSnap("snap-1-latest", "site-1", largeTimestamp1))
        snapsDao.insertSnap(createSnap("snap-2-latest", "site-2", largeTimestamp2))

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert
        assertEquals(2, results.size)
        val resultMap = results.associateBy { it.siteId }
        assertEquals("snap-1-latest", resultMap["site-1"]?.snapId)
        assertEquals("snap-2-latest", resultMap["site-2"]?.snapId)
    }

    @Test
    fun getAllMostRecentSnaps_preservesContentData() = runBlocking {
        // Arrange: Create snap with detailed content information
        sitesDao.insertSite(createSite("site-1", "https://example.com"))

        val snap = Snap(
            snapId = "snap-1",
            siteId = "site-1",
            timestamp = 1000L,
            contentType = "application/json",
            contentCharset = "ISO-8859-1",
            contentSize = 999,
            contentHash = "custom-hash-abc123",
            processedContentHash = "processed-hash-def456"
        )
        snapsDao.insertSnap(snap)

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Verify all content details are preserved
        assertEquals(1, results.size)
        val retrieved = results[0]
        assertEquals("snap-1", retrieved.snapId)
        assertEquals("site-1", retrieved.siteId)
        assertEquals(1000L, retrieved.timestamp)
        assertEquals("application/json", retrieved.contentType)
        assertEquals("ISO-8859-1", retrieved.contentCharset)
        assertEquals(999, retrieved.contentSize)
        assertEquals("custom-hash-abc123", retrieved.contentHash)
        assertEquals("processed-hash-def456", retrieved.processedContentHash)
    }

    @Test
    fun getAllMostRecentSnaps_multipleSnapsPerSiteReturnsMostRecentOnly() = runBlocking {
        // Arrange: Create a site with many snapshots (5 per site)
        sitesDao.insertSite(createSite("site-1", "https://example.com"))

        val snapIds = mutableListOf<String>()
        repeat(5) { i ->
            val snapId = "snap-$i"
            snapIds.add(snapId)
            snapsDao.insertSnap(createSnap(snapId, "site-1", 1000L + (i * 100L)))
        }

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Only the most recent snap (snap-4 with timestamp 1400L) should be returned
        assertEquals(1, results.size)
        assertEquals("snap-4", results[0].snapId)
        assertEquals(1400L, results[0].timestamp)
    }

    @Test
    fun getAllMostRecentSnaps_doesNotReturnOlderSnapshotsForSameSite() = runBlocking {
        // Arrange: Create multiple snapshots, verify only latest is returned
        sitesDao.insertSite(createSite("site-1", "https://example.com"))

        val oldSnap = createSnap("snap-old", "site-1", 1000L)
        val middleSnap = createSnap("snap-middle", "site-1", 2000L)
        val latestSnap = createSnap("snap-latest", "site-1", 3000L)

        snapsDao.insertSnap(oldSnap)
        snapsDao.insertSnap(middleSnap)
        snapsDao.insertSnap(latestSnap)

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Only latest snap should be in results
        assertEquals(1, results.size)
        assertEquals(latestSnap.snapId, results[0].snapId)
        assertEquals(latestSnap.timestamp, results[0].timestamp)
    }

    @Test
    fun getAllMostRecentSnaps_differentSitesSeparateResults() = runBlocking {
        // Arrange: Create sites with interleaved timestamp insertion
        sitesDao.insertSite(createSite("site-1", "https://example.com"))
        sitesDao.insertSite(createSite("site-2", "https://google.com"))

        // Insert snaps in interleaved order
        snapsDao.insertSnap(createSnap("snap-1-a", "site-1", 1000L))
        snapsDao.insertSnap(createSnap("snap-2-a", "site-2", 500L))
        snapsDao.insertSnap(createSnap("snap-1-b", "site-1", 2000L))
        snapsDao.insertSnap(createSnap("snap-2-b", "site-2", 1500L))
        snapsDao.insertSnap(createSnap("snap-2-c", "site-2", 3000L))

        // Act
        val results = snapsDao.getAllMostRecentSnaps()

        // Assert: Each site should have only its most recent snap
        assertEquals(2, results.size)
        val resultMap = results.associateBy { it.siteId }

        assertEquals("snap-1-b", resultMap["site-1"]?.snapId)
        assertEquals(2000L, resultMap["site-1"]?.timestamp)

        assertEquals("snap-2-c", resultMap["site-2"]?.snapId)
        assertEquals(3000L, resultMap["site-2"]?.timestamp)
    }

    // ========== Helper Methods ==========

    private fun createSite(id: String, url: String): Site {
        return Site(
            id = id,
            title = "Test Site $id",
            url = url,
            timestamp = System.currentTimeMillis(),
            isSuccessful = true,
            isSyncEnabled = true
        )
    }

    private fun createSnap(
        snapId: String,
        siteId: String,
        timestamp: Long,
        contentType: String = "text/html",
        contentCharset: String = "UTF-8",
        contentSize: Int = 1024,
        contentHash: String = "hash-$snapId",
        processedContentHash: String = "processed-$snapId"
    ): Snap {
        return Snap(
            snapId = snapId,
            siteId = siteId,
            timestamp = timestamp,
            contentType = contentType,
            contentCharset = contentCharset,
            contentSize = contentSize,
            contentHash = contentHash,
            processedContentHash = processedContentHash
        )
    }
}
