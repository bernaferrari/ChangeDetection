/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.bernaferrari.changedetection.browser.supportsPageRegionSelection
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bernaferrari.changedetection.detection.SelectableTextDocument
import com.bernaferrari.changedetection.detection.selectionDocumentWithWatchedAreas
import com.bernaferrari.changedetection.detection.RegionSelectionDraft
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.localization.stringResource

@Composable
internal fun TextRegionPickerAction(document: SelectableTextDocument, initialRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion(), onSelect: (TextRegion) -> Unit) {
    var open by remember(document) { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.region_picker_title))
    }
    if (open) {
        TextRegionPicker(document, { open = false }, onSelect = {
            open = false
            onSelect(it)
        }, initialRegion = initialRegion)
    }
}

@Composable
internal fun TextRegionPicker(document: SelectableTextDocument, onDismiss: () -> Unit, onSelect: (TextRegion) -> Unit, modifier: Modifier = Modifier, pageRenderer: RegionPageRenderer = { doc, selected, select, position, layout -> RenderedRegionPreview(doc, selected, select, position, layout) }, initialRegion: com.bernaferrari.changedetection.detection.TextRegion = com.bernaferrari.changedetection.detection.TextRegion()) {
    val hasPage = document.selectionHtml != null && document.regions.isNotEmpty() && supportsPageRegionSelection
    var showPreview by remember(document) { mutableStateOf(false) }
    var showPage by remember(document) { mutableStateOf(hasPage) }
    var draft by remember(document) { mutableStateOf(RegionSelectionDraft.fromRule(initialRegion)) }
    var ignoreMode by remember(document) { mutableStateOf(false) }
    val selection = draft.selected
    val candidateRule = draft.rule
    val validation by produceState<Pair<TextRegion, String?>?>(null, document, candidateRule) {
        value = null
        if (candidateRule != null) {
            val valid = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    document.sourceHtml?.let { com.bernaferrari.changedetection.util.readableSnapshotText(candidateRule.extractHtml(it, true), true).take(8_000) }
                } catch (_: com.bernaferrari.changedetection.detection.TextRegionException) {
                    null
                }
            }
            value = candidateRule to valid
        }
    }
    val canConfirm = candidateRule != null && validation?.first == candidateRule && validation?.second != null
    val retainedHighlights = if (ignoreMode) draft.watched else draft.areas
    val pageDocument = remember(document, retainedHighlights, draft.ignored) { selectionDocumentWithWatchedAreas(document, retainedHighlights, draft.ignored) }
    val pagePosition = remember(document) { RegionPagePosition() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier.widthIn(max = 640.dp).fillMaxSize().testTag("region-picker"), color = MaterialTheme.colorScheme.surface) {
            BoxWithConstraints {
                val availableHeight = maxHeight
                val compactType = maxWidth / androidx.compose.ui.platform.LocalDensity.current.fontScale < 200.dp
                Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.region_picker_heading), Modifier.weight(1f), style = if (compactType) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onDismiss) {
                            Icon(androidx.compose.material.icons.Icons.Outlined.Close, contentDescription = stringResource(Res.string.cancel))
                        }
                    }
                    val tabs = if (hasPage) listOf(Res.string.region_picker_page, Res.string.region_picker_list, Res.string.region_picker_preview) else listOf(Res.string.region_picker_list, Res.string.region_picker_preview)
                    val tabIndex = if (showPreview) {
                        tabs.lastIndex
                    } else if (hasPage && showPage) {
                        0
                    } else {
                        tabs.lastIndex - 1
                    }
                    PrimaryScrollableTabRow(selectedTabIndex = tabIndex, edgePadding = 0.dp, containerColor = MaterialTheme.colorScheme.surface, divider = {}) {
                        tabs.forEachIndexed { index, title ->
                            Tab(selected = index == tabIndex, onClick = {
                                showPreview = index == tabs.lastIndex
                                if (!showPreview) showPage = hasPage && index == 0
                            }, text = { Text(stringResource(title)) })
                        }
                    }
                    ConnectedChoice(listOf(false, true), ignoreMode, { ignoreMode = it }, Modifier) {
                        stringResource(if (it) Res.string.region_picker_ignore else Res.string.region_picker_watch)
                    }
                    RegionSelectionTools(document, draft, ignoreMode) {
                        draft = it
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            showPreview -> RegionSelectionSummary(document, draft, validation?.takeIf { it.first == candidateRule }?.second, loading = candidateRule != null && validation?.first != candidateRule, onRemove = {
                                draft = draft.removeArea(it)
                            }, onRestore = {
                                draft = draft.restoreIgnored(it)
                            })

                            showPage -> Surface(Modifier.fillMaxSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                pageRenderer(pageDocument, if (ignoreMode) draft.activeIgnored else selection, { candidate ->
                                    draft = if (ignoreMode) draft.toggleIgnored(candidate, document) else draft.choose(candidate, document)
                                }, pagePosition, Modifier.fillMaxSize().testTag("region-page-preview"))
                            }

                            else -> LazyColumn(Modifier.fillMaxSize().testTag("region-choices"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                item { Text(stringResource(if (ignoreMode) Res.string.region_picker_ignore_hint else Res.string.region_picker_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                if (document.limited) item { Text(stringResource(Res.string.region_picker_limited), style = MaterialTheme.typography.bodySmall) }
                                if (document.regions.isEmpty()) item { Text(stringResource(Res.string.region_picker_empty), style = MaterialTheme.typography.bodyMedium) }
                                items(document.regions, key = { it.selector }) { region ->
                                    val chosen = region.selector in if (ignoreMode) draft.ignored else draft.watched
                                    Surface(
                                        onClick = { draft = if (ignoreMode) draft.toggleIgnored(region.selector, document) else draft.toggle(region.selector, document) },
                                        modifier = Modifier.fillMaxWidth().semantics {
                                            selected = chosen
                                            role = Role.Checkbox
                                        },
                                        shape = MaterialTheme.shapes.medium,
                                        color = if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                        contentColor = if (chosen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    ) {
                                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Checkbox(checked = chosen, onCheckedChange = null)
                                            Text(region.text, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Column(Modifier.heightIn(max = availableHeight * .25f).verticalScroll(rememberScrollState())) {
                        if (draft.exceedsRuleLimit) Text(stringResource(Res.string.region_picker_rule_limit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        if (candidateRule != null && validation?.first == candidateRule && validation?.second == null) Text(stringResource(Res.string.region_picker_nothing_left), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { if (canConfirm) candidateRule?.let(onSelect) }, enabled = canConfirm, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.region_picker_use)) }
                    }
                }
            }
        }
    }
}
