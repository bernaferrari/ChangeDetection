package com.bernaferrari.changedetection.preview

import com.bernaferrari.changedetection.image.computeVisualChange

import com.bernaferrari.changedetection.image.ChangedBounds
import com.bernaferrari.changedetection.image.PlatformImageDiff
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.ui.comparison.calculateDiffRows
import com.bernaferrari.changedetection.ui.comparison.isHtmlSnapshot
import com.bernaferrari.changedetection.ui.comparison.isImageSnapshot
import com.bernaferrari.changedetection.ui.comparison.isTextSnapshot
import com.bernaferrari.changedetection.ui.comparison.textChangeSections
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.util.readableSnapshotText
import com.fleeksoft.ksoup.Ksoup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withPermit
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
        /** Every change only moved digits: counters, times, prices. */
        val numbersOnly: Boolean = false,
    ) : ChangePreview

    /**
     * Too much changed to quote as one passage. Instead of the diff, a few representative one-line
     * samples (meaningful words first, number churn last) plus the size of the change.
     */
    data class Large(
        val changedLines: Int,
        val totalLines: Int,
        val samples: List<List<EditSegment>> = emptyList(),
        val numbersOnly: Boolean = false,
        /** Lines that are genuinely new / gone, ignoring order and numbers. */
        val added: Int = 0,
        val removed: Int = 0,
        /** Lines that only moved or had their numbers change. */
        val updated: Int = 0,
        /** Exactly the same lines in a different order (re-sorted lists). */
        val reordered: Boolean = false,
    ) : ChangePreview

    /** The content changed, but not in a way that is visible as text (scripts, markup, whitespace). */
    data object Invisible : ChangePreview

    /** Two screenshots that differ only by re-rendering or movement: nothing visible changed. */
    data object NothingVisible : ChangePreview

    /** Screenshot pages: the two captures plus the box around changed pixels. */
    data class Crop(
        val beforeSnapId: String,
        val afterSnapId: String,
        val bounds: ChangedBounds?,
        val percent: Double,
        val sizeChanged: Boolean,
        /** Row-aligned areas of change (handles pages that shifted or grew); biggest first is not guaranteed. */
        val regions: List<com.bernaferrari.changedetection.image.VisualRegion> = emptyList(),
        val isPreparing: Boolean = false,
    ) : ChangePreview {
        /**
         * The one area worth showing on a card: the top-most thing that is new or different (what a person reads
         * first). Tiny slivers are skipped; removals only lead when nothing was added or changed.
         */
        val lead: com.bernaferrari.changedetection.image.VisualRegion?
            get() = regions.filter { it.kind != com.bernaferrari.changedetection.image.VisualRegion.Kind.Removed && it.afterBottom - it.afterTop > 0.004f }.minByOrNull { it.afterTop }
                ?: regions.maxByOrNull { it.weight }
    }

    /** The pair cannot be compared (missing file, mixed capture methods). */
    data object Unavailable : ChangePreview
}

