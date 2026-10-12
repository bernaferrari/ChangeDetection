package com.bernaferrari.changedetection.util

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch

object UnifiedDiffUtils {
    private val UNIFIED_DIFF_CHUNK_REGEXP =
        Regex("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$")
    private const val NULL_FILE_INDICATOR = "/dev/null"

    private data class DiffRange(
        val originalStart: Int,
        val originalCount: Int,
    )

    fun parseUnifiedDiff(diff: List<String>): Patch<String> {
        var inPrelude = true
        val rawChunk = mutableListOf<Array<String>>()
        val patch = Patch<String>()
        var oldLn = 0
        var newLn = 0

        for (line in diff) {
            if (inPrelude) {
                if (line.startsWith("+++")) {
                    inPrelude = false
                }
                continue
            }
            val matchResult = UNIFIED_DIFF_CHUNK_REGEXP.find(line)
            if (matchResult != null) {
                processLinesInPrevChunk(rawChunk, patch, oldLn, newLn)
                // groupValues[0] is whole match
                // Groups: 1, 2, 3, 4
                oldLn = matchResult.groups[1]?.value?.toIntOrNull() ?: 1
                // group 2 is optional.
                // We don't use group 2 in logic? Original: oldLn = matcher.group(1).toInt()...
                // The original code IGNORED group 2 and 4?
                // Original: oldLn = matcher.group(1)?.toInt() ?: 1
                //           newLn = matcher.group(3)?.toInt() ?: 1
                // Yes, it ignored line counts (groups 2 and 4).

                newLn = matchResult.groups[3]?.value?.toIntOrNull() ?: 1

                if (oldLn == 0) oldLn = 1
                if (newLn == 0) newLn = 1
            } else {
                if (line.isNotEmpty()) {
                    val tag = line.substring(0, 1)
                    val rest = line.substring(1)
                    if (tag == " " || tag == "+" || tag == "-") {
                        rawChunk.add(arrayOf(tag, rest))
                    }
                } else {
                    rawChunk.add(arrayOf(" ", ""))
                }
            }
        }
        processLinesInPrevChunk(rawChunk, patch, oldLn, newLn)
        return patch
    }

    private fun processLinesInPrevChunk(
        rawChunk: MutableList<Array<String>>,
        patch: Patch<String>,
        oldLn: Int,
        newLn: Int,
    ) {
        if (rawChunk.isEmpty()) {
            return
        }
        val oldChunkLines = mutableListOf<String>()
        val newChunkLines = mutableListOf<String>()
        val removePosition = mutableListOf<Int>()
        val addPosition = mutableListOf<Int>()
        var removeNum = 0
        var addNum = 0

        for (rawLine in rawChunk) {
            val tag = rawLine[0]
            val rest = rawLine[1]
            if (tag == " " || tag == "-") {
                removeNum++
                oldChunkLines.add(rest)
                if (tag == "-") {
                    removePosition.add(oldLn - 1 + removeNum)
                }
            }
            if (tag == " " || tag == "+") {
                addNum++
                newChunkLines.add(rest)
                if (tag == "+") {
                    addPosition.add(newLn - 1 + addNum)
                }
            }
        }

        patch.addDelta(
            ChangeDelta(
                Chunk(oldLn - 1, oldChunkLines, removePosition),
                Chunk(newLn - 1, newChunkLines, addPosition),
            ),
        )
        rawChunk.clear()
    }

