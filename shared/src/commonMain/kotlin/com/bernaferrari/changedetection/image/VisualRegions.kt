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
    /**
     * Whether any content moved between the captures (rows paired at a different height, or rows added or removed).
     * A page renders the same pixels every time when nothing moves, so then every differing pixel is a real change;
     * movement is what makes text re-draw differently, so then only [visible] areas count.
     */
    val moved: Boolean = false,
    /** Verified areas, including flat ones too blank to be worth a card in [regions]. Checks use this when [moved]. */
    val visible: Boolean = regions.isNotEmpty(),
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
    // Distances in working-copy rows (REGION_WORKING_WIDTH wide); tuned on real pages so one story's
    // headline, text and timestamp read as one area while neighbouring stories stay apart.
    private const val MERGE_GAP_ROWS = 30
    private const val MIN_REGION_ROWS = 4
    private const val MAX_REGIONS = 24
    private const val MAX_GROW_ROWS = 90

    /** How far (working rows) a Changed area's content may have moved and still count as the same content. */
    private const val SHIFT_NEAR_ROWS = 6
    private const val SHIFT_FAR_ROWS = 72
    private const val SHIFT_FAR_MAX_ROWS = 160

    /** Column block used to tell a dense edit from scattered noise. */
    private const val BLOCK = 16
    private const val MIN_CLUSTER_PIXELS = 14

    /** Largest blurred colour difference still explained by drawing the same thing a fraction of a pixel off. */
    private const val SUBPIXEL_TOLERANCE = 48

    /** Column bands in a row profile, and how far a band may drift and still be the same row. */
    private const val BANDS = 12
    private const val PROFILE_TOLERANCE = 36

    /** Above this many cells (rows × rows, after trimming the shared top and bottom) alignment falls back to exact rows. */
    private const val MAX_ALIGN_CELLS = 36_000_000L
    private const val BAND_GAP_ROWS = 2
    private const val DENSITY_ROWS = 8
    private const val SLACK_ROWS = 2
    private const val DIAGONAL = 0
    private const val UP = 1
    private const val LEFT = 2

    fun compare(before: IntArray, beforeHeight: Int, after: IntArray, afterHeight: Int, width: Int, threshold: Double = DEFAULT_IMAGE_DIFF_THRESHOLD, checkCancelled: () -> Unit = {}): VisualChange? {
        checkCancelled()
        if (width <= 0 || beforeHeight <= 0 || afterHeight <= 0) return null
        if (before.size < width * beforeHeight || after.size < width * afterHeight) return null
        val pixels = PixelDifference(threshold)
        val raw = mutableListOf<VisualRegion>()
        var changedRows = 0
        val alignment = align(before, beforeHeight, after, afterHeight, width, checkCancelled)
        alignment.runs.forEach { run ->
            // Rows paired by the alignment: look for real edits inside them, band by band.
            changedBands(before, beforeHeight, after, afterHeight, width, run, pixels, checkCancelled).forEach { band ->
                raw += band
                changedRows += ((band.afterBottom - band.afterTop) * afterHeight).toInt()
            }
        }
        alignment.gaps.forEach { gap ->
            // Content that moved by a fraction of a row leaves one- or two-row slivers in the alignment; not content.
            if (gap.sourceCount <= SLACK_ROWS && gap.targetCount <= SLACK_ROWS) return@forEach
            val paired = minOf(gap.sourceCount, gap.targetCount)
            if (paired > 0) {
                // Something was replaced: show the old part and the new part.
                raw += VisualRegion(
                    VisualRegion.Kind.Changed,
                    0f,
                    1f,
                    gap.sourceStart.toFloat() / beforeHeight,
                    (gap.sourceStart + gap.sourceCount).toFloat() / beforeHeight,
                    gap.targetStart.toFloat() / afterHeight,
                    (gap.targetStart + gap.targetCount).toFloat() / afterHeight,
                )
                changedRows += gap.targetCount
            } else if (gap.targetCount > 0) {
                val anchor = (gap.sourceStart).toFloat() / beforeHeight
                raw += VisualRegion(VisualRegion.Kind.Added, 0f, 1f, anchor, anchor, gap.targetStart.toFloat() / afterHeight, (gap.targetStart + gap.targetCount).toFloat() / afterHeight)
                changedRows += gap.targetCount
            } else if (gap.sourceCount > 0) {
                val anchor = gap.targetStart.toFloat() / afterHeight
                raw += VisualRegion(VisualRegion.Kind.Removed, 0f, 1f, gap.sourceStart.toFloat() / beforeHeight, (gap.sourceStart + gap.sourceCount).toFloat() / beforeHeight, anchor, anchor)
            }
        }
        val beforeBlank = BooleanArray(beforeHeight) {
            checkCancelled()
            isBlankRow(before, it, width)
        }
        val afterBlank = BooleanArray(afterHeight) {
            checkCancelled()
            isBlankRow(after, it, width)
        }

        // Very long pages are captured to a fixed height. When something new pushes the page down, the old
        // bottom falls out of the capture: it was pushed out of view, not removed from the page.
        fun pushedOut(region: VisualRegion) = region.kind == VisualRegion.Kind.Removed && beforeHeight == afterHeight && region.afterTop >= 1f - 2f / afterHeight
        val verified = merge(raw, MERGE_GAP_ROWS.toFloat() / afterHeight, MERGE_GAP_ROWS.toFloat() / beforeHeight)
            // The same content drawn a few pixels off, or pushed down by something above it, is not a change.
            .filterNot { it.kind == VisualRegion.Kind.Changed && sameAfterShift(it, before, beforeHeight, after, afterHeight, width, pixels, checkCancelled) }
            // Blank space that appeared or went away (a hidden ad slot that grew) only moved the page.
            .filterNot { it.kind != VisualRegion.Kind.Changed && trimBlank(it, beforeBlank, afterBlank) == null }
            .filterNot(::pushedOut)
            // Slivers thinner than a line of text are what a fractional move leaves behind.
            .filter { maxOf((it.afterBottom - it.afterTop) * afterHeight, (it.beforeBottom - it.beforeTop) * beforeHeight) >= MIN_REGION_ROWS }
        // What is shown is trimmed further: blank space is not worth a card.
        val merged = verified
            .mapNotNull { trimBlank(it, beforeBlank, afterBlank) }
            .map { growToBlank(it, beforeBlank, afterBlank) }
            .filter { maxOf((it.afterBottom - it.afterTop) * afterHeight, (it.beforeBottom - it.beforeTop) * beforeHeight) >= MIN_REGION_ROWS }
            .filterNot(::pushedOut)

        // Only content can move: flat rows pair with any other flat rows, so a band that changed colour may look shifted.
        fun hasContent(start: Int, count: Int, blank: BooleanArray) = (start until minOf(start + count, blank.size)).any { !blank[it] }
        val moved = alignment.runs.any { it.sourceStart != it.targetStart && hasContent(it.sourceStart, it.length, beforeBlank) } ||
            alignment.gaps.any { it.sourceCount != it.targetCount && (hasContent(it.sourceStart, it.sourceCount, beforeBlank) || hasContent(it.targetStart, it.targetCount, afterBlank)) }
        return VisualChange(
            regions = merged.sortedByDescending { it.weight }.take(MAX_REGIONS).sortedBy { if (it.kind == VisualRegion.Kind.Removed) it.beforeTop else it.afterTop },
            changedShare = (changedRows.toFloat() / afterHeight).coerceIn(0f, 1f),
            heightDelta = (afterHeight - beforeHeight).toFloat() / beforeHeight,
            moved = moved,
            visible = verified.isNotEmpty(),
        )
    }

    /** Consecutive rows paired one to one: old rows [sourceStart, +length) with new rows [targetStart, +length). */
    private class Run(val sourceStart: Int, val targetStart: Int, val length: Int)

    /** Rows only on one side, or replaced: old [sourceStart, +sourceCount), new [targetStart, +targetCount). */
    private class Gap(val sourceStart: Int, val sourceCount: Int, val targetStart: Int, val targetCount: Int)

    private class Alignment(val runs: List<Run>, val gaps: List<Gap>)

    /**
     * Pairs the rows of both captures like lines of text, but tolerantly: two rows are "the same row" when
     * their coarse profiles agree, so content that moved and was re-rasterised still lines up. Whether a
     * paired row actually changed is decided afterwards, pixel by pixel ([changedBands]), so this tolerance
     * never hides a small edit such as a changed digit.
     */
    private fun align(before: IntArray, beforeHeight: Int, after: IntArray, afterHeight: Int, width: Int, checkCancelled: () -> Unit): Alignment {
        val pb = profiles(before, beforeHeight, width, checkCancelled)
        val pa = profiles(after, afterHeight, width, checkCancelled)
        fun similar(i: Int, j: Int): Boolean {
            val bi = i * BANDS
            val aj = j * BANDS
            for (k in 0 until BANDS) if (kotlin.math.abs(pb[bi + k] - pa[aj + k]) > PROFILE_TOLERANCE) return false
            return true
        }
        // The unchanged top and bottom of the page need no search.
        var prefix = 0
        while (prefix < beforeHeight && prefix < afterHeight && similar(prefix, prefix)) prefix++
        var suffix = 0
        while (suffix < beforeHeight - prefix && suffix < afterHeight - prefix && similar(beforeHeight - 1 - suffix, afterHeight - 1 - suffix)) suffix++
        val n = beforeHeight - prefix - suffix
        val m = afterHeight - prefix - suffix
        val pairs = ArrayList<Long>()
        if (n > 0 && m > 0) {
            if (n.toLong() * m <= MAX_ALIGN_CELLS) {
                lcs(n, m, checkCancelled) { i, j -> similar(prefix + i, prefix + j) }.forEach { pairs += it }
            } else {
                // Too large to search tolerantly: fall back to exact row identity.
                val beforeRows = List(n) {
                    checkCancelled()
                    rowFingerprint(before, prefix + it, width)
                }
                val afterRows = List(m) {
                    checkCancelled()
                    rowFingerprint(after, prefix + it, width)
                }
                com.bernaferrari.difflib.DiffUtils.diff(beforeRows, afterRows, checkCancelled).let { patch ->
                    var i = 0
                    var j = 0
                    patch.deltas.sortedBy { it.source.position }.forEach { delta ->
                        while (i < delta.source.position && j < delta.target.position) pairs += pack(i++, j++)
                        i = delta.source.position + delta.source.lines.size
                        j = delta.target.position + delta.target.lines.size
                    }
                    while (i < n && j < m) pairs += pack(i++, j++)
                }
            }
        }
        val runs = mutableListOf<Run>()
        val gaps = mutableListOf<Gap>()
        if (prefix > 0) runs += Run(0, 0, prefix)
        var i = prefix
        var j = prefix
        var runStart = -1
        var runTarget = -1
        var runLength = 0
        fun closeRun() {
            if (runLength > 0) runs += Run(runStart, runTarget, runLength)
            runLength = 0
        }
        pairs.forEach { packed ->
            val pi = prefix + (packed ushr 32).toInt()
            val pj = prefix + (packed and 0xFFFFFFFFL).toInt()
            if (pi > i || pj > j) {
                closeRun()
                gaps += Gap(i, pi - i, j, pj - j)
            }
            if (runLength == 0) {
                runStart = pi
                runTarget = pj
            }
            runLength++
            i = pi + 1
            j = pj + 1
        }
        closeRun()
        if (i < beforeHeight - suffix || j < afterHeight - suffix) gaps += Gap(i, beforeHeight - suffix - i, j, afterHeight - suffix - j)
        if (suffix > 0) runs += Run(beforeHeight - suffix, afterHeight - suffix, suffix)
        return Alignment(runs, gaps)
    }

    private fun pack(i: Int, j: Int): Long = (i.toLong() shl 32) or j.toLong()

    /** Longest common subsequence under [same]; returns the matched (i, j) pairs in order. Two bits per cell. */
    private inline fun lcs(n: Int, m: Int, checkCancelled: () -> Unit, same: (Int, Int) -> Boolean): List<Long> {
        val directions = ByteArray(((n.toLong() * m + 3) / 4).toInt())
        var previous = IntArray(m + 1)
        var current = IntArray(m + 1)
        for (i in 0 until n) {
            checkCancelled()
            current[0] = 0
            for (j in 0 until m) {
                val direction: Int
                if (same(i, j)) {
                    current[j + 1] = previous[j] + 1
                    direction = DIAGONAL
                } else if (previous[j + 1] >= current[j]) {
                    current[j + 1] = previous[j + 1]
                    direction = UP
                } else {
                    current[j + 1] = current[j]
                    direction = LEFT
                }
                val cell = i.toLong() * m + j
                val index = (cell ushr 2).toInt()
                directions[index] = (directions[index].toInt() or (direction shl ((cell and 3L).toInt() * 2))).toByte()
            }
            val swap = previous
            previous = current
            current = swap
        }
        val pairs = ArrayList<Long>(previous[m])
        var i = n - 1
        var j = m - 1
        while (i >= 0 && j >= 0) {
            val cell = i.toLong() * m + j
            when ((directions[(cell ushr 2).toInt()].toInt() ushr ((cell and 3L).toInt() * 2)) and 3) {
                DIAGONAL -> {
                    pairs += pack(i, j)
                    i--
                    j--
                }

                UP -> i--

                else -> j--
            }
        }
        pairs.reverse()
        return pairs
    }

    /** Per row: average luminance of [BANDS] column bands, smoothed over three rows. */
    private fun profiles(pixels: IntArray, height: Int, width: Int, checkCancelled: () -> Unit): IntArray {
        val raw = IntArray(height * BANDS)
        for (y in 0 until height) {
            if (y % 32 == 0) checkCancelled()
            for (k in 0 until BANDS) {
                val from = k * width / BANDS
                val to = maxOf(from + 1, (k + 1) * width / BANDS)
                var sum = 0
                for (x in from until to) sum += luminance(pixels[y * width + x])
                raw[y * BANDS + k] = sum / (to - from)
            }
        }
        return IntArray(height * BANDS) { index ->
            val y = index / BANDS
            val k = index % BANDS
            val top = maxOf(0, y - 1)
            val bottom = minOf(height - 1, y + 1)
            var sum = 0
            for (r in top..bottom) sum += raw[r * BANDS + k]
            sum / (bottom - top + 1)
        }
    }

    /**
     * Within paired rows, the bands that really changed. A band counts only if somewhere in it a small window
     * (8 rows × 16 columns) holds enough strongly different pixels: a changed digit, word or picture always
     * does, while re-rasterised text leaves only a few scattered pixels per window.
     */
    private fun changedBands(before: IntArray, beforeHeight: Int, after: IntArray, afterHeight: Int, width: Int, run: Run, pixels: PixelDifference, checkCancelled: () -> Unit): List<VisualRegion> {
        val blockCount = width / BLOCK + 1
        val out = mutableListOf<VisualRegion>()
        var bandStart = -1
        var lastChanged = -1
        var left = width
        var right = -1
        val rowBlocks = ArrayList<IntArray>()
        fun flush() {
            if (bandStart < 0) return
            var dense = false
            val window = IntArray(blockCount)
            for (r in rowBlocks.indices) {
                for (k in 0 until blockCount) window[k] += rowBlocks[r][k]
                if (r >= DENSITY_ROWS) for (k in 0 until blockCount) window[k] -= rowBlocks[r - DENSITY_ROWS][k]
                if (window.any { it >= MIN_CLUSTER_PIXELS }) {
                    dense = true
                    break
                }
            }
            if (dense && right >= left) {
                out += VisualRegion(
                    VisualRegion.Kind.Changed,
                    left.toFloat() / width,
                    (right + 1).toFloat() / width,
                    (run.sourceStart + bandStart).toFloat() / beforeHeight,
                    (run.sourceStart + lastChanged + 1).toFloat() / beforeHeight,
                    (run.targetStart + bandStart).toFloat() / afterHeight,
                    (run.targetStart + lastChanged + 1).toFloat() / afterHeight,
                )
            }
            bandStart = -1
            left = width
            right = -1
            rowBlocks.clear()
        }
        for (r in 0 until run.length) {
            if (r % 32 == 0) checkCancelled()
            val sourceRow = run.sourceStart + r
            val targetRow = run.targetStart + r
            val b = sourceRow * width
            val a = targetRow * width
            val blocks = IntArray(blockCount)
            var rowDiffering = 0
            for (x in 0 until width) {
                if (pixels.changed(before[b + x], after[a + x]) &&
                    stillDifferent(before, beforeHeight, sourceRow, after, afterHeight, targetRow, x, width)
                ) {
                    rowDiffering++
                    blocks[x / BLOCK]++
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
            if (rowDiffering > 0) {
                if (bandStart < 0) bandStart = r
                lastChanged = r
            } else if (bandStart >= 0 && r - lastChanged > BAND_GAP_ROWS) {
                flush()
            }
            if (bandStart >= 0) rowBlocks += blocks
        }
        flush()
        return out
    }

    /**
     * Whether a Changed area is really the old content at another height. Pages shift for reasons that are not
     * changes (an ad slot that filled, a late font), which re-draws text a fraction of a pixel off and, on captures cut
     * at a fixed height, slides different content past the cut. The offset where the old rows match the area's new rows
     * best is found first; if at that offset there is no dense cluster of differences (the same test that finds edits)
     * and almost no differing pixel, nothing a person would see changed. A real edit, even one digit, still differs at
     * the best offset and is kept.
     */
    private fun sameAfterShift(region: VisualRegion, before: IntArray, beforeHeight: Int, after: IntArray, afterHeight: Int, width: Int, pixels: PixelDifference, checkCancelled: () -> Unit): Boolean {
        val top = (region.afterTop * afterHeight).toInt().coerceIn(0, afterHeight)
        val bottom = (region.afterBottom * afterHeight).toInt().coerceIn(top, afterHeight)
        val rows = bottom - top
        if (rows <= 0) return false
        val anchor = (region.beforeTop * beforeHeight).toInt()
        // Small areas (the slivers a shift leaves) may have moved far; a large one only by a few pixels.
        val reach = if (rows <= SHIFT_FAR_MAX_ROWS) SHIFT_FAR_ROWS else SHIFT_NEAR_ROWS
        // Whole rows, not just the area's columns: a narrow real edit could resemble something nearby, but the rest of
        // its row only lines up at the true offset, where the edit itself still differs.
        val left = 0
        val right = width
        val blockCount = width / BLOCK + 1
        val firstSource = if (region.beforeBottom > region.beforeTop) anchor else top
        // The offset where the old rows match best, judged on raw pixels (no forgiveness), nearest first on ties.
        var bestShift = Int.MIN_VALUE
        var bestCount = Long.MAX_VALUE
        for (step in 0..reach) {
            for (shift in if (step == 0) intArrayOf(0) else intArrayOf(step, -step)) {
                checkCancelled()
                val source = firstSource + shift
                if (source < 1 || source + rows >= beforeHeight - 1) continue
                var count = 0L
                for (r in 0 until rows) {
                    val b = (source + r) * width
                    val a = (top + r) * width
                    for (x in left until right) if (pixels.changed(before[b + x], after[a + x])) count++
                    if (count >= bestCount) break
                }
                if (count < bestCount) {
                    bestCount = count
                    bestShift = shift
                }
            }
        }
        if (bestShift == Int.MIN_VALUE) return false
        // At that offset, the usual test: no dense cluster of differences, and almost no differing pixels at all.
        val source = firstSource + bestShift
        val window = Array(DENSITY_ROWS) { IntArray(blockCount) }
        val totals = IntArray(blockCount)
        var differing = 0L
        val allowed = maxOf(4L, rows.toLong() * (right - left) / 100)
        for (r in 0 until rows) {
            checkCancelled()
            val slot = window[r % DENSITY_ROWS]
            for (k in 0 until blockCount) totals[k] -= slot[k]
            slot.fill(0)
            for (x in left until right) {
                if (pixels.changed(before[(source + r) * width + x], after[(top + r) * width + x]) &&
                    stillDifferent(before, beforeHeight, source + r, after, afterHeight, top + r, x, width)
                ) {
                    slot[x / BLOCK]++
                    differing++
                }
            }
            for (k in 0 until blockCount) {
                totals[k] += slot[k]
                if (totals[k] >= MIN_CLUSTER_PIXELS) return false
            }
            if (differing > allowed) return false
        }
        return true
    }

    /**
     * Whether pixel ([x], [targetRow]) of the new capture still differs from the old one after forgiving
     * sub-pixel movement: the old row may sit up to a row higher or lower, including halfway between rows.
     * The new pixel is never blurred, so a changed glyph keeps its full contrast.
     */
    private fun stillDifferent(before: IntArray, beforeHeight: Int, sourceRow: Int, after: IntArray, @Suppress("UNUSED_PARAMETER") afterHeight: Int, targetRow: Int, x: Int, width: Int): Boolean {
        val target = after[targetRow * width + x]
        val up = before[maxOf(0, sourceRow - 1) * width + x]
        val mid = before[sourceRow * width + x]
        val down = before[minOf(beforeHeight - 1, sourceRow + 1) * width + x]
        return distance(target, mid) > SUBPIXEL_TOLERANCE &&
            distance(target, up) > SUBPIXEL_TOLERANCE &&
            distance(target, down) > SUBPIXEL_TOLERANCE &&
            distance(target, average(up, mid)) > SUBPIXEL_TOLERANCE &&
            distance(target, average(mid, down)) > SUBPIXEL_TOLERANCE
    }

    private fun distance(a: Int, b: Int): Int = maxOf(
        kotlin.math.abs(((a ushr 16) and 255) - ((b ushr 16) and 255)),
        kotlin.math.abs(((a ushr 8) and 255) - ((b ushr 8) and 255)),
        kotlin.math.abs((a and 255) - (b and 255)),
    )

    private fun average(a: Int, b: Int): Int = (((((a ushr 16) and 255) + ((b ushr 16) and 255)) / 2) shl 16) or
        (((((a ushr 8) and 255) + ((b ushr 8) and 255)) / 2) shl 8) or
        (((a and 255) + (b and 255)) / 2)

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
        // A "changed" area whose one side is empty (a story replaced by a blank, still-loading slot) is really
        // content that appeared or disappeared: show it as such instead of an empty crop.
        val kind = when {
            region.kind != VisualRegion.Kind.Changed -> region.kind
            a == null -> VisualRegion.Kind.Removed
            b == null -> VisualRegion.Kind.Added
            else -> region.kind
        }
        return region.copy(
            kind = kind,
            beforeTop = b?.first?.toFloat()?.div(beforeBlank.size) ?: region.beforeTop,
            beforeBottom = b?.second?.toFloat()?.div(beforeBlank.size) ?: if (showsBefore) region.beforeTop else region.beforeBottom,
            afterTop = a?.first?.toFloat()?.div(afterBlank.size) ?: region.afterTop,
            afterBottom = a?.second?.toFloat()?.div(afterBlank.size) ?: region.afterTop,
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

/** Wide enough that one changed digit in body text stays a dense spot after averaging. */
internal const val REGION_WORKING_WIDTH = 540

/**
 * Area-averaging downscale, fed one source row at a time so platforms can decode in strips.
 * Averaging (not sampling) matters: sampling keeps or drops thin text strokes depending on where the page
 * happened to fall, so the same content moved by a few pixels would look different.
 */
internal class BoxDownscaler(private val sourceWidth: Int, private val sourceHeight: Int, targetWidth: Int) {
    val width = minOf(targetWidth, sourceWidth).coerceAtLeast(1)
    val height = maxOf(1, (sourceHeight.toLong() * width / sourceWidth).toInt())
    private val sums = LongArray(width * height * 3)
    private val counts = IntArray(width * height)
    private val columnOf = IntArray(sourceWidth) { (it.toLong() * width / sourceWidth).toInt() }

    /** [row] holds [sourceWidth] ARGB pixels starting at [offset]. */
    fun addRow(y: Int, row: IntArray, offset: Int = 0) {
        val base = ((y.toLong() * height / sourceHeight).toInt().coerceAtMost(height - 1)) * width
        for (x in 0 until sourceWidth) {
            val p = row[offset + x]
            val i = base + columnOf[x]
            sums[i * 3] += ((p ushr 16) and 255).toLong()
            sums[i * 3 + 1] += ((p ushr 8) and 255).toLong()
            sums[i * 3 + 2] += (p and 255).toLong()
            counts[i]++
        }
    }

    fun result(): IntArray = IntArray(width * height) { i ->
        val n = counts[i].coerceAtLeast(1)
        (0xFF shl 24) or ((sums[i * 3] / n).toInt() shl 16) or ((sums[i * 3 + 1] / n).toInt() shl 8) or (sums[i * 3 + 2] / n).toInt()
    }
}
