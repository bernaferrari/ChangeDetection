/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The formats beyond Screenshot and Text, which the main choice already offers. Choosing one adds it to that
 * choice; tapping it again goes back to Text.
 */
@Composable
internal fun OtherFormatsField(mode: CaptureMode, onChange: (CaptureMode) -> Unit, enabled: Boolean, isEditing: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.editor_formats), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(CaptureMode.SOURCE, CaptureMode.HTML).forEach { option ->
                val selected = mode == option
                FilterChip(
                    selected = selected,
                    onClick = { onChange(if (selected) CaptureMode.TEXT else option) },
                    enabled = enabled,
                    label = { Text(stringResource(option.label())) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
        Text(
            stringResource(if (isEditing) Res.string.capture_mode_baseline else Res.string.editor_formats_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
