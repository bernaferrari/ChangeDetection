package com.bernaferrari.changedetection.ui.logs

import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.source.SnapsRepository
import com.bernaferrari.changedetection.ui.detail.SavedPagePreview
import com.bernaferrari.changedetection.ui.detail.loadSavedPagePreview
import com.bernaferrari.changedetection.util.decodeByteArray
import com.bernaferrari.changedetection.util.readableSnapshotText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface HistoryPreviewContent {
    data class Capture(val preview: SavedPagePreview) : HistoryPreviewContent
    data class Passage(val before: String, val after: String) : HistoryPreviewContent
}

private const val MAX_PREVIEW_BYTES = 1_048_576

/** Bounded, immutable saved-file reads. Never fetches the website or marks a version read. */
internal suspend fun loadHistoryPreview(snap: Snap, repository: SnapsRepository, preceding: suspend () -> Snap?): HistoryPreviewContent {
    if (snap.contentSize !in 1..MAX_PREVIEW_BYTES) return HistoryPreviewContent.Capture(SavedPagePreview.Document)
    val textual = snap.contentType.startsWith("text/") || snap.contentType.contains("html") || snap.contentType.contains("json")
    if (!textual) return HistoryPreviewContent.Capture(loadSavedPagePreview(snap, repository))
    return try {
        withContext(Dispatchers.Default) {
            suspend fun read(version: Snap): String = readableSnapshotText(
                decodeByteArray(repository.getSnapContent(version.snapId), version.contentCharset),
                version.contentType.contains("html"),
            )
            val after = read(snap)
            val fallback = HistoryPreviewContent.Capture(SavedPagePreview.Text(after.take(1200)))
            try {
                val before = preceding()?.takeIf { it.contentSize in 1..MAX_PREVIEW_BYTES && it.contentType == snap.contentType }
                before?.let { firstChangedPassage(read(it), after) } ?: fallback
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // A pruned/missing older file must not hide a readable current capture.
                fallback
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        HistoryPreviewContent.Capture(SavedPagePreview.Unavailable)
    }
}

/** Linear scan, not a quadratic document diff. Shows context around the first text difference. */
internal fun firstChangedPassage(before: String, after: String): HistoryPreviewContent.Passage? {
    if (before == after) return null
    var start = 0
    val commonLength = minOf(before.length, after.length)
    while (start < commonLength && before[start] == after[start]) start++
    fun excerpt(text: String): String {
        val lineStart = if (start == 0) 0 else text.lastIndexOf('\n', start - 1) + 1
        val from = maxOf(lineStart, (start - 24).coerceAtLeast(0))
        val lineEnd = text.indexOf('\n', (start + 1).coerceAtMost(text.length)).let { if (it < 0) text.length else it }
        val to = minOf(lineEnd, start + 192, text.length)
        val passage = text.substring(from, to).replace(Regex("\\s+"), " ").trim()
        return (if (from > lineStart) "…" else "") + passage + if (to < lineEnd) "…" else ""
    }
    return HistoryPreviewContent.Passage(excerpt(before), excerpt(after))
}
