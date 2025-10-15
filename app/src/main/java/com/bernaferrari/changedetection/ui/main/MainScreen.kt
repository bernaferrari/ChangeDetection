package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.work.WorkInfo
import com.bernaferrari.changedetection.QuickMonitorService
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.compose.MainCard
import com.bernaferrari.changedetection.ui.compose.SiteOptionsBottomSheet
import com.bernaferrari.changedetection.ui.compose.SyncStatus
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val sites by viewModel.sitesFlow.collectAsState()
    val workInfo by viewModel.workInfoFlow.collectAsState(initial = emptyList())
    val loadingSites by viewModel.loadingSites.collectAsState()
    val quickMonitorState by viewModel.quickMonitorState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(loadingSites) {
        if (loadingSites.isEmpty()) {
            isRefreshing = false
        }
    }

    MainScreenContent(
        sites = sites,
        workInfo = workInfo,
        loadingSites = loadingSites,
        isRefreshing = isRefreshing,
        quickMonitorState = quickMonitorState,
        viewModel = viewModel,
        context = context,
        onRefresh = {
            if (sites.isEmpty()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.turbo_mode_no_sites),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                isRefreshing = true
                viewModel.refreshAllSites()
            }
        },
        onAddSite = { navController.navigate("add_edit") },
        onHowItWorks = {
            navController.navigate("how_it_works")
        },
        onSiteClick = { siteAndSnap ->
            // Simplified navigation - let ComparisonScreen auto-detect content type
            navController.navigate("comparison/${siteAndSnap.site.id}")
        },
        onEditSite = { site ->
            navController.navigate("add_edit/${site.id}")
        },
        onDeleteSite = { site ->
            viewModel.removeSite(site)
        },
        onToggleSync = { site ->
            viewModel.updateSite(site.copy(isSyncEnabled = !site.isSyncEnabled))
        },
        onToggleNotifications = { site ->
            viewModel.updateSite(site.copy(isNotificationEnabled = !site.isNotificationEnabled))
        },
        onReloadSite = { site ->
            viewModel.reloadSite(site)
        },
        onLongPress = { site ->
            if (!quickMonitorState.isActive) {
                viewModel.startQuickMonitor(setOf(site.id))
            }
        },
        onStopQuickMonitor = {
            QuickMonitorService.stop(context)
            viewModel.stopQuickMonitor()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    sites: List<SiteAndLastSnap>,
    workInfo: List<WorkInfo> = emptyList(),
    loadingSites: Set<String> = emptySet(),
    isRefreshing: Boolean = false,
    quickMonitorState: QuickMonitorState = QuickMonitorState(),
    viewModel: MainViewModel? = null,
    context: android.content.Context = LocalContext.current,
    onRefresh: () -> Unit = {},
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit = {},
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onEditSite: (Site) -> Unit,
    onDeleteSite: (Site) -> Unit,
    onToggleSync: (Site) -> Unit = {},
    onToggleNotifications: (Site) -> Unit = {},
    onReloadSite: (Site) -> Unit = {},
    onLongPress: (Site) -> Unit = {},
    onStopQuickMonitor: () -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()

    // Bottom sheet state
    var selectedSite by remember { mutableStateOf<Site?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTurboDialog by rememberSaveable { mutableStateOf(false) }
    var pendingTurboInterval by rememberSaveable { mutableStateOf(quickMonitorState.intervalSeconds) }
    var cachedTurboSelection by rememberSaveable { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(quickMonitorState.intervalSeconds, quickMonitorState.isActive) {
        if (quickMonitorState.isActive) {
            pendingTurboInterval = quickMonitorState.intervalSeconds
        }
    }

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
            siteWorkInfo?.state == WorkInfo.State.RUNNING -> SyncStatus.LOADING
            siteWorkInfo?.state == WorkInfo.State.ENQUEUED -> SyncStatus.LOADING
            !site.isSuccessful -> SyncStatus.ERROR
            else -> SyncStatus.OK
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(id = R.string.main_header_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = pluralStringResource(R.plurals.main_header_site_count, sites.size, sites.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (quickMonitorState.isActive) {
                        TextButton(
                            onClick = {
                                try {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.quick_monitor_extend_1h),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    QuickMonitorService.extend(context)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Text(text = stringResource(id = R.string.quick_monitor_extend_1h))
                        }

                        TextButton(
                            onClick = onStopQuickMonitor,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(id = R.string.action_stop_monitoring))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddSite,
                icon = { Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.main_add_site)) },
                text = { Text(stringResource(id = R.string.main_add_site)) }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                if (quickMonitorState.isActive) {
                    QuickMonitorBanner(
                        isActive = true,
                        sitesCount = quickMonitorState.siteIds.size,
                        intervalSeconds = quickMonitorState.intervalSeconds,
                        onToggle = onStopQuickMonitor,
                        startTime = quickMonitorState.startTime.takeIf { it > 0L },
                        deadlineTime = quickMonitorState.deadlineTime.takeIf { it > 0L },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                QuickMonitorCard(
                    isActive = quickMonitorState.isActive,
                    sitesCount = quickMonitorState.siteIds.size,
                    totalSitesCount = sites.size,
                    intervalSeconds = quickMonitorState.intervalSeconds,
                    onStart = { selectedInterval ->
                        if (sites.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.turbo_mode_no_sites),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            pendingTurboInterval = selectedInterval
                            if (quickMonitorState.siteIds.isNotEmpty()) {
                                cachedTurboSelection = quickMonitorState.siteIds.toList()
                            }
                            showTurboDialog = true
                        }
                    },
                    onStop = onStopQuickMonitor,
                    onExtend = {
                        try {
                            Toast.makeText(
                                context,
                                context.getString(R.string.quick_monitor_toast_extended),
                                Toast.LENGTH_SHORT
                            ).show()
                            QuickMonitorService.extend(context)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (sites.isEmpty()) {
                    MainEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onAddSite = onAddSite,
                        onHowItWorks = onHowItWorks
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(sites) { siteAndSnap ->
                            val syncStatus = getSyncStatus(siteAndSnap.site)
                            val isQuickMonitored = quickMonitorState.siteIds.contains(siteAndSnap.site.id)

                            MainCard(
                                site = siteAndSnap.site,
                                lastSnap = siteAndSnap.snap,
                                syncStatus = syncStatus,
                                isQuickMonitored = isQuickMonitored,
                                onClick = { onSiteClick(siteAndSnap) },
                                onLongClick = {
                                    selectedSite = siteAndSnap.site
                                    showBottomSheet = true
                                },
                                onReloadClick = {
                                    if (isQuickMonitored) {
                                        onStopQuickMonitor()
                                    } else {
                                        onReloadSite(siteAndSnap.site)
                                    }
                                },
                                onReloadLongClick = {
                                    if (!quickMonitorState.isActive) {
                                        onLongPress(siteAndSnap.site)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

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

        if (showTurboDialog) {
            val preselected = when {
                quickMonitorState.siteIds.isNotEmpty() -> quickMonitorState.siteIds.toList()
                cachedTurboSelection.isNotEmpty() -> cachedTurboSelection
                else -> emptyList()
            }

            TurboModeDialog(
                sites = sites.map { it.site },
                lastSelectedSiteIds = preselected,
                intervalSeconds = pendingTurboInterval,
                onDismiss = { showTurboDialog = false },
                onIntervalChange = { pendingTurboInterval = it },
                onConfirm = { selectedIds ->
                    if (selectedIds.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.turbo_mode_no_sites),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@TurboModeDialog
                    }
                    cachedTurboSelection = selectedIds
                    try {
                        Toast.makeText(
                            context,
                            context.getString(R.string.quick_monitor_toast_starting),
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel?.startQuickMonitor(selectedIds.toSet(), intervalSeconds = pendingTurboInterval)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(
                                R.string.quick_monitor_toast_failed,
                                e.message ?: context.getString(R.string.error)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    showTurboDialog = false
                }
            )
        }
    }
}

@Composable
private fun MainEmptyState(
    modifier: Modifier = Modifier,
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit
) {
    val featureItems = stringArrayResource(id = R.array.main_features_list)
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_scale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐",
                    fontSize = 48.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.main_empty_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = stringResource(id = R.string.main_empty_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.main_features_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    featureItems.forEach { feature ->
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = onAddSite, shape = RoundedCornerShape(16.dp)) {
                    Text(text = stringResource(id = R.string.main_add_site))
                }
                TextButton(onClick = onHowItWorks) {
                    Text(text = stringResource(id = R.string.main_empty_learn_more))
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
