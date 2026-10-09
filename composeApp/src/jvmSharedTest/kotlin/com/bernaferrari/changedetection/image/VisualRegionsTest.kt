/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualRegionsTest {
    private val white = 0xFFFFFFFF.toInt()
    private val ink = 0xFF202020.toInt()

    /** A page of blocks: each block is a few rows of ink with a distinct pattern, separated by white rows. */
    private fun page(width: Int, blocks: List<Int>): Pair<IntArray, Int> {
        val rows = mutableListOf<IntArray>()
        blocks.forEach { id ->
            repeat(6) { r -> rows += IntArray(width) { x -> if ((x + id * 7 + r) % (id % 5 + 3) == 0) ink else white } }
            repeat(4) { rows += IntArray(width) { white } }
        }
        return rows.flatMap { it.asList() }.toIntArray() to rows.size
    }

    @Test fun contentPushedDownIsNotAChange() {
        val (before, bh) = page(40, listOf(1, 2, 3, 4, 5))
        val (after, ah) = page(40, listOf(9, 1, 2, 3, 4, 5))
        val change = requireNotNull(VisualRegions.compare(before, bh, after, ah, 40))
        assertEquals(1, change.regions.size)
        assertEquals(VisualRegion.Kind.Added, change.regions.single().kind)
        assertTrue(change.regions.single().afterTop < 0.2f)
    }

    @Test fun editedBlockIsChangedAndBlankSpaceIsTrimmed() {
        val (before, bh) = page(40, listOf(1, 2, 3))
        val (after, ah) = page(40, listOf(1, 8, 3))
        val change = requireNotNull(VisualRegions.compare(before, bh, after, ah, 40))
        val region = change.regions.single()
        assertEquals(VisualRegion.Kind.Changed, region.kind)
        // Only the ink rows of the edited block, not the white gap after it.
        assertTrue(region.afterBottom - region.afterTop <= 6f / ah + 0.001f)
    }

    @Test fun identicalPagesHaveNoAreas() {
        val (before, h) = page(40, listOf(1, 2, 3))
        assertTrue(requireNotNull(VisualRegions.compare(before, h, before.copyOf(), h, 40)).regions.isEmpty())
    }
}
