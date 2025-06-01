package com.bernaferrari.changedetection.ui.main

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
import androidx.navigation.compose.rememberNavController
import com.bernaferrari.changedetection.MainViewModel
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreenContent(
        sites = uiState.listOfItems,
        onAddSite = { navController.navigate("add_edit") },
        onSiteClick = { siteAndSnap ->
            val contentType = siteAndSnap.snap?.contentType ?: "text/html"
            val encodedUrl = URLEncoder.encode(siteAndSnap.site.url, StandardCharsets.UTF_8.toString())

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
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    sites: List<SiteAndLastSnap>,
    onAddSite: () -> Unit,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onEditSite: (Site) -> Unit,
    onDeleteSite: (Site) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Detection") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Site")
            }
        }
    ) { paddingValues ->
        if (sites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No sites added yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sites) { siteAndSnap ->
                    SiteCard(
                        site = siteAndSnap.site,
                        lastSnap = siteAndSnap.snap,
                        onClick = { onSiteClick(siteAndSnap) },
                        onEdit = { onEditSite(siteAndSnap.site) },
                        onDelete = { onDeleteSite(siteAndSnap.site) }
                    )
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
                            text = "Last check: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(snap.timestamp))}",
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
        onDeleteSite = {}
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenWithSitesPreview() {
    val mockSites = listOf(
        SiteAndLastSnap(
            site = Site(
                id = "1",
                url = "https://example.com",
                title = "Example Site",
                isSuccessful = true
            ),
            snap = Snap(
                snapId = "snap1",
                siteId = "1",
                timestamp = System.currentTimeMillis(),
                contentType = "text/html",
                contentSize = 1024
            ),
            isExpanded = false
        ),
        SiteAndLastSnap(
            site = Site(
                id = "2",
                url = "https://another-example.com",
                title = "Another Example",
                isSuccessful = false
            ),
            snap = null,
            isExpanded = false
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

@Preview(showBackground = true)
@Composable
fun SiteCardPreview() {
    SiteCard(
        site = Site(
            id = "1",
            url = "https://example.com",
            title = "Example Site",
            isSuccessful = true
        ),
        lastSnap = Snap(
            snapId = "snap1",
            siteId = "1",
            timestamp = System.currentTimeMillis(),
            contentType = "text/html",
            contentSize = 1024
        ),
        onClick = {},
        onEdit = {},
        onDelete = {}
    )
}
