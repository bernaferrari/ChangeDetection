/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_text
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_title
import com.bernaferrari.changedetection.shared.generated.resources.show_all
import com.bernaferrari.changedetection.shared.generated.resources.show_changes
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** The text-diff viewport owns synchronized vertical context and horizontal code-line scrolling. */
@Composable
internal fun SimpleDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var showOnlyChanges by remember { mutableStateOf(false) }
    val oldLines = remember(oldContent) { oldContent.lines() }
    val newLines = remember(newContent) { newContent.lines() }
    val diffRows = remember(oldLines, newLines) { calculateDiffRows(oldLines, newLines) }
    val changeStats = remember(diffRows) {
        ChangeStats(
            added = diffRows.count { it.type == DiffLineType.ADDED },
            removed = diffRows.count { it.type == DiffLineType.REMOVED },
            modified = 0,
            unchanged = diffRows.count { it.type == DiffLineType.UNCHANGED },
        )
    }
    val primaryLineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    val addedLineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)

    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(verticalScrollState)
                .padding(start = spacing.md, top = spacing.md, bottom = spacing.md),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = spacing.sm, bottom = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                ComparisonStatsRow(stats = changeStats)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilterChip(
                        selected = !showOnlyChanges,
                        modifier = Modifier.heightIn(min = spacing.touchTarget),
                        onClick = { showOnlyChanges = false },
                        label = { Text(stringResource(Res.string.show_all)) },
                    )
                    Spacer(modifier = Modifier.width(spacing.xs))
                    FilterChip(
                        selected = showOnlyChanges,
                        modifier = Modifier.heightIn(min = spacing.touchTarget),
                        onClick = { showOnlyChanges = true },
                        label = { Text(stringResource(Res.string.show_changes)) },
                    )
                }
            }

            if (changeStats.added == 0 && changeStats.removed == 0) {
                SharedEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(Res.string.comparison_no_changes_title),
                    description = stringResource(Res.string.comparison_no_changes_text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.xl),
                )
            } else {
                Column(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                    diffRows
                        .asSequence()
                        .filter { !showOnlyChanges || it.type != DiffLineType.UNCHANGED }
                        .forEach { row ->
                            DiffLine(row.lineNumber, row.content, row.type)
                        }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = spacing.md, bottom = spacing.md, end = spacing.xs)
                .width(6.dp)
                .fillMaxHeight(),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val total = diffRows.size.coerceAtLeast(1)
                val step = size.height / total
                var y = 0f
                diffRows.forEach { row ->
                    val color = when (row.type) {
                        DiffLineType.UNCHANGED -> Color.Transparent
                        DiffLineType.ADDED -> addedLineColor
                        DiffLineType.REMOVED, DiffLineType.MODIFIED -> primaryLineColor
                    }
                    if (color.alpha > 0f) {
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                        )
                    }
                    y += step
                }
            }
        }
    }
}
