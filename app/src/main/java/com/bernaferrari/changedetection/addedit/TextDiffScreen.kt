package com.bernaferrari.changedetection.addedit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class DiffLine(
    val type: DiffType,
    val content: String,
    val lineNumber: Int?
)

enum class DiffType {
    UNCHANGED, ADDED, REMOVED, CONTEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDiffScreen(
    comparison: VersionComparison?,
    siteId: String? = null,
    onNavigateUp: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load comparison if not provided and siteId is available
    LaunchedEffect(siteId) {
        if (comparison == null && siteId != null) {
            viewModel.loadVersionsForSite(siteId)
        }
    }

    val activeComparison = comparison ?: uiState.comparison

    val diffLines = remember(activeComparison) {
        val previousContent = activeComparison?.previousContent ?: ""
        val currentContent = activeComparison?.currentContent ?: ""

        generateDiffLines(previousContent, currentContent)
    }

    var showLineNumbers by remember { mutableStateOf(true) }
    var showOnlyChanges by remember { mutableStateOf(false) }

    val filteredLines = remember(diffLines, showOnlyChanges) {
        if (showOnlyChanges) {
            diffLines.filter { it.type != DiffType.UNCHANGED }
        } else {
            diffLines
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Text Diff") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showLineNumbers = !showLineNumbers }) {
                    Icon(
                        if (showLineNumbers) Icons.Default.Numbers else Icons.Default.TextFields,
                        contentDescription = "Toggle line numbers"
                    )
                }
                IconButton(onClick = { showOnlyChanges = !showOnlyChanges }) {
                    Icon(
                        if (showOnlyChanges) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle show only changes"
                    )
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            activeComparison != null -> {
                // Stats card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DiffStat(
                            label = "Added",
                            count = diffLines.count { it.type == DiffType.ADDED },
                            color = MaterialTheme.colorScheme.primary
                        )
                        DiffStat(
                            label = "Removed",
                            count = diffLines.count { it.type == DiffType.REMOVED },
                            color = MaterialTheme.colorScheme.error
                        )
                        DiffStat(
                            label = "Total Lines",
                            count = diffLines.size,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Diff content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredLines) { line ->
                        DiffLineItem(
                            line = line,
                            showLineNumbers = showLineNumbers
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No comparison data available")
                }
            }
        }
    }
}

@Composable
private fun DiffStat(
    label: String,
    count: Int,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiffLineItem(
    line: DiffLine,
    showLineNumbers: Boolean
) {
    val backgroundColor = when (line.type) {
        DiffType.ADDED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        DiffType.REMOVED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        DiffType.UNCHANGED -> Color.Transparent
        DiffType.CONTEXT -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = when (line.type) {
        DiffType.ADDED -> MaterialTheme.colorScheme.onPrimaryContainer
        DiffType.REMOVED -> MaterialTheme.colorScheme.onErrorContainer
        DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
        DiffType.CONTEXT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val prefix = when (line.type) {
        DiffType.ADDED -> "+ "
        DiffType.REMOVED -> "- "
        DiffType.UNCHANGED -> "  "
        DiffType.CONTEXT -> "  "
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        if (showLineNumbers) {
            Text(
                text = "${line.lineNumber ?: ""}".padStart(4),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = "$prefix${line.content}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun generateDiffLines(previous: String, current: String): List<DiffLine> {
    val previousLines = previous.split('\n')
    val currentLines = current.split('\n')
    val result = mutableListOf<DiffLine>()

    var prevIndex = 0
    var currIndex = 0
    var lineNumber = 1

    while (prevIndex < previousLines.size || currIndex < currentLines.size) {
        when {
            prevIndex >= previousLines.size -> {
                // Only current lines left (additions)
                result.add(DiffLine(DiffType.ADDED, currentLines[currIndex], lineNumber))
                currIndex++
            }

            currIndex >= currentLines.size -> {
                // Only previous lines left (removals)
                result.add(DiffLine(DiffType.REMOVED, previousLines[prevIndex], lineNumber))
                prevIndex++
            }

            previousLines[prevIndex] == currentLines[currIndex] -> {
                // Lines match
                result.add(DiffLine(DiffType.UNCHANGED, previousLines[prevIndex], lineNumber))
                prevIndex++
                currIndex++
            }

            else -> {
                // Lines differ - simple approach: mark as removed and added
                result.add(DiffLine(DiffType.REMOVED, previousLines[prevIndex], lineNumber))
                result.add(DiffLine(DiffType.ADDED, currentLines[currIndex], lineNumber))
                prevIndex++
                currIndex++
            }
        }
        lineNumber++
    }

    return result
}
