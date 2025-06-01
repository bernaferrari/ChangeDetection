package com.bernaferrari.changedetection.ui.localbrowser

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalBrowserScreen(
    snapId: String,
    url: String,
    onNavigateBack: () -> Unit,
    viewModel: LocalBrowserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(snapId) {
        viewModel.loadContent(snapId, url)
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Saved Snapshot",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        )

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            Text(
                                text = "Loading snapshot...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Error Loading Snapshot",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = uiState.error!!,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { viewModel.loadContent(snapId, url) }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
                
                uiState.content.isNotBlank() -> {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = false
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                }
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(
                                "",
                                uiState.processedContent,
                                "text/html",
                                "UTF-8",
                                ""
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No content available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocalBrowserScreenPreview() {
    MaterialTheme {
        LocalBrowserScreen(
            snapId = "preview",
            url = "https://example.com",
            onNavigateBack = {}
        )
    }
}
