/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.image.ChangedBounds
import com.bernaferrari.changedetection.image.PlatformImageDiff
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.ui.comparison.DiffLineType
import com.bernaferrari.changedetection.ui.comparison.calculateDiffRows
import com.bernaferrari.changedetection.ui.comparison.isHtmlSnapshot
import com.bernaferrari.changedetection.ui.comparison.isImageSnapshot
import com.bernaferrari.changedetection.ui.comparison.isTextSnapshot
import com.bernaferrari.changedetection.ui.comparison.textChangeSections
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.util.readableSnapshotText
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One piece of an inline, word-level edit. */
data class EditSegment(val text: String, val kind: Kind) {
    enum class Kind { Same, Added, Removed }
}

/**
 * A preview that always fits on a card, however big the real change is.
 *
 * Real diffs can be thousands of lines of noisy text. The card never shows the diff itself; it shows
 * one of these bounded summaries instead, picked from the size and type of the change.
 */
sealed interface ChangePreview {
    /** Only one version exists, so there is nothing to compare yet. */
    data object FirstVersion : ChangePreview

    /** A small text edit: one changed passage with a little context, plus how many other passages changed. */
    data class Words(
        val section: String?,
        val segments: List<EditSegment>,
        val otherChanges: Int,
        val leadingEllipsis: Boolean,
        val trailingEllipsis: Boolean,
    ) : ChangePreview

    /** So much changed that quoting it would only be noise. */
    data class Large(val changedLines: Int, val totalLines: Int) : ChangePreview

    /** The content changed, but not in a way that is visible as text (scripts, markup, whitespace). */
    data object Invisible : ChangePreview

    /** Screenshot pages: the two captures plus the box around changed pixels. */
    data class Crop(
        val beforeSnapId: String,
        val afterSnapId: String,
        val bounds: ChangedBounds?,
        val percent: Double,
        val sizeChanged: Boolean,
    ) : ChangePreview

    /** The pair cannot be compared (missing file, mixed capture methods). */
    data object Unavailable : ChangePreview
}

