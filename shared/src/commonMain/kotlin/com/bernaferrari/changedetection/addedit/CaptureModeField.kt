/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The formats beyond Screenshot and Text, which the main choice already offers. Choosing one adds it to that
 * choice; Screenshot and Text remain available in the main selector.
 */
@Composable
internal fun OtherFormatsField(mode: CaptureMode, onChange: (CaptureMode) -> Unit, enabled: Boolean, isEditing: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.editor_formats), style = MaterialTheme.typography.titleSmall)
        ConnectedChoice(listOf(CaptureMode.SOURCE, CaptureMode.HTML), mode, onChange, Modifier, enabled = enabled) {
            stringResource(it.label())
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
