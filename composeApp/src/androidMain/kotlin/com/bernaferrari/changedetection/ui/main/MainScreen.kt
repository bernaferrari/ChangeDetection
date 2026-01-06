package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.components.SharedMainCard
import com.bernaferrari.changedetection.ui.components.SyncStatus
import com.bernaferrari.changedetection.util.readableFileSize
import com.github.marlonlom.utilities.timeago.TimeAgo

/**
 * MainScreen with callback-based navigation for flexibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddSite: () -> Unit,
    onSiteClick: (Site) -> Unit,
    onSiteEdit: (Site) -> Unit,
    modifier: Modifier = Modifier
) {
    val sites by viewModel.sitesFlow.collectAsState()
    val quickMonitorState by viewModel.quickMonitorState.collectAsState()
    val loadingSites by viewModel.loadingSites.collectAsState()

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAllSites() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_all)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSite,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_site)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sites == null) {
                // Initial loading from database
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else if (sites!!.isEmpty()) {
                EmptyStateComponent(
                    modifier = Modifier.fillMaxSize(),
                    onAddSite = onAddSite
                )
            } else {
                SitesList(
                    sites = sites!!,
                    quickMonitorSiteIds = quickMonitorState.siteIds,
                    loadingSites = loadingSites,
                    onSiteClick = { siteAndSnap -> onSiteClick(siteAndSnap.site) },
                    onSiteRefresh = { siteAndSnap -> viewModel.reloadSite(siteAndSnap.site) },
                    onSiteEdit = { siteAndSnap -> onSiteEdit(siteAndSnap.site) },
                    onSiteDelete = { siteAndSnap -> viewModel.removeSite(siteAndSnap.site) }
                )
            }
        }
    }
}

@Composable
private fun SitesList(
    sites: List<SiteAndLastSnap>,
    quickMonitorSiteIds: Set<String>,
    loadingSites: Set<String>,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onSiteRefresh: (SiteAndLastSnap) -> Unit,
    onSiteEdit: (SiteAndLastSnap) -> Unit,
    onSiteDelete: (SiteAndLastSnap) -> Unit
) {
    var showOptionsFor by remember { mutableStateOf<SiteAndLastSnap?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = sites,
            key = { it.site.id }
        ) { siteAndSnap ->
            val isLoadingSite = loadingSites.contains(siteAndSnap.site.id)
            val isQuickMonitored = quickMonitorSiteIds.contains(siteAndSnap.site.id)

            val syncStatus = when {
                isLoadingSite || siteAndSnap.isSyncing -> SyncStatus.LOADING
                else -> SyncStatus.OK
            }

            val lastSyncValue = siteAndSnap.site.timestamp.takeIf { it > 0 }?.let {
                TimeAgo.using(it)
            } ?: "-"

            val lastChangeValue = siteAndSnap.snap?.timestamp?.let {
                TimeAgo.using(it)
            } ?: "-"

            val fileSizeValue = siteAndSnap.snap?.contentSize?.takeIf { it > 0 }?.let {
                it.toLong().readableFileSize()
            } ?: ""

            SharedMainCard(
                site = siteAndSnap.site,
                lastSnap = siteAndSnap.snap,
                syncStatus = syncStatus,
                isQuickMonitored = isQuickMonitored,
                lastSyncValue = lastSyncValue,
                lastChangeValue = lastChangeValue,
                fileSizeValue = fileSizeValue,
                onClick = { onSiteClick(siteAndSnap) },
                onRefreshClick = { onSiteRefresh(siteAndSnap) },
                onOptionsClick = { showOptionsFor = siteAndSnap }
            )
        }
    }

    // Options bottom sheet
    showOptionsFor?.let { siteAndSnap ->
        SiteOptionsBottomSheet(
            site = siteAndSnap.site,
            onDismiss = { showOptionsFor = null },
            onRefresh = {
                onSiteRefresh(siteAndSnap)
                showOptionsFor = null
            },
            onEdit = {
                onSiteEdit(siteAndSnap)
                showOptionsFor = null
            },
            onDelete = {
                onSiteDelete(siteAndSnap)
                showOptionsFor = null
            }
        )
    }
}
