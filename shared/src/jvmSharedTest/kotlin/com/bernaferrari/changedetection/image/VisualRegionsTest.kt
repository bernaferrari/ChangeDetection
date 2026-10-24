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

    /**
     * A page pushed down a few rows by something blank above (an ad slot that filled) and captured to the same fixed
     * height: every row moved and the bottom slid past the cut, but nothing a person would see changed.
     */
    @Test fun aShiftedPageCutAtTheSameHeightIsNotAChange() {
        val width = 120
        val (before, h) = page(width, listOf(1, 2, 3, 4, 5, 6, 7, 8))
        val shift = 3
        val after = IntArray(width * shift) { white } + before.copyOf(width * (h - shift))
        val change = requireNotNull(VisualRegions.compare(before, h, after, h, width))
        assertTrue(change.regions.toString(), change.regions.isEmpty())
        // It moved and nothing verified remains, so a check must not save it as a version.
        assertTrue(change.moved && !change.visible)
    }

    /** A flat page that changed colour has nothing worth a card, but nothing moved, so checks go by its pixels. */
    @Test fun aFlatAreaThatChangedColourIsDecidedByItsPixels() {
        val width = 60
        val height = 40
        val green = IntArray(width * height) { 0xFF00FF00.toInt() }
        val blue = IntArray(width * height) { 0xFF0000FF.toInt() }
        assertTrue(!requireNotNull(VisualRegions.compare(green, height, blue, height, width)).moved)
    }

    /** The same shift with a real edit: the edit is still found, and only the edit. */
    @Test fun aRealEditIsStillFoundOnAShiftedPage() {
        val width = 120
        val (before, h) = page(width, listOf(1, 2, 3, 4, 5, 6, 7, 8))
        val shift = 3
        val after = IntArray(width * shift) { white } + before.copyOf(width * (h - shift))
        // A new bold mark inside block 4 (rows 30..35 before, +3 after), like a digit that changed.
        for (y in 31 + shift..35 + shift) for (x in 60..67) after[y * width + x] = ink
        val region = requireNotNull(VisualRegions.compare(before, h, after, h, width)).regions.single()
        assertEquals(VisualRegion.Kind.Changed, region.kind)
        assertTrue(region.left <= 60f / width && region.right >= 68f / width)
    }

    @Test fun identicalPagesHaveNoAreas() {
        val (before, h) = page(40, listOf(1, 2, 3))
        assertTrue(requireNotNull(VisualRegions.compare(before, h, before.copyOf(), h, 40)).regions.isEmpty())
    }

    /** A wide line of text where only one small glyph changes, like a single digit of a price. */
    @Test fun oneChangedDigitInAWideRowIsAChange() {
        val width = 360
        val (before, h) = page(width, listOf(1, 2, 3))
        val after = before.copyOf()
        // A 6 x 5 dense glyph change inside block 2 (rows 10..15): 30 pixels in a 360-wide band.
        for (y in 11..15) for (x in 200..205) after[y * width + x] = if (after[y * width + x] == ink) white else ink
        val change = requireNotNull(VisualRegions.compare(before, h, after, h, width))
        val region = change.regions.single()
        assertEquals(VisualRegion.Kind.Changed, region.kind)
        assertTrue(region.left <= 200f / width && region.right >= 206f / width)
    }

    /** Anti-aliasing jitter: a few isolated pixels spread across a wide row are not a change. */
    @Test fun scatteredSinglePixelsAreNoise() {
        val width = 360
        val (before, h) = page(width, listOf(1, 2, 3))
        val after = before.copyOf()
        for (x in 0 until width step 40) after[12 * width + x] = if (after[12 * width + x] == ink) white else ink
        assertTrue(requireNotNull(VisualRegions.compare(before, h, after, h, width)).regions.isEmpty())
    }
}
