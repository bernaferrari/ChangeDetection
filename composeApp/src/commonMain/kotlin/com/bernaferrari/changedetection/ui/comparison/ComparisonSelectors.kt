/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** One compact toolbar; pair selection lives in the version rail immediately below. */
@Composable
internal fun ComparisonModeSelector(
    modes: List<ComparisonMode>,
    selectedMode: ComparisonMode,
    onModeSelected: (ComparisonMode) -> Unit,
    canShare: Boolean,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (modes.size == 1) {
            Text(stringResource(modes.single().labelResource()), Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            BoxWithConstraints(Modifier.weight(1f).selectableGroup()) {
                val wrapWidth = if (modes.size > 3) 360.dp else 240.dp
                val columns = if (maxWidth / LocalDensity.current.fontScale < wrapWidth) 2 else modes.size
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    modes.chunked(columns).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                            row.forEachIndexed { index, option ->
                                ToggleButton(
                                    checked = selectedMode == option,
                                    onCheckedChange = { if (it) onModeSelected(option) },
                                    shapes = when {
                                        row.size == 1 -> ToggleButtonDefaults.shapes()
                                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        index == row.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    },
                                    colors = ToggleButtonDefaults.toggleButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    ),
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { role = Role.RadioButton },
                                ) { Text(stringResource(option.labelResource()), style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                    }
                }
            }
        }
        IconButton(onClick = onShare, enabled = canShare) {
            Icon(Icons.Default.Share, stringResource(Res.string.share_comparison))
        }
    }
}
