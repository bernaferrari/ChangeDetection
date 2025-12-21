package com.bernaferrari.changedetection.ui.comparison

import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch
import java.util.ArrayList
import java.util.HashMap
import java.util.regex.Pattern

object UnifiedDiffUtils {
    private val UNIFIED_DIFF_CHUNK_REGEXP =
        Pattern.compile("^@@\\s+-(\\d+)(?:,(\\d+))?\\s+\\+(\\d+)(?:,(\\d+))?\\s+@@.*$")
    private const val NULL_FILE_INDICATOR = "/dev/null"

    @JvmStatic
    fun parseUnifiedDiff(diff: List<String>): Patch<String> {
        var inPrelude = true
        val rawChunk = ArrayList<Array<String>>()
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
            val matcher = UNIFIED_DIFF_CHUNK_REGEXP.matcher(line)
            if (matcher.find()) {
                processLinesInPrevChunk(rawChunk, patch, oldLn, newLn)
                oldLn = matcher.group(1)?.toInt() ?: 1
                newLn = matcher.group(3)?.toInt() ?: 1
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
        newLn: Int
    ) {
        if (rawChunk.isEmpty()) {
            return
        }
        val oldChunkLines = ArrayList<String>()
        val newChunkLines = ArrayList<String>()
        val removePosition = ArrayList<Int>()
        val addPosition = ArrayList<Int>()
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
                Chunk(newLn - 1, newChunkLines, addPosition)
            )
        )
        rawChunk.clear()
    }

    @JvmStatic
    fun generateUnifiedDiff(
        originalFileName: String?,
        revisedFileName: String?,
        originalLines: List<String>,
        patch: Patch<String>,
        contextSize: Int
    ): List<String> {
        if (patch.deltas.isEmpty()) {
            return ArrayList()
        }
        val result = ArrayList<String>()
        result.add("--- " + (originalFileName ?: NULL_FILE_INDICATOR))
        result.add("+++ " + (revisedFileName ?: NULL_FILE_INDICATOR))

        val patchDeltas = ArrayList(patch.deltas)
        val deltas = ArrayList<AbstractDelta<String>>()
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
                patchDeltas.size == 1 && originalFileName == null
            )
        )
        return result
    }

    private fun processDeltas(
        origLines: List<String>,
        deltas: List<AbstractDelta<String>>,
        contextSize: Int,
        newFile: Boolean
    ): List<String> {
        val buffer = ArrayList<String>()
        var origTotal = 0
        var revTotal = 0
        var curDelta = deltas[0]
        var origStart = if (newFile) {
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
        val buffer = ArrayList<String>()
        delta.source.lines.forEach { buffer.add("-$it") }
        delta.target.lines.forEach { buffer.add("+$it") }
        return buffer
    }

    @JvmStatic
    fun generateOriginalAndDiff(
        original: List<String>,
        revised: List<String>,
        originalFileName: String? = null,
        revisedFileName: String? = null
    ): List<String> {
        val originalName = originalFileName ?: "original"
        val revisedName = revisedFileName ?: "revised"
        val patch = com.bernaferrari.difflib.DiffUtils.diff(original, revised)
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

    private fun insertOrig(original: List<String>, unifiedDiff: List<String>): List<String> {
        val result = ArrayList<String>()
        val diffList = ArrayList<List<String>>()
        val diff = ArrayList<String>()
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

    private fun insertOrig(diffList: List<List<String>>, result: MutableList<String>, original: List<String>) {
        for (i in diffList.indices) {
            val diff = diffList[i]
            val nextDiff = if (i == diffList.size - 1) null else diffList[i + 1]
            val simb = (if (i == 0) diff.getOrNull(2) else diff.firstOrNull()) ?: continue
            val nextSimb = nextDiff?.firstOrNull()
            insert(result, diff)
            val map = getRowMap(simb)
            if (nextSimb != null) {
                val nextMap = getRowMap(nextSimb)
                var start = if (map["orgRow"] != 0) map["orgRow"]!! + map["orgDel"]!! - 1 else 0
                val end = nextMap["revRow"]!! - 2
                insert(result, getOrigList(original, start, end))
            }
            var start = map["orgRow"]!! + map["orgDel"]!! - 1
            if (start == -1) start = 0
            if (simb.contains("@@ -1,") && nextSimb == null && map["orgDel"] != original.size) {
                insert(result, getOrigList(original, start, original.size - 1))
            } else if (nextSimb == null && start < original.size) {
                insert(result, getOrigList(original, start, original.size - 1))
            }
        }
    }

    private fun insert(result: MutableList<String>, content: List<String>) {
        result.addAll(content)
    }

    private fun getRowMap(header: String): Map<String, Int> {
        val map = HashMap<String, Int>()
        if (header.startsWith("@@")) {
            val sections = header.split(" ")
            val org = sections[1]
            val orgSplit = org.split(",")
            map["orgRow"] = orgSplit[0].substring(1).toInt()
            map["orgDel"] = orgSplit[1].toInt()
            val revSplit = org.split(",")
            map["revRow"] = revSplit[0].substring(1).toInt()
            map["revAdd"] = revSplit[1].toInt()
        }
        return map
    }

    private fun getOrigList(original: List<String>, start: Int, end: Int): List<String> {
        val list = ArrayList<String>()
        if (original.isNotEmpty() && start <= end && end < original.size) {
            for (index in start..end) {
                list.add(original[index])
            }
        }
        return list
    }
}
