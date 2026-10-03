/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun VisualChangeFilterField(basisPoints: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val options = listOf(
        0 to stringResource(Res.string.visual_filter_off),
        10 to stringResource(Res.string.visual_filter_small),
        100 to stringResource(Res.string.visual_filter_medium),
        500 to stringResource(Res.string.visual_filter_large),
    ).let { if (basisPoints in 1..10_000 && it.none { entry -> entry.first == basisPoints }) it + (basisPoints to "${basisPoints / 100.0}%") else it }
    val selectedLabel = options.firstOrNull { it.first == basisPoints }?.second ?: options.first().second
    Surface(onClick = { choosing = true }, enabled = enabled, color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.visual_filter_title), style = MaterialTheme.typography.bodyMedium)
            Text(selectedLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text(stringResource(Res.string.visual_filter_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                    Text(stringResource(Res.string.visual_filter_description), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    options.forEach { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(basisPoints == value, role = Role.RadioButton) {
                                onChange(value)
                                choosing = false
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = basisPoints == value, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
