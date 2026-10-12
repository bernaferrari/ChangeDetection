package com.bernaferrari.changedetection.detection

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SelectableTextDocumentTest {
    @Test fun selectionTracksAnIdentifiedRegionAcrossSiblingInsertionAndContentChanges() = runBlocking {
        val source = "<nav>Sale</nav><main><p id='stock'>Available: 3</p><p>Delivery tomorrow</p></main>"
        val selected = selectableTextDocument(source).regions.single { it.text == "Available: 3" }
        val rule = TextRegion(selected.selector)
        val changed = source.replace("<main>", "<main><p>New announcement</p>").replace("Available: 3", "Sold out")
        assertTrue(rule.extractHtml(changed, true).contains("Sold out"))
        assertFalse(rule.extractHtml(changed, true).contains("announcement"))
        assertEquals(
            TextRegionFailure.REGION_MISSING,
            try {
                rule.extractHtml("<main><p>Other content</p></main>", true)
                null
            } catch (failure: TextRegionException) {
                failure.failure
            },
        )
    }

    @Test fun ignoredNodesDoNotShiftSavedPositionalSelectors() = runBlocking {
        val source = "<main><p hidden>Hidden sibling</p><p>Available</p><p aria-hidden='true'>Noise</p></main>"
        val document = selectableTextDocument(source)
        assertFalse(document.regions.any { it.text == "Hidden sibling" || it.text == "Noise" })
        val selected = document.regions.single { it.text == "Available" }
        val markup = TextRegion(selected.selector).extractHtml(source, true)
        assertTrue(markup.contains("Available"))
        assertFalse(markup.contains("Hidden sibling"))
    }

    @Test fun duplicateIdsStillProduceUnambiguousChoices() = runBlocking {
        val source = "<main><p id='price'>100</p><p id='price'>200</p></main>"
        val choices = selectableTextDocument(source).regions
        assertTrue(choices.any { it.text == "100" })
        assertTrue(choices.any { it.text == "200" })
        choices.forEach { choice ->
            assertEquals(choice.text, com.fleeksoft.ksoup.Ksoup.parse(TextRegion(choice.selector).extractHtml(source, true)).text())
        }
    }

    @Test fun largeDocumentsExposeTheirLimitInsteadOfClaimingAllContentIsListed() = runBlocking {
        val document = selectableTextDocument("<main>" + (1..1000).joinToString("") { "<p>Item $it</p>" } + "</main>")
        assertTrue(document.limited)
        assertEquals(300, document.regions.size)
        assertTrue(document.regions.all { it.text.length <= 1200 })
    }
}
