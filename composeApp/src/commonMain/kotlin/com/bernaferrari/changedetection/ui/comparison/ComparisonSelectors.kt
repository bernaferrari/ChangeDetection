/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.share_comparison
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** Keeps mode changes compact on small displays while keeping the share action immediately reachable. */
@Composable
internal fun ComparisonModeSelector(
    modes: List<ComparisonMode>,
    selectedMode: ComparisonMode,
    onModeSelected: (ComparisonMode) -> Unit,
    canShare: Boolean,
    onShare: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            modes.forEach { option ->
                FilterChip(
                    selected = selectedMode == option,
                    modifier = Modifier.heightIn(min = spacing.touchTarget),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onModeSelected(option)
                    },
                    label = { Text(stringResource(option.labelResource())) },
                )
            }
        }
        if (canShare) {
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(Res.string.share_comparison),
                )
            }
        }
    }
}
