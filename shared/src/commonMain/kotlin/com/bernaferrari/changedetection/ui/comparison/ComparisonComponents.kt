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
import com.bernaferrari.changedetection.localization.stringResource
import kotlin.math.roundToInt

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

fun formatBytes(bytes: Int): String = when {
    bytes < 1024 -> "${com.bernaferrari.changedetection.util.localizedInteger(bytes)} B"
    bytes < 1024 * 1024 -> "${com.bernaferrari.changedetection.util.localizedNumber(bytes / 1024.0)} KB"
    else -> "${com.bernaferrari.changedetection.util.localizedNumber(bytes / (1024.0 * 1024))} MB"
}

fun calculateDiffRows(oldLines: List<String>, newLines: List<String>, checkCancelled: () -> Unit = {}): List<DiffRowModel> {
    val patch = com.bernaferrari.difflib.DiffUtils.diff(oldLines, newLines, checkCancelled)
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
            text = if (showLineNumber) com.bernaferrari.changedetection.util.localizedInteger(lineNumber).padStart(4) else "    ",
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
