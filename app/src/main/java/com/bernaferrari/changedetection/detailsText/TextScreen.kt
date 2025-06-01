package com.bernaferrari.changedetection.detailstext

import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.extensions.replaceRelativePathWithAbsolute
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.compose.LoadingIndicator
import com.bernaferrari.changedetection.ui.compose.TextDiffItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextScreen(
    siteId: String,
    url: String,
    contentType: String,
    onNavigateUp: () -> Unit,
    viewModel: TextViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snapsPagingData = viewModel.getSnapsPaged(siteId, contentType).collectAsLazyPagingItems()
    val diffLines by viewModel.diffLines.collectAsStateWithLifecycle()
    val selectedSnaps by viewModel.selectedSnaps.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val webContent by viewModel.webContent.collectAsStateWithLifecycle()
    
    var showDiffList by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        TopAppBar(
            title = { Text("Text Changes") },
            navigationIcon = {
                IconButton(onClick = onNavigateUp) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showDiffList = !showDiffList }) {
                    Icon(
                        if (showDiffList) Icons.Default.ViewColumn else Icons.Default.List,
                        contentDescription = "Toggle view"
                    )
                }
            }
        )
        
        // Horizontal snap selector
        LazyRow(
            modifier = Modifier.height(100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(snapsPagingData) { snap ->
                if (snap != null) {
                    SnapSelectorItem(
                        snap = snap,
                        isSelected = snap.snapId in selectedSnaps.map { it.snapId },
                        selectionType = when {
                            selectedSnaps.firstOrNull()?.snapId == snap.snapId -> ItemSelected.REVISED
                            selectedSnaps.getOrNull(1)?.snapId == snap.snapId -> ItemSelected.ORIGINAL
                            else -> ItemSelected.NONE
                        },
                        onClick = { viewModel.selectSnap(snap) },
                        onLongClick = { viewModel.removeSnap(snap.snapId) }
                    )
                }
            }
        }
        
        // View mode toggles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = viewMode == TextViewMode.DIFF,
                onClick = { viewModel.setViewMode(TextViewMode.DIFF) },
                label = { Text("Diff") }
            )
            FilterChip(
                selected = viewMode == TextViewMode.ORIGINAL,
                onClick = { viewModel.setViewMode(TextViewMode.ORIGINAL) },
                label = { Text("Original") }
            )
            FilterChip(
                selected = viewMode == TextViewMode.REVISED,
                onClick = { viewModel.setViewMode(TextViewMode.REVISED) },
                label = { Text("Revised") }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Content area
        if (showDiffList && viewMode == TextViewMode.DIFF) {
            // Show diff lines
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(diffLines.size) { index ->
                    val line = diffLines[index]
                    TextDiffItem(
                        title = line,
                        index = index + 1
                    )
                }
            }
        } else {
            // Show web content
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                    }
                },
                update = { webView ->
                    if (webContent.isNotBlank()) {
                        val processedContent = webContent.replaceRelativePathWithAbsolute(url)
                        webView.loadDataWithBaseURL("", processedContent, "text/html", "UTF-8", "")
                    } else {
                        webView.loadUrl("about:blank")
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SnapSelectorItem(
    snap: Snap,
    isSelected: Boolean,
    selectionType: ItemSelected,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val backgroundColor = when (selectionType) {
        ItemSelected.REVISED -> MaterialTheme.colorScheme.primaryContainer
        ItemSelected.ORIGINAL -> MaterialTheme.colorScheme.secondaryContainer
        ItemSelected.NONE -> MaterialTheme.colorScheme.surface
    }
    
    Card(
        modifier = Modifier
            .width(120.dp)
            .fillMaxHeight(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = snap.timestamp.convertTimestampToDate(),
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = snap.contentSize.readableFileSize(),
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove snapshot?") },
            text = { Text("Are you sure you want to remove this snapshot?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLongClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

enum class TextViewMode {
    DIFF, ORIGINAL, REVISED
}

enum class ItemSelected {
    NONE, ORIGINAL, REVISED
}
