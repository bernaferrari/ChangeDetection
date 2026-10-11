/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.groupTile
import com.bernaferrari.changedetection.util.localizedNumber
import org.jetbrains.compose.resources.stringResource

/** Three common waits, with a custom editor that leaves the settings row's layout stable. */
@Composable
internal fun ScreenshotWaitChoice(delayMs: Long, onChange: (Long) -> Unit) {
    val presets = listOf(2000L, 5000L, 15000L)
    var editing by remember { mutableStateOf(false) }
    val customLabel = stringResource(Res.string.checks_custom)
    val waitLabel = "${localizedNumber(delayMs / 1000.0, maxFractionDigits = 2)} s"
    Column(Modifier.groupTile().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.settings_screenshot_wait), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text(waitLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(stringResource(Res.string.settings_wait_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            presets.forEachIndexed { index, value ->
                ToggleButton(
                    checked = delayMs == value,
                    onCheckedChange = { if (it) onChange(value) },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                        role = Role.RadioButton
                        selected = delayMs == value
                    },
                    shapes = if (index == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedMiddleButtonShapes(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) { Text("${localizedNumber((value / 1000).toDouble(), maxFractionDigits = 0)} s", style = MaterialTheme.typography.labelLarge) }
            }
            ToggleButton(
                checked = delayMs !in presets,
                onCheckedChange = { editing = true },
                modifier = Modifier.size(48.dp).semantics {
                    role = Role.RadioButton
                    selected = delayMs !in presets
                    contentDescription = customLabel
                },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                contentPadding = PaddingValues(0.dp),
            ) { Icon(Icons.Rounded.Edit, null, Modifier.size(20.dp)) }
        }
    }
    if (editing) {
        var text by remember { mutableStateOf((delayMs / 1000).toString()) }
        val seconds = text.toIntOrNull()?.takeIf { it in 0..60 }
        val save: () -> Unit = {
            seconds?.let {
                onChange(it * 1000L)
                editing = false
            }
        }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(Res.string.settings_screenshot_wait)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    label = { Text(customLabel) },
                    suffix = { Text(stringResource(Res.string.settings_wait_seconds)) },
                    supportingText = { Text("0–60") },
                    singleLine = true,
                    isError = seconds == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = { TextButton(onClick = save, enabled = seconds != null) { Text(stringResource(Res.string.done)) } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
