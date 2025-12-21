package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bernaferrari.changedetection.ui.compose.MainCard
import com.bernaferrari.changedetection.ui.compose.SiteOptionsBottomSheet
import com.bernaferrari.changedetection.ui.compose.SyncStatus
import com.bernaferrari.changedetection.ui.design.CanvasSectionHeader

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

    var showQuickMonitorSheet by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp)
            ) {
                item {
                    HomeHeroSection(
                        sitesCount = sites.size,
                        onAddSite = onAddSite,
                        onHowItWorks = onHowItWorks,
                        quickMonitorSites = quickMonitorState.siteIds.size
                    )
                }

                item {
                    QuickMonitorStatusCard(
                        quickMonitorState = quickMonitorState,
                        onQuickMonitorClick = {
                            if (sites.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.turbo_mode_no_sites),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showQuickMonitorSheet = true
                            }
                        },
                        onStopQuickMonitor = onStopQuickMonitor
                    )
                }

                if (sites.isEmpty()) {
                    item {
                        MainEmptyState(
                            modifier = Modifier.fillMaxWidth(),
                            onAddSite = onAddSite,
                            onHowItWorks = onHowItWorks
                        )
                    }
                } else {
                    item {
                        CanvasSectionHeader(
                            title = stringResource(id = R.string.home_monitored_sites_title)
                        )
                    }

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
                            onOptionsClick = {
                                selectedSite = siteAndSnap.site
                                showBottomSheet = true
                            },
                            onQuickMonitorClick = when {
                                quickMonitorState.isActive -> null
                                viewModel != null -> {
                                    {
                                        viewModel.startQuickMonitor(setOf(siteAndSnap.site.id))
                                    }
                                }
                                else -> null
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
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

        if (showQuickMonitorSheet) {
            QuickMonitorBottomSheet(
                isActive = quickMonitorState.isActive,
                sitesCount = quickMonitorState.siteIds.size,
                totalSitesCount = sites.size,
                intervalSeconds = quickMonitorState.intervalSeconds,
                onStart = { selectedInterval ->
                    if (sites.isEmpty()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.turbo_mode_no_sites),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        pendingTurboInterval = selectedInterval
                        if (quickMonitorState.siteIds.isNotEmpty()) {
                            cachedTurboSelection = quickMonitorState.siteIds.toList()
                        }
                        showQuickMonitorSheet = false
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
                onDismiss = { showQuickMonitorSheet = false },
                sheetState = androidx.compose.material3.rememberModalBottomSheetState()
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
private fun HomeHeroSection(
    sitesCount: Int,
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit,
    quickMonitorSites: Int
) {
    val subtitle = if (sitesCount == 0) {
        stringResource(id = R.string.home_hero_subtitle_empty)
    } else {
        stringResource(id = R.string.home_hero_subtitle, sitesCount)
    }
    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.tertiary
        ),
        start = Offset(0f, 0f),
        end = Offset(600f, 600f)
    )
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(heroGradient)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = contentColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = stringResource(id = R.string.home_hero_badge),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(id = R.string.home_hero_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.85f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroMetricChip(
                label = stringResource(id = R.string.home_stats_sites_label),
                value = sitesCount.toString(),
                icon = Icons.Filled.Language,
                modifier = Modifier.weight(1f),
                containerColor = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor
            )
            HeroMetricChip(
                label = stringResource(id = R.string.home_stats_quick_monitor_label),
                value = quickMonitorSites.toString(),
                icon = Icons.Filled.Speed,
                modifier = Modifier.weight(1f),
                containerColor = contentColor.copy(alpha = 0.12f),
                contentColor = contentColor
            )
        }
        Button(
            onClick = onAddSite,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = contentColor,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = stringResource(id = R.string.main_add_site))
        }
        TextButton(
            onClick = onHowItWorks,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.home_action_learn))
        }
    }
}

@Composable
private fun QuickMonitorStatusCard(
    quickMonitorState: QuickMonitorState,
    onQuickMonitorClick: () -> Unit,
    onStopQuickMonitor: () -> Unit
) {
    val isActive = quickMonitorState.isActive
    val description = if (isActive) {
        stringResource(
            id = R.string.home_quick_monitor_description_active,
            quickMonitorState.intervalSeconds,
            quickMonitorState.siteIds.size
        )
    } else {
        stringResource(id = R.string.home_quick_monitor_description_idle)
    }

    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = contentColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = contentColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isActive) {
                            stringResource(id = R.string.home_quick_monitor_active)
                        } else {
                            stringResource(id = R.string.home_quick_monitor_idle)
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = contentColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
                StatusBadge(
                    text = if (isActive) {
                        stringResource(id = R.string.quick_monitor_banner_live_badge)
                    } else {
                        stringResource(id = R.string.home_stats_syncing_idle)
                    },
                    containerColor = contentColor.copy(alpha = 0.15f),
                    contentColor = contentColor
                )
            }
            Button(
                onClick = if (isActive) onStopQuickMonitor else onQuickMonitorClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = if (isActive) {
                    ButtonDefaults.buttonColors(
                        containerColor = contentColor,
                        contentColor = containerColor
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            ) {
                Text(
                    text = if (isActive) {
                        stringResource(id = R.string.home_quick_monitor_stop)
                    } else {
                        stringResource(id = R.string.home_quick_monitor_start)
                    }
                )
            }
            TextButton(
                onClick = onQuickMonitorClick,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = ButtonDefaults.textButtonColors(contentColor = contentColor.copy(alpha = 0.9f))
            ) {
                Text(text = stringResource(id = R.string.home_quick_monitor_manage))
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HeroMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val backgroundColor = containerColor ?: MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    val foregroundColor = contentColor ?: MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = foregroundColor.copy(alpha = 0.12f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = foregroundColor
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = foregroundColor.copy(alpha = 0.75f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = foregroundColor
                )
            }
        }
    }
}

@Composable
private fun MainEmptyState(
    modifier: Modifier = Modifier,
    onAddSite: () -> Unit,
    onHowItWorks: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.main_empty_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.main_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onAddSite,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(text = stringResource(id = R.string.home_empty_primary))
            }
            TextButton(onClick = onHowItWorks) {
                Text(text = stringResource(id = R.string.main_empty_learn_more))
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
