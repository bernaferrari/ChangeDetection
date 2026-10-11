/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.localization.stringResource

/** Lazy-item positions account for expanded originals and bounded long-line fragments. */
internal fun passageItemIndices(sections: List<TextChangeSection>, expanded: Map<Int, Boolean>): List<Int> {
    var next = 1 // The pinned reader toolbar.
    return sections.mapIndexed { index, section ->
        val start = next
        val showsBefore = expanded[index] ?: section.after.all { it.content.isBlank() }
        next += 4 + maxOf(1, section.after.size) + if (showsBefore) maxOf(1, section.before.size) else 0
        start
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PassageNavigation(sections: List<TextChangeSection>, anchors: List<Int>, listState: LazyListState, showContext: Boolean, onToggleContext: () -> Unit) {
    var toolbarHeight by remember { mutableIntStateOf(0) }
    var showPicker by remember(sections) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var destination by remember(sections) { mutableStateOf<PassageLocation?>(null) }
    val current by remember(listState, anchors, toolbarHeight) {
        derivedStateOf {
            val firstReadable = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index != 0 && it.offset + it.size > toolbarHeight }?.index ?: 1
            val position = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            destination?.takeIf { it.scrollPosition == position }?.passage
                ?: anchors.indexOfLast { it <= firstReadable }.coerceAtLeast(0)
        }
    }
    fun jump(index: Int) {
        anchors.getOrNull(index)?.let { anchor ->
            // Instant intentional navigation also respects reduced-motion users.
            scope.launch {
                listState.scrollToItem(anchor, scrollOffset = -toolbarHeight)
                // Near the end, layout may clamp scrolling. Keep the chosen passage until
                // the user scrolls again, even when several short passages fit at once.
                destination = PassageLocation(index, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().onSizeChanged { toolbarHeight = it.height }) {
        FlowRow(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (sections.size > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { jump(current - 1) }, enabled = current > 0) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(Res.string.comparison_previous_passage))
                    }
                    val position = stringResource(Res.string.comparison_passage_position, current + 1, sections.size)
                    TextButton(onClick = { showPicker = true }, modifier = Modifier.semantics { contentDescription = position }) {
                        Text("${com.bernaferrari.changedetection.util.localizedInteger(current + 1)} / ${com.bernaferrari.changedetection.util.localizedInteger(sections.size)}", style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp))
                    }
                    IconButton(onClick = { jump(current + 1) }, enabled = current < sections.lastIndex) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(Res.string.comparison_next_passage))
                    }
                }
            } else {
                Text(pluralStringResource(Res.plurals.comparison_changed_passages, sections.size, sections.size), Modifier.padding(vertical = 14.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onToggleContext) { Text(stringResource(if (showContext) Res.string.show_only_changes else Res.string.show_context)) }
        }
    }
    if (showPicker) {
        ModalBottomSheet(onDismissRequest = { showPicker = false }) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(pluralStringResource(Res.plurals.comparison_changed_passages, sections.size, sections.size), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showPicker = false }) { Icon(Icons.Rounded.Close, stringResource(Res.string.a11y_close)) }
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(sections.size, key = { it }) { index ->
                        Surface(
                            onClick = {
                                showPicker = false
                                jump(index)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = com.bernaferrari.changedetection.ui.components.segmentShape(com.bernaferrari.changedetection.ui.components.segmentPosition(index == 0, index == sections.lastIndex)),
                            color = if (index == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        ) {
                            Row(Modifier.heightIn(min = 48.dp).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(com.bernaferrari.changedetection.util.localizedInteger(index + 1), style = MaterialTheme.typography.labelLarge)
                                Text(sections[index].context ?: stringResource(Res.string.comparison_passage_position, index + 1, sections.size), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PassageLocation(val passage: Int, val scrollPosition: Pair<Int, Int>)
