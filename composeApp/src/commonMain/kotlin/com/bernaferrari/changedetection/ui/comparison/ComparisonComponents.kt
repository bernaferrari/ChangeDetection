/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.util.formatTimestamp
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

data class ChangeStats(
    val added: Int,
    val removed: Int,
    val modified: Int,
    val unchanged: Int,
)

data class DiffRowModel(
    val lineNumber: Int,
    val content: String,
    val type: DiffLineType,
    val isContinuation: Boolean = false,
)

enum class DiffLineType {
    UNCHANGED,
    ADDED,
    REMOVED,
    MODIFIED,
}

fun formatBytes(bytes: Int): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${(kb * 10).roundToInt() / 10.0} KB"
    val mb = kb / 1024.0
    return "${(mb * 10).roundToInt() / 10.0} MB"
}

@Composable
fun ComparisonStatsRow(
    stats: ChangeStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (stats.added > 0) {
            ChangeStatChip(
                count = stats.added,
                label = "+",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }
        if (stats.removed > 0) {
            ChangeStatChip(
                count = stats.removed,
                label = "-",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
        if (stats.modified > 0) {
            ChangeStatChip(
                count = stats.modified,
                label = "~",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

@Composable
fun ChangeStatChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = "$label$count",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

fun calculateChangeStats(oldLines: List<String>, newLines: List<String>): ChangeStats {
    val rows = calculateDiffRows(oldLines, newLines)

    return ChangeStats(
        added = rows.count { it.type == DiffLineType.ADDED },
        removed = rows.count { it.type == DiffLineType.REMOVED },
        modified = 0,
        unchanged = rows.count { it.type == DiffLineType.UNCHANGED },
    )
}

fun calculateDiffRows(oldLines: List<String>, newLines: List<String>): List<DiffRowModel> {
    val patch = com.bernaferrari.difflib.DiffUtils.diff(oldLines, newLines)
    if (patch.deltas.isEmpty()) {
        return oldLines.mapIndexed { index, line ->
            DiffRowModel(index + 1, line, DiffLineType.UNCHANGED)
        }
    }

    val rows = mutableListOf<DiffRowModel>()
    var oldCursor = 0
    var newCursor = 0

    patch.deltas.sortedBy { it.source.position }.forEach { delta ->
        while (oldCursor < delta.source.position && newCursor < delta.target.position) {
            rows += DiffRowModel(newCursor + 1, oldLines[oldCursor], DiffLineType.UNCHANGED)
            oldCursor++
            newCursor++
        }

        delta.source.lines.forEachIndexed { index, line ->
            rows += DiffRowModel(delta.source.position + index + 1, line, DiffLineType.REMOVED)
        }
        delta.target.lines.forEachIndexed { index, line ->
            rows += DiffRowModel(delta.target.position + index + 1, line, DiffLineType.ADDED)
        }

        oldCursor = delta.source.position + delta.source.lines.size
        newCursor = delta.target.position + delta.target.lines.size
    }

    while (oldCursor < oldLines.size && newCursor < newLines.size) {
        rows += DiffRowModel(newCursor + 1, newLines[newCursor], DiffLineType.UNCHANGED)
        oldCursor++
        newCursor++
    }
    return rows
}

@Composable
fun ComparisonPairCard(
    snap1: Snap,
    snap2: Snap,
    pairIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        Color.Transparent
                    },
                )
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.comparison_pair_title, pairIndex + 1, pairIndex + 2),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val diff = kotlin.math.abs(snap1.timestamp - snap2.timestamp)
            // Ideally use TimeAgo or similar, for now just simplistic
            // val diffStr = TimeAgo.using(snap1.timestamp) // Not available in pure KMP stdlib easily
            // We'll use a placeholder or calculate roughly
            val diffSeconds = diff / 1000
            val diffStr = when {
                diffSeconds < 60 -> "${diffSeconds}s"
                diffSeconds < 3600 -> "${diffSeconds / 60}m"
                diffSeconds < 86400 -> "${diffSeconds / 3600}h"
                else -> "${diffSeconds / 86400}d"
            }
            Text(
                text = stringResource(Res.string.comparison_pair_time_diff, diffStr),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.older_label, formatTimestamp(snap2.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatBytes(snap2.contentSize),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(Res.string.newer_label, formatTimestamp(snap1.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = formatBytes(snap1.contentSize),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun SnapshotCard(
    snap: Snap,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.type_label, snap.contentType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(Res.string.size_label, formatBytes(snap.contentSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun DiffLine(
    lineNumber: Int,
    content: String,
    type: DiffLineType,
    modifier: Modifier = Modifier,
    showLineNumber: Boolean = true,
) {
    val (prefix, backgroundColor, textColor) = when (type) {
        DiffLineType.UNCHANGED -> Triple(
            "  ",
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DiffLineType.ADDED -> Triple(
            "+ ",
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.46f),
            MaterialTheme.colorScheme.primary,
        )

        DiffLineType.REMOVED -> Triple(
            "- ",
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.46f),
            MaterialTheme.colorScheme.error,
        )

        DiffLineType.MODIFIED -> Triple(
            "~ ",
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f),
            MaterialTheme.colorScheme.tertiary,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Line number
        Text(
            text = if (showLineNumber) lineNumber.toString().padStart(4) else "    ",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )

        // Prefix indicator
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            ),
            color = textColor,
            modifier = Modifier.padding(end = 4.dp),
        )

        // Content
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = true,
        )
    }
}
