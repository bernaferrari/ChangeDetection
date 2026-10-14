@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.kit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
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
fun <T> ConnectedChoice(options: List<T>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier.padding(horizontal = 16.dp), enabled: Boolean = true, columns: Int? = null, wrap: Boolean = false, label: @Composable (T) -> String) {
    val labels = options.map { label(it) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelLarge
    if (wrap) {
        // The Material connected flow layout keeps natural label widths as choices wrap.
        FlowRow(
            modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { index, option ->
                ToggleButton(
                    checked = option == selected,
                    onCheckedChange = { if (it) onSelect(option) },
                    enabled = enabled,
                    modifier = Modifier.width(with(density) { measurer.measure(labels[index], style, softWrap = false).size.width.toDp() }.plus(26.dp).coerceAtLeast(48.dp)).heightIn(min = 48.dp).semantics {
                        role = Role.RadioButton
                        this.selected = option == selected
                    },
                    shapes = when {
                        options.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                        index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        index == options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) { Text(labels[index], Modifier.fillMaxWidth(), style = style, textAlign = TextAlign.Center) }
            }
        }
        return
    }
    val minimumWidth = with(density) {
        labels.maxOfOrNull { measurer.measure(it, style, softWrap = false).size.width }?.toDp()
    }?.plus(24.dp)?.coerceAtLeast(48.dp) ?: 48.dp
    BoxWithConstraints(modifier.fillMaxWidth().selectableGroup()) {
        val spacing = ButtonGroupDefaults.ConnectedSpaceBetween
        val fitting = ((maxWidth + spacing) / (minimumWidth + spacing)).toInt().coerceAtLeast(1)
        // A requested column count wins when the labels fit, so a grid can come out even (3 × 3, not 4 + 4 + 1).
        val columns = columns?.takeIf { it <= fitting } ?: fitting
        val rows = options.indices.toList().chunked(columns)
        val gridShapes = rememberConnectedGridShapes()
        // Rows share the group's gap, so a multi-row choice reads as one grid rather than stacked groups.
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            rows.forEachIndexed { rowIndex, row ->
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
                                options.size == 1 -> ToggleButtonDefaults.shapesFor(ToggleButtonDefaults.size)
                                else -> gridShapes(
                                    GridCorners(
                                        topStart = rowIndex == 0 && index == 0,
                                        topEnd = rowIndex == 0 && index == row.lastIndex,
                                        bottomStart = rowIndex == rows.lastIndex && index == 0,
                                        // A short last row leaves the cell above its gap exposed at that corner.
                                        bottomEnd = index == row.lastIndex && (rowIndex == rows.lastIndex || rows[rowIndex + 1].size <= index),
                                    ),
                                )
                            },
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

/** Which of a cell's corners sit on the outside of the whole group. */
private data class GridCorners(val topStart: Boolean, val topEnd: Boolean, val bottomStart: Boolean, val bottomEnd: Boolean)

/**
 * Connected button group shapes generalised to a grid: corners on the group's outline are fully
 * rounded, inner corners use the group's inner radius (tighter while pressed), and the checked
 * cell morphs to a full pill. A single row yields exactly the stock leading/middle/trailing shapes.
 */
@Composable
private fun rememberConnectedGridShapes(): (GridCorners) -> ToggleButtonShapes {
    val middle = ButtonGroupDefaults.connectedMiddleButtonShapes()
    val inner = (middle.shape as? CornerBasedShape)?.topStart ?: CornerSize(8.dp)
    val pressedInner = (middle.pressedShape as? CornerBasedShape)?.topStart ?: CornerSize(4.dp)
    val outer = CornerSize(50)
    fun shape(corners: GridCorners, innerSize: CornerSize) = RoundedCornerShape(
        topStart = if (corners.topStart) outer else innerSize,
        topEnd = if (corners.topEnd) outer else innerSize,
        bottomEnd = if (corners.bottomEnd) outer else innerSize,
        bottomStart = if (corners.bottomStart) outer else innerSize,
    )
    return { corners -> ToggleButtonShapes(shape(corners, inner), shape(corners, pressedInner), middle.checkedShape) }
}
