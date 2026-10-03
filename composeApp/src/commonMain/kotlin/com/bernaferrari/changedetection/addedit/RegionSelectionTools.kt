/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.detection.*
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private data class SelectionTool(val label: String, val enabled: Boolean, val apply: () -> RegionSelectionDraft)

/** Keep every tool discoverable without consuming the selected page's reading space. */
@Composable
internal fun RegionSelectionTools(document: SelectableTextDocument, draft: RegionSelectionDraft, ignoreMode: Boolean, onChange: (RegionSelectionDraft) -> Unit) {
    val tools = listOf(
        SelectionTool(stringResource(Res.string.region_picker_smaller), draft.smallerTarget(document, ignoreMode) != null) { draft.smaller(document, ignoreMode) },
        SelectionTool(stringResource(Res.string.region_picker_larger), draft.largerTarget(document, ignoreMode) != null) { draft.larger(document, ignoreMode) },
        SelectionTool(stringResource(Res.string.region_picker_add_area), !ignoreMode && draft.selected != null && !draft.exceedsRuleLimit) { draft.addArea() },
        SelectionTool(stringResource(Res.string.region_picker_undo), draft.canUndo) { draft.undo() },
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelLarge
    val requiredWidth = tools.fold(12.dp) { width, tool ->
        width + with(density) { measurer.measure(tool.label, style).size.width.toDp() + 32.dp }.coerceAtLeast(64.dp)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= requiredWidth) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                tools.forEachIndexed { index, tool ->
                    if (index == 2) {
                        FilledTonalButton(onClick = { onChange(tool.apply()) }, enabled = tool.enabled, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text(tool.label) }
                    } else {
                        TextButton(onClick = { onChange(tool.apply()) }, enabled = tool.enabled, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) { Text(tool.label) }
                    }
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(stringResource(Res.string.region_picker_tools))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    tools.forEach { tool ->
                        DropdownMenuItem(text = { Text(tool.label) }, enabled = tool.enabled, onClick = {
                            expanded = false
                            onChange(tool.apply())
                        })
                    }
                }
            }
        }
    }
}
