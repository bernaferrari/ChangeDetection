/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
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
    var custom by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.text_region_title), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(region.isEmpty, { onChange(TextRegion()) }, enabled = enabled, label = { Text(stringResource(Res.string.text_region_whole)) })
            FilterChip(region.include == MAIN_TEXT_REGION && region.exclude.isBlank(), { onChange(TextRegion(MAIN_TEXT_REGION)) }, enabled = enabled, label = { Text(stringResource(Res.string.text_region_main)) })
            TextButton(onClick = { custom = !custom }, enabled = enabled) { Text(stringResource(Res.string.text_region_custom)) }
        }
        if (!region.isEmpty && !custom && !error) {
            Text(stringResource(Res.string.region_selection_active), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        if (custom || error) {
            Text(stringResource(Res.string.text_region_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (custom || error) {
            OutlinedTextField(
                value = region.include,
                onValueChange = { onChange(region.copy(include = it)) },
                label = { Text(stringResource(Res.string.text_region_include)) },
                placeholder = { Text("#price, .availability") },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                singleLine = true,
                isError = error,
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
            )
        }
        if (error) Text(stringResource(Res.string.text_region_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
