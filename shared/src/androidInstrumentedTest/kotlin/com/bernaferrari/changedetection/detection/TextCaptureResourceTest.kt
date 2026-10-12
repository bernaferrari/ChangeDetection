package com.bernaferrari.changedetection.detection

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bernaferrari.changedetection.util.readableSnapshotText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Android parser/allocator, not only desktop text rendering. */
@RunWith(AndroidJUnit4::class)
class TextCaptureResourceTest {
    @Test(timeout = 60_000)
    fun repeatedLargePagesKeepSelectedChangesWithoutSavingDiscardedContent() {
        val items = (1..20_000).joinToString("") { "<p>Product $it available</p>" }
        val noise = "Promotion ".repeat(600_000)
        val region = TextRegion("main")
        var baselineHash: String? = null
        repeat(3) { check ->
            val lastItem = if (check == 2) items.replace("Product 20000 available", "Product 20000 sold out") else items
            val source = "<nav>$check$noise</nav><main>$lastItem</main>".encodeToByteArray()
            assertTrue(source.size > 6_000_000)
            val capture = prepareTextCapture(source, "text/html", region)
            assertTrue(capture.data.size < 700_000)
            val text = readableSnapshotText(capture.data.decodeToString(), true)
            assertEquals(20_000, text.lineSequence().count())
            assertFalse(text.contains("Promotion"))
            when (check) {
                0 -> baselineHash = capture.contentHash

                1 -> assertEquals(baselineHash, capture.contentHash)

                2 -> {
                    assertNotEquals(baselineHash, capture.contentHash)
                    assertTrue(text.endsWith("Product 20000 sold out"))
                }
            }
        }
    }

    @Test(timeout = 30_000)
    fun deeplyNestedSelectedMarkupStaysCompactAndReadable() {
        val source = "<main>" + "<div>".repeat(2_000) + "In stock" + "</div>".repeat(2_000) + "</main>"
        val capture = prepareTextCapture(source.encodeToByteArray(), "text/html", TextRegion("main"))
        assertTrue(capture.data.size <= source.length + 100)
        assertEquals("In stock", readableSnapshotText(capture.data.decodeToString(), true))
    }
}
