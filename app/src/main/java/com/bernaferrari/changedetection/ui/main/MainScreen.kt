package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.ui.compose.MainCard
import com.bernaferrari.changedetection.ui.compose.SyncStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val sites by viewModel.sitesFlow.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Initialize the sites loading
    LaunchedEffect(Unit) {
        viewModel.loadSites()
    }

    MainScreenContent(
        sites = sites,
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.loadSites()
            viewModel.updateItems()
            isRefreshing = false
        },
        onAddSite = { navController.navigate("add_edit") },
        onHowItWorks = {
            navController.navigate("how_it_works")
        },
        onSiteClick = { siteAndSnap ->
            val contentType = siteAndSnap.snap?.contentType ?: "text/html"
            val encodedUrl =
                URLEncoder.encode(siteAndSnap.site.url, StandardCharsets.UTF_8.toString())

            if (contentType.startsWith("image/") || contentType == "application/pdf") {
                navController.navigate("visual/${siteAndSnap.site.id}/$contentType/$encodedUrl")
            } else {
                navController.navigate("text/${siteAndSnap.site.id}/$encodedUrl")
            }
        },
        onEditSite = { site ->
            navController.navigate("add_edit/${site.id}")
        },
        onDeleteSite = { site ->
            viewModel.removeSite(site)
            viewModel.updateItems() // Refresh the list
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    sites: List<SiteAndLastSnap>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit = {},
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onEditSite: (Site) -> Unit,
    onDeleteSite: (Site) -> Unit,
    onLearnMore: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Change Detection") }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddSite,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Site")
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateComponent(
                        modifier = Modifier.padding(16.dp),
                        onAddSite = onAddSite,
                        onLearnMore = { onHowItWorks() }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sites) { siteAndSnap ->
                        MainCard(
                            site = siteAndSnap.site,
                            lastSnap = siteAndSnap.snap,
                            syncStatus = when {
                                siteAndSnap.isSyncing -> SyncStatus.LOADING
                                !siteAndSnap.site.isSuccessful -> SyncStatus.ERROR
                                else -> SyncStatus.OK
                            },
                            onClick = { onSiteClick(siteAndSnap) },
                            onReloadClick = { 
                                // TODO: Add reload functionality
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteCard(
    site: Site,
    lastSnap: Snap?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = site.title ?: site.url,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = site.url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastSnap?.let { snap ->
                        Text(
                            text = "Last check: ${
                                SimpleDateFormat(
                                    "MMM dd, HH:mm",
                                    Locale.getDefault()
                                ).format(Date(snap.timestamp))
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenEmptyPreview() {
    MainScreenContent(
        sites = emptyList(),
        onAddSite = {},
        onSiteClick = {},
        onEditSite = {},
        onDeleteSite = {},
        onLearnMore = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenWithSitesPreview() {
    val mockSites = listOf(
        SiteAndLastSnap(
            site = Site(
                title = "Example Site",
                url = "https://example.com",
                timestamp = System.currentTimeMillis(),
                id = "1",
                isSuccessful = true,
                isBrowser = false,
                threshold = 0,
                isRead = false,
                isSyncEnabled = true,
                isNotificationEnabled = true,
                notes = "",
                colors = Pair(0xFF2196F3.toInt(), 0xFF1976D2.toInt()),
                constraints = ConstraintsRequired(false, false, false, false)
            ),
            snap = Snap(
                snapId = "snap1",
                siteId = "1",
                timestamp = System.currentTimeMillis(),
                contentType = "text/html",
                contentCharset = "UTF-8",
                contentSize = 1024
            ),
            isSyncing = false
        ),
        SiteAndLastSnap(
            site = Site(
                title = "Another Example",
                url = "https://another-example.com",
                timestamp = System.currentTimeMillis(),
                id = "2",
                isSuccessful = false,
                isBrowser = false,
                threshold = 0,
                isRead = false,
                isSyncEnabled = true,
                isNotificationEnabled = true,
                notes = "",
                colors = Pair(0xFF4CAF50.toInt(), 0xFF388E3C.toInt()),
                constraints = ConstraintsRequired(false, false, false, false)
            ),
            snap = null,
            isSyncing = false
        )
    )

    MainScreenContent(
        sites = mockSites,
        onAddSite = {},
        onSiteClick = {},
        onEditSite = {},
        onDeleteSite = {}
    )
}
