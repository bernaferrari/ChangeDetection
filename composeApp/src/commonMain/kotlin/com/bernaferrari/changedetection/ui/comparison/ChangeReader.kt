/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.preview.wordDiff
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.digest_new
import com.bernaferrari.changedetection.shared.generated.resources.digest_changed
import com.bernaferrari.changedetection.shared.generated.resources.digest_removed
import com.bernaferrari.changedetection.shared.generated.resources.digest_numbers
import com.bernaferrari.changedetection.shared.generated.resources.digest_numbers_body
import com.bernaferrari.changedetection.shared.generated.resources.digest_show_full
import com.bernaferrari.changedetection.shared.generated.resources.preview_reordered
import com.bernaferrari.changedetection.shared.generated.resources.preview_reordered_body
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_text
import com.bernaferrari.changedetection.shared.generated.resources.reader_hidden_body
import com.bernaferrari.changedetection.shared.generated.resources.reader_hidden_title
import com.bernaferrari.changedetection.shared.generated.resources.reader_more_lines
import com.bernaferrari.changedetection.shared.generated.resources.reader_position
import com.bernaferrari.changedetection.shared.generated.resources.reader_hide_unchanged
import com.bernaferrari.changedetection.shared.generated.resources.reader_collapse_change
import com.bernaferrari.changedetection.shared.generated.resources.preview_before
import com.bernaferrari.changedetection.shared.generated.resources.preview_after
import com.bernaferrari.changedetection.shared.generated.resources.reader_show_source
import com.bernaferrari.changedetection.shared.generated.resources.reader_unchanged
import com.bernaferrari.changedetection.ui.kit.DiffText
import com.bernaferrari.changedetection.ui.kit.editText
import com.bernaferrari.changedetection.ui.theme.Reading
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.readableSnapshotText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** One block of the reader: either a run of unchanged lines or one change. */
internal sealed interface ReaderBlock {
    data class Unchanged(val id: Int, val lines: List<String>, val originalLineCount: Int = lines.size) : ReaderBlock
    data class Change(val number: Int, val section: String?, val before: List<String>, val after: List<String>) : ReaderBlock
}

private const val SHORT_RUN = 2
private const val INLINE_CHARS = 700

/** Pure grouping so long pages become "a few changes with folded text between them". */
internal fun readerBlocks(oldLines: List<String>, newLines: List<String>, headings: Set<String>): List<ReaderBlock> {
    val rows = calculateDiffRows(oldLines, newLines)
    val blocks = mutableListOf<ReaderBlock>()
    var unchanged = mutableListOf<String>()
    var before = mutableListOf<String>()
    var after = mutableListOf<String>()
    var section: String? = null
    var changeSection: String? = null
    var foldId = 0
    var number = 0
    fun flushChange() {
        if (before.any { it.isNotBlank() } || after.any { it.isNotBlank() }) {
            blocks += ReaderBlock.Change(++number, changeSection, boundedReaderLines(before), boundedReaderLines(after))
        }
        before = mutableListOf()
        after = mutableListOf()
    }
    fun flushUnchanged() {
        if (unchanged.isNotEmpty()) {
            blocks += ReaderBlock.Unchanged(foldId++, boundedReaderLines(unchanged), unchanged.size)
            unchanged = mutableListOf()
        }
    }
    for (row in rows) {
        if (row.type == DiffLineType.UNCHANGED) {
            flushChange()
            unchanged += row.content
            val text = row.content.trim()
            if (text.isNotEmpty() && (if (headings.isEmpty()) text.length <= 60 else text in headings)) section = text.take(80)
        } else {
            if (before.isEmpty() && after.isEmpty()) {
                flushUnchanged()
                changeSection = section
            }
            if (row.type == DiffLineType.REMOVED) before += row.content else after += row.content
        }
    }
    flushChange()
    flushUnchanged()
    return blocks
}

/** One oversized source line must not produce an unbounded text-layout operation. */
private fun boundedReaderLines(lines: List<String>): List<String> = displayDiffRows(
    lines.mapIndexed { index, line -> DiffRowModel(index + 1, line, DiffLineType.UNCHANGED) },
).map { it.content }

/** Lets the floating navigator move between changes in the reader below it. */
@Stable
/** Steps through the changes of whatever comparison is on screen; drives the shared bottom bar. */
internal interface ChangeCursor {
    val current: Int
    val changeCount: Int
    suspend fun go(delta: Int)
}

/**
 * The changed areas of a screenshot comparison. One cursor is shared by the Changes and Page views, so
 * switching between them keeps your place; the visible view registers how to scroll to an area.
 */
