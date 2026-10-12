@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.util.readableSnapshotText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.localization.stringResource

/** A reading surface: new text leads; each original passage is expandable in place. */
@Composable
internal fun SimpleDiffView(oldContent: String, newContent: String, modifier: Modifier = Modifier, oldIsHtml: Boolean = false, newIsHtml: Boolean = false) {
    var showContext by remember(oldContent, newContent) { mutableStateOf(false) }
    val expanded = remember(oldContent, newContent) { mutableStateMapOf<Int, Boolean>() }
    val diff by produceState<TextReaderContent?>(null, oldContent, newContent, oldIsHtml, newIsHtml) {
        value = null
        value = withContext(Dispatchers.Default) {
            val rows = displayDiffRows(calculateDiffRows(readableSnapshotText(oldContent, oldIsHtml).lines(), readableSnapshotText(newContent, newIsHtml).lines()))
            val headings = if (newIsHtml) com.fleeksoft.ksoup.Ksoup.parse(newContent).select("h1,h2,h3,h4,h5,h6").map { it.text() }.toSet() else emptySet()
            TextReaderContent(rows, textChangeSections(rows, headings))
        }
    }
    val content = diff
    if (content == null) {
        Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val listState = remember(oldContent, newContent) { androidx.compose.foundation.lazy.LazyListState() }
    val rows = content.rows
    val sections = content.sections
    val beforeLabel = stringResource(Res.string.comparison_before)
    val afterLabel = stringResource(Res.string.comparison_mode_after)
    val showPrevious = stringResource(Res.string.comparison_show_previous)
    val hidePrevious = stringResource(Res.string.comparison_hide_previous)
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("comparison-lines"),
            state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            stickyHeader(key = "stats") {
                PassageNavigation(sections, passageItemIndices(sections, expanded), listState, showContext) { showContext = !showContext }
            }
            if (sections.isEmpty()) {
                item(key = "unchanged") {
                    SharedEmptyState(Icons.Default.CheckCircle, stringResource(if (oldIsHtml || newIsHtml) Res.string.comparison_no_readable_changes else Res.string.comparison_no_changes_title), stringResource(if (oldIsHtml || newIsHtml) Res.string.comparison_no_readable_changes_detail else Res.string.comparison_no_changes_text))
                }
            }
            sections.forEachIndexed { sectionIndex, section ->
                item(key = "section-$sectionIndex") {
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        section.context?.let { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } ?: Spacer(Modifier.weight(1f))
                        Text(com.bernaferrari.changedetection.util.localizedInteger(sectionIndex + 1), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item(key = "after-heading-$sectionIndex") { PassageHeading(afterLabel) }
                passageLines("after-$sectionIndex", section.after, before = false)
                item(key = "before-toggle-$sectionIndex") {
                    val isExpanded = (expanded[sectionIndex] ?: section.after.all { it.content.isBlank() })
                    Surface(
                        onClick = { expanded[sectionIndex] = !isExpanded },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = if (isExpanded) hidePrevious else showPrevious },
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = if (isExpanded) 0.dp else 24.dp, bottomEnd = if (isExpanded) 0.dp else 24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Row(Modifier.heightIn(min = 48.dp).padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(beforeLabel, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Icon(if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if ((expanded[sectionIndex] ?: section.after.all { it.content.isBlank() })) passageLines("before-$sectionIndex", section.before, before = true)
                item(key = "gap-$sectionIndex") { Spacer(Modifier.height(24.dp)) }
            }
            if (showContext) {
                items(rows.size, key = { "context-$it" }) { index ->
                    val row = rows[index]
                    DiffLine(row.lineNumber, row.content, row.type, showLineNumber = !row.isContinuation)
                }
            }
        }
    }
}

private fun LazyListScope.passageLines(key: String, rows: List<DiffRowModel>, before: Boolean) {
    items(maxOf(rows.size, 1), key = { "$key-$it" }) { index ->
        val last = index == maxOf(rows.lastIndex, 0)
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = if (last) (if (before) 24.dp else 8.dp) else 0.dp, bottomEnd = if (last) (if (before) 24.dp else 8.dp) else 0.dp),
            color = if (before) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (before) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(rows.getOrNull(index)?.content?.ifBlank { " " } ?: "—", Modifier.padding(start = 18.dp, end = 18.dp, bottom = if (last) 18.dp else 8.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun PassageHeading(label: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Text(label, Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium)
    }
}

private data class TextReaderContent(val rows: List<DiffRowModel>, val sections: List<TextChangeSection>)
