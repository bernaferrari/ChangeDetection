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
import com.bernaferrari.changedetection.localization.stringResource

private enum class RegionChoice { Whole, Main, Custom }

internal const val MAIN_TEXT_REGION = "main, [role=main], article"

@Composable
internal fun TextRegionFields(region: TextRegion, error: Boolean, enabled: Boolean, onChoose: (TextRegion) -> Unit, onType: (TextRegion) -> Unit = onChoose) {
    val main = region.include == MAIN_TEXT_REGION && region.exclude.isBlank()
    val custom = !region.isEmpty && !main
    var selectors by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.text_region_title), style = MaterialTheme.typography.titleSmall)
        // The same button group as Screenshot / Text; a picked or typed region shows as a third, chosen option.
        val options = if (custom) listOf(RegionChoice.Whole, RegionChoice.Main, RegionChoice.Custom) else listOf(RegionChoice.Whole, RegionChoice.Main)
        val whole = stringResource(Res.string.text_region_whole)
        val mainLabel = stringResource(Res.string.text_region_main)
        val picked = stringResource(Res.string.editor_part_picked)
        com.bernaferrari.changedetection.ui.kit.ConnectedChoice(
            options = options,
            selected = when {
                custom -> RegionChoice.Custom
                main -> RegionChoice.Main
                else -> RegionChoice.Whole
            },
            onSelect = { choice ->
                when (choice) {
                    RegionChoice.Whole -> onChoose(TextRegion())
                    RegionChoice.Main -> onChoose(TextRegion(MAIN_TEXT_REGION))
                    RegionChoice.Custom -> selectors = true
                }
            },
            modifier = Modifier,
            enabled = enabled,
        ) {
            when (it) {
                RegionChoice.Whole -> whole
                RegionChoice.Main -> mainLabel
                RegionChoice.Custom -> picked
            }
        }
        TextButton(onClick = { selectors = !selectors }, enabled = enabled, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(stringResource(Res.string.editor_selectors))
            Icon(if (selectors || error) androidx.compose.material.icons.Icons.Default.ExpandLess else androidx.compose.material.icons.Icons.Default.ExpandMore, null)
        }
        if (selectors || error) {
            Text(stringResource(Res.string.text_region_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = region.include,
                onValueChange = { onType(region.copy(include = it)) },
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
                onValueChange = { onType(region.copy(exclude = it)) },
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
