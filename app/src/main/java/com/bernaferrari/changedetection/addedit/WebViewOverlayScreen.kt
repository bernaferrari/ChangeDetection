package com.bernaferrari.changedetection.addedit

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.repo.Snap

enum class ComparisonMode {
    VISUAL_OVERLAY, TEXT_DIFF
}

enum class VersionSelection {
    PREVIOUS, CURRENT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewOverlayScreen(
    siteId: String,
    onNavigateUp: () -> Unit,
    onShowTextDiff: (VersionComparison) -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedVersion by remember { mutableStateOf(VersionSelection.CURRENT) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(siteId) {
        viewModel.loadVersionsForSite(siteId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top App Bar
        TopAppBar(
            title = { Text("Visual Comparison") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            },
            actions = {
                uiState.comparison?.let { comparison ->
                    IconButton(
                        onClick = { onShowTextDiff(comparison) }
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "Text Diff")
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
                        Text("Comparing versions...")
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
                        Text("Error: ${uiState.error}")
                    }
                }
            }

            uiState.comparison != null -> {
                ComparisonContent(
                    comparison = uiState.comparison!!,
                    selectedVersion = selectedVersion,
                    onVersionChange = { selectedVersion = it },
                    onWebViewReady = { webView = it }
                )
            }
        }
    }
}

@Composable
private fun ComparisonContent(
    comparison: VersionComparison,
    selectedVersion: VersionSelection,
    onVersionChange: (VersionSelection) -> Unit,
    onWebViewReady: (WebView) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Version selector
        VersionSelector(
            comparison = comparison,
            selectedVersion = selectedVersion,
            onVersionChange = onVersionChange
        )

        // WebView content
        val contentToShow = when (selectedVersion) {
            VersionSelection.PREVIOUS -> comparison.previousVersion
            VersionSelection.CURRENT -> comparison.currentVersion
        }

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    onWebViewReady(this)
                }
            },
            update = { view ->
                view.loadDataWithBaseURL(
                    null,
                    contentToShow?.contentCharset ?: "",
                    "text/html",
                    "UTF-8",
                    null
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VersionSelector(
    comparison: VersionComparison,
    selectedVersion: VersionSelection,
    onVersionChange: (VersionSelection) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Changes detected: ${comparison.diffPercentage.format(1)}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                if (comparison.hasChanges) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Changed") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Difference,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            // Version toggle buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    onClick = { onVersionChange(VersionSelection.PREVIOUS) },
                    label = { Text("Previous Version") },
                    selected = selectedVersion == VersionSelection.PREVIOUS,
                    leadingIcon = if (selectedVersion == VersionSelection.PREVIOUS) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    onClick = { onVersionChange(VersionSelection.CURRENT) },
                    label = { Text("Current Version") },
                    selected = selectedVersion == VersionSelection.CURRENT,
                    leadingIcon = if (selectedVersion == VersionSelection.CURRENT) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
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
        // Version selector
        // ...existing code...

        // Side by side WebViews
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WebViewContainer(
                snap = selectedPreviousVersion,
                title = "Previous",
                modifier = Modifier.weight(1f),
                borderColor = MaterialTheme.colorScheme.error
            )

            WebViewContainer(
                snap = selectedCurrentVersion,
                title = "Current",
                modifier = Modifier.weight(1f),
                borderColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WebViewContainer(
    snap: Snap?,
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
                        }
                    },
                    update = { webView ->
                        // Create placeholder HTML content since we don't have direct access to raw content
                        val htmlContent = """
                            <html>
                            <head>
                                <title>Captured Content</title>
                                <style>
                                    body { font-family: Arial, sans-serif; padding: 20px; }
                                    .info { background: #f5f5f5; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
                                    .timestamp { color: #666; font-size: 14px; }
                                    .size { color: #333; font-weight: bold; }
                                </style>
                            </head>
                            <body>
                                <div class="info">
                                    <h3>Captured Content</h3>
                                    <div class="timestamp">Captured: ${
                            java.text.SimpleDateFormat(
                                "MMM dd, yyyy 'at' HH:mm",
                                java.util.Locale.getDefault()
                            ).format(java.util.Date(snap.timestamp))
                        }</div>
                                    <div class="size">Content size: ${snap.contentSize} bytes</div>
                                    <div>Site ID: ${snap.siteId}</div>
                                </div>
                                <p><em>Note: This is a placeholder view. In a real implementation, you would display the actual captured HTML content from the snap data.</em></p>
                                <p>To implement actual content display, you would need to:</p>
                                <ul>
                                    <li>Store the raw HTML content in the Snap model</li>
                                    <li>Extract and display that content here</li>
                                    <li>Handle any encoding or formatting issues</li>
                                </ul>
                            </body>
                            </html>
                        """.trimIndent()

                        webView.loadDataWithBaseURL(
                            null,
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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

private fun Float.format(digits: Int) = "%.${digits}f".format(this)
