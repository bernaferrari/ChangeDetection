package com.bernaferrari.changedetection.detection

import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RenderedSelectionDocumentTest {
    @Test fun savedAdvancedSelectorsHighlightTheirOriginalMatchedCandidates() = runBlocking {
        val document = selectableTextDocument("<p id='a' class='stock'>First</p><p id='b' class='stock'>Second</p><p id='other'>Other</p>")
        val rendered = Ksoup.parse(requireNotNull(selectionDocumentWithWatchedAreas(document, listOf(".stock")).selectionHtml))
        assertTrue(rendered.select("#a").hasAttr("data-cd-watched"))
        assertTrue(rendered.select("#b").hasAttr("data-cd-watched"))
        assertFalse(rendered.select("#other").hasAttr("data-cd-watched"))
    }

    @Test fun viewportMessagesAreBoundedAndSelectionCarriesItsExactPosition() {
        val prefix = "https://selection.changedetection.invalid/"
        val regions = listOf(SelectableTextRegion("#stock", "Available", "p", 0))
        assertEquals(RegionViewport(12, 900), regionViewportFromNavigation(prefix + "view/12/900"))
        assertEquals(RegionViewport(0, 700), regionViewportFromNavigation(prefix + "pick/0/0/700"))
        assertSame(regions[0], selectionFromNavigation(prefix + "pick/0/0/700", regions))
        listOf("view/-1/0", "view/0/10000001", "view/0/NaN", "view/0/1?x=2", "pick/0/0/2/3", "pick/-1/0/0", "view/0/9999999999999999999").forEach {
            assertNull(it, regionViewportFromNavigation(prefix + it))
            assertNull(it, selectionFromNavigation(prefix + it, regions))
        }
        assertNull(regionViewportFromNavigation("https://selection.changedetection.invalid.evil/view/0/2"))
    }

    @Test fun viewportRestorationPreservesAuthorizedScriptEvenWithoutASelection() = runBlocking {
        val model = selectableTextDocument("<p>Available</p>")
        val restored = Ksoup.parse(selectionDocumentWithInitialRegion(model, null, RegionViewport(5, 900)))
        assertEquals("5", restored.body().attr("data-cd-scroll-x"))
        assertEquals("900", restored.body().attr("data-cd-scroll-y"))
        assertEquals(Ksoup.parse(requireNotNull(model.selectionHtml)).select("script").single().data(), restored.select("script").single().data())
    }

    @Test fun excludedAncestorsMaskCandidateDescendantsWithoutRemovingTheirGeometry() = runBlocking {
        val model = selectableTextDocument("<aside id='noise'><p id='clock'>10 seconds</p></aside><p id='stock'>Available</p>")
        val rendered = Ksoup.parse(requireNotNull(selectionDocumentWithWatchedAreas(model, listOf("#stock"), listOf("#noise")).selectionHtml))
        assertEquals("true", rendered.select("#clock").attr("data-cd-ignored"))
        assertEquals("10 seconds", rendered.select("#clock").text())
        assertFalse(rendered.select("#stock").hasAttr("data-cd-ignored"))
    }

    @Test fun renderedCandidatesReferToOriginalTreeAndRemoveUntrustedCode() = runBlocking {
        val original = """<main><script>unsafe()</script><p id="duplicate" onclick="unsafe()" data-cd-region="99">First</p><p id="duplicate">Second</p><iframe src="https://example.com"></iframe></main>"""
        val model = selectableTextDocument(original)
        val rendered = Ksoup.parse(requireNotNull(model.selectionHtml))
        val source = Ksoup.parse(original)
        assertEquals(1, rendered.select("script").size) // Only the trusted picker.
        assertTrue(rendered.select("iframe,[onclick]").isEmpty())
        assertTrue(rendered.select("meta[http-equiv=Content-Security-Policy]").attr("content").contains("script-src 'sha256-"))
        rendered.select("[data-cd-region]").forEach { candidate ->
            val region = model.regions[candidate.attr("data-cd-region").toInt()]
            assertEquals(candidate.text(), source.select(region.selector).single().text())
        }
        assertFalse(requireNotNull(model.selectionHtml).contains("unsafe()"))
    }

    @Test fun retainedAreasAndActiveSelectionHaveIndependentHighlights() = runBlocking {
        val model = selectableTextDocument("<p id='price'>Price</p><p id='stock'>Stock</p>")
        val pinned = selectionDocumentWithWatchedAreas(model, listOf("#price"))
        val rendered = Ksoup.parse(selectionDocumentWithInitialRegion(pinned, "#stock"))
        assertEquals("true", rendered.select("#price").attr("data-cd-watched"))
        assertEquals("true", rendered.select("#stock").attr("data-cd-selected"))
        assertEquals(2, rendered.select("[aria-pressed=true]").size)
        assertEquals(Ksoup.parse(requireNotNull(model.selectionHtml)).select("script").single().data(), rendered.select("script").single().data())
    }

    @Test fun navigationMessagesMustBeExactBoundedCandidateIndices() {
        val regions = listOf(SelectableTextRegion("#stock", "Available", "p", 0))
        assertSame(regions[0], selectionFromNavigation("https://selection.changedetection.invalid/0", regions))
        listOf("https://selection.changedetection.invalid.evil/0", "https://selection.changedetection.invalid/-1", "https://selection.changedetection.invalid/1", "https://selection.changedetection.invalid/0?anything", "https://selection.changedetection.invalid/999999999999999999999999", "javascript:alert(1)").forEach {
            assertNull(it, selectionFromNavigation(it, regions))
        }
    }

    @Test fun restoringSelectionPreservesTheAuthorizedScriptAndItsPolicy() = runBlocking {
        val model = selectableTextDocument("<p id='stock'>Available</p>")
        val original = Ksoup.parse(requireNotNull(model.selectionHtml))
        val restored = Ksoup.parse(selectionDocumentWithInitialRegion(model, "#stock"))
        assertEquals(original.select("script").single().data(), restored.select("script").single().data())
        assertEquals(original.select("meta[http-equiv]").attr("content"), restored.select("meta[http-equiv]").attr("content"))
        assertEquals("true", restored.select("#stock").attr("aria-pressed"))
        assertEquals(1, restored.select("[data-cd-selected]").size)
    }
}
