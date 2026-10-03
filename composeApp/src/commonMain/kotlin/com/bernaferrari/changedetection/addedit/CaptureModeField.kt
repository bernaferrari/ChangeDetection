/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.supportsVisualMonitoring
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CaptureModeField(mode: CaptureMode, onChange: (CaptureMode) -> Unit, enabled: Boolean, isEditing: Boolean = false) {
    var choosing by remember { mutableStateOf(false) }
    Surface(
        onClick = { choosing = true },
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.capture_mode_title), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(stringResource(mode.label()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (choosing) {
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text(stringResource(Res.string.capture_mode_title)) },
            text = {
                Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                    CaptureMode.entries.filter { supportsVisualMonitoring || it == mode || it !in setOf(CaptureMode.DEFAULT, CaptureMode.SCREENSHOT) }.forEach { option ->
                        val available = supportsVisualMonitoring || option !in setOf(CaptureMode.DEFAULT, CaptureMode.SCREENSHOT)
                        Row(
                            Modifier.fillMaxWidth().selectable(mode == option, enabled = available, role = Role.RadioButton) {
                                onChange(option)
                                choosing = false
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = mode == option, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource(option.label()), style = MaterialTheme.typography.bodyLarge)
                                Text(stringResource(if (available) option.description() else Res.string.capture_mode_saved_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (isEditing) Text(stringResource(Res.string.capture_mode_baseline), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { choosing = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

private fun CaptureMode.label() = when (this) {
    CaptureMode.TEXT -> Res.string.capture_mode_text
    CaptureMode.SOURCE -> Res.string.capture_mode_source
    CaptureMode.DEFAULT -> Res.string.capture_mode_default
    CaptureMode.HTML -> Res.string.capture_mode_html
    CaptureMode.SCREENSHOT -> Res.string.capture_mode_screenshot
}
private fun CaptureMode.description() = when (this) {
    CaptureMode.TEXT -> Res.string.capture_mode_text_description
    CaptureMode.SOURCE -> Res.string.capture_mode_source_description
    CaptureMode.DEFAULT -> Res.string.capture_mode_default_description
    CaptureMode.HTML -> Res.string.capture_mode_html_description
    CaptureMode.SCREENSHOT -> Res.string.capture_mode_screenshot_description
}
