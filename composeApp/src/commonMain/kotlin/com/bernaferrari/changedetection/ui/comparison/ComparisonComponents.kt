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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
    val unchanged: Int
)

enum class DiffLineType {
    UNCHANGED, ADDED, REMOVED, MODIFIED
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stats.added > 0) {
            ChangeStatChip(
                count = stats.added,
                label = "+",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
        if (stats.removed > 0) {
            ChangeStatChip(
                count = stats.removed,
                label = "-",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        }
        if (stats.modified > 0) {
            ChangeStatChip(
                count = stats.modified,
                label = "~",
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
fun ChangeStatChip(
    count: Int,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    SuggestionChip(
        onClick = { },
        label = { Text("$label$count") },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = containerColor,
            labelColor = contentColor
        ),
        border = null
    )
}

fun calculateChangeStats(oldLines: List<String>, newLines: List<String>): ChangeStats {
    val oldSet = oldLines.toSet()
    val newSet = newLines.toSet()

    val added = newSet.minus(oldSet).size
    val removed = oldSet.minus(newSet).size
    val unchanged = oldSet.intersect(newSet).size
    val modified = maxOf(0, (oldLines.size + newLines.size - added - removed - unchanged * 2) / 2)

    return ChangeStats(
        added = added,
        removed = removed,
        modified = modified,
        unchanged = unchanged
    )
}

@Composable
fun ComparisonPairCard(
    snap1: Snap,
    snap2: Snap,
    pairIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else Color.Transparent
                )
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.comparison_pair_title, pairIndex + 1, pairIndex + 2),
                style = MaterialTheme.typography.titleMedium
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.older_label, formatTimestamp(snap2.timestamp)),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatBytes(snap2.contentSize),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(Res.string.newer_label, formatTimestamp(snap1.timestamp)),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = formatBytes(snap1.contentSize),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SnapshotCard(
    snap: Snap,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimestamp(snap.timestamp),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(Res.string.type_label, snap.contentType),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.size_label, formatBytes(snap.contentSize)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    val (prefix, backgroundColor, textColor) = when (type) {
        DiffLineType.UNCHANGED -> Triple(
            "  ",
            Color.Transparent,
            MaterialTheme.colorScheme.onSurfaceVariant
        )

        DiffLineType.ADDED -> Triple(
            "+ ",
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.primary
        )

        DiffLineType.REMOVED -> Triple(
            "- ",
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.error
        )

        DiffLineType.MODIFIED -> Triple(
            "~ ",
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.tertiary
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Line number
        Text(
            text = lineNumber.toString().padStart(4),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 8.dp)
        )

        // Prefix indicator
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Content
        Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
