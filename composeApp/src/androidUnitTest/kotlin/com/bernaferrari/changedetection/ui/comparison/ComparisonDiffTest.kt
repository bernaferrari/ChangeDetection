package com.bernaferrari.changedetection.ui.comparison

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonDiffTest {
    @Test
    fun insertedLineDoesNotMarkFollowingLinesAsChanged() {
        val rows = calculateDiffRows(
            oldLines = listOf("alpha", "bravo", "charlie"),
            newLines = listOf("alpha", "inserted", "bravo", "charlie"),
        )

        assertEquals(
            listOf(
                DiffLineType.UNCHANGED,
                DiffLineType.ADDED,
                DiffLineType.UNCHANGED,
                DiffLineType.UNCHANGED,
            ),
            rows.map { it.type },
        )
        assertEquals("inserted", rows.single { it.type == DiffLineType.ADDED }.content)
    }

    @Test
    fun replacementProducesRemovalAndAddition() {
        val rows = calculateDiffRows(
            oldLines = listOf("before"),
            newLines = listOf("after"),
        )

        assertEquals(
            listOf(DiffLineType.REMOVED, DiffLineType.ADDED),
            rows.map { it.type },
        )
    }
}
