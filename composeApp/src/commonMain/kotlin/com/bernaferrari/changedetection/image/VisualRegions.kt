/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.image

import com.bernaferrari.changedetection.detection.PixelDifference

/** One area of a screenshot change, in fractions of each capture's size so any resolution can use it. */
data class VisualRegion(
    val kind: Kind,
    /** Horizontal extent (shared by both captures). */
    val left: Float,
    val right: Float,
    /** Vertical extent in the old capture; empty for [Kind.Added]. */
    val beforeTop: Float,
    val beforeBottom: Float,
    /** Vertical extent in the new capture; empty for [Kind.Removed]. */
    val afterTop: Float,
    val afterBottom: Float,
) {
    enum class Kind { Changed, Added, Removed }

    /** Size used to rank regions: the area it covers in whichever capture shows it. */
    val weight: Float get() = (right - left) * maxOf(afterBottom - afterTop, beforeBottom - beforeTop)
}

/** Screenshot change, explained as areas instead of one box around everything. */
data class VisualChange(
    val regions: List<VisualRegion>,
    /** Share of the new capture covered by changed, added or removed rows (0–1). */
    val changedShare: Float,
    /** The page got taller (>0) or shorter (<0), as a share of the old height. */
    val heightDelta: Float,
)

/**
 * Compares two screenshots of a page the way a person would.
 *
 * Pages shift: a new banner pushes everything down, a feed grows, the page gets taller. Comparing pixel by
 * pixel would call all of that "changed". Instead, rows are aligned like lines of text (Myers diff on row
 * fingerprints). Rows that line up are unchanged even if they moved; rows that differ become Changed areas,
 * and rows that only exist on one side become Added or Removed areas.
 *
 * Inputs are ARGB pixels of the same width, already scaled down by the platform decoder.
 */
internal object VisualRegions {
    private const val MERGE_GAP_ROWS = 18
    private const val MIN_REGION_ROWS = 3
    private const val MAX_REGIONS = 24
    private const val MAX_GROW_ROWS = 60

    fun compare(before: IntArray, beforeHeight: Int, after: IntArray, afterHeight: Int, width: Int, threshold: Double = DEFAULT_IMAGE_DIFF_THRESHOLD): VisualChange? {
        if (width <= 0 || beforeHeight <= 0 || afterHeight <= 0) return null
        if (before.size < width * beforeHeight || after.size < width * afterHeight) return null
        val beforeRows = LongArray(beforeHeight) { rowFingerprint(before, it, width) }.toList()
        val afterRows = LongArray(afterHeight) { rowFingerprint(after, it, width) }.toList()
        val patch = com.bernaferrari.difflib.DiffUtils.diff(beforeRows, afterRows)
        val pixels = PixelDifference(threshold)
        val raw = mutableListOf<VisualRegion>()
        var changedRows = 0
        patch.deltas.sortedBy { it.target.position }.forEach { delta ->
            val sourceStart = delta.source.position
            val sourceCount = delta.source.lines.size
            val targetStart = delta.target.position
            val targetCount = delta.target.lines.size
            val paired = minOf(sourceCount, targetCount)
            if (paired > 0) {
                // Rows that exist on both sides but differ: find the columns that actually changed.
                var left = width
                var right = -1
                var differing = 0L
                for (i in 0 until paired) {
                    val b = (sourceStart + i) * width
                    val a = (targetStart + i) * width
                    for (x in 0 until width) {
                        if (pixels.changed(before[b + x], after[a + x])) {
                            differing++
                            if (x < left) left = x
                            if (x > right) right = x
                        }
                    }
                }
                // Re-rendering noise (anti-aliasing, photo re-encoding) touches a few scattered pixels; real edits touch many.
                if (right >= left && differing >= paired.toLong() * width / 50) {
                    raw += VisualRegion(
                        VisualRegion.Kind.Changed,
                        left.toFloat() / width, (right + 1).toFloat() / width,
                        sourceStart.toFloat() / beforeHeight, (sourceStart + paired).toFloat() / beforeHeight,
                        targetStart.toFloat() / afterHeight, (targetStart + paired).toFloat() / afterHeight,
                    )
                    changedRows += paired
                }
            }
            if (targetCount > paired) {
                val top = targetStart + paired
                val anchor = (sourceStart + sourceCount).toFloat() / beforeHeight
                raw += VisualRegion(VisualRegion.Kind.Added, 0f, 1f, anchor, anchor, top.toFloat() / afterHeight, (targetStart + targetCount).toFloat() / afterHeight)
                changedRows += targetCount - paired
            }
            if (sourceCount > paired) {
                val top = sourceStart + paired
                val anchor = (targetStart + targetCount).toFloat() / afterHeight
                raw += VisualRegion(VisualRegion.Kind.Removed, 0f, 1f, top.toFloat() / beforeHeight, (sourceStart + sourceCount).toFloat() / beforeHeight, anchor, anchor)
            }
        }
        val beforeBlank = BooleanArray(beforeHeight) { isBlankRow(before, it, width) }
        val afterBlank = BooleanArray(afterHeight) { isBlankRow(after, it, width) }
        val merged = merge(raw, MERGE_GAP_ROWS.toFloat() / afterHeight, MERGE_GAP_ROWS.toFloat() / beforeHeight)
            .mapNotNull { trimBlank(it, beforeBlank, afterBlank) }
            .map { growToBlank(it, beforeBlank, afterBlank) }
            .filter { maxOf((it.afterBottom - it.afterTop) * afterHeight, (it.beforeBottom - it.beforeTop) * beforeHeight) >= MIN_REGION_ROWS }
        return VisualChange(
            regions = merged.sortedByDescending { it.weight }.take(MAX_REGIONS).sortedBy { if (it.kind == VisualRegion.Kind.Removed) it.beforeTop else it.afterTop },
            changedShare = (changedRows.toFloat() / afterHeight).coerceIn(0f, 1f),
            heightDelta = (afterHeight - beforeHeight).toFloat() / beforeHeight,
        )
    }

