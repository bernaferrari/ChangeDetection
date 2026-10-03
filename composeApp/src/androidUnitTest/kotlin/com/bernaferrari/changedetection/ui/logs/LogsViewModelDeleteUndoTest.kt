/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bernaferrari.changedetection.repo.ContentTypeInfo
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsDataSource
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.repo.source.local.ContentDuplicationStats
import com.bernaferrari.changedetection.repo.source.local.ContentTypeCount
import com.bernaferrari.changedetection.repo.source.local.SitesDao
import com.bernaferrari.changedetection.repo.source.local.SnapsDao
import com.bernaferrari.changedetection.repo.source.local.SiteSnapshotCount
import com.bernaferrari.changedetection.util.FileStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Tests the delete/undo contract of [LogsViewModel]:
 * - Undo after a completed delete restores row and bytes.
 * - Undo that races an in-flight delete waits for it instead of losing the snap.
 * - Back-to-back deletes keep independent per-snapId slots (no single-slot overwrite).
 * - Discard on snackbar expiry releases the pending entry (late Undo is a no-op).
 * - Discard with nothing pending is a safe no-op (including null snap).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelDeleteUndoTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `history previews use stored content and skip oversized captures`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()
        val snap = testSnap("preview")
        assertEquals(
            HistoryPreviewContent.Capture(com.bernaferrari.changedetection.ui.detail.SavedPagePreview.Text("snapshot content")),
            viewModel.loadPreview(snap),
        )
        assertEquals(HistoryPreviewContent.Capture(com.bernaferrari.changedetection.ui.detail.SavedPagePreview.Document), viewModel.loadPreview(snap.copy(contentSize = 1_048_577)))
        assertEquals(HistoryPreviewContent.Capture(com.bernaferrari.changedetection.ui.detail.SavedPagePreview.Document), viewModel.loadPreview(snap.copy(contentSize = -1)))
        assertEquals(listOf("preview"), fakes.source.previewReads)
        assertTrue(fakes.dao.deleted.isEmpty())
        assertTrue(fakes.storage.written.isEmpty())
    }

    @Test
    fun `undo after delete completes restores bytes and row`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()
        val snap = testSnap("snap-1")

        viewModel.removeSnap(snap)
        runCurrent() // let the delete land without expiring its Undo window
        assertTrue("Delete should have removed the row", "snap-1" in fakes.dao.deleted)

        viewModel.undoRemoveSnap(snap)
        advanceUntilIdle()

        assertTrue("Bytes should be rewritten on undo", "snap-1" in fakes.storage.written)
        assertEquals(snap, fakes.dao.inserted["snap-1"])
    }

    @Test
    fun `undo racing in-flight delete still restores`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes(gated = true)
        val viewModel = fakes.createViewModel()
        val snap = testSnap("snap-race")

        viewModel.removeSnap(snap)
        // Undo arrives while the delete is still blocked mid-flight.
        viewModel.undoRemoveSnap(snap)

        assertFalse("Row must not be restored before delete finished", "snap-race" in fakes.dao.inserted)
        fakes.gate.open()
        advanceUntilIdle()

        assertEquals("Undo must land after the gated delete completes", snap, fakes.dao.inserted["snap-race"])
        assertEquals(TEST_BYTES, fakes.storage.written["snap-race"])
    }

    @Test
    fun `second delete does not clobber first undo`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()
        val snapB = testSnap("snap-b")
        val snapA = testSnap("snap-a")

        // Delete B, then A while B's undo window is still open (single-slot bug).
        viewModel.removeSnap(snapB)
        runCurrent()
        viewModel.removeSnap(snapA)
        runCurrent()

        viewModel.undoRemoveSnap(snapB) // old code: silent no-op, B permanently lost
        advanceUntilIdle()

        assertEquals("Snapshot B must be restorable despite later delete of A", snapB, fakes.dao.inserted["snap-b"])
    }

    @Test
    fun `discard on snackbar expiry prevents late undo`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()
        val snap = testSnap("snap-gone")

        viewModel.removeSnap(snap)
        advanceUntilIdle()
        viewModel.discardPendingDeletion(snap)
        advanceUntilIdle()

        viewModel.undoRemoveSnap(snap) // user cannot undo after the window closed
        advanceUntilIdle()

        assertFalse("Discarded snapshot must not resurrect", "snap-gone" in fakes.dao.inserted)
    }

    @Test
    fun `discard with no pending deletion is safe no-op`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()

        viewModel.discardPendingDeletion(testSnap("never-deleted"))
        viewModel.discardPendingDeletion(null)
        advanceUntilIdle()

        assertTrue(fakes.dao.inserted.isEmpty())
    }

    @Test
    fun `retained bytes expire even when UI never dismisses snackbar`() = runTest(mainDispatcherRule.dispatcher) {
        val fakes = Fakes()
        val viewModel = fakes.createViewModel()
        val snap = testSnap("snap-expired")

        viewModel.removeSnap(snap)
        runCurrent()
        advanceTimeBy(8_001L)
        runCurrent()

        viewModel.undoRemoveSnap(snap)
        runCurrent()

        assertFalse("Expired snapshot must not remain restorable", "snap-expired" in fakes.dao.inserted)
    }

    private fun testSnap(id: String) = Snap(
        snapId = id,
        siteId = "site",
        timestamp = 0L,
        contentType = "text/plain",
        contentCharset = "utf-8",
        contentSize = TEST_BYTES.size,
    )

    /** Blocks [Gate.open]able operations to simulate slow IO deterministically. */
    class Gate {
        private var isOpen = false

        @Synchronized
        fun open() {
            isOpen = true
        }

        suspend fun awaitOpen() {
            while (!isOpen) kotlinx.coroutines.yield()
        }
    }

    class MainDispatcherRule(
        val dispatcher: TestDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(),
    ) : TestWatcher() {
        override fun starting(description: Description) {
            Dispatchers.setMain(dispatcher)
        }

        override fun finished(description: Description) {
            Dispatchers.resetMain()
        }
    }

    private companion object {
        val TEST_BYTES = "snapshot content".toByteArray()
    }

    private inner class Fakes(val gated: Boolean = false) {
        val gate = Gate().also { if (!gated) it.open() }
        val sitesDao = FakeSitesDao()
        val dao = FakeSnapsDao()
        val storage = FakeFileStorage(if (gated) gate else null)

        val source = FakeSnapsDataSource(dao, storage, gate)

        fun createViewModel(): LogsViewModel = LogsViewModel(
            sitesDao = sitesDao,
            snapsDao = dao,
            snapsRepository = SnapsRepository(source),
            fileStorage = storage,
            ioDispatcher = mainDispatcherRule.dispatcher,
        )
    }

    private class FakeSnapsDao : SnapsDao {
        override suspend fun getPrecedingSnap(siteId: String, timestamp: Long, snapId: String): Snap? = inserted.values
            .filter { it.siteId == siteId && it.snapId !in deleted && (it.timestamp < timestamp || (it.timestamp == timestamp && it.snapId < snapId)) }
            .maxWithOrNull(compareBy<Snap> { it.timestamp }.thenBy { it.snapId })

        override suspend fun olderVersionIds(siteId: String, batchSize: Int): List<String> = inserted.values
            .filter { it.siteId == siteId && it.snapId !in deleted }
            .sortedWith(compareByDescending<Snap> { it.timestamp }.thenByDescending { it.snapId })
            .drop(2).take(batchSize).map { it.snapId }

        override suspend fun retentionBatch(versionsPerPage: Int, byteBudget: Long, batchSize: Int): List<String> = com.bernaferrari.changedetection.storage.SnapshotRetention(versionsPerPage, byteBudget)
            .obsoleteVersions(inserted.values.filterNot { it.snapId in deleted }).take(batchSize).map { it.snapId }

        override fun observeStorageBytes() = kotlinx.coroutines.flow.flowOf(0L)
        val inserted = mutableMapOf<String, Snap>()
        val deleted = mutableSetOf<String>()

        override suspend fun insertSnap(snap: Snap) {
            inserted[snap.snapId] = snap
        }

        override suspend fun deleteSnapById(snapId: String) {
            deleted.add(snapId)
        }

        override suspend fun deleteSnapsByIds(snapIds: List<String>): Int {
            deleted.addAll(snapIds)
            return snapIds.size
        }

        override fun getSnapsPagedSource(): PagingSource<Int, Snap> = object : PagingSource<Int, Snap>() {
            override suspend fun load(params: PagingSource.LoadParams<Int>): PagingSource.LoadResult<Int, Snap> = PagingSource.LoadResult.Page(emptyList(), prevKey = null, nextKey = null)

            override fun getRefreshKey(state: PagingState<Int, Snap>): Int? = null
        }

        override fun getSnapsPagedSource(query: String): PagingSource<Int, Snap> = getSnapsPagedSource()

        override suspend fun getAllSnapsForSiteId(siteId: String): List<Snap> = emptyList()

        override fun getAllSnapsForSiteIdWithFlow(siteId: String): Flow<List<Snap>> = flowOf(emptyList())

        override suspend fun getContentTypesWithCount(siteId: String): List<ContentTypeCount> = emptyList()

        override suspend fun getContentTypesCount(siteId: String): List<Int> = emptyList()

        override suspend fun getContentTypesParams(siteId: String): List<String> = emptyList()

        override suspend fun getSnapById(id: String): Snap? = null

        override suspend fun getLastSnapForSiteId(siteId: String): Snap? = null

        override suspend fun getLastNSnapsForSiteId(siteId: String, limit: Int): List<Snap> = emptyList()

        override suspend fun getLastSnapsSize(siteId: String): List<Int> = emptyList()

        override suspend fun getAllSnaps(): List<Snap> = emptyList()

        override suspend fun deleteAllSnapsForSite(id: String) = Unit

        override fun observeSnapsTable(): Flow<Boolean> = flowOf(false)

        override suspend fun deleteTasks() = Unit

        override fun countNumberOfChanges(): Flow<Int> = flowOf(0)

        override fun countSitesWithChanges(): Flow<Int> = flowOf(0)

        override fun countVersionsInHistory(): Flow<Int> = flowOf(0)

        override suspend fun getSnapsWithChanges(): List<Snap> = emptyList()

        override suspend fun getLatestSnapsFromSitesWithChangesList(): List<Snap> = emptyList()

        override fun getSnapsWithChange(siteId: String, filter: String): Flow<List<Snap>> = flowOf(emptyList())

        override suspend fun getSnapsWithChange2(): List<Snap> = emptyList()

        override fun countSnapsWithChange(siteId: String, filter: String): Flow<Int> = flowOf(0)

        override suspend fun getLastContentHash(siteId: String): String? = null

        override suspend fun getLastProcessedContentHash(siteId: String): String? = null

        override suspend fun hasContentWithHash(siteId: String, hash: String): Int = 0

        override suspend fun getUniqueContentSnaps(siteId: String): List<Snap> = emptyList()

        override suspend fun getAverageContentSize(siteId: String): Double? = null

        override suspend fun getContentDuplicationStats(siteId: String): ContentDuplicationStats = ContentDuplicationStats(unique_content = 0, total_snaps = 0)
        override suspend fun getAllMostRecentSnaps(): List<Snap> = emptyList()

        override suspend fun getSnapshotCounts(): List<SiteSnapshotCount> = emptyList()
    }

    private class FakeSitesDao : SitesDao {
        override suspend fun updateMonitoringOptions(siteId: String, enabled: Boolean, notifications: Boolean) = Unit
        val sites = mutableMapOf<String, Site>()

        override suspend fun getSitesList(): List<Site> = emptyList()

        override fun observeWatchlist() = flowOf(emptyList<com.bernaferrari.changedetection.repo.source.local.WatchlistRow>())

        override fun getDataWithChanges(): Flow<List<Site>> = flowOf(emptyList())

        override suspend fun getSiteById(siteId: String): Site? = null

        override suspend fun getSiteByUrl(siteUrl: String): Site? = null

        override suspend fun insertSite(site: Site) {
            sites[site.id] = site
        }

        override suspend fun updateSite(site: Site) = Unit

        override suspend fun deleteSnapshotRows(siteId: String) = Unit

        override suspend fun markSiteRead(siteId: String, snapId: String) = 1
        override suspend fun undoSiteRead(siteId: String, snapId: String) = 1

        override suspend fun updateCompleted(siteId: String, completed: Boolean) = Unit

        override suspend fun updateSyncMetadata(
            siteId: String,
            timestamp: Long,
            successful: Boolean,
            isBrowser: Boolean,
            isRead: Boolean?,
            failureCode: String?,
        ) = Unit

        override suspend fun deleteSiteById(siteId: String) {
            sites.remove(siteId)
        }

        override suspend fun deleteSites() = Unit
    }

    private class FakeSnapsDataSource(
        private val dao: FakeSnapsDao,
        private val storage: FakeFileStorage,
        private val gate: Gate,
    ) : SnapsDataSource {
        override fun observeStorageBytes() = kotlinx.coroutines.flow.flowOf(0L)
        override suspend fun deleteSiteWithSnaps(siteId: String) = Unit
        override suspend fun deleteSnap(snapId: String) {
            // Mirrors SnapsLocalDataSource (file sweep + row delete), gated for races.
            gate.awaitOpen()
            storage.deleteFile(snapId)
            dao.deleteSnapById(snapId)
        }

        override suspend fun getSingleSnapPair(id: String): ByteArray = ByteArray(0)

        override suspend fun getAllSnapsForSite(siteId: String): List<Snap> = emptyList()

        override suspend fun pruneSnaps(siteId: String) = Unit

        override suspend fun getContentTypeInfo(siteId: String): List<ContentTypeInfo> = emptyList()

        override suspend fun getMostRecentSnap(siteId: String): Snap? = null

        override suspend fun getAllMostRecentSnaps(): Map<String, Snap> = emptyMap()

        override fun getSnaps(siteId: String): Flow<List<Snap>> = flowOf(emptyList())

        override fun observeSnapsTable(): Flow<Boolean> = flowOf(false)

        override fun getSnapsFiltered(siteId: String, filter: String): Flow<List<Snap>> = flowOf(emptyList())

        override suspend fun getSnapById(snapId: String): Snap? = null

        val previewReads = mutableListOf<String>()
        override suspend fun getSnapContent(snapId: String): ByteArray {
            previewReads.add(snapId)
            return TEST_BYTES
        }

        override suspend fun getSnapPair(originalId: String, newId: String): Pair<Pair<Snap, ByteArray>, Pair<Snap, ByteArray>>? = null

        override suspend fun getLastNSnapsForSiteId(siteId: String, limit: Int): List<Snap>? = emptyList()

        override suspend fun getLastContentHash(siteId: String): String? = null

        override suspend fun getLastProcessedContentHash(siteId: String): String? = null
    }

    private class FakeFileStorage(private val gate: Gate?) : FileStorage {
        val written = mutableMapOf<String, ByteArray>()

        override fun writeBytes(fileName: String, bytes: ByteArray) {
            written[fileName] = bytes
        }

        override fun readBytes(fileName: String): ByteArray = TEST_BYTES

        override fun deleteFile(fileName: String): Boolean = true

        override fun fileExists(fileName: String): Boolean = true
    }
}
