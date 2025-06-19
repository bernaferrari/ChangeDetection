package com.bernaferrari.changedetection.addedit

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.repo.Snap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualComparisonScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    onShowTextDiff: (VersionComparison) -> Unit = {},
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedPreviousVersion by remember { mutableStateOf<Snap?>(null) }
    var selectedCurrentVersion by remember { mutableStateOf<Snap?>(null) }
    var showVersionSelector by remember { mutableStateOf(false) }
    var selectingVersionFor by remember { mutableStateOf<String?>(null) }

    // Auto-load versions when screen opens
    LaunchedEffect(siteId) {
        viewModel.loadVersionsForSite(siteId)
    }

    // Auto-select the two most recent versions
    LaunchedEffect(uiState.comparison) {
        uiState.comparison?.let { comparison ->
            selectedCurrentVersion = comparison.currentVersion
            selectedPreviousVersion = comparison.previousVersion
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Visual Comparison") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Text diff button
                IconButton(
                    onClick = {
                        uiState.comparison?.let { onShowTextDiff(it) }
                    },
                    enabled = uiState.comparison != null
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Text Diff")
                }

                // Version selector button
                if (uiState.comparison?.allVersions?.size ?: 0 > 2) {
                    IconButton(
                        onClick = { showVersionSelector = true }
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Version History")
                    }
                }
            }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading versions...")
                    }
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            uiState.comparison != null -> {
                ComparisonContent(
                    comparison = uiState.comparison!!,
                    selectedPreviousVersion = selectedPreviousVersion,
                    selectedCurrentVersion = selectedCurrentVersion,
                    onVersionSelect = { side, version ->
                        when (side) {
                            "previous" -> selectedPreviousVersion = version
                            "current" -> selectedCurrentVersion = version
                        }
                    },
                    onShowVersionSelector = { side ->
                        selectingVersionFor = side
                        showVersionSelector = true
                    }
                )
            }
        }
    }

    // Version selector bottom sheet
    if (showVersionSelector && uiState.comparison != null) {
        VersionSelectorBottomSheet(
            versions = uiState.comparison!!.allVersions,
            selectingFor = selectingVersionFor,
            onVersionSelected = { version ->
                when (selectingVersionFor) {
                    "previous" -> selectedPreviousVersion = version
                    "current" -> selectedCurrentVersion = version
                    else -> {
                        // Auto-assign when selecting from history
                        selectedCurrentVersion = version
                    }
                }
                showVersionSelector = false
                selectingVersionFor = null
            },
            onDismiss = {
                showVersionSelector = false
                selectingVersionFor = null
            }
        )
    }
}

@Composable
private fun ComparisonContent(
    comparison: VersionComparison,
    selectedPreviousVersion: Snap?,
    selectedCurrentVersion: Snap?,
    onVersionSelect: (String, Snap) -> Unit,
    onShowVersionSelector: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Comparison stats if there are changes
        if (comparison.hasChanges) {
            ComparisonStatsCard(
                diffPercentage = comparison.diffPercentage,
                previousVersion = selectedPreviousVersion,
                currentVersion = selectedCurrentVersion
            )
        }

        // Version selectors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VersionSelectorChip(
                title = "Previous",
                selectedVersion = selectedPreviousVersion,
                onVersionSelect = { onShowVersionSelector("previous") },
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.errorContainer
            )

            VersionSelectorChip(
                title = "Current",
                selectedVersion = selectedCurrentVersion,
                onVersionSelect = { onShowVersionSelector("current") },
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        }

        // Side by side WebViews with actual content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WebViewContentContainer(
                snap = selectedPreviousVersion,
                content = comparison.previousContent,
                title = "Previous",
                modifier = Modifier.weight(1f),
                borderColor = MaterialTheme.colorScheme.error
            )

            WebViewContentContainer(
                snap = selectedCurrentVersion,
                content = comparison.currentContent,
                title = "Current",
                modifier = Modifier.weight(1f),
                borderColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VersionSelectorChip(
    title: String,
    selectedVersion: Snap?,
    onVersionSelect: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color
) {
    Card(
        onClick = onVersionSelect,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = selectedVersion?.let {
                    java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(it.timestamp))
                } ?: "No version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonStatsCard(
    diffPercentage: Float,
    previousVersion: Snap?,
    currentVersion: Snap?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Changes detected",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${diffPercentage.format(1)}% difference in size",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AssistChip(
                onClick = { },
                label = { Text("Changed") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun WebViewContentContainer(
    snap: Snap?,
    content: String?,
    title: String,
    modifier: Modifier = Modifier,
    borderColor: Color
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (snap != null) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            // Sync scroll behavior could be added here
                        }
                    },
                    update = { webView ->
                        // Load the actual captured content, or fallback to URL if content is not available
                        if (!content.isNullOrEmpty()) {
                            webView.loadDataWithBaseURL(
                                null,
                                content,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        } else {
                            // Fallback: show a message that content isn't available
                            val fallbackHtml = """
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                    <style>
                                        body { 
                                            font-family: -apple-system, system-ui, sans-serif;
                                            padding: 20px; 
                                            text-align: center; 
                                            color: #666;
                                        }
                                        .info { 
                                            background: #f8f9fa; 
                                            padding: 20px; 
                                            border-radius: 8px; 
                                            margin: 20px 0; 
                                        }
                                        .timestamp { 
                                            font-size: 14px; 
                                            color: #999; 
                                            margin-top: 10px; 
                                        }
                                    </style>
                                </head>
                                <body>
                                    <div class="info">
                                        <h3>Snapshot Preview</h3>
                                        <p>Content captured on:</p>
                                        <div class="timestamp">${
                                java.text.SimpleDateFormat(
                                    "MMM dd, yyyy 'at' HH:mm",
                                    java.util.Locale.getDefault()
                                )
                                    .format(java.util.Date(snap.timestamp))
                            }</div>
                                        <p style="margin-top: 15px;">Size: ${snap.contentSize} bytes</p>
                                        <p style="margin-top: 10px; font-size: 12px; color: #999;">
                                            Full content rendering requires the original HTML to be stored in the database.
                                        </p>
                                    </div>
                                </body>
                                </html>
                            """.trimIndent()

                            webView.loadDataWithBaseURL(
                                null,
                                fallbackHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.WebAsset,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No version selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun Float.format(digits: Int) = "%.${digits}f".format(this)
