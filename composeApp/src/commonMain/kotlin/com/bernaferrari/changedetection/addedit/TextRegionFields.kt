/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.detection.TextRegion
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

internal const val MAIN_TEXT_REGION = "main, [role=main], article"

@Composable
internal fun TextRegionFields(region: TextRegion, error: Boolean, enabled: Boolean, onChange: (TextRegion) -> Unit) {
    val main = region.include == MAIN_TEXT_REGION && region.exclude.isBlank()
    val custom = !region.isEmpty && !main
    var selectors by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.text_region_title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(region.isEmpty, { onChange(TextRegion()) }, enabled = enabled, label = { Text(stringResource(Res.string.text_region_whole)) })
            FilterChip(main, { onChange(TextRegion(MAIN_TEXT_REGION)) }, enabled = enabled, label = { Text(stringResource(Res.string.text_region_main)) })
            // A picked or typed region is shown as a chosen chip; its CSS stays out of sight unless asked for.
            if (custom) FilterChip(true, { selectors = !selectors }, enabled = enabled, label = { Text(stringResource(Res.string.editor_region_custom)) })
        }
        TextButton(onClick = { selectors = !selectors }, enabled = enabled, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(Res.string.editor_selectors))
            Icon(if (selectors || error) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore, null)
        }
        if (selectors || error) {
            Text(stringResource(Res.string.text_region_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = region.include,
                onValueChange = { onChange(region.copy(include = it)) },
                label = { Text(stringResource(Res.string.text_region_include)) },
                placeholder = { Text("#price, .availability") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = error,
                shape = MaterialTheme.shapes.medium,
            )
            OutlinedTextField(
                value = region.exclude,
                onValueChange = { onChange(region.copy(exclude = it)) },
                label = { Text(stringResource(Res.string.text_region_exclude)) },
                placeholder = { Text(".advert, .countdown") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = error,
                shape = MaterialTheme.shapes.medium,
            )
        }
        if (error) Text(stringResource(Res.string.text_region_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