    /**
     * Empty space (an ad slot that was hidden, page background) is not worth showing: trim blank rows from the
     * edges of an area, and drop areas that are blank on every side that shows them.
     */
    private fun trimBlank(region: VisualRegion, beforeBlank: BooleanArray, afterBlank: BooleanArray): VisualRegion? {
        fun range(top: Float, bottom: Float, blank: BooleanArray): Pair<Int, Int>? {
            var t = (top * blank.size).toInt().coerceIn(0, blank.size)
            var b = (bottom * blank.size).toInt().coerceIn(t, blank.size)
            while (t < b && blank[t]) t++
            while (b > t && blank[b - 1]) b--
            return if (b - t >= MIN_REGION_ROWS) t to b else null
        }
        val showsBefore = region.kind != VisualRegion.Kind.Added
        val showsAfter = region.kind != VisualRegion.Kind.Removed
        val b = if (showsBefore) range(region.beforeTop, region.beforeBottom, beforeBlank) else null
        val a = if (showsAfter) range(region.afterTop, region.afterBottom, afterBlank) else null
        if (b == null && a == null) return null
        return region.copy(
            beforeTop = b?.first?.toFloat()?.div(beforeBlank.size) ?: region.beforeTop,
            beforeBottom = b?.second?.toFloat()?.div(beforeBlank.size) ?: if (showsBefore) region.beforeTop else region.beforeBottom,
            afterTop = a?.first?.toFloat()?.div(afterBlank.size) ?: region.afterTop,
            afterBottom = a?.second?.toFloat()?.div(afterBlank.size) ?: if (showsAfter) region.afterTop else region.afterBottom,
        )
    }