    fun generateUnifiedDiff(
        originalFileName: String?,
        revisedFileName: String?,
        originalLines: List<String>,
        patch: Patch<String>,
        contextSize: Int,
    ): List<String> {
        if (patch.deltas.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<String>()
        result.add("--- " + (originalFileName ?: NULL_FILE_INDICATOR))
        result.add("+++ " + (revisedFileName ?: NULL_FILE_INDICATOR))

        val patchDeltas = ArrayList(patch.deltas) // Patch.deltas might be unmodifiable or simple list
        val deltas = mutableListOf<AbstractDelta<String>>()
        var delta = patchDeltas[0]
        deltas.add(delta)

        if (patchDeltas.size > 1) {
            for (i in 1 until patchDeltas.size) {
                val position = delta.source.position
                val nextDelta = patchDeltas[i]
                if (position + delta.source.size() + contextSize >= nextDelta.source.position - contextSize) {
                    deltas.add(nextDelta)
                } else {
                    result.addAll(processDeltas(originalLines, deltas, contextSize, false))
                    deltas.clear()
                    deltas.add(nextDelta)
                }
                delta = nextDelta
            }
        }

        result.addAll(
            processDeltas(
                originalLines,
                deltas,
                contextSize,
                patchDeltas.size == 1 && originalFileName == null,
            ),
        )
        return result
    }

    private fun processDeltas(
        origLines: List<String>,
        deltas: List<AbstractDelta<String>>,
        contextSize: Int,
        newFile: Boolean,
    ): List<String> {
        val buffer = mutableListOf<String>()
        var origTotal = 0
        var revTotal = 0
        var curDelta = deltas[0]
        var origStart =
            if (newFile) {
                0
            } else {
                var start = curDelta.source.position + 1 - contextSize
                if (start < 1) {
                    start = 1
                }
                start
            }

        var revStart = curDelta.target.position + 1 - contextSize
        if (revStart < 1) {
            revStart = 1
        }

        var contextStart = curDelta.source.position - contextSize
        if (contextStart < 0) {
            contextStart = 0
        }

        var line = contextStart
        while (line < curDelta.source.position && line < origLines.size) {
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }

        buffer.addAll(getDeltaText(curDelta))
        origTotal += curDelta.source.lines.size
        revTotal += curDelta.target.lines.size

        var deltaIndex = 1
        while (deltaIndex < deltas.size) {
            val nextDelta = deltas[deltaIndex]
            val intermediateStart = curDelta.source.position + curDelta.source.lines.size
            line = intermediateStart
            while (line < nextDelta.source.position && line < origLines.size) {
                buffer.add(" " + origLines[line])
                origTotal++
                revTotal++
                line++
            }
            buffer.addAll(getDeltaText(nextDelta))
            origTotal += nextDelta.source.lines.size
            revTotal += nextDelta.target.lines.size
            curDelta = nextDelta
            deltaIndex++
        }

        contextStart = curDelta.source.position + curDelta.source.lines.size
        line = contextStart
        while (line < contextStart + contextSize && line < origLines.size) {
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }

        val header = "@@ -$origStart,$origTotal +$revStart,$revTotal @@"
        buffer.add(0, header)
        return buffer
    }

    private fun getDeltaText(delta: AbstractDelta<String>): List<String> {
        val buffer = mutableListOf<String>()
        delta.source.lines.forEach { buffer.add("-$it") }
        delta.target.lines.forEach { buffer.add("+$it") }
        return buffer
    }

    fun generateOriginalAndDiff(
        original: List<String>,
        revised: List<String>,
        originalFileName: String? = null,
        revisedFileName: String? = null,
    ): List<String> {
        val originalName = originalFileName ?: "original"
        val revisedName = revisedFileName ?: "revised"

        // Use full class name if ambiguity or just import DiffUtils
        val patch = DiffUtils.diff(original, revised)

        val unifiedDiff = generateUnifiedDiff(originalName, revisedName, original, patch, 0).toMutableList()
        if (unifiedDiff.isEmpty()) {
            unifiedDiff.add("--- $originalName")
            unifiedDiff.add("+++ $revisedName")
            unifiedDiff.add("@@ -0,0 +0,0 @@")
        } else if (unifiedDiff.size >= 3 && !unifiedDiff[2].contains("@@ -1,")) {
            unifiedDiff.add(2, "@@ -0,0 +0,0 @@")
        }
        val originalWithPrefix = original.map { " $it" }
        return insertOrig(originalWithPrefix, unifiedDiff)
    }

    private fun insertOrig(
        original: List<String>,
        unifiedDiff: List<String>,
    ): List<String> {
        val result = mutableListOf<String>()
        val diffList = mutableListOf<List<String>>()
        val diff = mutableListOf<String>()
        for (i in unifiedDiff.indices) {
            val line = unifiedDiff[i]
            if (line.startsWith("@@") && line != "@@ -0,0 +0,0 @@" && !line.contains("@@ -1,")) {
                diffList.add(ArrayList(diff))
                diff.clear()
                diff.add(line)
                continue
            }
            if (i == unifiedDiff.lastIndex) {
                diff.add(line)
                diffList.add(ArrayList(diff))
                diff.clear()
                break
            }
            diff.add(line)
        }
        insertOrig(diffList, result, original)
        return result
    }

    private fun insertOrig(
        diffList: List<List<String>>,
        result: MutableList<String>,
        original: List<String>,
    ) {
        for (i in diffList.indices) {
            val diff = diffList[i]
            val nextDiff = if (i == diffList.size - 1) null else diffList[i + 1]
            val simb = (if (i == 0) diff.getOrNull(2) else diff.firstOrNull()) ?: continue
            val nextSimb = nextDiff?.firstOrNull()
            result.addAll(diff)
            val range = parseDiffRange(simb) ?: continue
            val nextRange = nextSimb?.let(::parseDiffRange)
            if (nextRange != null) {
                val start = if (range.originalStart != 0) range.originalStart + range.originalCount - 1 else 0
                val end = nextRange.originalStart - 2
                result.addAll(getOrigList(original, start, end))
            }
            var start = range.originalStart + range.originalCount - 1
            if (start == -1) start = 0
            if (simb.contains("@@ -1,") && nextSimb == null && range.originalCount != original.size) {
                result.addAll(getOrigList(original, start, original.size - 1))
            } else if (nextSimb == null && start < original.size) {
                result.addAll(getOrigList(original, start, original.size - 1))
            }
        }
    }

    private fun parseDiffRange(header: String): DiffRange? {
        val match = UNIFIED_DIFF_CHUNK_REGEXP.matchEntire(header) ?: return null
        val originalStart = match.groups[1]?.value?.toIntOrNull() ?: return null
        val originalCount = match.groups[2]?.value?.toIntOrNull() ?: 1
        if (match.groups[3]?.value?.toIntOrNull() == null) return null
        return DiffRange(originalStart, originalCount)
    }

    private fun getOrigList(
        original: List<String>,
        start: Int,
        end: Int,
    ): List<String> {
        val list = mutableListOf<String>()
        if (original.isNotEmpty() && start <= end && end < original.size) {
            for (index in start..end) {
                list.add(original[index])
            }
        }
        return list
    }
}