@Stable
internal class RegionCursor : ChangeCursor {
    override var current by mutableIntStateOf(0)
    override var changeCount by mutableIntStateOf(0)
    var scroller: (suspend (Int) -> Unit)? = null

    /** The change the reader asked for (arrows, a tap); cleared as soon as they scroll by hand. */
    var emphasized by mutableStateOf<Int?>(null)

    /** True while [go] is scrolling, so views can tell their own scrolling from the reader's. */
    var navigating = false
        private set

    override suspend fun go(delta: Int) {
        if (changeCount == 0) return
        current = (current + delta).coerceIn(0, changeCount - 1)
        emphasized = current
        navigating = true
        try {
            scroller?.invoke(current)
        } finally {
            navigating = false
        }
    }
}

internal class ReaderState : ChangeCursor {
    val list = LazyListState()
    var changeIndices by mutableStateOf(emptyList<Int>())
    override val changeCount: Int get() = changeIndices.size
    private var destination by mutableStateOf<Pair<Int, Pair<Int, Int>>?>(null)
    override val current: Int by derivedStateOf {
        if (changeIndices.isEmpty()) return@derivedStateOf 0
        val position = list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset
        destination?.takeIf { it.second == position }?.first?.coerceAtMost(changeIndices.lastIndex)
            ?: changeIndices.indexOfLast { it <= position.first }.coerceAtLeast(0)
    }

    override suspend fun go(delta: Int) {
        if (changeIndices.isEmpty()) return
        val target = (current + delta).coerceIn(0, changeIndices.lastIndex)
        // Intentional navigation is immediate, including when system animations are disabled.
        list.scrollToItem(changeIndices[target])
        // A short final change may not reach the top. Keep its selection until manual scrolling.
        destination = target to (list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset)
    }
}

private data class ReaderContent(val blocks: List<ReaderBlock>, val changes: Int, val digest: com.bernaferrari.changedetection.preview.TextDigest? = null)

private sealed interface ReaderItem {
    val key: String
    data class Change(val block: ReaderBlock.Change, val expanded: Boolean) : ReaderItem {
        override val key = "c${block.number}"
    }
    data class ChangedLine(val change: Int, val lineIndex: Int, val text: String, val kind: EditSegment.Kind) : ReaderItem {
        override val key = "c$change-$kind-$lineIndex"
    }
    data class Fold(val block: ReaderBlock.Unchanged, val open: Boolean) : ReaderItem {
        override val key = "u${block.id}"
    }
    data class ContextLine(val blockId: Int, val lineIndex: Int, val text: String) : ReaderItem {
        override val key = "u$blockId-$lineIndex"
    }
}

