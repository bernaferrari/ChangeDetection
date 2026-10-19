package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.changedetection.preview.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class ReaderPreparationTest {
    @Test fun inlineChangesArriveWithTheirWordDiffAlreadyPrepared() = runBlocking {
        val content = prepareReaderContent("Price 10", "Price 12", false, false)
        val block = content.blocks.filterIsInstance<ReaderBlock.Change>().single()
        assertEquals(wordDiff("Price 10", "Price 12"), block.inlineSegments)
        assertEquals("Price 10", block.beforeText)
        assertEquals("Price 12", block.afterText)
    }

    @Test fun largeReaderReusesOneDigestAndPreparesItsBoundedVisibleRows() = runBlocking {
        val old = (1..300).joinToString("\n") { "Original listing $it with reserved theatre seats available today" }
        val new = old.replace("Original", "Replacement")
        val content = prepareReaderContent(old, new, false, false)
        val prepared = requireNotNull(content.digest)
        assertEquals(textDigest(old.lines(), new.lines()), prepared.source)
        assertEquals(minOf(200, prepared.source.numberUpdates.size), prepared.numbers.size)
        assertEquals(minOf(200, prepared.source.edited.size), prepared.edited.size)
    }
}
