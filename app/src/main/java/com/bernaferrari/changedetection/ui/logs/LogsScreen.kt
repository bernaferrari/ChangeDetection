package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.Site
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    navController: NavController,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val snapsPagingItems = viewModel.snaps.collectAsLazyPagingItems()
    val sitesMap by viewModel.sitesMap.collectAsStateWithLifecycle()
    val totalChanges by viewModel.totalChanges.collectAsStateWithLifecycle()
    
    var snapToDelete by remember { mutableStateOf<Snap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Last Changes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalChanges changes detected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Content
            when {
                snapsPagingItems.loadState.refresh is LoadState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                snapsPagingItems.itemCount == 0 -> {
                    LogsEmptyState()
                }
                
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapsPagingItems) { snap ->
                            snap?.let { nonNullSnap ->
                                sitesMap[nonNullSnap.siteId]?.let { site ->
                                    LogsItem(
                                        snap = nonNullSnap,
                                        site = site,
                                        onClick = {
                                            // Navigate to local browser
                                            navController.navigate("local_browser/${nonNullSnap.snapId}/${site.url}")
                                        },
                                        onDelete = {
                                            snapToDelete = nonNullSnap
                                        }
                                    )
                            }
                        }
                    }
                    
                    if (snapsPagingItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        snapToDelete?.let { snap ->
            val site = sitesMap[snap.siteId]
            val dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(snap.timestamp),
                ZoneId.systemDefault()
            )
            
            AlertDialog(
                onDismissRequest = { snapToDelete = null },
                title = { Text("Remove Change?") },
                text = {
                    Text("${snap.contentSize.readableFileSize()} | ${dateTime.dayOfMonth} ${dateTime.format(DateTimeFormatter.ofPattern("MMM"))}")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeSnap(snap.snapId)
                            snapToDelete = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { snapToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun LogsItem(
    snap: Snap,
    site: Site,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(snap.timestamp),
        ZoneId.systemDefault()
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(site.colors.first),
                                Color(site.colors.second)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateTime.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dateTime.format(DateTimeFormatter.ofPattern("MMM")),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = site.title ?: site.url,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = getLogsSubtitle(snap),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = snap.contentSize.readableFileSize(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LogsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No changes detected yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Changes will appear here when your tracked sites are updated",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getLogsSubtitle(snap: Snap): String {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(snap.timestamp),
        ZoneId.systemDefault()
    )
    
    return "${dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))} at ${
        dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }"
}
