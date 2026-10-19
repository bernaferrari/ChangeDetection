package com.bernaferrari.difflib

import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch

/** Small multiplatform line diff used by the comparison UI on Android, iOS, and Wasm. */
object DiffUtils {
    private const val MaxLcsCells = 2_000_000L

    fun <T> diff(original: List<T>, revised: List<T>, checkCancelled: () -> Unit = {}): Patch<T> {
        checkCancelled()
        if (original == revised) return Patch()
        val result = Patch<T>()
        var remainingCells = MaxLcsCells

        fun appendGap(oldStart: Int, oldEnd: Int, newStart: Int, newEnd: Int) {
            checkCancelled()
            var prefix = 0
            while (oldStart + prefix < oldEnd && newStart + prefix < newEnd && original[oldStart + prefix] == revised[newStart + prefix]) prefix++
            var suffix = 0
            while (oldEnd - suffix > oldStart + prefix && newEnd - suffix > newStart + prefix && original[oldEnd - suffix - 1] == revised[newEnd - suffix - 1]) suffix++
            val old = original.subList(oldStart + prefix, oldEnd - suffix)
            val new = revised.subList(newStart + prefix, newEnd - suffix)
            if (old.isEmpty() && new.isEmpty()) return
            val cells = (old.size.toLong() + 1) * (new.size.toLong() + 1)
            val patch = if (old.isNotEmpty() && new.isNotEmpty() && cells <= remainingCells) {
                remainingCells -= cells
                lcsDiff(old, new, checkCancelled)
            } else {
                boundedDiff(old, new, checkCancelled)
            }
            patch.deltas.forEach {
                result.addDelta(ChangeDelta(Chunk(oldStart + prefix + it.source.position, it.source.lines), Chunk(newStart + prefix + it.target.position, it.target.lines)))
            }
        }

        if ((original.size.toLong() + 1) * (revised.size.toLong() + 1) <= MaxLcsCells || original.isEmpty() || revised.isEmpty()) {
            appendGap(0, original.size, 0, revised.size)
            return result
        }
        // Unique matching lines provide ordered anchors without a quadratic full-page table.
        // Bound auxiliary maps as well; extremely fragmented input retains the linear fallback.
        val anchors = if (original.size + revised.size <= 200_000) orderedAnchors(original, revised, checkCancelled) else emptyList()
        var oldCursor = 0
        var newCursor = 0
        anchors.forEach { (oldIndex, newIndex) ->
            appendGap(oldCursor, oldIndex, newCursor, newIndex)
            oldCursor = oldIndex + 1
            newCursor = newIndex + 1
        }
        appendGap(oldCursor, original.size, newCursor, revised.size)
        return result
    }

    private fun <T> orderedAnchors(original: List<T>, revised: List<T>, checkCancelled: () -> Unit): List<Pair<Int, Int>> {
        fun positions(lines: List<T>): Map<T, Int> = mutableMapOf<T, Int>().apply {
            lines.forEachIndexed { index, line ->
                if (index % 1024 == 0) checkCancelled()
                this[line] = if (containsKey(line)) -1 else index
            }
        }
        val oldPositions = positions(original)
        val newPositions = positions(revised)
        val matches = original.mapIndexedNotNull { index, line ->
            if (index % 1024 == 0) checkCancelled()
            val target = newPositions[line]
            if (oldPositions[line] == index && target != null && target >= 0) index to target else null
        }
        if (matches.isEmpty()) return emptyList()
        val tails = IntArray(matches.size)
        val previous = IntArray(matches.size) { -1 }
        var length = 0
        matches.indices.forEach { index ->
            if (index % 1024 == 0) checkCancelled()
            var low = 0
            var high = length
            while (low < high) {
                val middle = (low + high) / 2
                if (matches[tails[middle]].second < matches[index].second) low = middle + 1 else high = middle
            }
            if (low > 0) previous[index] = tails[low - 1]
            tails[low] = index
            if (low == length) length++
        }
        val anchors = mutableListOf<Pair<Int, Int>>()
        var index = tails[length - 1]
        while (index >= 0) {
            anchors += matches[index]
            index = previous[index]
        }
        return anchors.asReversed()
    }

    private fun <T> lcsDiff(original: List<T>, revised: List<T>, checkCancelled: () -> Unit): Patch<T> {
        val rows = original.size + 1
        val columns = revised.size + 1
        val lcs = IntArray(rows * columns)
        for (oldIndex in original.lastIndex downTo 0) {
            checkCancelled()
            for (newIndex in revised.lastIndex downTo 0) {
                lcs[oldIndex * columns + newIndex] = if (original[oldIndex] == revised[newIndex]) {
                    lcs[(oldIndex + 1) * columns + newIndex + 1] + 1
                } else {
                    maxOf(lcs[(oldIndex + 1) * columns + newIndex], lcs[oldIndex * columns + newIndex + 1])
                }
            }
        }

        val patch = Patch<T>()
        var oldIndex = 0
        var newIndex = 0
        while (oldIndex < original.size || newIndex < revised.size) {
            if ((oldIndex + newIndex) % 1024 == 0) checkCancelled()
            if (oldIndex < original.size && newIndex < revised.size && original[oldIndex] == revised[newIndex]) {
                oldIndex++
                newIndex++
                continue
            }

            val oldStart = oldIndex
            val newStart = newIndex
            val removed = mutableListOf<T>()
            val added = mutableListOf<T>()
            while (
                oldIndex < original.size || newIndex < revised.size
            ) {
                if (oldIndex < original.size && newIndex < revised.size && original[oldIndex] == revised[newIndex]) break
                val shouldAdd = newIndex < revised.size &&
                    (oldIndex == original.size || lcs[oldIndex * columns + newIndex + 1] >= lcs[(oldIndex + 1) * columns + newIndex])
                if (shouldAdd) {
                    added += revised[newIndex++]
                } else {
                    removed += original[oldIndex++]
                }
            }
            patch.addDelta(ChangeDelta(Chunk(oldStart, removed), Chunk(newStart, added)))
        }
        return patch
    }

    private fun <T> boundedDiff(original: List<T>, revised: List<T>, checkCancelled: () -> Unit): Patch<T> {
        var prefix = 0
        while (prefix < original.size && prefix < revised.size && original[prefix] == revised[prefix]) prefix++
        var suffix = 0
        while (
            suffix < original.size - prefix &&
            suffix < revised.size - prefix &&
            original[original.lastIndex - suffix] == revised[revised.lastIndex - suffix]
        ) {
            suffix++
        }
        return Patch<T>().apply {
            addDelta(
                ChangeDelta(
                    Chunk(prefix, original.subList(prefix, original.size - suffix)),
                    Chunk(prefix, revised.subList(prefix, revised.size - suffix)),
                ),
            )
        }
    }
}
