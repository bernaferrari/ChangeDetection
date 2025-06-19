package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.WorkInfo
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.compose.MainCard
import com.bernaferrari.changedetection.ui.compose.SiteOptionsBottomSheet
import com.bernaferrari.changedetection.ui.compose.SyncStatus
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val sites by viewModel.sitesFlow.collectAsState()
    val workInfo by viewModel.workInfoFlow.collectAsState(initial = emptyList())
    val loadingSites by viewModel.loadingSites.collectAsState()
    val turboModeState by viewModel.turboModeState.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var showTurboDialog by remember { mutableStateOf(false) }
    var lastTurboSiteIds by remember { mutableStateOf<List<String>>(emptyList()) }

    // Load last selected turbo sites
    LaunchedEffect(Unit) {
        lastTurboSiteIds = viewModel.getLastTurboSiteIds()
    }

    MainScreenContent(
        sites = sites,
        workInfo = workInfo,
        loadingSites = loadingSites,
        isRefreshing = isRefreshing,
        turboModeState = turboModeState,
        onRefresh = {
            isRefreshing = true
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
        },
        onToggleSync = { site ->
            viewModel.updateSite(site.copy(isSyncEnabled = !site.isSyncEnabled))
            viewModel.updateItems()
        },
        onToggleNotifications = { site ->
            viewModel.updateSite(site.copy(isNotificationEnabled = !site.isNotificationEnabled))
            viewModel.updateItems()
        },
        onReloadSite = { site ->
            viewModel.reloadSite(site)
        },
        onLongPress = {
            if (!turboModeState.isActive) {
                showTurboDialog = true
            }
        },
        onStopTurbo = {
            viewModel.stopTurboMode()
        }
    )

    // Turbo Mode Dialog
    if (showTurboDialog) {
        TurboModeDialog(
            sites = sites.map { it.site },
            lastSelectedSiteIds = lastTurboSiteIds,
            onDismiss = { showTurboDialog = false },
            onConfirm = { selectedSiteIds ->
                viewModel.startTurboMode(selectedSiteIds)
                lastTurboSiteIds = selectedSiteIds
                showTurboDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    sites: List<SiteAndLastSnap>,
    workInfo: List<WorkInfo> = emptyList(),
    loadingSites: Set<String> = emptySet(),
    isRefreshing: Boolean = false,
    turboModeState: TurboModeState = TurboModeState(),
    onRefresh: () -> Unit = {},
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit = {},
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onEditSite: (Site) -> Unit,
    onDeleteSite: (Site) -> Unit,
    onLearnMore: () -> Unit = {},
    onToggleSync: (Site) -> Unit = {},
    onToggleNotifications: (Site) -> Unit = {},
    onReloadSite: (Site) -> Unit = {},
    onLongPress: () -> Unit = {},
    onStopTurbo: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()

    // Bottom sheet state
    var selectedSite by remember { mutableStateOf<Site?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    // Helper function to determine sync status - improved logic
    fun getSyncStatus(site: Site): SyncStatus {
        // First check if site is in loading set
        if (loadingSites.contains(site.id)) {
            return SyncStatus.LOADING
        }

        // Check for any work info that contains the site ID or reload work
        val siteWorkInfo = workInfo.find { workInfo ->
            workInfo.tags.contains(site.id) ||
                    workInfo.tags.contains("reload_${site.id}") ||
                    (workInfo.tags.contains("singleEvent") && workInfo.tags.contains(site.id))
        }

        return when {
            siteWorkInfo?.state == WorkInfo.State.RUNNING -> {
                println("Site ${site.id} is LOADING - WorkInfo: ${siteWorkInfo.tags}")
                SyncStatus.LOADING
            }

            siteWorkInfo?.state == WorkInfo.State.ENQUEUED -> {
                println("Site ${site.id} is LOADING (enqueued) - WorkInfo: ${siteWorkInfo.tags}")
                SyncStatus.LOADING
            }

            !site.isSuccessful -> SyncStatus.ERROR
            else -> SyncStatus.OK
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Change Detection",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    if (turboModeState.isActive) {
                        TextButton(
                            onClick = onStopTurbo,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Stop Turbo")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSite,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
                Text("Add Site")
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
                    Text("No sites yet")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (turboModeState.isActive) {
                        item {
                            TurboModeBanner(
                                turboModeState = turboModeState,
                                totalSites = sites.size,
                                onStop = onStopTurbo
                            )
                        }
                    }

                    items(sites) { siteAndSnap ->
                        val syncStatus = getSyncStatus(siteAndSnap.site)
                        val isTurboSite = turboModeState.siteIds.contains(siteAndSnap.site.id)

                        MainCard(
                            site = siteAndSnap.site,
                            lastSnap = siteAndSnap.snap,
                            syncStatus = syncStatus,
                            onClick = { onSiteClick(siteAndSnap) },
                            onLongClick = {
                                selectedSite = siteAndSnap.site
                                showBottomSheet = true
                            },
                            onReloadClick = { onReloadSite(siteAndSnap.site) }
                        )
                    }
                }
            }
        }

        // Bottom sheet
        if (showBottomSheet && selectedSite != null) {
            SiteOptionsBottomSheet(
                site = selectedSite!!,
                onDismiss = {
                    showBottomSheet = false
                    selectedSite = null
                },
                onEdit = {
                    onEditSite(selectedSite!!)
                },
                onToggleSync = {
                    onToggleSync(selectedSite!!)
                },
                onToggleNotifications = {
                    onToggleNotifications(selectedSite!!)
                },
                onDelete = {
                    onDeleteSite(selectedSite!!)
                    selectedSite = null
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
private fun TurboModeBanner(
    turboModeState: TurboModeState,
    totalSites: Int,
    onStop: () -> Unit
) {
    val elapsed = System.currentTimeMillis() - turboModeState.startTime
    val remainingMs = (3 * 60 * 60 * 1000) - elapsed
    val remainingMinutes = (remainingMs / (60 * 1000)).coerceAtLeast(0)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = "Turbo Mode Active",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${remainingMinutes}min remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TextButton(
                onClick = onStop,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Stop")
            }
        }
    }
}

@Composable
private fun TurboIndicator() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    ) {
        Text(
            text = "TURBO",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusRow(
    site: Site,
    lastSnap: Snap?,
    syncStatus: SyncStatus
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (lastSnap != null) "Last: ${lastSnap.timestamp}" else "No snapshots",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReloadButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
    } else {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Reload",
                modifier = Modifier.size(20.dp)
            )
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