/** Builds and caches previews keyed by the newest version, so a card renders its preview once. */
class ChangePreviewRepository(
    private val snaps: SnapsRepository,
    private val preceding: suspend (Snap) -> Snap? = { null },
) {
    private val cache = LinkedHashMap<String, ChangePreview>()
    private val lock = Mutex()

    suspend fun latest(siteId: String): ChangePreview {
        val pair = snaps.getLastNSnapsForSiteId(siteId, 2).orEmpty()
        val after = pair.getOrNull(0) ?: return ChangePreview.FirstVersion
        val before = pair.getOrNull(1) ?: return ChangePreview.FirstVersion
        return between(before, after)
    }

    /** What changed in this version compared with the one saved before it. */
    suspend fun forVersion(snap: Snap): ChangePreview {
        val before = preceding(snap) ?: return ChangePreview.FirstVersion
        return between(before, snap)
    }

    suspend fun between(before: Snap, after: Snap): ChangePreview {
        val key = "${before.snapId}>${after.snapId}"
        lock.withLock { cache[key] }?.let { return it }
        val preview = try {
            withContext(Dispatchers.Default) { build(before, after) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            ChangePreview.Unavailable
        }
        lock.withLock {
            cache[key] = preview
            while (cache.size > MAX_CACHED) cache.remove(cache.keys.first())
        }
        return preview
    }

    private suspend fun build(before: Snap, after: Snap): ChangePreview {
        if (before.isImageSnapshot() && after.isImageSnapshot()) {
            val diff = PlatformImageDiff.compare(
                beforeBytes = snaps.getSnapContent(before.snapId),
                afterBytes = snaps.getSnapContent(after.snapId),
                includeDiffImage = false,
            )
            if (diff.error != null) return ChangePreview.Unavailable
            return ChangePreview.Crop(
                beforeSnapId = before.snapId,
                afterSnapId = after.snapId,
                bounds = diff.changedBounds,
                percent = diff.mismatchPercentage,
                sizeChanged = diff.dimensionMismatch != null,
            )
        }
        if (!before.isTextSnapshot() || !after.isTextSnapshot()) return ChangePreview.Unavailable
        val oldRaw = decodeByteArray(snaps.getSnapContent(before.snapId), before.contentCharset)
        val newRaw = decodeByteArray(snaps.getSnapContent(after.snapId), after.contentCharset)
        val headings = if (after.isHtmlSnapshot()) htmlHeadings(newRaw) + htmlHeadings(oldRaw) else emptySet()
        return textPreview(
            readableSnapshotText(oldRaw, before.isHtmlSnapshot()).lines(),
            readableSnapshotText(newRaw, after.isHtmlSnapshot()).lines(),
            headings,
        )
    }

    private fun htmlHeadings(html: String): Set<String> = runCatching {
        Ksoup.parse(html).select("h1,h2,h3,h4,h5,h6,dt,legend,caption,th").map { it.text().trim() }.filter { it.isNotEmpty() && it.length <= 80 }.toSet()
    }.getOrDefault(emptySet())

    private companion object {
        const val MAX_CACHED = 64
    }
}

private const val CONTEXT_CHARS = 48
private const val SNIPPET_CHARS = 200
private const val LARGE_CHANGED_CHARS = 900

/** Pure and synchronous so it is easy to test with plain strings. */
internal fun textPreview(oldLines: List<String>, newLines: List<String>, headings: Set<String> = emptySet()): ChangePreview {
    val rows = calculateDiffRows(oldLines, newLines)
    val changed = rows.filter { it.type != DiffLineType.UNCHANGED }
    if (changed.isEmpty()) return ChangePreview.Invisible
    val sections = textChangeSections(rows, headings)
    val changedChars = changed.sumOf { it.content.length }
    val total = maxOf(oldLines.size, newLines.size, 1)
    val sectionsShare = changed.size.toFloat() / total
    if (changedChars > LARGE_CHANGED_CHARS || (changed.size > 8 && sectionsShare > 0.4f)) {
        return ChangePreview.Large(changed.size, total)
    }
    val first = sections.first()
    val before = first.before.joinToString(" ") { it.content }
    val after = first.after.joinToString(" ") { it.content }
    val segments = wordDiff(before, after)
    val (trimmed, leading, trailing) = trimAroundFirstEdit(segments)
    val section = first.context?.takeIf { headings.isEmpty() || it in headings }?.takeIf { it.length <= 60 }
    return ChangePreview.Words(section, trimmed, sections.size - 1, leading, trailing)
}

private val wordPattern = Regex("\\s+|[^\\s]+")

/** Word-level edit between two short passages; whole-line rewrites still read naturally as old → new. */
internal fun wordDiff(before: String, after: String): List<EditSegment> {
    val old = wordPattern.findAll(before).map { it.value }.toList()
    val new = wordPattern.findAll(after).map { it.value }.toList()
    if (old.isEmpty()) return listOf(EditSegment(after, EditSegment.Kind.Added))
    if (new.isEmpty()) return listOf(EditSegment(before, EditSegment.Kind.Removed))
    val patch = com.bernaferrari.difflib.DiffUtils.diff(old, new)
    val sameShare = 1f - patch.deltas.sumOf { it.source.lines.size + it.target.lines.size }.toFloat() / (old.size + new.size)
    // When almost nothing survives, interleaving word fragments is harder to read than old then new.
    if (sameShare < 0.25f) {
        return listOf(
            EditSegment(before, EditSegment.Kind.Removed),
            EditSegment(" ", EditSegment.Kind.Same),
            EditSegment(after, EditSegment.Kind.Added),
        )
    }
    // Token-level operations, in reading order.
    val ops = mutableListOf<Pair<EditSegment.Kind, String>>()
    var cursor = 0
    patch.deltas.sortedBy { it.source.position }.forEach { delta ->
        while (cursor < delta.source.position) {
            ops += EditSegment.Kind.Same to old[cursor++]
        }
        delta.source.lines.forEach { ops += EditSegment.Kind.Removed to it }
        delta.target.lines.forEach { ops += EditSegment.Kind.Added to it }
        cursor = delta.source.position + delta.source.lines.size
    }
    while (cursor < old.size) ops += EditSegment.Kind.Same to old[cursor++]
    return clusterEdits(ops)
}

/**
 * Neighbouring edits separated only by spaces read as one replacement: "31 October" → "14 November",
 * never "31 14 October November".
 */
private fun clusterEdits(ops: List<Pair<EditSegment.Kind, String>>): List<EditSegment> {
    val out = mutableListOf<EditSegment>()
    fun push(text: String, kind: EditSegment.Kind) {
        if (text.isEmpty()) return
        val last = out.lastOrNull()
        if (last != null && last.kind == kind) out[out.lastIndex] = last.copy(text = last.text + text) else out += EditSegment(text, kind)
    }
    var index = 0
    while (index < ops.size) {
        val (kind, token) = ops[index]
        if (kind == EditSegment.Kind.Same) {
            push(token, kind)
            index++
            continue
        }
        // Extend the cluster across whitespace-only "same" tokens that sit between edits.
        var end = index
        var lastEdit = index
        while (end < ops.size) {
            val (k, t) = ops[end]
            if (k != EditSegment.Kind.Same) {
                lastEdit = end
            } else if (t.isNotBlank()) {
                break
            }
            end++
        }
        val cluster = ops.subList(index, lastEdit + 1)
        val removed = cluster.filter { it.first != EditSegment.Kind.Added }.joinToString("") { it.second }.trim()
        val added = cluster.filter { it.first != EditSegment.Kind.Removed }.joinToString("") { it.second }.trim()
        val hasRemoved = cluster.any { it.first == EditSegment.Kind.Removed }
        val hasAdded = cluster.any { it.first == EditSegment.Kind.Added }
        if (hasRemoved && removed.isNotEmpty()) push(removed, EditSegment.Kind.Removed)
        if (hasRemoved && hasAdded && removed.isNotEmpty() && added.isNotEmpty()) push(" ", EditSegment.Kind.Same)
        if (hasAdded && added.isNotEmpty()) push(added, EditSegment.Kind.Added)
        index = lastEdit + 1
    }
    return out
}

/** Keep a little context before the first edit and stop once the snippet is card-sized. */
private fun trimAroundFirstEdit(segments: List<EditSegment>): Triple<List<EditSegment>, Boolean, Boolean> {
    val firstEdit = segments.indexOfFirst { it.kind != EditSegment.Kind.Same }
    if (firstEdit < 0) return Triple(segments, false, false)
    val out = mutableListOf<EditSegment>()
    var leading = false
    if (firstEdit > 0) {
        val context = segments.subList(0, firstEdit).joinToString("") { it.text }
        if (context.length > CONTEXT_CHARS) {
            leading = true
            val cut = context.substring(context.length - CONTEXT_CHARS)
            out += EditSegment(cut.substringAfter(' ', cut), EditSegment.Kind.Same)
        } else {
            out += EditSegment(context, EditSegment.Kind.Same)
        }
    }
    var length = out.sumOf { it.text.length }
    var trailing = false
    for (segment in segments.drop(firstEdit)) {
        val room = SNIPPET_CHARS - length
        if (room <= 0) {
            trailing = true
            break
        }
        if (segment.text.length > room) {
            val cut = segment.text.take(room)
            out += segment.copy(text = cut.substringBeforeLast(' ', cut))
            trailing = true
            break
        }
        out += segment
        length += segment.text.length
    }
    return Triple(out, leading, trailing)
}
