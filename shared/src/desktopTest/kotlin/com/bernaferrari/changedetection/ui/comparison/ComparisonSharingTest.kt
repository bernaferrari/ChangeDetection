package com.bernaferrari.changedetection.ui.comparison

import androidx.lifecycle.viewModelScope
import com.bernaferrari.changedetection.image.*
import com.bernaferrari.changedetection.platform.ShareManager
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SitesRepository
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.util.localizedInteger
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonSharingTest {
    @Test fun sharingUsesTheSelectedLanguageAndLocalDigitsWithoutTranslatingPageContent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val previous = Locale.getDefault()
        val sites = mock<SitesRepository>()
        val snaps = mock<SnapsRepository>()
        val site = Site("Studio 2026", "https://example.test/2026", 0L, "", "page", 0 to 0)
        val baseline = Snap("baseline", "page", 1, "text/plain", "UTF-8", 1)
        whenever(sites.getSiteById(site.id)).thenReturn(site)
        whenever(sites.getDataWithChanges()).thenReturn(emptyFlow())
        whenever(snaps.getAllSnapsForSite(site.id)).thenReturn(listOf(baseline))
        whenever(snaps.getSnaps(site.id)).thenReturn(emptyFlow())
        whenever(snaps.getSnapContent(baseline.snapId)).thenReturn("Baseline".encodeToByteArray())
        val output = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val share = mock<ShareManager>()
        doAnswer {
            output.tryEmit(it.getArgument(0))
            null
        }.whenever(share).shareText(any(), any())
        val model = ComparisonViewModel(sites, snaps, mock(), share, mock(), mock())
        try {
            model.loadSiteAndSnaps(site.id)
            model.uiState.first { it.singleSnapContent != null }
            val headings = mapOf("en" to "changed", "es" to "Cambios", "it" to "cambiato", "pt" to "mudou", "de" to "geändert", "hi" to "बदलाव", "bn" to "পরিবর্তন", "zh" to "变化")
            for ((language, heading) in headings) {
                Locale.setDefault(Locale.forLanguageTag(language))
                val textResult = async(start = CoroutineStart.UNDISPATCHED) { output.first() }
                val old = (1..15).joinToString("\n") { "old $it\nseparator $it" }
                val new = (1..15).joinToString("\n") { "new $it\nseparator $it" }
                model.shareDiff(baseline, baseline, old, new)
                val text = textResult.await()
                assertTrue(language, text.contains(heading))
                assertTrue(language, text.contains(localizedInteger(9)))
                assertTrue(text.contains("old 1 → new 1"))
                assertTrue(text.contains(site.title!!))
                assertTrue(text.endsWith(site.url))
                for (readable in listOf(true, false)) {
                    val markupResult = async(start = CoroutineStart.UNDISPATCHED) { output.first() }
                    val html = baseline.copy(contentType = "text/html")
                    model.shareDiff(html, html, "<p>Sold out</p>", "<p>Available</p>", readableText = readable)
                    val markup = markupResult.await()
                    assertTrue(markup, markup.contains(if (readable) "Sold out → Available" else "<p>Sold out</p> → <p>Available</p>"))
                    if (readable) assertFalse(markup.contains("<p>"))
                }
                for (count in listOf(0, 1, 12)) {
                    val imageResult = async(start = CoroutineStart.UNDISPATCHED) { output.first() }
                    val regions = List(count) { VisualRegion(VisualRegion.Kind.Changed, 0f, 1f, 0f, 1f, 0f, 1f) }
                    model.shareImageDiff(baseline, baseline, SnapshotImageDiff(false, 10.0, 1, 10, 10, 10, 10, 10, null, visualChange = VisualChange(regions, 1f, 0f)))
                    val image = imageResult.await()
                    assertTrue(language, image.contains(heading))
                    assertEquals(language, count > 0, image.contains(" · "))
                    if (count > 0) assertTrue(language, image.contains(localizedInteger(count)))
                    assertTrue(image.endsWith(site.url))
                }
            }
        } finally {
            model.viewModelScope.coroutineContext.job.cancelAndJoin()
            Locale.setDefault(previous)
            Dispatchers.resetMain()
        }
    }
}
