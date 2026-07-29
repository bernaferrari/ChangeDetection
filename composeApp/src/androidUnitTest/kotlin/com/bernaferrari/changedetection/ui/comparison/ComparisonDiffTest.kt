/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonDiffTest {
    @Test
    fun renderedSimilarityIgnoresHtmlAndWhitespace() {
        assertEquals(
            100,
            calculateRenderedSimilarity("<p>Garden   store</p>", "Garden store"),
        )
    }

    @Test
    fun renderedSimilarityUsesSharedWords() {
        assertEquals(
            33,
            calculateRenderedSimilarity("price dropped", "price updated"),
        )
    }

    @Test
    fun renderedSimilarityHandlesEmptyContent() {
        assertEquals(100, calculateRenderedSimilarity("", ""))
        assertEquals(0, calculateRenderedSimilarity("content", ""))
    }

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
