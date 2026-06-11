/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.SharedMainCard
import com.bernaferrari.changedetection.ui.components.SyncStatus
import com.bernaferrari.changedetection.util.readableFileSize
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.app_name
import com.bernaferrari.changedetection.shared.generated.resources.refresh_all
import com.bernaferrari.changedetection.shared.generated.resources.add_site
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.bernaferrari.changedetection.background.SyncState
import androidx.compose.material.icons.rounded.Bolt
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring_description
import com.bernaferrari.changedetection.shared.generated.resources.add_first_site
import com.bernaferrari.changedetection.shared.generated.resources.delete_site_title
import com.bernaferrari.changedetection.shared.generated.resources.delete_site_message
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.site_deleted
import com.bernaferrari.changedetection.shared.generated.resources.undo
import com.bernaferrari.changedetection.shared.generated.resources.connection_error
import com.bernaferrari.changedetection.shared.generated.resources.changes_detected
import com.bernaferrari.changedetection.shared.generated.resources.site_saved
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onAddSite: () -> Unit,
    onSiteClick: (Site) -> Unit,
    onSiteEdit: (Site) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sites by viewModel.sitesFlow.collectAsState()
    val quickMonitorState by viewModel.quickMonitorState.collectAsState()
    val loadingSites by viewModel.loadingSites.collectAsState()
    val syncStatus by viewModel.syncStatusFlow.collectAsState(initial = emptyList())
    val currentError by viewModel.currentError.collectAsState()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // Pre-compute string resources for use in coroutines
    val connectionErrorString = stringResource(Res.string.connection_error)
    val siteDeletedString = stringResource(Res.string.site_deleted)
    val undoString = stringResource(Res.string.undo)

    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }

    // Delete confirmation state
    var siteToDelete by remember { mutableStateOf<SiteAndLastSnap?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Consume UI messages (success feedback)
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { message ->
            when (message) {
                is UiMessage.Success -> {
                    snackbarHostState.showSnackbar(
                        message = message.message,
                        duration = SnackbarDuration.Short,
                    )
                }
                is UiMessage.Error -> {
                    snackbarHostState.showSnackbar(
                        message = message.message,
                        duration = SnackbarDuration.Long,
                    )
                }
            }
        }
    }

    // Error handling for sync failures
    LaunchedEffect(syncStatus) {
        val hasError = syncStatus.any { it.state == SyncState.FAILED }
        if (hasError) {
            snackbarHostState.showSnackbar(
                message = connectionErrorString,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
        }
    }

    // Handle pull-to-refresh
    fun onRefresh() {
        scope.launch {
            isRefreshing = true
            viewModel.refreshAllSites()
            delay(1500)
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.refreshAllSites()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.refresh_all),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddSite()
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.add_site),
                )
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        if (sites == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else if (sites!!.isEmpty()) {
            SharedEmptyState(
                icon = Icons.Rounded.Bolt,
                title = stringResource(Res.string.start_monitoring),
                description = stringResource(Res.string.start_monitoring_description),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                primaryActionLabel = stringResource(Res.string.add_first_site),
                onPrimaryAction = onAddSite,
            )
        } else {
            // Box to allow ErrorBanner to align to bottom
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Pull-to-refresh wrapper around the list
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { onRefresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    SitesList(
                        sites = sites!!,
                        quickMonitorSiteIds = quickMonitorState.siteIds,
                        loadingSites = loadingSites,
                        syncErrors = syncStatus.filter { it.state == SyncState.FAILED }.map { it.tags }.flatten().toSet(),
                        onSiteClick = { siteAndSnap -> onSiteClick(siteAndSnap.site) },
                        onSiteRefresh = { siteAndSnap ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.reloadSite(siteAndSnap.site)
                        },
                        onSiteEdit = { siteAndSnap -> onSiteEdit(siteAndSnap.site) },
                        onSiteDelete = { siteAndSnap ->
                            siteToDelete = siteAndSnap
                            showDeleteDialog = true
                        },
                    )
                }

                // Error Banner
                ErrorBanner(
                    error = currentError,
                    onDismiss = { viewModel.dismissError() },
                    onRetry = { viewModel.retryLastAction() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && siteToDelete != null) {
        val site = siteToDelete!!.site
        ConfirmDialog(
            visible = true,
            onDismiss = {
                showDeleteDialog = false
                siteToDelete = null
            },
            onConfirm = {
                viewModel.onDeleteSite(site)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = siteDeletedString,
                        actionLabel = undoString,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoDeleteSite(site)
                    } else {
                        viewModel.onConfirmDeleteSite(site)
                    }
                }
            },
            title = stringResource(Res.string.delete_site_title),
            message = stringResource(Res.string.delete_site_message, site.title ?: site.url),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}

@Composable
private fun SitesList(
    sites: List<SiteAndLastSnap>,
    quickMonitorSiteIds: Set<String>,
    loadingSites: Set<String>,
    syncErrors: Set<String>,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onSiteRefresh: (SiteAndLastSnap) -> Unit,
    onSiteEdit: (SiteAndLastSnap) -> Unit,
    onSiteDelete: (SiteAndLastSnap) -> Unit,
) {
    var showOptionsFor by remember { mutableStateOf<SiteAndLastSnap?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = sites,
            key = { it.site.id },
        ) { siteAndSnap ->
            val isLoadingSite = loadingSites.contains(siteAndSnap.site.id)
            val isQuickMonitored = quickMonitorSiteIds.contains(siteAndSnap.site.id)
            val hasError = syncErrors.contains(siteAndSnap.site.id)

            val syncStatus = when {
                isLoadingSite || siteAndSnap.isSyncing -> SyncStatus.LOADING
                hasError -> SyncStatus.ERROR
                else -> SyncStatus.OK
            }

            val lastSyncValue = siteAndSnap.site.timestamp.takeIf { it > 0 }?.let {
                getRelativeTimeLabel(it)
            } ?: "-"

            val lastChangeValue = siteAndSnap.snap?.timestamp?.let {
                getRelativeTimeLabel(it)
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
                onOptionsClick = { showOptionsFor = siteAndSnap },
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
            },
        )
    }
}