/** Builds and caches previews keyed by the newest version, so a card renders its preview once. */
class ChangePreviewRepository(
    private val snaps: SnapsRepository,
    private val preceding: suspend (Snap) -> Snap? = { null },
) {
    private val cache = com.bernaferrari.changedetection.util.SynchronizedCache<String, ChangePreview>(MAX_CACHED)
    private val versions = com.bernaferrari.changedetection.util.SynchronizedCache<String, ChangePreview>(MAX_CACHED)
    private val builds = com.bernaferrari.changedetection.sync.PageCheckLocks()
    private val capacity = kotlinx.coroutines.sync.Semaphore(2)

    /** Immutable snapshot IDs let returning lists paint cached content on their first frame. */
    fun cachedVersion(snapId: String?): ChangePreview? = snapId?.let { versions[it] }

    suspend fun latest(siteId: String, onImagePair: (Snap, Snap) -> Unit = { _, _ -> }): ChangePreview {
        val pair = snaps.getLastNSnapsForSiteId(siteId, 2).orEmpty()
        val after = pair.getOrNull(0) ?: return ChangePreview.FirstVersion
        val before = pair.getOrNull(1) ?: return ChangePreview.FirstVersion
        if (before.isImageSnapshot() && after.isImageSnapshot() && cache["${before.snapId}>${after.snapId}"] == null) onImagePair(before, after)
        return between(before, after)
    }

    /** What changed in this version compared with the one saved before it. */
    suspend fun forVersion(snap: Snap): ChangePreview {
        val before = preceding(snap) ?: return ChangePreview.FirstVersion
        return between(before, snap)
    }

    suspend fun between(before: Snap, after: Snap): ChangePreview {
        val key = "${before.snapId}>${after.snapId}"
        cache[key]?.let { return it }
        return builds.withLock(key) {
            cache[key]?.let { return@withLock it }
            val preview = try {
                capacity.withPermit { withContext(Dispatchers.Default) { build(before, after) } }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ChangePreview.Unavailable
            }
            // Missing/pruned files can return after Undo or a retry; failures are not immutable.
            if (preview != ChangePreview.Unavailable) {
                cache[key] = preview
                versions[after.snapId] = preview
            }
            preview
        }
    }

    private suspend fun build(before: Snap, after: Snap): ChangePreview {
        val context = kotlinx.coroutines.currentCoroutineContext()
        context.ensureActive()
        if (before.isImageSnapshot() && after.isImageSnapshot()) {
            val beforeBytes = snaps.getSnapContent(before.snapId)
            val afterBytes = snaps.getSnapContent(after.snapId)
            val diff = PlatformImageDiff.compare(
                beforeBytes = beforeBytes,
                afterBytes = afterBytes,
                includeDiffImage = false,
            )
            if (diff.error != null && diff.dimensionMismatch == null) return ChangePreview.Unavailable
            val visual = diff.visualChange ?: computeVisualChange(beforeBytes, afterBytes)
            if (diff.dimensionMismatch == null && diff.differentPixels == 0L && diff.totalPixels > 0) return ChangePreview.NothingVisible
            return ChangePreview.Crop(
                beforeSnapId = before.snapId,
                afterSnapId = after.snapId,
                bounds = diff.changedBounds,
                percent = diff.mismatchPercentage,
                sizeChanged = diff.dimensionMismatch != null,
                regions = visual?.regions.orEmpty(),
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
            checkCancelled = { context.ensureActive() },
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
private const val SAMPLE_CHARS = 90

private val digits = Regex("\\d+([.,:]\\d+)*")

/** "12 minutes ago", "3 hours ago", "yesterday": relative times tick like counters, so they are masked like numbers. */
private val relativeTime = Regex(
    "\\b(#\\s*)?(seconds?|secs?|minutes?|mins?|hours?|hrs?|days?|weeks?|months?|years?|segundos?|minutos?|horas?|d[ií]as?|semanas?|meses|mes|años?|anos?|secondi|minuti|ore|ora|giorni|giorno|settimane|mesi|anni)\\b(\\s+(ago|atrás|fa))?|\\b(just now|yesterday|today|ayer|hoy|ontem|hoje|ieri|oggi)\\b",
    RegexOption.IGNORE_CASE,
)

/** Masks the parts of a line that change on their own: numbers and relative times. */
private fun mask(line: String): String = relativeTime.replace(digits.replace(line, "#"), "#t")

/** True when two passages differ only in their numbers ("76 points · 1 hour ago" → "80 points · 2 hours ago" is not, "76 points" → "80 points" is). */
internal fun onlyNumbersDiffer(before: String, after: String): Boolean = before != after && mask(before) == mask(after)

/** How much real wording a change carries, so previews lead with words rather than counters. */
private val currency = Regex("[$€£¥₹₩₽]|R\\$|\\b(USD|EUR|GBP|BRL|JPY|CHF|CAD|AUD)\\b")

private fun meaning(before: String, after: String): Int {
    // Prices are numbers people care about; counters and times are not.
    if (onlyNumbersDiffer(before, after)) return if (currency.containsMatchIn(after)) 1_000 else 0
    val words = wordDiff(before.take(400), after.take(400)).filter { it.kind != EditSegment.Kind.Same }.sumOf { segment -> segment.text.count { it.isLetter() } }
    return words.coerceAtLeast(1)
}

/** A busy page's change, sorted the way a person reads it: what is new, what is gone, what only ticked. */
data class TextDigest(
    val added: List<String>,
    val removed: List<String>,
    /** Old → new lines that are mostly the same words: real edits, shown as word-level changes. */
    val edited: List<Pair<String, String>>,
    /** Old → new lines whose words are the same and only numbers moved (counters, times, prices). */
    val numberUpdates: List<Pair<String, String>>,
    /** Exactly the same lines in a different order. */
    val reordered: Boolean,
)

/** Compares the sets of lines with numbers masked, so shifting and re-sorting are not mistaken for change. */
fun textDigest(oldLines: List<String>, newLines: List<String>, checkCancelled: () -> Unit = {}): TextDigest {
    fun norm(line: String) = mask(line.trim())
    val old = oldLines.map { it.trim() }.filter { it.isNotEmpty() }
    val new = newLines.map { it.trim() }.filter { it.isNotEmpty() }
    if (old != new && old.sorted() == new.sorted()) return TextDigest(emptyList(), emptyList(), emptyList(), emptyList(), reordered = true)
    val oldByNorm = old.groupBy(::norm)
    val newNorms = new.mapTo(HashSet(), ::norm)
    val oldExact = old.toHashSet()

    // "1.", "·", "|" and other letterless fragments are layout, not content.
    fun words(line: String) = line.count { it.isLetter() } >= 3
    val addedAll = new.filter { words(it) && norm(it) !in oldByNorm }.distinct()
    val removedAll = old.filter { words(it) && norm(it) !in newNorms }.distinct()
    val updates = new.filter { it !in oldExact }.mapNotNull { line -> oldByNorm[norm(line)]?.firstOrNull()?.let { it to line } }.distinct()
    // A "new" line that mostly repeats the words of a "gone" line is an edit of it, not a new item.
    val edited = mutableListOf<Pair<String, String>>()
    // Tokenize each candidate once, rather than once for every possible match.
    val unmatched = removedAll.take(300).map { it to wordSet(it) }.toMutableList()
    val added = mutableListOf<String>()
    addedAll.forEachIndexed { index, line ->
        checkCancelled()
        val words = wordSet(line)
        val match = if (index < 300) unmatched.maxByOrNull { similarity(words, it.second) }?.takeIf { similarity(words, it.second) >= 0.5f } else null
        if (match != null) {
            edited += match.first to line
            unmatched.remove(match)
        } else {
            added += line
        }
    }
    val editedBefore = edited.mapTo(HashSet()) { it.first }
    val removed = removedAll.filter { it !in editedBefore }

    // Lines that repeat one template with different values ("12 points by ana 3 hours ago") are churn: when three
    // or more edits keep the same unchanged words, they join the folded updates instead of reading as edits.
    fun template(pair: Pair<String, String>) = wordDiff(pair.first, pair.second, checkCancelled).filter { it.kind == EditSegment.Kind.Same }.joinToString(" ") { it.text.trim() }.trim()
    val withTemplates = edited.map { it to template(it) }
    val templateCounts = withTemplates.groupingBy { it.second }.eachCount()
    val (churn, realEdits) = withTemplates.partition { (_, key) -> key.length >= 8 && (templateCounts[key] ?: 0) >= 3 }
    return TextDigest(added, removed, realEdits.map { it.first }, updates + churn.map { it.first }, reordered = false)
}

private val wordSeparators = Regex("[^\\p{L}#]+")
private fun wordSet(line: String): Set<String> = mask(line.lowercase()).split(wordSeparators).filter { it.length > 1 || it == "#" }.toSet()

private fun similarity(a: Set<String>, b: Set<String>): Float {
    if (a.isEmpty() || b.isEmpty()) return 0f
    val common = if (a.size <= b.size) a.count { it in b } else b.count { it in a }
    return common.toFloat() / (a.size + b.size - common)
}

/** Pure and synchronous so it is easy to test with plain strings. */
internal fun textPreview(oldLines: List<String>, newLines: List<String>, headings: Set<String> = emptySet(), checkCancelled: () -> Unit = {}): ChangePreview = analyzeTextPreview(oldLines, newLines, headings, checkCancelled).preview

internal data class TextPreviewAnalysis(val preview: ChangePreview, val digest: TextDigest? = null)

/** Reuse the digest already needed for a large preview instead of calculating it twice in the reader. */
internal fun analyzeTextPreview(oldLines: List<String>, newLines: List<String>, headings: Set<String> = emptySet(), checkCancelled: () -> Unit = {}): TextPreviewAnalysis {
    checkCancelled()
    val rows = calculateDiffRows(oldLines, newLines, checkCancelled)
    val sections = textChangeSections(rows, headings).filter { section ->
        section.before.any { it.content.isNotBlank() } || section.after.any { it.content.isNotBlank() }
    }
    if (sections.isEmpty()) return TextPreviewAnalysis(ChangePreview.Invisible)
    val passages = sections.map { section ->
        val before = section.before.joinToString(" ") { it.content }
        val after = section.after.joinToString(" ") { it.content }
        Triple(section, before, after)
    }
    val numbersOnly = passages.all { (_, before, after) -> onlyNumbersDiffer(before, after) }
    val changed = sections.flatMap { it.before + it.after }.filter { it.content.isNotBlank() }
    val changedChars = changed.sumOf { it.content.length }
    val total = maxOf(oldLines.size, newLines.size, 1)
    // A replaced line is one change, rather than one deletion plus one addition.
    val changedLines = sections.sumOf { section ->
        maxOf(section.before.count { it.content.isNotBlank() }, section.after.count { it.content.isNotBlank() })
    }.coerceAtMost(total)
    val sectionsShare = changedLines.toFloat() / total
    val scored = passages.mapIndexed { index, passage -> Triple(index, passage, meaning(passage.second, passage.third)) }
    if (changedChars > LARGE_CHANGED_CHARS || (changedLines > 8 && sectionsShare > 0.4f)) {
        val digest = textDigest(oldLines, newLines, checkCancelled)
        if (digest.reordered) return TextPreviewAnalysis(ChangePreview.Large(changedLines, total, reordered = true), digest)
        val addedLines = digest.added
        val removedLines = digest.removed
        val updated = digest.numberUpdates.size + digest.edited.size
        val samples = buildList {
            digest.edited.sortedByDescending { meaning(it.first, it.second) }.take(1).filter { meaning(it.first, it.second) >= 24 && meaning(it.first, it.second) < 1_000 }.forEach { (before, after) ->
                add(trimAroundFirstEdit(wordDiff(before.take(300), after.take(300)), SAMPLE_CHARS).first)
            }
            addedLines.sortedByDescending { line -> line.count { it.isLetter() } }.take(3 - size).forEach { add(listOf(EditSegment(clip(it), EditSegment.Kind.Added))) }
            removedLines.sortedByDescending { line -> line.count { it.isLetter() } }.take(3 - size).forEach { add(listOf(EditSegment(clip(it), EditSegment.Kind.Removed))) }
            if (isEmpty()) {
                val pairs = sections.flatMap { section ->
                    val before = section.before.map { it.content }.filter { it.isNotBlank() }
                    val after = section.after.map { it.content }.filter { it.isNotBlank() }
                    (0 until maxOf(before.size, after.size)).map { i -> before.getOrElse(i) { "" } to after.getOrElse(i) { "" } }
                }.filter { it.first.trim() != it.second.trim() }
                pairs.withIndex().map { (index, pair) -> Triple(index, pair, meaning(pair.first, pair.second)) }
                    .sortedByDescending { it.third }.take(3).sortedBy { it.first }
                    .forEach { (_, pair, _) -> add(trimAroundFirstEdit(wordDiff(pair.first.take(300), pair.second.take(300)), SAMPLE_CHARS).first) }
            }
        }
        return TextPreviewAnalysis(ChangePreview.Large(changedLines, total, samples, numbersOnly || (addedLines.isEmpty() && removedLines.isEmpty() && digest.edited.isEmpty() && updated > 0), addedLines.size, removedLines.size, updated), digest)
    }
    // Page order among meaningful changes; only counter/time churn is skipped.
    val (first, before, after) = (scored.firstOrNull { it.third > 0 } ?: scored.first()).second
    val segments = wordDiff(before, after)
    val (trimmed, leading, trailing) = trimAroundFirstEdit(segments)
    val section = first.context?.takeIf { headings.isEmpty() || it in headings }?.takeIf { it.length <= 60 }
    return TextPreviewAnalysis(ChangePreview.Words(section, trimmed, sections.size - 1, leading, trailing, numbersOnly))
}

private fun clip(line: String): String = line.trim().let { if (it.length > SAMPLE_CHARS) it.take(SAMPLE_CHARS).substringBeforeLast(' ') + "…" else it }

private val wordPattern = Regex("\\s+|[^\\s]+")

/** Word-level edit between two short passages; whole-line rewrites still read naturally as old → new. */
internal fun wordDiff(before: String, after: String, checkCancelled: () -> Unit = {}): List<EditSegment> {
    val old = wordPattern.findAll(before).map { it.value }.toList()
    val new = wordPattern.findAll(after).map { it.value }.toList()
    if (old.isEmpty()) return listOf(EditSegment(after, EditSegment.Kind.Added))
    if (new.isEmpty()) return listOf(EditSegment(before, EditSegment.Kind.Removed))
    val patch = com.bernaferrari.difflib.DiffUtils.diff(old, new, checkCancelled)
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
        if (hasRemoved && !hasAdded) {
            // Keep boundary whitespace for insertions/deletions so adjacent words do not run together.
            push(cluster.joinToString("") { it.second }, EditSegment.Kind.Removed)
        } else if (hasAdded && !hasRemoved) {
            push(cluster.joinToString("") { it.second }, EditSegment.Kind.Added)
        } else {
            if (removed.isNotEmpty()) push(removed, EditSegment.Kind.Removed)
            if (removed.isNotEmpty() && added.isNotEmpty()) push(" ", EditSegment.Kind.Same)
            if (added.isNotEmpty()) push(added, EditSegment.Kind.Added)
        }
        index = lastEdit + 1
    }
    return out
}

/** Keep a little context before the first edit and stop once the snippet is card-sized. */
private fun trimAroundFirstEdit(segments: List<EditSegment>, limit: Int = SNIPPET_CHARS): Triple<List<EditSegment>, Boolean, Boolean> {
    val firstEdit = segments.indexOfFirst { it.kind != EditSegment.Kind.Same }
    if (firstEdit < 0) return Triple(segments, false, false)
    val out = mutableListOf<EditSegment>()
    var leading = false
    if (firstEdit > 0) {
        val context = segments.subList(0, firstEdit).joinToString("") { it.text }
        val keep = if (limit < SNIPPET_CHARS) 24 else CONTEXT_CHARS
        if (context.length > keep) {
            leading = true
            val cut = context.substring(context.length - keep)
            out += EditSegment(cut.substringAfter(' ', cut), EditSegment.Kind.Same)
        } else {
            out += EditSegment(context, EditSegment.Kind.Same)
        }
    }
    var length = out.sumOf { it.text.length }
    var trailing = false
    for (segment in segments.drop(firstEdit)) {
        val room = limit - length
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
