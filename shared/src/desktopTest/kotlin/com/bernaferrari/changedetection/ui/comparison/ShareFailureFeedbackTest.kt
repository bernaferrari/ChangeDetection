package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.platform.ShareUnavailableException
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShareFailureFeedbackTest {
    @Test fun unavailableShareAppProducesUserFeedbackInsteadOfAnUncaughtCoroutineError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sites = mock<SitesRepository>();val snaps = mock<SnapsRepository>()
        val site = Site("Fixture", "https://example.test", 0L, "", "page", 0 to 0)
        val snap = Snap("baseline", "page", 1, "text/plain", "UTF-8", 1)
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(sites.getDataWithChanges()).thenReturn(emptyFlow())
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(listOf(snap))
        whenever(snaps.getSnaps(site.id)).thenReturn(emptyFlow())
        whenever(snaps.getSnapContent(snap.snapId)).thenReturn("before".encodeToByteArray())
        val share = object : ShareManager {
            override fun shareText(text: String, title: String) { throw ShareUnavailableException() }
            override fun shareFile(fileName: String, text: String, title: String) { throw ShareUnavailableException() }
        }
        val model = ComparisonViewModel(sites, snaps, mock(), share, mock(), mock())
        try {
            model.loadSiteAndSnaps(site.id);model.uiState.first { it.singleSnapContent != null }
            val message = async(start = CoroutineStart.UNDISPATCHED) { model.actionMessages.first() }
            model.shareDiff(snap, snap, "before", "after")
            assertTrue(message.await().isNotBlank())
        } finally { model.viewModelScope.coroutineContext.job.cancelAndJoin();Dispatchers.resetMain() }
    }
}
