/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.add_first_site
import com.bernaferrari.changedetection.shared.generated.resources.add_site
import com.bernaferrari.changedetection.shared.generated.resources.connection_error
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.delete_site_message
import com.bernaferrari.changedetection.shared.generated.resources.delete_site_title
import com.bernaferrari.changedetection.shared.generated.resources.error
import com.bernaferrari.changedetection.shared.generated.resources.home_search_clear
import com.bernaferrari.changedetection.shared.generated.resources.home_search_close
import com.bernaferrari.changedetection.shared.generated.resources.home_search_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.home_search_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.home_search_monitors
import com.bernaferrari.changedetection.shared.generated.resources.learn_how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_paused
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_errors
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_new
import com.bernaferrari.changedetection.shared.generated.resources.monitor_review_changes
import com.bernaferrari.changedetection.shared.generated.resources.monitor_signal_label
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_checking
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_checking_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_error
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_error_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_quiet
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_quiet_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_updated
import com.bernaferrari.changedetection.shared.generated.resources.monitor_summary_updated_supporting
import com.bernaferrari.changedetection.shared.generated.resources.monitor_watchlist
import com.bernaferrari.changedetection.shared.generated.resources.monitor_watchlist_supporting
import com.bernaferrari.changedetection.shared.generated.resources.nav_home
import com.bernaferrari.changedetection.shared.generated.resources.pruned_snapshots
import com.bernaferrari.changedetection.shared.generated.resources.refresh_all
import com.bernaferrari.changedetection.shared.generated.resources.show_all
import com.bernaferrari.changedetection.shared.generated.resources.site_deleted
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring
import com.bernaferrari.changedetection.shared.generated.resources.start_monitoring_description
import com.bernaferrari.changedetection.shared.generated.resources.undo
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.SignalEmptyState
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The watchlist route. State collection and destructive actions stay here; the list is stateless. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    selectedSiteId: String? = null,
    onAddSite: () -> Unit,
    onLearnHowItWorks: () -> Unit,
    onSiteClick: (Site) -> Unit,
    onSiteEdit: (Site) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sites by viewModel.sitesFlow.collectAsStateWithLifecycle()
    val quickMonitorState by viewModel.quickMonitorState.collectAsStateWithLifecycle()
    val loadingSites by viewModel.loadingSites.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatusFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentError by viewModel.currentError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showOptionsFor by remember { mutableStateOf<SiteAndLastSnap?>(null) }
    var siteToDelete by remember { mutableStateOf<SiteAndLastSnap?>(null) }
    var pullRefreshRequested by remember { mutableStateOf(false) }
    var pullRefreshObserved by remember { mutableStateOf(false) }
    val isRefreshing = loadingSites.isNotEmpty()

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) searchFocusRequester.requestFocus()
    }
    LaunchedEffect(pullRefreshRequested, isRefreshing) {
        if (pullRefreshRequested && isRefreshing) {
            pullRefreshObserved = true
        } else if (pullRefreshRequested && pullRefreshObserved && !isRefreshing) {
            pullRefreshRequested = false
            pullRefreshObserved = false
        }
    }
    LaunchedEffect(pullRefreshRequested) {
        if (pullRefreshRequested) {
            delay(2_000)
            if (!isRefreshing && !pullRefreshObserved) pullRefreshRequested = false
        }
    }
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        searchQuery = ""
    }

    val connectionErrorString = stringResource(Res.string.connection_error)
    val snapshotsPrunedString = stringResource(Res.string.pruned_snapshots)
    val siteDeletedString = stringResource(Res.string.site_deleted)
    val undoString = stringResource(Res.string.undo)
    val addMonitorLabel = stringResource(Res.string.add_site)
    val failedSiteIds = remember(syncStatus, sites) {
        syncStatus
            .filter { it.state == SyncState.FAILED }
            .flatMapTo(mutableSetOf()) { it.tags } +
            sites.orEmpty().filterNot { it.site.isSuccessful }.mapTo(mutableSetOf()) { it.site.id }
    }

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { message ->
            if (message == UiMessage.SnapshotsPruned) {
                snackbarHostState.showSnackbar(snapshotsPrunedString, duration = SnackbarDuration.Short)
            }
        }
    }
    LaunchedEffect(syncStatus) {
        if (syncStatus.any { it.state == SyncState.FAILED }) {
            snackbarHostState.showSnackbar(
                message = connectionErrorString,
                withDismissAction = true,
                duration = SnackbarDuration.Short,
            )
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MonitorTopBar(
                siteCount = sites?.size ?: 0,
                searchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSearchOpen = { isSearchActive = true },
                onSearchClose = {
                    isSearchActive = false
                    searchQuery = ""
                },
                onSearchClear = { searchQuery = "" },
                isRefreshing = isRefreshing,
                onRefreshAll = viewModel::refreshAllSites,
                searchFocusRequester = searchFocusRequester,
                onSearchSubmitted = { keyboardController?.hide() },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (!sites.isNullOrEmpty() && !isSearchActive) {
                ExtendedFloatingActionButton(
                    onClick = onAddSite,
                    modifier = Modifier.semantics { contentDescription = addMonitorLabel },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(addMonitorLabel) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        when {
            sites == null -> MonitorLoading(modifier = Modifier.padding(paddingValues))

            sites.isNullOrEmpty() -> SignalEmptyState(
                title = stringResource(Res.string.start_monitoring),
                description = stringResource(Res.string.start_monitoring_description),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                primaryActionLabel = stringResource(Res.string.add_first_site),
                onPrimaryAction = onAddSite,
                secondaryActionLabel = stringResource(Res.string.learn_how_it_works),
                onSecondaryAction = onLearnHowItWorks,
            )

            else -> Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                PullToRefreshBox(
                    isRefreshing = pullRefreshRequested,
                    onRefresh = {
                        pullRefreshObserved = false
                        pullRefreshRequested = true
                        viewModel.refreshAllSites()
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    MonitorWatchlist(
                        sites = sites.orEmpty(),
                        searchQuery = if (isSearchActive) searchQuery else "",
                        selectedSiteId = selectedSiteId,
                        quickMonitorState = quickMonitorState,
                        loadingSites = loadingSites,
                        failedSiteIds = failedSiteIds,
                        isRefreshing = isRefreshing && !pullRefreshRequested,
                        onSiteClick = { onSiteClick(it.site) },
                        onSiteOptions = { showOptionsFor = it },
                    )
                }
                ErrorBanner(
                    error = currentError,
                    onDismiss = viewModel::dismissError,
                    onRetry = viewModel::retryLastAction,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                )
            }
        }
    }

    showOptionsFor?.let { selected ->
        SiteOptionsBottomSheet(
            site = selected.site,
            onDismiss = { showOptionsFor = null },
            onRefresh = {
                viewModel.reloadSite(selected.site)
                showOptionsFor = null
            },
            onEdit = {
                onSiteEdit(selected.site)
                showOptionsFor = null
            },
            onDelete = {
                showOptionsFor = null
                siteToDelete = selected
            },
        )
    }

    siteToDelete?.let { selected ->
        ConfirmDialog(
            visible = true,
            onDismiss = { siteToDelete = null },
            onConfirm = {
                siteToDelete = null
                viewModel.onDeleteSite(selected.site)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = siteDeletedString,
                        actionLabel = undoString,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoDeleteSite(selected.site)
                    } else {
                        viewModel.onConfirmDeleteSite(selected.site)
                    }
                }
            },
            title = stringResource(Res.string.delete_site_title),
            message = stringResource(
                Res.string.delete_site_message,
                selected.site.title?.takeIf { it.isNotBlank() } ?: selected.site.url,
            ),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonitorTopBar(
    siteCount: Int,
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchOpen: () -> Unit,
    onSearchClose: () -> Unit,
    onSearchClear: () -> Unit,
    isRefreshing: Boolean,
    onRefreshAll: () -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchSubmitted: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val refreshLabel = stringResource(Res.string.refresh_all)
    TopAppBar(
        title = {
            if (searchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                    placeholder = { Text(stringResource(Res.string.home_search_monitors)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchSubmitted() }),
                    shape = MaterialTheme.shapes.large,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
            } else {
                Text(
                    text = stringResource(Res.string.nav_home),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (searchActive) {
                IconButton(onClick = onSearchClose) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.home_search_close),
                    )
                }
            }
        },
        actions = {
            if (searchActive && searchQuery.isNotBlank()) {
                IconButton(onClick = onSearchClear) {
                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(Res.string.home_search_clear))
                }
            } else if (!searchActive && siteCount > 0) {
                IconButton(onClick = onSearchOpen) {
                    Icon(Icons.Rounded.Search, contentDescription = stringResource(Res.string.home_search_monitors))
                }
                IconButton(onClick = onRefreshAll, enabled = !isRefreshing) {
                    Icon(Icons.Rounded.Refresh, contentDescription = refreshLabel)
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}

@Composable
private fun MonitorWatchlist(
    sites: List<SiteAndLastSnap>,
    searchQuery: String,
    selectedSiteId: String?,
    quickMonitorState: QuickMonitorState,
    loadingSites: Set<String>,
    failedSiteIds: Set<String>,
    isRefreshing: Boolean,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onSiteOptions: (SiteAndLastSnap) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(MonitorFilter.All) }
    val changedIds = remember(sites) {
        sites.filterTo(mutableListOf()) { it.hasDetectedChange() }.mapTo(mutableSetOf()) { it.site.id }
    }
    val counts = remember(sites, changedIds, failedSiteIds) {
        MonitorCounts(
            all = sites.size,
            changed = sites.count { it.site.id in changedIds },
            errors = sites.count { it.site.id in failedSiteIds },
            paused = sites.count { !it.site.isSyncEnabled },
        )
    }
    val visibleSites = remember(sites, searchQuery, filter, changedIds, failedSiteIds) {
        val query = searchQuery.trim()
        sites.asSequence()
            .filter { item ->
                query.isBlank() ||
                    item.site.title.orEmpty().contains(query, ignoreCase = true) ||
                    item.site.url.contains(query, ignoreCase = true) ||
                    item.site.notes.contains(query, ignoreCase = true)
            }
            .filter { item ->
                when (filter) {
                    MonitorFilter.All -> true
                    MonitorFilter.Changed -> item.site.id in changedIds
                    MonitorFilter.Errors -> item.site.id in failedSiteIds
                    MonitorFilter.Paused -> !item.site.isSyncEnabled
                }
            }
            .sortedWith(
                compareBy<SiteAndLastSnap> { item ->
                    when {
                        item.site.id in failedSiteIds -> 0
                        item.site.id in changedIds -> 1
                        item.site.id in loadingSites -> 2
                        !item.site.isSyncEnabled -> 4
                        else -> 3
                    }
                }.thenByDescending { it.site.timestamp },
            ).toList()
    }

    // Keep the list pane anchored from its first frame. On expanded layouts Nav3 reserves the
    // detail pane immediately, so selecting a monitor never makes the watchlist jump sideways.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 168.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (searchQuery.isBlank()) {
                item(key = "summary") {
                    MonitorPulseCard(
                        counts = counts,
                        isRefreshing = isRefreshing,
                        onReviewChanges = { filter = MonitorFilter.Changed },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
            item(key = "filters") {
                MonitorFilters(
                    selected = filter,
                    counts = counts,
                    onSelected = { filter = it },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            if (visibleSites.isEmpty()) {
                item(key = "empty") { MonitorSearchEmptyState() }
            }
            itemsIndexed(visibleSites, key = { _, item -> item.site.id }) { index, item ->
                SiteRow(
                    site = item.site,
                    status = item.toSiteStatus(
                        isLoading = item.site.id in loadingSites,
                        hasError = item.site.id in failedSiteIds,
                    ),
                    isQuickMonitored = item.site.id in quickMonitorState.siteIds,
                    isSelected = selectedSiteId == item.site.id,
                    onClick = { onSiteClick(item) },
                    onLongClick = { onSiteOptions(item) },
                    position = siteRowPosition(index, visibleSites.lastIndex),
                )
            }
        }
    }
}

@Composable
private fun MonitorPulseCard(
    counts: MonitorCounts,
    isRefreshing: Boolean,
    onReviewChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val signalState = when {
        isRefreshing -> MonitorSignalState.Listening
        counts.changed > 0 -> MonitorSignalState.Changed
        counts.errors > 0 -> MonitorSignalState.Error
        else -> MonitorSignalState.Quiet
    }
    val summary = when (signalState) {
        MonitorSignalState.Listening -> MonitorSummary(
            title = stringResource(Res.string.monitor_summary_checking),
            supporting = stringResource(Res.string.monitor_summary_checking_supporting),
        )

        MonitorSignalState.Changed -> MonitorSummary(
            title = pluralStringResource(
                Res.plurals.monitor_summary_updated,
                counts.changed,
                counts.changed,
            ),
            supporting = stringResource(Res.string.monitor_summary_updated_supporting),
        )

        MonitorSignalState.Error -> MonitorSummary(
            title = pluralStringResource(
                Res.plurals.monitor_summary_error,
                counts.errors,
                counts.errors,
            ),
            supporting = stringResource(Res.string.monitor_summary_error_supporting),
        )

        MonitorSignalState.Quiet -> MonitorSummary(
            title = stringResource(Res.string.monitor_summary_quiet),
            supporting = stringResource(Res.string.monitor_summary_quiet_supporting, counts.all),
        )
    }
    val contentColor = when (signalState) {
        MonitorSignalState.Changed -> MaterialTheme.colorScheme.primary
        MonitorSignalState.Error -> MaterialTheme.colorScheme.error
        MonitorSignalState.Listening -> MaterialTheme.colorScheme.secondary
        MonitorSignalState.Quiet -> MaterialTheme.colorScheme.tertiary
    }
    val refreshDescription = stringResource(Res.string.refresh_all)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.monitor_signal_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                    )
                    Text(
                        text = summary.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = summary.supporting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Crossfade(
                    targetState = signalState,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    label = "monitorSignal",
                ) { state ->
                    if (state == MonitorSignalState.Listening) {
                        LoadingIndicator(
                            modifier = Modifier
                                .size(52.dp)
                                .semantics { contentDescription = refreshDescription },
                            color = contentColor,
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = MaterialTheme.shapes.large,
                            color = contentColor.copy(alpha = 0.14f),
                            contentColor = contentColor,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (state) {
                                        MonitorSignalState.Changed -> Icons.Rounded.History
                                        MonitorSignalState.Error -> Icons.Rounded.Error
                                        MonitorSignalState.Quiet -> Icons.Rounded.CheckCircle
                                        MonitorSignalState.Listening -> Icons.Rounded.History
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }
                }
            }
            if (counts.changed > 0 && !isRefreshing) {
                Button(
                    onClick = onReviewChanges,
                    modifier = Modifier.fillMaxWidth(),
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.monitor_review_changes), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun MonitorFilters(
    selected: MonitorFilter,
    counts: MonitorCounts,
    onSelected: (MonitorFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        MonitorFilterOption(MonitorFilter.All, stringResource(Res.string.show_all), counts.all),
        MonitorFilterOption(MonitorFilter.Changed, stringResource(Res.string.monitor_filter_new), counts.changed),
        MonitorFilterOption(MonitorFilter.Errors, stringResource(Res.string.monitor_filter_errors), counts.errors),
        MonitorFilterOption(MonitorFilter.Paused, stringResource(Res.string.monitor_filter_paused), counts.paused),
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selected == option.filter
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { checked -> if (checked) onSelected(option.filter) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MaterialTheme.spacing.touchTarget)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "${option.label}, ${option.count}"
                    },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        option.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                        },
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorSearchEmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }
        Text(
            stringResource(Res.string.home_search_empty_title),
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            stringResource(Res.string.home_search_empty_subtitle),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonitorLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(66.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Box(contentAlignment = Alignment.CenterStart) {
                    LoadingIndicator(modifier = Modifier.padding(start = 18.dp).size(24.dp))
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(68.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {}
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(4) {
                    Surface(
                        modifier = Modifier.widthIn(min = 78.dp).height(44.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {}
                }
            }
            repeat(5) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {}
            }
        }
    }
}

private enum class MonitorFilter { All, Changed, Errors, Paused }

private data class MonitorCounts(val all: Int, val changed: Int, val errors: Int, val paused: Int)

private data class MonitorFilterOption(
    val filter: MonitorFilter,
    val label: String,
    val count: Int,
)

private data class MonitorSummary(
    val title: String,
    val supporting: String,
)

private enum class MonitorSignalState { Listening, Changed, Error, Quiet }

private fun siteRowPosition(index: Int, lastIndex: Int): SiteRowPosition = when {
    lastIndex <= 0 -> SiteRowPosition.Single
    index == 0 -> SiteRowPosition.First
    index == lastIndex -> SiteRowPosition.Last
    else -> SiteRowPosition.Middle
}
