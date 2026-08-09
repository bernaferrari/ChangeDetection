/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.comparison_before
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_text
import com.bernaferrari.changedetection.shared.generated.resources.comparison_no_changes_title
import com.bernaferrari.changedetection.shared.generated.resources.comparison_now
import com.bernaferrari.changedetection.shared.generated.resources.show_context
import com.bernaferrari.changedetection.shared.generated.resources.show_only_changes
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/** Presents text changes as a human before/after story, with source context available on demand. */
@Composable
internal fun SimpleDiffView(
    oldContent: String,
    newContent: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    var showContext by remember { mutableStateOf(false) }
    val diffRows = remember(oldContent, newContent) {
        calculateDiffRows(oldContent.lines(), newContent.lines())
    }
    val removedRows = remember(diffRows) { diffRows.filter { it.type == DiffLineType.REMOVED } }
    val addedRows = remember(diffRows) { diffRows.filter { it.type == DiffLineType.ADDED } }
    val stats = remember(diffRows) {
        ChangeStats(
            added = addedRows.size,
            removed = removedRows.size,
            modified = 0,
            unchanged = diffRows.count { it.type == DiffLineType.UNCHANGED },
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ComparisonStatsRow(stats = stats, modifier = Modifier.weight(1f))
            TextButton(onClick = { showContext = !showContext }) {
                Text(stringResource(if (showContext) Res.string.show_only_changes else Res.string.show_context))
            }
        }

        if (addedRows.isEmpty() && removedRows.isEmpty()) {
            SharedEmptyState(
                icon = Icons.Default.CheckCircle,
                title = stringResource(Res.string.comparison_no_changes_title),
                description = stringResource(Res.string.comparison_no_changes_text),
                modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth >= 620.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChangeStoryPanel(
                            label = stringResource(Res.string.comparison_before),
                            rows = removedRows,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        ChangeDirection(horizontal = true)
                        ChangeStoryPanel(
                            label = stringResource(Res.string.comparison_now),
                            rows = addedRows,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ChangeStoryPanel(
                            label = stringResource(Res.string.comparison_before),
                            rows = removedRows,
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        ChangeDirection(horizontal = false)
                        ChangeStoryPanel(
                            label = stringResource(Res.string.comparison_now),
                            rows = addedRows,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        if (showContext) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                diffRows.forEach { row -> DiffLine(row.lineNumber, row.content, row.type) }
            }
        }
    }
}

@Composable
private fun ChangeStoryPanel(
    label: String,
    rows: List<DiffRowModel>,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.72f),
            )
            rows.ifEmpty { listOf(DiffRowModel(0, "—", DiffLineType.UNCHANGED)) }.forEach { row ->
                Text(
                    text = row.content.ifBlank { " " },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChangeDirection(horizontal: Boolean) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Icon(
            imageVector = if (horizontal) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.ArrowDownward,
            contentDescription = null,
            modifier = Modifier.padding(9.dp).size(18.dp),
        )
    }
}
