package com.bernaferrari.changedetection.ui.main

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.work.WorkInfo
import com.bernaferrari.changedetection.QuickMonitorService
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.navigation.AddEditDestination
import com.bernaferrari.changedetection.navigation.ComparisonDestination
import com.bernaferrari.changedetection.navigation.EditSiteDestination
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.compose.MainCard
import com.bernaferrari.changedetection.ui.compose.SiteOptionsBottomSheet
import com.bernaferrari.changedetection.ui.compose.SyncStatus

@Composable
fun MainScreen(
    backStack: NavBackStack<NavKey>,
    viewModel: MainViewModel = hiltViewModel()
) {
    // Hoist MainViewModel or use existing? 
    // We will just show HomeScreen directly.
    // The FAB logic for empty state or adding site is in HomeScreen or MainScreen?
    // In original code, FAB was in Scaffold of MainScreen.
    // We should move FAB to ChangeDetectionBottomNavigation? No, that's a shell.
    // Nav3 usually allows FAB in the destination content.
    // So HomeScreen can have its own Scaffold or Box.

    // Simplification: Direct mapping to HomeScreen logic, keeping FAB.

    val sites by viewModel.sitesFlow.collectAsState()
    val workInfo by viewModel.workInfoFlow.collectAsState(initial = emptyList())
    val loadingSites by viewModel.loadingSites.collectAsState()
    val quickMonitorState by viewModel.quickMonitorState.collectAsState()
    val isInitialLoading by viewModel.isInitialLoading.collectAsState()
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(loadingSites) {
        if (loadingSites.isEmpty()) {
            isRefreshing = false
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { backStack.add(AddEditDestination) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(id = R.string.main_add_site)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            HomeScreen(
                sites = sites,
                workInfo = workInfo,
                loadingSites = loadingSites,
                isRefreshing = isRefreshing,
                isInitialLoading = isInitialLoading,
                quickMonitorState = quickMonitorState,
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
                onSiteClick = { backStack.add(ComparisonDestination(it.site.id)) },
                onEditSite = { backStack.add(EditSiteDestination(it.id)) },
                viewModel = viewModel,
                context = context
            )
        }
    }
}

@Composable
fun HomeScreen(
    sites: List<SiteAndLastSnap>,
    workInfo: List<WorkInfo>,
    loadingSites: Set<String>,
    isRefreshing: Boolean,
    isInitialLoading: Boolean,
    quickMonitorState: QuickMonitorState,
    onRefresh: () -> Unit,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onEditSite: (Site) -> Unit,
    viewModel: MainViewModel,
    context: android.content.Context
) {
    val pullToRefreshState = rememberPullToRefreshState()

    // Helper function to determine sync status
    fun getSyncStatus(site: Site): SyncStatus {
        if (loadingSites.contains(site.id)) return SyncStatus.LOADING
        val siteWorkInfo = workInfo.find { info ->
            info.tags.contains(site.id) || info.tags.contains("reload_${site.id}")
        }
        return when {
            siteWorkInfo?.state == WorkInfo.State.RUNNING -> SyncStatus.LOADING
            siteWorkInfo?.state == WorkInfo.State.ENQUEUED -> SyncStatus.LOADING
            !site.isSuccessful -> SyncStatus.ERROR
            else -> SyncStatus.OK
        }
    }

    var selectedSite by remember { mutableStateOf<Site?>(null) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isInitialLoading -> {
                // Loading State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            sites.isEmpty() -> {
                // Empty State
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "No sites yet",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add a website to start monitoring for changes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 20.dp,
                        bottom = 100.dp,
                        start = 20.dp,
                        end = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    state = rememberLazyListState()
                ) {
                    items(sites, key = { it.site.id }) { siteAndSnap ->
                        val syncStatus = getSyncStatus(siteAndSnap.site)
                        val isQuickMonitored = quickMonitorState.siteIds.contains(siteAndSnap.site.id)

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                                animationSpec = tween(300)
                            )
                        ) {
                            MainCard(
                                site = siteAndSnap.site,
                                lastSnap = siteAndSnap.snap,
                                syncStatus = syncStatus,
                                isQuickMonitored = isQuickMonitored,
                                onClick = { onSiteClick(siteAndSnap) },
                                onLongClick = { selectedSite = siteAndSnap.site },
                                onReloadClick = { viewModel.reloadSite(siteAndSnap.site) },
                                onOptionsClick = { selectedSite = siteAndSnap.site },
                                onQuickMonitorClick = null
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedSite != null) {
        SiteOptionsBottomSheet(
            site = selectedSite!!,
            onDismiss = { selectedSite = null },
            onEdit = {
                onEditSite(selectedSite!!)
                selectedSite = null
            },
            onDelete = {
                viewModel.removeSite(selectedSite!!)
                selectedSite = null
            },
            onToggleSync = {
                viewModel.updateSite(selectedSite!!.copy(isSyncEnabled = !selectedSite!!.isSyncEnabled))
            },
            onToggleNotifications = {
                viewModel.updateSite(selectedSite!!.copy(isNotificationEnabled = !selectedSite!!.isNotificationEnabled))
            }
        )
    }
}
