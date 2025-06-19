package com.bernaferrari.changedetection.detailsWebview

import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
    siteId: String,
    onNavigateUp: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Load comparison data
    LaunchedEffect(siteId) {
        viewModel.loadVersionsForSite(siteId)
    }

    val diffLines = remember(uiState.comparison) {
        val comparison = uiState.comparison
        if (comparison?.previousContent != null && comparison.currentContent != null) {
            generateDiffLines(comparison.previousContent, comparison.currentContent)
        } else {
            emptyList()
        }
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
            title = { Text("HTML Diff") },
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

            uiState.comparison != null -> {
                // Simple stats
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "+${diffLines.count { it.type == DiffType.ADDED }}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "-${diffLines.count { it.type == DiffType.REMOVED }}",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${diffLines.size} lines",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Simple diff list
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

enum class DiffViewMode {
    SIDE_BY_SIDE, UNIFIED
}

@Composable
private fun DiffStatsCard(
    comparison: VersionComparison,
    diffLines: List<DiffLine>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HTML Content Comparison",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (comparison.hasChanges) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Changed",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DiffStat(
                    label = "Added",
                    count = diffLines.count { it.type == DiffType.ADDED },
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Add
                )
                DiffStat(
                    label = "Removed",
                    count = diffLines.count { it.type == DiffType.REMOVED },
                    color = MaterialTheme.colorScheme.error,
                    icon = Icons.Default.Remove
                )
                DiffStat(
                    label = "Total Lines",
                    count = diffLines.size,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = Icons.Default.FormatListNumbered
                )
            }
        }
    }
}

@Composable
private fun DiffStat(
    label: String,
    count: Int,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = color.copy(alpha = 0.1f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SideBySideDiffView(
    comparison: VersionComparison,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error
    val primaryColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Previous version
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Previous Version",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = errorColor,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                    }
                },
                update = { webView ->
                    val formattedHtml = formatHtmlForDisplay(
                        comparison.previousContent ?: "No content",
                        "Previous Version",
                        errorColor
                    )
                    webView.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        }

        // Current version
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Current Version",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        settings.domStorageEnabled = false
                    }
                },
                update = { webView ->
                    val formattedHtml = formatHtmlForDisplay(
                        comparison.currentContent ?: "No content",
                        "Current Version",
                        primaryColor
                    )
                    webView.loadDataWithBaseURL(null, formattedHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            )
        }
    }
}

@Composable
private fun UnifiedDiffView(
    lines: List<DiffLine>,
    showLineNumbers: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(lines) { line ->
            DiffLineItem(
                line = line,
                showLineNumbers = showLineNumbers
            )
        }
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
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        if (showLineNumbers) {
            Text(
                text = "${line.lineNumber ?: ""}".padStart(4),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(48.dp)
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

private fun formatHtmlForDisplay(
    content: String,
    title: String,
    accentColor: Color
): String {
    // Convert Compose Color to hex string - fix the type mismatch
    val colorValue = accentColor.value
    val red = ((colorValue shr 16) and 0xFFu).toInt()
    val green = ((colorValue shr 8) and 0xFFu).toInt()
    val blue = (colorValue and 0xFFu).toInt()
    val colorHex = String.format("#%02X%02X%02X", red, green, blue)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                body {
                    font-family: 'SF Mono', Monaco, Consolas, monospace;
                    background: #fafafa;
                    color: #333;
                    line-height: 1.6;
                    padding: 16px;
                    font-size: 12px;
                }
                .header {
                    background: $colorHex;
                    color: white;
                    padding: 12px 16px;
                    margin: -16px -16px 16px -16px;
                    font-weight: 600;
                    font-size: 14px;
                    border-radius: 0 0 8px 8px;
                }
                .content {
                    white-space: pre-wrap;
                    overflow-wrap: break-word;
                    background: white;
                    padding: 16px;
                    border-radius: 8px;
                    border: 1px solid #e0e0e0;
                    min-height: 200px;
                }
                .content:empty::after {
                    content: "No content available";
                    color: #999;
                    font-style: italic;
                }
            </style>
        </head>
        <body>
            <div class="header">$title</div>
            <div class="content">${escapeHtml(content)}</div>
        </body>
        </html>
    """.trimIndent()
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
                // Lines differ
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

private fun escapeHtml(text: String): String {
    return text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