/** Expanded context is still virtualized; a long page never becomes one enormous lazy item. */
private fun readerItems(blocks: List<ReaderBlock>, expanded: List<Int>, expandedChanges: List<Int>): List<ReaderItem> = buildList {
    blocks.forEach { block ->
        when (block) {
            is ReaderBlock.Change -> {
                val open = block.number in expandedChanges
                add(ReaderItem.Change(block, open))
                if (open) {
                    block.before.forEachIndexed { index, line -> if (line.isNotBlank()) add(ReaderItem.ChangedLine(block.number, index, line, EditSegment.Kind.Removed)) }
                    block.after.forEachIndexed { index, line -> if (line.isNotBlank()) add(ReaderItem.ChangedLine(block.number, index, line, EditSegment.Kind.Added)) }
                }
            }

            is ReaderBlock.Unchanged -> {
                val open = block.id in expanded || block.lines.size <= SHORT_RUN
                if (block.lines.size > SHORT_RUN) add(ReaderItem.Fold(block, open))
                if (open) {
                    block.lines.forEachIndexed { index, line ->
                        if (line.isNotBlank()) add(ReaderItem.ContextLine(block.id, index, line))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ChangeReader(
    oldContent: String,
    newContent: String,
    oldIsHtml: Boolean,
    newIsHtml: Boolean,
    state: ReaderState,
    onShowSource: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val content by produceState<ReaderContent?>(null, oldContent, newContent, oldIsHtml, newIsHtml) {
        value = null
        value = withContext(Dispatchers.Default) {
            val headings = if (newIsHtml) {
                runCatching { com.fleeksoft.ksoup.Ksoup.parse(newContent).select("h1,h2,h3,h4,h5,h6,dt,legend,th").map { it.text().trim() }.filter { it.isNotEmpty() }.toSet() }.getOrDefault(emptySet())
            } else {
                emptySet()
            }
            val oldLines = readableSnapshotText(oldContent, oldIsHtml).lines()
            val newLines = readableSnapshotText(newContent, newIsHtml).lines()
            val blocks = readerBlocks(oldLines, newLines, headings)
            // Busy pages read better as "what's new / gone / only ticked" than as a positional diff.
            val busy = com.bernaferrari.changedetection.preview.textPreview(oldLines, newLines, headings) is com.bernaferrari.changedetection.preview.ChangePreview.Large
            ReaderContent(blocks, blocks.count { it is ReaderBlock.Change }, if (busy) com.bernaferrari.changedetection.preview.textDigest(oldLines, newLines) else null)
        }
    }
    val loaded = content
    if (loaded == null) {
        SideEffect { state.changeIndices = emptyList() }
        Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    if (loaded.changes == 0) {
        SideEffect { state.changeIndices = emptyList() }
        NothingVisible(oldIsHtml || newIsHtml, onShowSource, modifier)
        return
    }
    var showFull by remember(loaded) { mutableStateOf(false) }
    val digest = loaded.digest
    if (digest != null && !showFull) {
        SideEffect { state.changeIndices = emptyList() }
        DigestView(digest, onShowFull = { showFull = true }, modifier = modifier)
        return
    }
    val expanded: SnapshotStateList<Int> = remember(loaded) { emptyList<Int>().toMutableStateList() }
    val expandedChanges = remember(loaded) { emptyList<Int>().toMutableStateList() }
    val items = remember(loaded, expanded.toList(), expandedChanges.toList()) { readerItems(loaded.blocks, expanded, expandedChanges) }
    val indices = remember(items) { items.indices.filter { items[it] is ReaderItem.Change } }
    SideEffect { state.changeIndices = indices }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = state.list,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("comparison-lines"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        ) {
            items(items, key = { it.key }, contentType = { it::class }) { item ->
                when (item) {
                    is ReaderItem.Change -> ChangeBlock(item.block, loaded.changes, item.expanded, { if (item.block.number in expandedChanges) expandedChanges.remove(item.block.number) else expandedChanges.add(item.block.number) }, Modifier.padding(vertical = 4.dp))

                    is ReaderItem.ChangedLine -> ChangedLine(item)

                    is ReaderItem.Fold -> UnchangedFold(item.block, item.open) {
                        if (item.block.id in expanded) expanded.remove(item.block.id) else expanded.add(item.block.id)
                    }

                    is ReaderItem.ContextLine -> Text(
                        item.text,
                        Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 2.dp),
                        style = Reading.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A busy page's change as three plain lists: new, removed, and (folded) lines where only numbers moved. */
@Composable
private fun DigestView(digest: com.bernaferrari.changedetection.preview.TextDigest, onShowFull: () -> Unit, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    var numbersOpen by remember(digest) { mutableStateOf(false) }
    val newTitle = stringResource(Res.string.digest_new)
    val removedTitle = stringResource(Res.string.digest_removed)
    val changedTitle = stringResource(Res.string.digest_changed)
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("comparison-digest"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (digest.reordered) {
                item("reordered") {
                    DigestNote(Icons.Rounded.UnfoldMore, stringResource(Res.string.preview_reordered), stringResource(Res.string.preview_reordered_body))
                }
            }
            fun group(key: String, title: String, lines: List<String>, kind: EditSegment.Kind) {
                if (lines.isEmpty()) return
                item("$key-h") { DigestHeader(title, lines.size) }
                itemsIndexed(lines.take(200), key = { index, _ -> "$key-$index" }) { index, line ->
                    Box(Modifier.fillMaxWidth().clip(com.bernaferrari.changedetection.ui.kit.segmentShape(index, minOf(lines.size, 200), outer = 20.dp)).background(MaterialTheme.colorScheme.surfaceBright).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        DiffText(editText(listOf(EditSegment(line.take(600), kind)), marks), style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface))
                    }
                }
            }
            group("new", newTitle, digest.added, EditSegment.Kind.Added)
            group("gone", removedTitle, digest.removed, EditSegment.Kind.Removed)
            if (digest.edited.isNotEmpty()) {
                item("edited-h") { DigestHeader(changedTitle, digest.edited.size) }
                itemsIndexed(digest.edited.take(200), key = { index, _ -> "edited-$index" }) { index, (before, after) ->
                    Box(Modifier.fillMaxWidth().clip(com.bernaferrari.changedetection.ui.kit.segmentShape(index, minOf(digest.edited.size, 200), outer = 20.dp)).background(MaterialTheme.colorScheme.surfaceBright).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        DiffText(editText(com.bernaferrari.changedetection.preview.wordDiff(before.take(600), after.take(600)), marks), style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface))
                    }
                }
            }
            if (digest.numberUpdates.isNotEmpty()) {
                item("numbers-h") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 20.dp).clip(MaterialTheme.shapes.extraLarge).clickable(role = Role.Button) { numbersOpen = !numbersOpen }
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh).heightIn(min = 56.dp).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.Numbers, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(Res.string.digest_numbers, digest.numberUpdates.size), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(Res.string.digest_numbers_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(if (numbersOpen) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (numbersOpen) {
                    itemsIndexed(digest.numberUpdates.take(200), key = { index, _ -> "num-$index" }) { _, (before, after) ->
                        DiffText(
                            editText(com.bernaferrari.changedetection.preview.wordDiff(before.take(400), after.take(400)), marks),
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                    }
                }
            }
            item("full") {
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onShowFull, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.digest_show_full)) }
                }
            }
        }
    }
}

@Composable
private fun DigestHeader(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 20.dp, bottom = 8.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Box(Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest).padding(horizontal = 8.dp, vertical = 1.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DigestNote(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceBright).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChangeBlock(block: ReaderBlock.Change, total: Int, expanded: Boolean, onExpand: () -> Unit, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    val beforeText = block.before.joinToString(" ").trim()
    val afterText = block.after.joinToString(" ").trim()
    val inline = beforeText.length + afterText.length <= INLINE_CHARS && block.before.size <= 4 && block.after.size <= 4
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceBright) {
        Row(Modifier.padding(start = 0.dp, top = 14.dp, bottom = 14.dp, end = 16.dp)) {
            Box(Modifier.padding(start = 10.dp, end = 12.dp).width(4.dp).heightIn(min = 24.dp).clip(RoundedCornerShape(2.dp)).background(marks.highlightEdge))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (block.section != null) {
                    Text(block.section, Modifier.semantics { heading() }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expanded) {
                    Text(stringResource(Res.string.reader_position, block.number, total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onExpand, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.reader_collapse_change)) }
                } else if (inline) {
                    val beforeLabel = stringResource(Res.string.preview_before)
                    val afterLabel = stringResource(Res.string.preview_after)
                    val description = listOfNotNull(
                        beforeText.takeIf { it.isNotEmpty() }?.let { "$beforeLabel: $it" },
                        afterText.takeIf { it.isNotEmpty() }?.let { "$afterLabel: $it" },
                    ).joinToString("\n")
                    DiffText(
                        editText(wordDiff(beforeText, afterText), marks),
                        Modifier.clearAndSetSemantics { contentDescription = description },
                        style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                } else {
                    CappedLines(block.before, EditSegment.Kind.Removed, onExpand)
                    CappedLines(block.after, EditSegment.Kind.Added, onExpand)
                }
            }
        }
    }
}

@Composable
private fun CappedLines(lines: List<String>, kind: EditSegment.Kind, onExpand: () -> Unit) {
    if (lines.isEmpty()) return
    val visible = lines.take(12)
    val marks = MaterialTheme.marks
    val label = stringResource(if (kind == EditSegment.Kind.Removed) Res.string.preview_before else Res.string.preview_after)
    visible.filter { it.isNotBlank() }.forEach { line ->
        DiffText(
            editText(listOf(EditSegment(line, kind)), marks),
            Modifier.clearAndSetSemantics { contentDescription = "$label: $line" },
            style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface),
        )
    }
    if (lines.size > visible.size) {
        TextButton(onClick = onExpand, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(pluralStringResource(Res.plurals.reader_more_lines, lines.size - visible.size, lines.size - visible.size))
        }
    }
}

/** Revealed long changes stay lazy, including very large replacements and minified content. */
@Composable
private fun ChangedLine(item: ReaderItem.ChangedLine) {
    val label = stringResource(if (item.kind == EditSegment.Kind.Removed) Res.string.preview_before else Res.string.preview_after)
    Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceBright) {
        DiffText(
            editText(listOf(EditSegment(item.text, item.kind)), MaterialTheme.marks),
            Modifier.padding(start = 32.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).clearAndSetSemantics { contentDescription = "$label: ${item.text}" },
            style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun UnchangedFold(block: ReaderBlock.Unchanged, open: Boolean, onToggle: () -> Unit) {
    val label = if (open) stringResource(Res.string.reader_hide_unchanged) else pluralStringResource(Res.plurals.reader_unchanged, block.originalLineCount, block.originalLineCount)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(role = Role.Button, onClick = onToggle)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(if (open) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NothingVisible(html: Boolean, onShowSource: (() -> Unit)?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(80.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(Res.string.reader_hidden_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(if (html) Res.string.reader_hidden_body else Res.string.comparison_no_changes_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onShowSource != null && html) OutlinedButton(onClick = onShowSource) { Text(stringResource(Res.string.reader_show_source)) }
        }
    }
}
