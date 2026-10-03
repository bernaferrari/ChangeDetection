/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.preview.EditSegment
import com.bernaferrari.changedetection.preview.wordDiff
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_text
import com.bernaferrari.changedetection.shared.generated.resources.reader_hidden_body
import com.bernaferrari.changedetection.shared.generated.resources.reader_hidden_title
import com.bernaferrari.changedetection.shared.generated.resources.reader_more_lines
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
    data class Unchanged(val id: Int, val lines: List<String>) : ReaderBlock
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
        if (before.isNotEmpty() || after.isNotEmpty()) {
            blocks += ReaderBlock.Change(++number, changeSection, before, after)
            before = mutableListOf()
            after = mutableListOf()
        }
    }
    fun flushUnchanged() {
        if (unchanged.isNotEmpty()) {
            blocks += ReaderBlock.Unchanged(foldId++, unchanged)
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

/** Lets the floating navigator move between changes in the reader below it. */
@Stable
internal class ReaderState {
    val list = LazyListState()
    var changeIndices by mutableStateOf(emptyList<Int>())
    val changeCount: Int get() = changeIndices.size
    val current: Int by derivedStateOf {
        val first = list.firstVisibleItemIndex
        changeIndices.indexOfFirst { it >= first }.let { if (it < 0) changeIndices.lastIndex else it }.coerceAtLeast(0)
    }

    suspend fun go(delta: Int) {
        if (changeIndices.isEmpty()) return
        val target = (current + delta).coerceIn(0, changeIndices.lastIndex)
        list.animateScrollToItem(changeIndices[target])
    }
}

private data class ReaderContent(val blocks: List<ReaderBlock>, val changes: Int)

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
    val content by produceState<ReaderContent?>(null, oldContent, newContent) {
        value = null
        value = withContext(Dispatchers.Default) {
            val headings = if (newIsHtml) {
                runCatching { com.fleeksoft.ksoup.Ksoup.parse(newContent).select("h1,h2,h3,h4,h5,h6,dt,legend,th").map { it.text().trim() }.filter { it.isNotEmpty() }.toSet() }.getOrDefault(emptySet())
            } else {
                emptySet()
            }
            val blocks = readerBlocks(
                readableSnapshotText(oldContent, oldIsHtml).lines(),
                readableSnapshotText(newContent, newIsHtml).lines(),
                headings,
            )
            ReaderContent(blocks, blocks.count { it is ReaderBlock.Change })
        }
    }
    val loaded = content
    if (loaded == null) {
        Box(modifier, contentAlignment = Alignment.Center) { LoadingIndicator() }
        return
    }
    if (loaded.changes == 0) {
        SideEffect { state.changeIndices = emptyList() }
        NothingVisible(oldIsHtml || newIsHtml, onShowSource, modifier)
        return
    }
    val expanded: SnapshotStateList<Int> = remember(loaded) { emptyList<Int>().toMutableStateList() }
    val indices = remember(loaded) {
        var index = 0
        buildList {
            loaded.blocks.forEach { block ->
                if (block is ReaderBlock.Change) add(index)
                index++
            }
        }
    }
    SideEffect { state.changeIndices = indices }
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = state.list,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("comparison-lines"),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
        ) {
            itemsIndexed(loaded.blocks, key = { _, block -> if (block is ReaderBlock.Change) "c${block.number}" else "u${(block as ReaderBlock.Unchanged).id}" }) { _, block ->
                when (block) {
                    is ReaderBlock.Change -> ChangeBlock(block, Modifier.padding(vertical = 4.dp))

                    is ReaderBlock.Unchanged -> UnchangedBlock(block, block.id in expanded || block.lines.size <= SHORT_RUN) {
                        if (block.id in expanded) expanded.remove(block.id) else expanded.add(block.id)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeBlock(block: ReaderBlock.Change, modifier: Modifier = Modifier) {
    val marks = MaterialTheme.marks
    val beforeText = block.before.joinToString(" ").trim()
    val afterText = block.after.joinToString(" ").trim()
    val inline = beforeText.length + afterText.length <= INLINE_CHARS && block.before.size <= 4 && block.after.size <= 4
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Row(Modifier.padding(start = 0.dp, top = 14.dp, bottom = 14.dp, end = 16.dp)) {
            Box(Modifier.padding(start = 10.dp, end = 12.dp).width(4.dp).heightIn(min = 24.dp).clip(RoundedCornerShape(2.dp)).background(marks.highlightEdge))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (block.section != null) {
                    Text(block.section, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (inline) {
                    DiffText(editText(wordDiff(beforeText, afterText), marks), style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface))
                } else {
                    CappedLines(block.before, EditSegment.Kind.Removed)
                    CappedLines(block.after, EditSegment.Kind.Added)
                }
            }
        }
    }
}

@Composable
private fun CappedLines(lines: List<String>, kind: EditSegment.Kind) {
    if (lines.isEmpty()) return
    var showAll by remember(lines) { mutableStateOf(false) }
    val visible = if (showAll) lines else lines.take(12)
    val marks = MaterialTheme.marks
    visible.filter { it.isNotBlank() }.forEach { line ->
        DiffText(editText(listOf(EditSegment(line, kind)), marks), style = Reading.body.copy(color = MaterialTheme.colorScheme.onSurface))
    }
    if (!showAll && lines.size > visible.size) {
        Text(
            pluralStringResource(Res.plurals.reader_more_lines, lines.size - visible.size, lines.size - visible.size),
            Modifier.clip(MaterialTheme.shapes.small).clickable(role = Role.Button) { showAll = true }.padding(vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun UnchangedBlock(block: ReaderBlock.Unchanged, open: Boolean, onToggle: () -> Unit) {
    if (open) {
        Column(
            Modifier.fillMaxWidth().clickable(enabled = block.lines.size > SHORT_RUN, role = Role.Button, onClick = onToggle).padding(horizontal = 30.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            block.lines.filter { it.isNotBlank() }.forEach {
                Text(it, style = Reading.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.Button, onClick = onToggle)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .heightIn(min = 40.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.UnfoldMore, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                pluralStringResource(Res.plurals.reader_unchanged, block.lines.size, block.lines.size),
                Modifier.padding(start = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NothingVisible(html: Boolean, onShowSource: (() -> Unit)?, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
