/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** An M3 Expressive connected button group behaving as a single-choice control. */
@Composable
fun <T> ConnectedChoice(options: List<T>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier.padding(horizontal = 16.dp), enabled: Boolean = true, label: @Composable (T) -> String) {
    val labels = options.map { label(it) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelLarge
    val minimumWidth = with(density) {
        labels.maxOfOrNull { measurer.measure(it, style, softWrap = false).size.width }?.toDp()
    }?.plus(24.dp)?.coerceAtLeast(48.dp) ?: 48.dp
    BoxWithConstraints(modifier.fillMaxWidth().selectableGroup()) {
        val spacing = ButtonGroupDefaults.ConnectedSpaceBetween
        val columns = ((maxWidth + spacing) / (minimumWidth + spacing)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.indices.toList().chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEachIndexed { index, optionIndex ->
                        val option = options[optionIndex]
                        ToggleButton(
                            checked = option == selected,
                            onCheckedChange = { if (it) onSelect(option) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics {
                                role = Role.RadioButton
                                this.selected = option == selected
                            },
                            shapes = when {
                                row.size == 1 -> ToggleButtonDefaults.shapes()
                                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == row.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            // Give the paragraph the button's available width. Its rounded
                            // intrinsic width can otherwise clip fractional glyph bounds.
                            Text(labels[optionIndex], Modifier.fillMaxWidth(), style = style, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}