    /** Areas end on blank rows, so a headline or a photo is never cut in half. */
    private fun growToBlank(region: VisualRegion, beforeBlank: BooleanArray, afterBlank: BooleanArray): VisualRegion {
        fun grow(top: Float, bottom: Float, blank: BooleanArray): Pair<Float, Float> {
            if (bottom <= top) return top to bottom
            var t = (top * blank.size).toInt().coerceIn(0, blank.size - 1)
            var b = (bottom * blank.size).toInt().coerceIn(t + 1, blank.size)
            var steps = 0
            while (t > 0 && !blank[t - 1] && steps++ < MAX_GROW_ROWS) t--
            steps = 0
            while (b < blank.size && !blank[b] && steps++ < MAX_GROW_ROWS) b++
            return t.toFloat() / blank.size to b.toFloat() / blank.size
        }
        val (bt, bb) = grow(region.beforeTop, region.beforeBottom, beforeBlank)
        val (at, ab) = grow(region.afterTop, region.afterBottom, afterBlank)
        return region.copy(beforeTop = bt, beforeBottom = bb, afterTop = at, afterBottom = ab)
    }

    /**
     * A row with no visible content. Text, icons and photos have many sharp edges along a row; page background,
     * an empty ad slot or a flat panel (even with a different-coloured margin) have almost none.
     */
    private fun isBlankRow(pixels: IntArray, row: Int, width: Int): Boolean {
        val start = row * width
        var edges = 0
        var previous = luminance(pixels[start])
        for (x in 1 until width) {
            val current = luminance(pixels[start + x])
            if (kotlin.math.abs(current - previous) > 28) {
                edges++
                if (edges > 4) return false
            }
            previous = current
        }
        return true
    }

    private fun luminance(p: Int): Int = (((p ushr 16) and 255) * 3 + ((p ushr 8) and 255) * 6 + (p and 255)) / 10

    /**
     * Near neighbours read as one area. A removal next to an addition is a replacement (a story swapped for
     * another), so mixed neighbours become one Changed area showing the old part and the new part.
     */
    private fun merge(regions: List<VisualRegion>, afterGap: Float, beforeGap: Float): List<VisualRegion> {
        val out = mutableListOf<VisualRegion>()
        regions.sortedBy { it.afterTop }.forEach { region ->
            val last = out.lastOrNull()
            val close = last != null && region.afterTop - last.afterBottom <= afterGap && region.beforeTop - last.beforeBottom <= beforeGap * 4
            if (close && last != null) {
                out[out.lastIndex] = VisualRegion(
                    kind = if (last.kind == region.kind) last.kind else VisualRegion.Kind.Changed,
                    left = minOf(last.left, region.left),
                    right = maxOf(last.right, region.right),
                    beforeTop = minOf(last.beforeTop, region.beforeTop),
                    beforeBottom = maxOf(last.beforeBottom, region.beforeBottom),
                    afterTop = minOf(last.afterTop, region.afterTop),
                    afterBottom = maxOf(last.afterBottom, region.afterBottom),
                )
            } else {
                out += region
            }
        }
        return out
    }

    /**
     * A row's identity. The low bits of each channel are dropped so anti-aliasing jitter does not count,
     * while any visible edit (a digit in a price) still changes the fingerprint.
     */
    private fun rowFingerprint(pixels: IntArray, row: Int, width: Int): Long {
        var hash = -0x340d631b7bdddcdbL
        val start = row * width
        for (x in 0 until width) {
            val p = pixels[start + x] and 0x00F0F0F0
            hash = (hash xor p.toLong()) * 0x100000001b3L
        }
        return hash
    }
}

/** Nearest-neighbour downscale of ARGB pixels to [targetWidth], keeping the aspect ratio. */
internal fun scaleArgb(pixels: IntArray, width: Int, height: Int, targetWidth: Int): Triple<IntArray, Int, Int> {
    if (width <= targetWidth) return Triple(pixels, width, height)
    val targetHeight = maxOf(1, (height.toLong() * targetWidth / width).toInt())
    val out = IntArray(targetWidth * targetHeight)
    for (y in 0 until targetHeight) {
        val sy = (y.toLong() * height / targetHeight).toInt()
        val row = sy * width
        for (x in 0 until targetWidth) out[y * targetWidth + x] = pixels[row + (x.toLong() * width / targetWidth).toInt()]
    }
    return Triple(out, targetWidth, targetHeight)
}

internal const val REGION_WORKING_WIDTH = 360
