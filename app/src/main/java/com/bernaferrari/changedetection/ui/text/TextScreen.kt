package com.bernaferrari.changedetection.ui.text

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    siteId: String,
    url: String,
    onNavigateBack: () -> Unit,
    viewModel: TextViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showSnapshotDialog by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var currentSnapIndex by remember { mutableStateOf(0) }
    
    LaunchedEffect(siteId) {
        viewModel.loadSnaps(siteId)
    }
    
    LaunchedEffect(currentSnapIndex, uiState.snaps.size) {
        if (uiState.snaps.isNotEmpty() && currentSnapIndex < uiState.snaps.size) {
            val currentSnap = uiState.snaps[currentSnapIndex]
            val previousSnap = if (currentSnapIndex > 0) uiState.snaps[currentSnapIndex - 1] else null
            viewModel.loadDiff(currentSnap, previousSnap)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top AppBar
        TopAppBar(
            title = {
                Text(
                    text = uiState.currentSnap?.let { 
                        TextViewModel.formatTimestamp(it.timestamp) 
                    } ?: "Text Comparison",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                IconButton(onClick = { showControls = !showControls }) {
                    Icon(
                        imageVector = if (showControls) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showControls) "Hide controls" else "Show controls"
                    )
                }
                IconButton(onClick = { showSnapshotDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Show snapshots"
                    )
                }
                IconButton(onClick = { viewModel.openInBrowser(url, context) }) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Open in browser"
                    )
                }
            }
        )
        
        // Main content area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                                    text = "Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(text = uiState.error)
                            }
                        }
                    }
                }
                
                uiState.snaps.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No text snapshots available",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                else -> {
                    // Text diff viewer
                    if (uiState.diffHtml.isNotEmpty()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    webViewClient = WebViewClient()
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                    }
                                }
                            },
                            update = { webView ->
                                webView.loadDataWithBaseURL(
                                    null,
                                    uiState.diffHtml,
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
                                text = "No diff available",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Control bar at bottom
            if (showControls && uiState.snaps.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous button
                        IconButton(
                            onClick = {
                                if (currentSnapIndex > 0) {
                                    currentSnapIndex--
                                }
                            },
                            enabled = currentSnapIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIos,
                                contentDescription = "Previous"
                            )
                        }
                        
                        // Snapshot indicator
                        Text(
                            text = "${currentSnapIndex + 1} / ${uiState.snaps.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        // Next button
                        IconButton(
                            onClick = {
                                if (currentSnapIndex < uiState.snaps.size - 1) {
                                    currentSnapIndex++
                                }
                            },
                            enabled = currentSnapIndex < uiState.snaps.size - 1
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Next"
                            )
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Share button
                        IconButton(
                            onClick = {
                                uiState.currentSnap?.let { snap ->
                                    viewModel.shareSnap(snap, context)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }
                        
                        // Delete button
                        IconButton(
                            onClick = {
                                uiState.currentSnap?.let { snap ->
                                    viewModel.deleteSnap(snap.snapId)
                                    // Adjust current index if needed
                                    if (currentSnapIndex >= uiState.snaps.size - 1) {
                                        currentSnapIndex = maxOf(0, uiState.snaps.size - 2)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Snapshot selection dialog
    if (showSnapshotDialog && uiState.snaps.isNotEmpty()) {
        Dialog(onDismissRequest = { showSnapshotDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    TopAppBar(
                        title = { Text("Select Snapshot") },
                        actions = {
                            IconButton(onClick = { showSnapshotDialog = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    )
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.snaps.withIndex().toList()) { (index, snap) ->
                            SnapshotItem(
                                snap = snap,
                                isSelected = index == currentSnapIndex,
                                onClick = {
                                    currentSnapIndex = index
                                    showSnapshotDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SnapshotItem(
    snap: com.bernaferrari.changedetection.repo.Snap,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = TextViewModel.formatTimestamp(snap.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Text(
                text = "Size: ${formatFileSize(snap.contentSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return "%.1f %s".format(size, units[unitIndex])
}
