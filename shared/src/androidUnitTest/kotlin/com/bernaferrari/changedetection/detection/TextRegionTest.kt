package com.bernaferrari.changedetection.detection

import com.bernaferrari.changedetection.util.readableSnapshotText
import org.junit.Assert.*
import org.junit.Test

class TextRegionTest {
    private fun text(html: String, region: TextRegion) = readableSnapshotText(region.extractHtml(html, true), true)

    @Test fun `exclusions do not shift positional include selectors`() {
        val html = "<main><p id='noise'>Ad</p><p>Watched stock</p><p>Other product</p></main>"
        assertEquals("Watched stock", text(html, TextRegion("main > p:nth-child(2)", "#noise")))
    }

    @Test fun `exclusions do not change whether a structural include matches`() {
        val html = "<section><p>Price</p><span class='clock'>10s</span></section>"
        assertFailure(TextRegionFailure.REGION_MISSING) { TextRegion("section:not(:has(.clock))", ".clock").extractHtml(html, true) }
    }

    @Test fun `ignored ancestors and entirely ignored contents cannot produce a watched fragment`() {
        val html = "<main><section id='stock'><p>Available</p></section></main>"
        assertFailure(TextRegionFailure.REGION_MISSING) { TextRegion("#stock", "main").extractHtml(html, true) }
        assertFailure(TextRegionFailure.REGION_MISSING) { TextRegion("#stock", "#stock > p").extractHtml(html, true) }
    }

    @Test fun `region identity changes create a distinct detection policy without changing old whole page hashes`() {
        val page = "<p id='price'>100</p><p id='stock'>100</p>".toByteArray()
        val price = prepareTextCapture(page, "text/html", TextRegion("#price"))
        val stock = prepareTextCapture(page, "text/html", TextRegion("#stock"))
        assertNotEquals(com.bernaferrari.changedetection.util.ContentHasher.detectionPolicy(price.contentHash), com.bernaferrari.changedetection.util.ContentHasher.detectionPolicy(stock.contentHash))
        assertEquals(price.contentHash, prepareTextCapture(page, "text/html", TextRegion("  #price  ")).contentHash)
        assertEquals(com.bernaferrari.changedetection.util.ContentHasher.detectionHash(page, "text/html", com.bernaferrari.changedetection.repo.CaptureMode.TEXT), prepareTextCapture(page, "text/html", TextRegion()).contentHash)
    }

    @Test fun `selected content ignores changing navigation and excluded countdowns`() {
        val region = TextRegion("#stock", ".countdown")
        val first = "<nav>Promotion A</nav><main id='stock'><p>Available: 3</p><span class='countdown'>10s</span></main>"
        val second = first.replace("Promotion A", "Promotion B").replace("10s", "9s")
        assertEquals("Available: 3", text(first, region))
        assertEquals(text(first, region), text(second, region))
        assertNotEquals(text(first, region), text(second.replace("Available: 3", "Available: 2"), region))
        assertFalse(region.extractHtml(first, true).contains("countdown"))
        assertFalse(region.extractHtml(first, true).contains("Promotion"))
    }

    @Test fun `nested matching elements are stored once in document order`() {
        val html = "<main><article><p>First</p></article><article><p>Second</p></article></main>"
        assertEquals("First\nSecond", text(html, TextRegion("main, article")))
    }

    @Test fun `multiple disjoint regions retain ordering and repeated text`() {
        val html = "<p class='price'>100</p><p>Ignored</p><p class='price'>100</p>"
        assertEquals("100\n100", text(html, TextRegion(".price")))
    }

    @Test fun `missing region never silently monitors the whole page`() {
        assertFailure(TextRegionFailure.REGION_MISSING) { TextRegion("#missing").extractHtml("<p>Other data</p>", true) }
        assertFailure(TextRegionFailure.REGION_MISSING) { TextRegion("#price", "#price").extractHtml("<p id='price'>100</p>", true) }
    }

    @Test fun `ignore only rules preserve other page text`() {
        assertEquals("Price 100\nIn stock", text("<p>Price 100</p><aside>Ad</aside><p>In stock</p>", TextRegion(exclude = "aside")))
    }

    @Test fun `invalid include and exclude selectors are rejected before matching`() {
        assertFailure(TextRegionFailure.INVALID_SELECTOR) { TextRegion("[").validate() }
        assertFailure(TextRegionFailure.INVALID_SELECTOR) { TextRegion(exclude = "[").validate() }
        assertFailure(TextRegionFailure.INVALID_SELECTOR) { TextRegion("x".repeat(4_097)).validate() }
        assertFailure(TextRegionFailure.INVALID_SELECTOR) { TextRegion(":matches((a+)+)").validate() }
    }

    @Test fun `rules require HTML while no rules preserve literal input`() {
        val source = "literal <price>100</price>"
        assertEquals(source, TextRegion().extractHtml(source, false))
        assertEquals(source, TextRegion().extractHtml(source, true))
        assertFailure(TextRegionFailure.HTML_REQUIRED) { TextRegion("price").extractHtml(source, false) }
    }

    @Test(timeout = 30_000)
    fun `deep selected markup does not expand into formatting whitespace`() {
        val source = "<main>" + "<div>".repeat(2_000) + "In stock" + "</div>".repeat(2_000) + "</main>"
        val selected = TextRegion("main").extractHtml(source, true)
        assertTrue("Selected markup expanded from ${source.length} to ${selected.length} characters", selected.length <= source.length + 100)
        assertEquals("In stock", readableSnapshotText(selected, true))
    }

    @Test(timeout = 30_000)
    fun `large changing regions keep text changes and discard noisy siblings`() {
        val items = (1..20_000).joinToString("") { "<p>Product $it available</p>" }
        val page = "<nav>" + "Promotion ".repeat(100_000) + "</nav><main>$items</main>"
        val region = TextRegion("main")
        val before = prepareTextCapture(page.toByteArray(), "text/html", region)
        val noisy = prepareTextCapture(page.replace("Promotion", "Advertising").toByteArray(), "text/html", region)
        val changed = prepareTextCapture(page.replace("Product 20000 available", "Product 20000 sold out").toByteArray(), "text/html", region)
        assertEquals(before.contentHash, noisy.contentHash)
        assertNotEquals(before.contentHash, changed.contentHash)
        assertTrue(before.data.size < page.length / 2)
        assertTrue(readableSnapshotText(changed.data.decodeToString(), true).endsWith("Product 20000 sold out"))
    }

    private fun assertFailure(expected: TextRegionFailure, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: TextRegionException) {
            assertEquals(expected, error.failure)
        }
    }
}
