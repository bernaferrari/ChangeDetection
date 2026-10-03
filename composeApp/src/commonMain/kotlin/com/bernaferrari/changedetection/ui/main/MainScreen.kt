/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.SyncState
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
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
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_changes_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_changes_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_errors_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_errors_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_paused_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.home_filter_paused_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.home_search_monitors
import com.bernaferrari.changedetection.shared.generated.resources.learn_how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_paused
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_errors
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_new
import com.bernaferrari.changedetection.shared.generated.resources.monitor_review_changes
import com.bernaferrari.changedetection.shared.generated.resources.monitor_signal_changed_unit
import com.bernaferrari.changedetection.shared.generated.resources.monitor_signal_error_unit
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
import com.bernaferrari.changedetection.shared.generated.resources.turbo_ended
import com.bernaferrari.changedetection.shared.generated.resources.undo
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.ListSkeleton
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
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
    onEnableNotifications: () -> Unit = {},
    onNotificationSettings: () -> Unit = {},
    onOpenTurbo: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val sites by viewModel.sitesFlow.collectAsStateWithLifecycle()
    val schedule by viewModel.monitoringSchedule.collectAsStateWithLifecycle()
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
    val turboEndedString = stringResource(Res.string.turbo_ended)
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
            when (message) {
                UiMessage.SnapshotsPruned -> snackbarHostState.showSnackbar(
                    snapshotsPrunedString,
                    duration = SnackbarDuration.Short,
                )

                UiMessage.TurboEnded -> snackbarHostState.showSnackbar(
                    turboEndedString,
                    duration = SnackbarDuration.Short,
                )
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
            if (!sites.isNullOrEmpty() && !isSearchActive) AddPageFab(onAddSite)
        },
        snackbarHost = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorBanner(
                    error = currentError,
                    onDismiss = viewModel::dismissError,
                    onRetry = viewModel::retryLastAction,
                )
                SnackbarHost(snackbarHostState)
            }
        },
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
                visual = {
                    ChangeRibbon(
                        colors = emptyList(),
                        isListening = false,
                        modifier = Modifier.widthIn(max = 300.dp),
                    )
                },
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
                        scheduledEnabled = schedule.enabled,
                        scheduleIntervalMinutes = schedule.intervalMinutes,
                        scheduledJob = syncStatus.firstOrNull { it.isPeriodic },
                        onResumeSchedule = viewModel::resumeScheduledChecks,
                        sites = sites.orEmpty(),
                        searchQuery = if (isSearchActive) searchQuery else "",
                        selectedSiteId = selectedSiteId,
                        quickMonitorState = quickMonitorState,
                        onOpenTurbo = onOpenTurbo,
                        loadingSites = loadingSites,
                        failedSiteIds = failedSiteIds,
                        isRefreshing = isRefreshing && !pullRefreshRequested,
                        onSiteClick = { onSiteClick(it.site) },
                        onSiteOptions = { showOptionsFor = it },
                    )
                }
            }
        }
    }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    showOptionsFor?.let { selected ->
        SiteOptionsBottomSheet(
            notificationDeliveryAvailable = org.koin.compose.koinInject<com.bernaferrari.changedetection.sync.ChangeNotifier>().supportsDelivery,
            site = selected.site,
            onOpenInBrowser = {
                runCatching { uriHandler.openUri(selected.site.url) }
                showOptionsFor = null
            },
            onToggleMonitoring = {
                viewModel.updateSite(selected.site.copy(isSyncEnabled = !selected.site.isSyncEnabled))
                showOptionsFor = null
            },
            onNotificationSettings = onNotificationSettings,
            onToggleNotifications = {
                if (!selected.site.isNotificationEnabled) onEnableNotifications()
                viewModel.updateSite(selected.site.copy(isNotificationEnabled = !selected.site.isNotificationEnabled))
                showOptionsFor = null
            },
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
                val deletion = viewModel.onDeleteSite(selected.site)
                scope.launch {
                    viewModel.resolveDeletion(deletion) {
                        snackbarHostState.showSnackbar(
                            message = siteDeletedString,
                            actionLabel = undoString,
                            duration = SnackbarDuration.Short,
                        ) == SnackbarResult.ActionPerformed
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
internal fun MonitorTopBar(
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
    val searchButtonFocusRequester = remember { FocusRequester() }
    var wasSearchActive by remember { mutableStateOf(false) }
    LaunchedEffect(searchActive, siteCount) {
        if (wasSearchActive && !searchActive && siteCount > 0) searchButtonFocusRequester.requestFocus()
        wasSearchActive = searchActive
    }
    TopAppBar(
        expandedHeight = 96.dp,
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
                    style = MaterialTheme.typography.headlineLarge,
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
                IconButton(onClick = onSearchOpen, modifier = Modifier.focusRequester(searchButtonFocusRequester)) {
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
internal fun MonitorWatchlist(
    scheduledEnabled: Boolean,
    scheduleIntervalMinutes: Long,
    scheduledJob: com.bernaferrari.changedetection.background.SyncJobInfo?,
    onResumeSchedule: () -> Unit,
    sites: List<SiteAndLastSnap>,
    searchQuery: String,
    selectedSiteId: String?,
    quickMonitorState: QuickMonitorState,
    loadingSites: Set<String>,
    failedSiteIds: Set<String>,
    isRefreshing: Boolean,
    onSiteClick: (SiteAndLastSnap) -> Unit,
    onSiteOptions: (SiteAndLastSnap) -> Unit,
    onOpenTurbo: () -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(MonitorFilter.All) }
    val projection = remember(sites, searchQuery, filter, failedSiteIds) {
        projectWatchlist(sites, searchQuery, filter, failedSiteIds)
    }
    val counts = projection.totalCounts
    val visibleSites = projection.visibleSites

    // Keep the list pane anchored from its first frame. On expanded layouts Nav3 reserves the
    // detail pane immediately, so selecting a monitor never makes the watchlist jump sideways.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (quickMonitorState.isActive) {
                item(key = "active-turbo") {
                    com.bernaferrari.changedetection.ui.main.components.ActiveTurboCard(quickMonitorState, sites, onOpenTurbo, Modifier.padding(bottom = 12.dp))
                }
            }
            if (searchQuery.isBlank() && (isRefreshing || counts.errors > 0 || counts.waiting > 0 || !scheduledEnabled)) {
                item(key = "summary") {
                    WatchlistSummary(
                        scheduledEnabled = scheduledEnabled,
                        counts = counts,
                        isRefreshing = isRefreshing,
                        onReviewChanges = { filter = MonitorFilter.Changed },
                        onReviewErrors = { filter = MonitorFilter.Errors },
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                }
            }
            if (!scheduledEnabled) {
                item(key = "resume-schedule") {
                    TextButton(onClick = onResumeSchedule) { Text(stringResource(Res.string.resume_monitoring)) }
                }
            }
            item(key = "filters") {
                MonitorFilters(
                    selected = filter,
                    counts = projection.matchingCounts,
                    onSelected = { filter = it },
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
            if (visibleSites.isEmpty()) {
                item(key = "empty") {
                    val emptyCopy = when {
                        searchQuery.isNotBlank() && projection.matchingCounts.all == 0 -> stringResource(Res.string.home_search_empty_title) to
                            stringResource(Res.string.home_search_empty_subtitle)

                        filter == MonitorFilter.Changed -> stringResource(Res.string.home_filter_changes_empty_title) to
                            stringResource(Res.string.home_filter_changes_empty_subtitle)

                        filter == MonitorFilter.Errors -> stringResource(Res.string.home_filter_errors_empty_title) to
                            stringResource(Res.string.home_filter_errors_empty_subtitle)

                        filter == MonitorFilter.Paused -> stringResource(Res.string.home_filter_paused_empty_title) to
                            stringResource(Res.string.home_filter_paused_empty_subtitle)

                        else -> stringResource(Res.string.home_search_empty_title) to
                            stringResource(Res.string.home_search_empty_subtitle)
                    }
                    SharedEmptyState(
                        icon = if (searchQuery.isNotBlank() && projection.matchingCounts.all == 0) Icons.Rounded.SearchOff else Icons.Rounded.CheckCircle,
                        title = emptyCopy.first,
                        description = emptyCopy.second,
                    )
                }
            }
            itemsIndexed(visibleSites, key = { _, item -> item.site.id }) { index, item ->
                SiteRow(
                    position = com.bernaferrari.changedetection.ui.components.segmentPosition(index == 0, index == visibleSites.lastIndex),
                    site = item.site,
                    status = item.toSiteStatus(
                        isLoading = item.site.id in loadingSites,
                        hasError = item.site.id in failedSiteIds,
                    ),
                    isQuickMonitored = quickMonitorState.isActive && item.site.id in quickMonitorState.siteIds,
                    hasUnreadChange = item.hasDetectedChange(),
                    isSelected = selectedSiteId == item.site.id,
                    selectionSemanticsEnabled = selectedSiteId != null,
                    onClick = { onSiteClick(item) },
                    onLongClick = { onSiteOptions(item) },
                )
            }
            if (scheduledEnabled && counts.all > counts.paused) {
                item(key = "schedule-health") {
                    ScheduleHealth(scheduleIntervalMinutes, scheduledJob, Modifier.padding(top = 16.dp))
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
    val visibleOptions = options.filter { it.filter != MonitorFilter.Paused || it.count > 0 || selected == MonitorFilter.Paused }
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visibleOptions.forEach { option ->
            androidx.compose.material3.FilterChip(
                selected = selected == option.filter,
                onClick = { onSelected(option.filter) },
                modifier = Modifier.heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium,
                label = { Text("${option.label}  ${option.count}", style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
private fun MonitorLoading(modifier: Modifier = Modifier) {
    ListSkeleton(modifier = modifier, itemCount = 5, showHeaderChips = true)
}

private data class MonitorFilterOption(
    val filter: MonitorFilter,
    val label: String,
    val count: Int,
)

@Composable
internal fun AddPageFab(onClick: () -> Unit) {
    val label = stringResource(Res.string.add_site)
    androidx.compose.material3.ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = label },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(label) },
    )
}
