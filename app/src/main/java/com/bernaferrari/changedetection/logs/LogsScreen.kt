package com.bernaferrari.changedetection.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.mainnew.getLogsSubtitle
import com.bernaferrari.changedetection.mainnew.getTitle
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.compose.LoadingIndicator
import com.bernaferrari.changedetection.ui.compose.SectionHeader
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun LogsScreen(
    onSnapClick: (String, String) -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val snapsPagingData = viewModel.pagedSnaps.collectAsLazyPagingItems()
    var showDeleteDialog by remember { mutableStateOf<Snap?>(null) }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            versionsCount == 0 && snapsPagingData.loadState.refresh !is LoadState.Loading -> {
                EmptyLogsContent()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Last Changes",
                            subtitle = "$versionsCount changes detected"
                        )
                    }
                    
                    items(snapsPagingData) { snap ->
                        if (snap != null) {
                            val site = siteMap[snap.siteId]
                            if (site != null) {
                                LogItem(
                                    snap = snap,
                                    site = site,
                                    onClick = { onSnapClick(snap.snapId, site.url) },
                                    onLongClick = { showDeleteDialog = snap }
                                )
                            }
                        }
                    }
                    
                    snapsPagingData.apply {
                        when {
                            loadState.refresh is LoadState.Loading -> {
                                item { LoadingIndicator() }
                            }
                            loadState.append is LoadState.Loading -> {
                                item { LoadingIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { snap ->
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(snap.timestamp),
            ZoneId.systemDefault()
        )
        
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Remove snapshot?") },
            text = { 
                Text("${snap.contentSize.readableFileSize()} | ${dateTime.dayOfMonth}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSnap(snap.snapId)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LogItem(
    snap: Snap,
    site: com.bernaferrari.changedetection.repo.Site,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(snap.timestamp),
        ZoneId.systemDefault()
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box {
            // Gradient background
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(site.colors.first),
                                Color(site.colors.second)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = dateTime.dayOfMonth.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dateTime.format(DateTimeFormatter.ofPattern("MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getTitle(site),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Text(
                        text = getLogsSubtitle(snap),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                // Size indicator
                Text(
                    text = snap.contentSize.readableFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun EmptyLogsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No changes detected yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Start monitoring websites to see changes here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
