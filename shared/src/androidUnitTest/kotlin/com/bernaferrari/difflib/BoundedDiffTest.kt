package com.bernaferrari.difflib

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class BoundedDiffTest {
    @Test fun distantEditsOnLongPagesKeepUnchangedContentOutOfThePatch() {
        val old = List(40_000) { "Product $it: available" }
        val new = old.toMutableList().apply {
            this[10] = "Product 10: sold out"
            this[39_990] = "Product 39990: sold out"
        }
        val patch = DiffUtils.diff(old, new)
        assertEquals(2, patch.deltas.size)
        assertEquals(2, patch.deltas.sumOf { it.source.lines.size })
        assertEquals(2, patch.deltas.sumOf { it.target.lines.size })
        assertReconstructs(old, new)
    }

    @Test fun emptySideAndCompletelyDifferentLargeInputsReconstructExactly() {
        val old = List(20_000) { "old $it" }
        val new = List(20_000) { "new $it" }
        assertReconstructs(old, emptyList())
        assertReconstructs(emptyList(), new)
        assertReconstructs(old, new)
    }

    @Test fun duplicateLinesAndReorderedBlocksNeverDropContent() {
        val random = Random(92)
        repeat(12) {
            val old = List(2_000) { "line ${random.nextInt(50)}" }
            val new = old.shuffled(random).toMutableList().apply {
                add(123, "inserted")
                removeAt(987)
            }
            assertReconstructs(old, new)
        }
    }

    private fun assertReconstructs(old: List<String>, new: List<String>) {
        val output = old.toMutableList()
        DiffUtils.diff(old, new).deltas.sortedByDescending { it.source.position }.forEach {
            assertEquals(it.source.lines, old.subList(it.source.position, it.source.position + it.source.lines.size))
            repeat(it.source.lines.size) { _ -> output.removeAt(it.source.position) }
            output.addAll(it.source.position, it.target.lines)
        }
        assertEquals(new, output)
    }
}
