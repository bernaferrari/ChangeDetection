package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.platform.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonReadStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val share: ShareManager = mock()
    private val sites: SitesRepository = mock()
    private val snaps: SnapsRepository = mock()
    private val site = Site("Tickets", "https://example.test", 3L, "", "page", 0 to 0)
    private val history = listOf(snapshot("new", 3), snapshot("older", 2), snapshot("baseline", 1))
    private lateinit var model: ComparisonViewModel

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        model = ComparisonViewModel(sites, snaps, mock(), share, mock(), mock())
    }

    @After fun teardown() {
        runTest(dispatcher) { model.viewModelScope.coroutineContext.job.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test fun returningToTheEntryPreservesHistoricalSelectionButExplicitLatestStillWorks() = runTest(dispatcher) {
        prepare()
        model.openComparison(site.id)
        model.uiState.first { it.snapContent1 == "new" && !it.isLoadingDiff }
        model.selectPairForComparison("older")
        model.uiState.first { it.snapContent1 == "older" && !it.isLoadingDiff }
        model.openComparison(site.id)
        runCurrent()
        assertEquals("older", model.uiState.value.snapContent1)
        assertEquals(1, model.uiState.value.selectedPairIndex)
        verify(snaps, times(1)).getAllSnapsForSite(site.id)
        model.loadSiteAndSnaps(site.id)
        model.uiState.first { it.snapContent1 == "new" && !it.isLoadingDiff }
        assertEquals(0, model.uiState.value.selectedPairIndex)
    }

    @Test fun savedBaselineOpensItsActualContentWithoutInventingAnUnavailableComparison() = runTest(dispatcher) {
        prepare()
        model.openComparison(site.id, "baseline")
        val loaded = model.uiState.first { it.singleSnapContent != null && !it.isLoadingDiff }
        assertEquals("baseline", loaded.singleSnap?.snapId)
        assertEquals("baseline", loaded.singleSnapContent)
        assertNull(loaded.selectedPairIndex)
        assertFalse(loaded.requestedSnapshotUnavailable)
        assertFalse(loaded.canMarkRead)
        verify(sites, never()).markSiteRead(any(), any())
        model.selectPairForComparison(0)
        val pair = model.uiState.first { it.snapContent1 == "new" && !it.isLoadingDiff }
        assertNull(pair.singleSnap)
        assertNull(pair.singleSnapContent)
    }

    @Test fun failedBaselineRetryPreservesTheSavedVersionIdentity() = runTest(dispatcher) {
        prepare()
        whenever(snaps.getSnapContent("baseline")).thenThrow(IllegalStateException("Temporary storage failure"))
        model.openComparison(site.id, "baseline")
        model.uiState.first { it.error != null }
        doReturn("baseline".encodeToByteArray()).whenever(snaps).getSnapContent("baseline")
        model.retryComparison()
        val recovered = model.uiState.first { it.singleSnapContent != null && !it.isLoadingDiff }
        assertEquals("baseline", recovered.singleSnap?.snapId)
        assertEquals("baseline", recovered.singleSnapContent)
        assertFalse(recovered.requestedSnapshotUnavailable)
        assertNull(recovered.snapContent1)
    }

    @Test fun retryKeepsTheHistoricalIdentityAfterContentFailure() = runTest(dispatcher) {
        prepare()
        whenever(snaps.getSnapContent("baseline")).thenThrow(IllegalStateException("Temporary storage failure"))
        model.openComparison(site.id, "older")
        model.uiState.first { it.error != null }
        doReturn("baseline".encodeToByteArray()).whenever(snaps).getSnapContent("baseline")
        model.retryComparison()
        val recovered = model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff && it.error == null }
        assertEquals("older", recovered.snapContent1)
        assertEquals("baseline", recovered.snapContent2)
        assertEquals(1, recovered.selectedPairIndex)
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun retryRetainsTheRequestedVersionWhenInitialDatabaseLoadFails() = runTest(dispatcher) {
        prepare()
        whenever(sites.getSiteById(site.id)).thenThrow(IllegalStateException("Database busy"))
        model.openComparison(site.id, "older")
        model.uiState.first { it.error != null }
        doReturn(site).whenever(sites).getSiteById(site.id)
        model.retryComparison()
        val recovered = model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff && it.error == null }
        assertEquals("older", recovered.snapContent1)
        assertEquals(1, recovered.selectedPairIndex)
    }

    @Test fun retryReportsUnavailableInsteadOfReplacingADeletedHistoricalVersion() = runTest(dispatcher) {
        prepare()
        model.openComparison(site.id, "older")
        model.uiState.first { it.snapContent1 == "older" && !it.isLoadingDiff }
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(history.filter { it.snapId != "older" })
        model.retryComparison()
        val missing = model.uiState.first { it.requestedSnapshotUnavailable }
        assertNull(missing.selectedPairIndex)
        assertNull(missing.snapContent1)
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun olderAlertLoadsItsPairWithoutAcknowledgingNewerChange() = runTest(dispatcher) {
        prepare()
        model.loadSiteAndSnaps(site.id, "older")
        val state = model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        assertEquals(1, state.selectedPairIndex)
        assertEquals("older", state.snapContent1)
        assertEquals("baseline", state.snapContent2)
        assertFalse(state.site!!.isRead)
        runCurrent()
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun latestAlertRequiresExplicitAcknowledgementAfterBothVersionsLoad() = runTest(dispatcher) {
        prepare()
        whenever(sites.markSiteRead(site.id, "new")).thenReturn(true)
        model.loadSiteAndSnaps(site.id, "new")
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        verify(sites, never()).markSiteRead(any(), any())
        val receipt = model.markVisibleComparisonRead()
        assertEquals("new", receipt?.snapId)
        val state = model.uiState.value
        assertEquals("new", state.snapContent1)
        assertEquals("older", state.snapContent2)
        verify(sites).markSiteRead(site.id, "new")
    }

    @Test fun failedContentLeavesTheChangeUnread() = runTest(dispatcher) {
        prepare()
        whenever(snaps.getSnapContent("older")).thenThrow(IllegalStateException("Saved content missing"))
        model.loadSiteAndSnaps(site.id)
        val state = model.uiState.first { it.error != null }
        assertEquals(ComparisonLoadFailure.Content, state.error)
        assertFalse(state.site!!.isRead)
        assertNull(state.snapContent1)
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun missingAlertDoesNotSilentlyOpenLatestAndCanExplicitlyRecover() = runTest(dispatcher) {
        prepare()
        model.loadSiteAndSnaps(site.id, "removed")
        val missing = model.uiState.first { it.requestedSnapshotUnavailable }
        assertNull(missing.selectedPairIndex)
        verify(snaps, never()).getSnapContent(any())
        verify(sites, never()).markSiteRead(any(), any())
        whenever(sites.markSiteRead(site.id, "new")).thenReturn(true)
        model.loadSiteAndSnaps(site.id)
        val latest = model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        assertFalse(latest.site!!.isRead)
        assertFalse(latest.requestedSnapshotUnavailable)
        assertEquals("new", latest.snapContent1)
    }

    @Test fun newerCaptureArrivingDuringLoadKeepsUnreadState() = runTest(dispatcher) {
        prepare()
        whenever(sites.markSiteRead(site.id, "new")).thenReturn(false)
        model.loadSiteAndSnaps(site.id)
        val loaded = model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        assertNull(model.markVisibleComparisonRead())
        runCurrent()
        verify(sites).markSiteRead(site.id, "new")
        assertFalse(loaded.site!!.isRead)
        assertFalse(model.uiState.value.site!!.isRead)
    }

    @Test fun pruningAnOlderComparisonPreservesItsSavedVersionAsTheNewBaseline() = runTest(dispatcher) {
        prepare()
        model.loadSiteAndSnaps(site.id, "older")
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(history.take(2))
        model.pruneSite()
        val state = model.uiState.first { it.singleSnap?.snapId == "older" && it.singleSnapContent != null && !it.isLoadingDiff }
        assertEquals("older", state.singleSnapContent)
        assertFalse(state.requestedSnapshotUnavailable)
        assertNull(state.snapContent1)
        assertNull(state.snapContent2)
        assertNull(state.selectedPairIndex)
        assertFalse(state.canMarkRead)
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun failingToOpenAnotherPageCannotDisplayThePreviousPagesContent() = runTest(dispatcher) {
        prepare()
        model.loadSiteAndSnaps(site.id, "older")
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        whenever(sites.getSiteById("missing-page")).thenThrow(IllegalStateException("Database unavailable"))
        model.loadSiteAndSnaps("missing-page")
        val failed = model.uiState.first { it.error != null }
        assertEquals(ComparisonLoadFailure.Page, failed.error)
        assertNull(failed.site)
        assertNull(failed.snapContent1)
        assertTrue(failed.snaps.isEmpty())
    }

    @Test fun retryHealthUpdatesWithoutReplacingTheHistoricalComparison() = runTest(dispatcher) {
        prepare()
        val updates = MutableStateFlow(listOf(site))
        whenever(sites.getDataWithChanges()).thenReturn(updates)
        model.loadSiteAndSnaps(site.id, "older")
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        updates.value = listOf(site.copy(isSuccessful = false, lastFailureCode = "NOT_FOUND"))
        model.uiState.first { it.site?.lastFailureCode == "NOT_FOUND" }
        updates.value = listOf(site.copy(isSuccessful = true, lastFailureCode = null, lastSuccessfulCheck = 123))
        val recovered = model.uiState.first { it.site?.lastSuccessfulCheck == 123L }
        assertEquals(1, recovered.selectedPairIndex)
        assertEquals("older", recovered.snapContent1)
        assertEquals(null, recovered.site!!.lastFailureCode)
        assertFalse(recovered.site.isRead)
    }

    @Test fun undoUsesTheAcknowledgedCaptureAndReopensAttention() = runTest(dispatcher) {
        prepare()
        whenever(sites.markSiteRead(site.id, "new")).thenReturn(true)
        whenever(sites.undoSiteRead(site.id, "new")).thenReturn(true)
        model.loadSiteAndSnaps(site.id)
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        val receipt = model.markVisibleComparisonRead()!!
        assertTrue(model.uiState.value.site!!.isRead)
        model.undoRead(receipt)
        verify(sites).undoSiteRead(site.id, "new")
        assertFalse(model.uiState.value.site!!.isRead)
    }

    @Test fun olderPairCannotAcknowledgeEvenOnExplicitRequest() = runTest(dispatcher) {
        prepare()
        model.loadSiteAndSnaps(site.id, "older")
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        assertNull(model.markVisibleComparisonRead())
        verify(sites, never()).markSiteRead(any(), any())
    }

    @Test fun failedAcknowledgementKeepsContentAndAllowsRetry() = runTest(dispatcher) {
        prepare()
        whenever(sites.markSiteRead(site.id, "new")).thenThrow(IllegalStateException("Storage unavailable"))
        model.loadSiteAndSnaps(site.id)
        model.uiState.first { it.snapContent1 != null && !it.isLoadingDiff }
        assertNull(model.markVisibleComparisonRead())
        assertTrue(model.uiState.value.canMarkRead)
        assertEquals("new", model.uiState.value.snapContent1)
        doReturn(true).whenever(sites).markSiteRead(site.id, "new")
        assertNotNull(model.markVisibleComparisonRead())
    }

    @Test fun liveVersionsPreserveTheVisiblePairAndOfferTheNewChange() = runTest(dispatcher) {
        prepare()
        val changes = MutableStateFlow(history)
        whenever(snaps.getSnaps(site.id)).thenReturn(changes)
        model.openComparison(site.id)
        model.uiState.first { it.snapContent1 == "new" && !it.isLoadingDiff }
        val latest = snapshot("newest", 4)
        whenever(snaps.getSnapContent(latest.snapId)).thenReturn("newest".encodeToByteArray())
        changes.value = listOf(latest) + history
        runCurrent()
        val preserved = model.uiState.value
        assertEquals(1, preserved.selectedPairIndex)
        assertEquals("new", preserved.snapContent1)
        assertEquals("older", preserved.snapContent2)
        assertTrue(preserved.newVersionAvailable)
        assertFalse(preserved.canMarkRead)
        model.selectPairForComparison(0)
        model.uiState.first { it.snapContent1 == "newest" && !it.isLoadingDiff }
        assertFalse(model.uiState.value.newVersionAvailable)
    }

    @Test fun liveVersionsKeepTheSavedBaselineReadable() = runTest(dispatcher) {
        prepare()
        val changes = MutableStateFlow(listOf(history.last()))
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(changes.value)
        whenever(snaps.getSnaps(site.id)).thenReturn(changes)
        model.openComparison(site.id)
        model.uiState.first { it.singleSnapContent == "baseline" && !it.isLoadingDiff }
        changes.value = history
        runCurrent()
        assertEquals(3, model.uiState.value.snaps.size)
        assertEquals("baseline", model.uiState.value.singleSnapContent)
        assertTrue(model.uiState.value.newVersionAvailable)
    }

    @Test fun deletingHalfTheVisiblePairDoesNotInventAnotherComparison() = runTest(dispatcher) {
        prepare()
        val changes = MutableStateFlow(history)
        whenever(snaps.getSnaps(site.id)).thenReturn(changes)
        model.openComparison(site.id)
        model.uiState.first { it.snapContent1 == "new" && !it.isLoadingDiff }
        changes.value = history.filter { it.snapId != "older" }
        runCurrent()
        assertTrue(model.uiState.value.requestedSnapshotUnavailable)
        assertNull(model.uiState.value.selectedPairIndex)
    }

    private suspend fun prepare() {
        whenever(sites.getDataWithChanges()).thenReturn(emptyFlow())
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(history)
        whenever(snaps.getSnaps(site.id)).thenReturn(emptyFlow())
        history.forEach { whenever(snaps.getSnapContent(it.snapId)).thenReturn(it.snapId.encodeToByteArray()) }
    }

    private fun snapshot(id: String, time: Long) = Snap(id, "page", time, "text/html", "UTF-8", 10)
}
