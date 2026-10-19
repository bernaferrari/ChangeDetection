package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonProgressiveLoadTest {
    @Test fun imagePairIsPublishedBeforeTheDiffFinishes() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val sites = mock<SitesRepository>()
        val snaps = mock<SnapsRepository>()
        val site = Site("Tickets", "https://example.test", 3L, "", "page", 0 to 0)
        val newer = Snap("new", "page", 3L, "image/png", "", 10)
        val older = newer.copy(snapId = "old", timestamp = 2L)
        whenever(sites.getSiteById("page")).thenReturn(site)
        whenever(sites.getDataWithChanges()).thenReturn(emptyFlow())
        whenever(snaps.getAllSnapsForSite("page")).thenReturn(listOf(newer, older))
        whenever(snaps.getSnaps("page")).thenReturn(emptyFlow())
        whenever(snaps.getSnapContent(any())).thenReturn(byteArrayOf(1, 2, 3))
        val model = ComparisonViewModel(sites, snaps, mock(), mock(), mock(), mock())
        val states = mutableListOf<ComparisonUiState>()
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.uiState.toList(states) }
        try {
            model.openComparison("page", "new")
            withContext(Dispatchers.Default) { withTimeout(5_000) { model.uiState.first { !it.isLoading && !it.isLoadingDiff && (it.snapBytes1 != null || it.error != null) } } }
            assertTrue("Keep the two images visible while comparing", states.any { it.isLoadingDiff && it.snapBytes1 != null && it.snapBytes2 != null })
        } finally {
            collector.cancel()
            model.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }
}
