@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.bernaferrari.changedetection.ui.home

import com.bernaferrari.changedetection.ui.kit.avatarIcon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.outlined.ScheduleSend
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.repo.CaptureMode
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.ErrorBanner
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.kit.Callout
import com.bernaferrari.changedetection.ui.kit.ChangePreviewBlock
import com.bernaferrari.changedetection.ui.kit.highlightMatches
import com.bernaferrari.changedetection.ui.kit.matchesQuery
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.ui.kit.NewBadge
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.segmentShape
import com.bernaferrari.changedetection.ui.kit.tileColor
import com.bernaferrari.changedetection.ui.kit.SectionHeader
import com.bernaferrari.changedetection.ui.kit.domain
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.rememberLatestPreview
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.UiMessage
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.ui.turbo.TurboLiveCard
import com.bernaferrari.changedetection.util.BackHandler
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.localization.pluralStringResource
import com.bernaferrari.changedetection.localization.stringResource

/** What changed comes first, then every page in a stable, quiet list. */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onAddPage: (url: String?) -> Unit,
    onOpenPage: (siteId: String) -> Unit,
    onOpenChange: (siteId: String, snapId: String?) -> Unit,
    onEditPage: (siteId: String) -> Unit,
    onSetUpTurbo: (siteIds: Set<String>) -> Unit,
    onOpenTurbo: () -> Unit,
    onHowItWorks: () -> Unit,
    onEnableNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    selectedSiteId: String? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    topBarState: androidx.compose.material3.TopAppBarState = androidx.compose.material3.rememberTopAppBarState(),
) {
    // Home draws its own header; on iOS the native bar is hidden here, while push, pop and swipe-back stay native.
    com.bernaferrari.changedetection.ui.navigation.HideNativeBar()
    val sites by viewModel.sitesFlow.collectAsStateWithLifecycle()
    val schedule by viewModel.monitoringSchedule.collectAsStateWithLifecycle()
    val turbo by viewModel.quickMonitorState.collectAsStateWithLifecycle()
    val loading by viewModel.loadingSites.collectAsStateWithLifecycle()
    val error by viewModel.currentError.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selection by rememberSaveable(stateSaver = HomeSelection.Saver) { mutableStateOf(HomeSelection()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var pullRequested by remember { mutableStateOf(false) }
    val selecting = selection.active

    BackHandler(enabled = selecting || searching) {
        if (selecting) {
            selection = selection.clear()
        } else {
            searching = false
            query = ""
        }
    }
    LaunchedEffect(loading.isEmpty()) { if (loading.isEmpty()) pullRequested = false }
    LaunchedEffect(sites) {
        if (sites == null) return@LaunchedEffect
        val ids = sites.orEmpty().mapTo(mutableSetOf()) { it.site.id }
        selection = if (ids.isEmpty()) selection.clear() else selection.retain(ids)
    }

    val prunedText = stringResource(Res.string.pruned_snapshots)
    val turboEndedText = stringResource(Res.string.turbo_ended)
    val nowWatching = stringResource(Res.string.home_now_watching)
    val openLabel = stringResource(Res.string.detail_open_short)
    LaunchedEffect(prunedText, turboEndedText, nowWatching, openLabel) {
        viewModel.uiMessage.collect { message ->
            when (message) {
                UiMessage.SnapshotsPruned -> snackbar.showSnackbar(prunedText)

                UiMessage.TurboEnded -> snackbar.showSnackbar(turboEndedText)

                is UiMessage.PageAdded -> launch {
                    val open = snackbar.showSnackbar(nowWatching.replace("%1\$s", message.name), actionLabel = openLabel, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
                    if (open) onOpenPage(message.siteId)
                }
            }
        }
    }

    val undoText = stringResource(Res.string.undo)
    val addPageLabel = stringResource(Res.string.add_site)
    val deletedOne = stringResource(Res.string.home_deleted_one)
    val deletedMany = stringResource(Res.string.home_deleted_many)
    fun deleteSelected() {
        val rows = sites.orEmpty().filter { it.site.id in selection.ids }
        selection = selection.clear()
        if (rows.isEmpty()) return
        val tickets = rows.map { viewModel.onDeleteSite(it.site) }
        scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            val message = if (rows.size == 1) deletedOne.replace("%1\$s", rows.single().site.name()) else deletedMany.replace("%1\$d", com.bernaferrari.changedetection.util.localizedInteger(rows.size))
            viewModel.resolveDeletions(tickets) { snackbar.showSnackbar(message, actionLabel = undoText, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed }
        }
    }

    val all = sites
    val filtered = remember(all, query, searching) {
        if (!searching || query.isBlank()) {
            all.orEmpty()
        } else {
            all.orEmpty().filter {
                it.site.name().contains(query, ignoreCase = true) || it.site.url.contains(query, ignoreCase = true)
            }
        }
    }
    val unread = filtered.filter { !it.site.isRead && it.snap != null && it.snapshotCount > 1 }.sortedByDescending { it.snap?.timestamp ?: 0 }
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = topBarState)

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            com.bernaferrari.changedetection.ui.kit.TopBarSwitcher(
                when {
                    selecting -> TopMode.Select
                    searching -> TopMode.Search
                    else -> TopMode.Normal
                },
            ) { mode ->
                when (mode) {
                    TopMode.Select -> SelectionBar(
                        count = selection.ids.size,
                        rows = all.orEmpty().filter { it.site.id in selection.ids },
                        turboAvailable = monitoringCapabilities.turbo,
                        onClose = { selection = selection.clear() },
                        onCheck = {
                            all.orEmpty().filter { it.site.id in selection.ids }.forEach { viewModel.reloadSite(it.site) }
                            selection = selection.clear()
                        },
                        onTurbo = {
                            val ids = selection.ids
                            if (ids.isEmpty()) return@SelectionBar
                            selection = selection.clear()
                            onSetUpTurbo(ids)
                        },
                        onDelete = ::deleteSelected,
                        onSetPaused = { paused ->
                            all.orEmpty().filter { it.site.id in selection.ids }.forEach { viewModel.setMonitoringEnabled(it.site.id, !paused) }
                            selection = selection.clear()
                        },
                        onSetNotify = { on ->
                            if (on) onEnableNotifications()
                            all.orEmpty().filter { it.site.id in selection.ids }.forEach { viewModel.setNotificationsEnabled(it.site.id, on) }
                            selection = selection.clear()
                        },
                        onEdit = {
                            val id = selection.ids.singleOrNull() ?: return@SelectionBar
                            selection = selection.clear()
                            onEditPage(id)
                        },
                    )

                    TopMode.Search -> com.bernaferrari.changedetection.ui.kit.SearchTopBar(query, { query = it }, stringResource(Res.string.home_search_hint)) {
                        searching = false
                        query = ""
                    }

                    TopMode.Normal -> HomeTopBar(
                        rows = all,
                        unreadCount = all.orEmpty().count { !it.site.isRead && it.snap != null && it.snapshotCount > 1 },
                        checking = loading.size,
                        turboAvailable = monitoringCapabilities.turbo && !all.isNullOrEmpty(),
                        onTurbo = { if (turbo.isActive) onOpenTurbo() else onSetUpTurbo(emptySet()) },
                        onSearch = { searching = true },
                        onCheckAll = viewModel::refreshAllSites,
                        onSelectPages = { selection = selection.enter() },
                        onHowItWorks = onHowItWorks,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !all.isNullOrEmpty() && !selecting && !searching,
                enter = androidx.compose.animation.scaleIn(initialScale = 0.6f) + fadeIn(),
                exit = androidx.compose.animation.scaleOut(targetScale = 0.6f) + fadeOut(),
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onAddPage(null) },
                    expanded = fabExpanded,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text(stringResource(Res.string.home_watch_page)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.semantics { contentDescription = addPageLabel },
                )
            }
        },
        snackbarHost = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ErrorBanner(error = error, onDismiss = viewModel::dismissError, onRetry = viewModel::retryLastAction)
                SnackbarHost(snackbar)
            }
        },
    ) { padding ->
        when {
            all == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { com.bernaferrari.changedetection.ui.components.DelayedLoadingIndicator() }

            all.isEmpty() -> HomeEmptyState(onAddPage = onAddPage, onHowItWorks = onHowItWorks, modifier = Modifier.padding(padding))

            else -> PullToCheckBox(
                scrollBehavior = scrollBehavior,
                refreshing = pullRequested && loading.isNotEmpty(),
                enabled = !selecting && !searching,
                onRefresh = {
                    pullRequested = true
                    viewModel.refreshAllSites()
                },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 720.dp).fillMaxSize().testTag("home-list"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (turbo.isActive && !searching) {
                        item(key = "turbo") {
                            TurboLiveCard(turbo, onClick = onOpenTurbo, modifier = Modifier.animateItem().padding(bottom = 8.dp))
                        }
                    }
                    if (!schedule.enabled && !searching) {
                        item(key = "schedule-off") {
                            Callout(
                                title = stringResource(Res.string.home_schedule_off_title),
                                body = stringResource(Res.string.home_schedule_off_body),
                                icon = Icons.Outlined.ScheduleSend,
                                modifier = Modifier.animateItem().padding(bottom = 8.dp),
                                actions = { FilledTonalButton(onClick = viewModel::resumeScheduledChecks) { Text(stringResource(Res.string.home_schedule_turn_on)) } },
                            )
                        }
                    }
                    items(unread, key = { "card-" + it.site.id }) { row ->
                        ChangeCard(
                            row = row,
                            selected = row.site.id in selection.ids,
                            selecting = selecting,
                            onOpen = { if (selecting) selection = selection.toggle(row.site.id) else onOpenChange(row.site.id, row.snap?.snapId) },
                            onLongPress = { selection = selection.toggle(row.site.id) },
                            onMarkRead = { viewModel.markRead(row) },
                            query = if (searching) query else "",
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    item(key = "pages-header") {
                        SectionHeader(
                            title = stringResource(Res.string.home_pages),
                            count = filtered.size,
                            modifier = Modifier.animateItem().padding(top = if (unread.isEmpty()) 0.dp else 10.dp),
                        )
                    }
                    if (filtered.isEmpty()) {
                        item(key = "no-match") {
                            Text(
                                stringResource(Res.string.home_search_none, query),
                                Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    itemsIndexed(filtered, key = { _, it -> "row-" + it.site.id }) { index, row ->
                        PageRow(
                            row = row,
                            shape = segmentShape(index, filtered.size),
                            checking = row.site.id in loading || row.isSyncing,
                            selected = row.site.id in selection.ids,
                            current = row.site.id == selectedSiteId,
                            selecting = selecting,
                            onClick = { if (selecting) selection = selection.toggle(row.site.id) else onOpenPage(row.site.id) },
                            onLongPress = { selection = selection.toggle(row.site.id) },
                            query = if (searching) query else "",
                            modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

private enum class TopMode { Normal, Search, Select }

/**
 * Pull down to check every page, with an explicit release cue at the actual trigger threshold.
 * Disabled while searching or selecting.
 */
@Composable
internal fun PullToCheckBox(
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    val threshold = androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.PositionalThreshold
    val ready by remember { derivedStateOf { state.distanceFraction >= 1f } }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(ready) {
        if (ready && enabled && !refreshing && !state.isAnimating) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
    }
    // Expand the title before pull-to-refresh consumes downward scroll at the top of the list.
    Box(modifier.pullToRefresh(isRefreshing = refreshing, state = state, enabled = enabled, threshold = threshold, onRefresh = onRefresh).nestedScroll(scrollBehavior.nestedScrollConnection)) {
        content()
        if (enabled || refreshing) {
            val feedback = stringResource(
                when {
                    refreshing -> Res.string.pull_check_checking
                    ready -> Res.string.pull_check_release
                    else -> Res.string.pull_check_pull
                },
            )
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = refreshing,
                maxDistance = threshold,
                modifier = Modifier.align(Alignment.TopCenter).semantics {
                    stateDescription = feedback
                    liveRegion = LiveRegionMode.Polite
                }.testTag("pull-check-indicator"),
            )
        }
    }
}

/** Empty selection is a real mode: the user chooses the first page explicitly. */
internal data class HomeSelection(val active: Boolean = false, val ids: Set<String> = emptySet()) {
    fun enter() = copy(active = true)
    fun toggle(id: String) = copy(active = true, ids = if (id in ids) ids - id else ids + id)
    fun retain(available: Set<String>) = copy(ids = ids.intersect(available))
    fun clear() = HomeSelection()

    companion object {
        val Saver = listSaver<HomeSelection, String>(
            save = { listOf(if (it.active) "active" else "closed") + it.ids.sorted() },
            restore = { HomeSelection(active = it.first() == "active", ids = it.drop(1).toSet()) },
        )
    }
}

@Composable
private fun HomeTopBar(
    rows: List<SiteAndLastSnap>?,
    unreadCount: Int,
    checking: Int,
    turboAvailable: Boolean,
    onTurbo: () -> Unit,
    onSearch: () -> Unit,
    onCheckAll: () -> Unit,
    onSelectPages: () -> Unit,
    onHowItWorks: () -> Unit,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
) {
    var menu by remember { mutableStateOf(false) }
    val pages = rows.orEmpty()
    val density = LocalDensity.current
    val largeText = density.fontScale > 1.3f
    val expandedHeight = if (largeText) {
        TopAppBarDefaults.LargeAppBarCollapsedHeight + with(density) {
            MaterialTheme.typography.headlineLarge.lineHeight.toDp() * 2 + MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2
        } + 32.dp
    } else {
        TopAppBarDefaults.LargeFlexibleAppBarWithSubtitleExpandedHeight
    }
    val checkAllLabel = stringResource(Res.string.refresh_all)
    val failing = pages.count { it.site.isSyncEnabled && !it.site.isSuccessful }
    val lastCheck = pages.maxOfOrNull { it.site.timestamp }?.takeIf { it > 0 }
    if (rows != null && pages.isEmpty()) {
        TopAppBar(
            title = {},
            actions = {
                TextButton(onClick = onHowItWorks) { Text(stringResource(Res.string.how_it_works)) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        return
    }
    LargeFlexibleTopAppBar(
        title = {
            // The bar composes this slot for both its expanded and collapsed titles; each keeps its own layout.
            var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
            Text(
                when {
                    // Blank until the pages load, so the hero never reads "Change Detection" and then jumps to a count.
                    rows == null -> AnnotatedString("")

                    pages.isEmpty() -> AnnotatedString(stringResource(Res.string.app_name))

                    unreadCount > 0 -> highlightedCount(pluralStringResource(Res.plurals.home_new_changes, unreadCount, unreadCount), unreadCount)

                    else -> AnnotatedString(stringResource(Res.string.home_nothing_new))
                },
                // The slot is called with separate expanded/collapsed text styles.
                // Keep collapsed chrome compact while making the hero readable.
                maxLines = if (largeText && LocalTextStyle.current.fontSize > MaterialTheme.typography.titleLarge.fontSize) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (unreadCount > 0) Modifier.highlighterStroke(layout, com.bernaferrari.changedetection.util.localizedInteger(unreadCount)) else Modifier,
                onTextLayout = { layout = it },
            )
        },
        subtitle = if (pages.isEmpty()) {
            null
        } else {
            {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (color, text) = when {
                        checking > 0 -> MaterialTheme.colorScheme.primary to pluralStringResource(Res.plurals.home_checking, checking, checking)

                        failing > 0 -> MaterialTheme.colorScheme.error to pluralStringResource(Res.plurals.home_failing, failing, failing)

                        // The page count is already in the Pages section; this line answers "is it working?".
                        lastCheck != null -> MaterialTheme.marks.healthy to stringResource(Res.string.home_status_checked, getRelativeTimeLabel(lastCheck).replaceFirstChar { it.lowercase() })

                        else -> MaterialTheme.colorScheme.outline to stringResource(Res.string.home_status_first_check)
                    }
                    if (checking > 0) {
                        LoadingIndicator(Modifier.size(18.dp))
                    } else {
                        Box(Modifier.size(8.dp).background(color, CircleShape))
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (failing > 0 && checking == 0) color else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            if (turboAvailable) {
                IconButton(onClick = onTurbo) { Icon(Icons.Rounded.Bolt, stringResource(Res.string.turbo_mode)) }
            }
            if (pages.isNotEmpty()) {
                IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(Res.string.home_search)) }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (pages.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.home_select_pages)) },
                            leadingIcon = { Icon(Icons.Outlined.CheckBox, null) },
                            onClick = {
                                menu = false
                                onSelectPages()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.home_check_all)) },
                            leadingIcon = { Icon(Icons.Outlined.Refresh, null) },
                            onClick = {
                                menu = false
                                onCheckAll()
                            },
                            enabled = checking == 0,
                            modifier = Modifier.semantics { contentDescription = checkAllLabel },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.how_it_works)) },
                        onClick = {
                            menu = false
                            onHowItWorks()
                        },
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
        expandedHeight = expandedHeight,
        scrollBehavior = scrollBehavior,
    )
}

@Composable
internal fun SelectionBar(
    count: Int,
    rows: List<SiteAndLastSnap>,
    turboAvailable: Boolean,
    onClose: () -> Unit,
    onCheck: () -> Unit,
    onTurbo: () -> Unit,
    onDelete: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onSetNotify: (Boolean) -> Unit,
    onEdit: () -> Unit,
    notificationsAvailable: Boolean = monitoringCapabilities.notifications,
) {
    var menu by remember { mutableStateOf(false) }
    val selectionLabel = pluralStringResource(Res.plurals.turbo_mode_selected_sites, count, count)
    val allPaused = rows.isNotEmpty() && rows.all { !it.site.isSyncEnabled }
    val allNotify = rows.isNotEmpty() && rows.all { it.site.isNotificationEnabled }
    val hasSelection = count > 0
    TopAppBar(
        title = {
            Text(
                com.bernaferrari.changedetection.util.localizedInteger(count),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { contentDescription = selectionLabel },
            )
        },
        navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, stringResource(Res.string.home_clear_selection)) } },
        actions = {
            IconButton(onClick = onCheck, enabled = hasSelection) { Icon(Icons.Outlined.Refresh, stringResource(Res.string.home_check_now)) }
            if (turboAvailable && !allPaused) {
                FilledIconButton(
                    onClick = onTurbo,
                    enabled = hasSelection,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                ) { Icon(Icons.Rounded.Bolt, stringResource(Res.string.turbo_mode)) }
            }
            IconButton(onClick = onDelete, enabled = hasSelection) { Icon(Icons.Outlined.DeleteOutline, stringResource(Res.string.delete)) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (count == 1) {
                        DropdownMenuItem(text = { Text(stringResource(Res.string.edit)) }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = {
                            menu = false
                            onEdit()
                        })
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(if (allPaused) Res.string.home_resume_checks else Res.string.home_pause_checks)) },
                        leadingIcon = { Icon(if (allPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null) },
                        enabled = hasSelection,
                        onClick = {
                            menu = false
                            onSetPaused(!allPaused)
                        },
                    )
                    if (notificationsAvailable) {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (allNotify) Res.string.home_notify_off else Res.string.home_notify_on)) },
                            leadingIcon = { Icon(if (allNotify) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications, null) },
                            enabled = hasSelection,
                            onClick = {
                                menu = false
                                onSetNotify(!allNotify)
                            },
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    )
}

/** A change waiting to be read: who changed, when, and a bounded look at what. */
@Composable
private fun ChangeCard(
    row: SiteAndLastSnap,
    selected: Boolean,
    selecting: Boolean,
    onOpen: () -> Unit,
    onLongPress: () -> Unit,
    onMarkRead: () -> Unit,
    /** The search being typed; matches are highlighted in the name and address. */
    query: String = "",
    modifier: Modifier = Modifier,
) {
    val preview = rememberLatestPreview(row.site.id, row.snap?.snapId)
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    val selectLabel = stringResource(Res.string.home_select_page)
    val container by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else tileColor,
        label = "card",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = container,
    ) {
        Column(
            Modifier
                .combinedClickable(onClick = onOpen, onLongClick = {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onLongPress()
                }, onLongClickLabel = selectLabel, role = Role.Button)
                .semantics { if (selecting) this.selected = selected }
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selecting) SelectMark(selected) else PageAvatar(row.site.name(), row.site.id, size = 44.dp, icon = row.site.avatarIcon(), iconShape = row.site.iconShape, colors = row.site.colors)
                Column(Modifier.weight(1f)) {
                    Text(highlightMatches(row.site.name(), query), style = MaterialTheme.typography.titleMedium, maxLines = if (largeText) 3 else 1, overflow = TextOverflow.Ellipsis)
                    // The address shown is the full one while searching, so a match anywhere in it is visible.
                    val address = if (query.isNotBlank() && !matchesQuery(row.site.name(), query)) row.site.url.displayPageAddress() else row.site.domain()
                    Text(
                        highlightMatches(if (largeText) address else address + " · " + getRelativeTimeLabel(row.snap?.timestamp ?: row.site.timestamp), query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (largeText) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (largeText) {
                        Text(getRelativeTimeLabel(row.snap?.timestamp ?: row.site.timestamp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!selecting) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm)
                            onMarkRead()
                        },
                        shapes = IconButtonDefaults.shapes(),
                    ) { Icon(Icons.Rounded.Check, stringResource(Res.string.mark_read_short)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            ChangePreviewBlock(preview)
        }
    }
}

internal fun formatPercent(value: Double): String = when {
    value < 0.1 -> "<" + com.bernaferrari.changedetection.util.localizedPercent(0.1)
    value < 10 -> com.bernaferrari.changedetection.util.localizedPercent((value * 10).toInt() / 10.0, minFractionDigits = 1)
    else -> com.bernaferrari.changedetection.util.localizedPercent(value.toInt().toDouble(), maxFractionDigits = 0)
}

@Composable
private fun SelectMark(selected: Boolean) {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary),
        )
    }
}

/** One page in the stable list: identity on the left, its single most important fact underneath. */
@Composable
private fun PageRow(
    row: SiteAndLastSnap,
    shape: androidx.compose.ui.graphics.Shape,
    checking: Boolean,
    selected: Boolean,
    current: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    /** The search being typed; matches are highlighted, and an address-only match shows the address. */
    query: String = "",
    modifier: Modifier = Modifier,
) {
    val site = row.site
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val largeText = LocalDensity.current.fontScale > 1.3f
    val selectLabel = stringResource(Res.string.home_select_page)
    val paused = !site.isSyncEnabled
    // While a new check runs, the last failure is old news: show progress, not an error.
    val failing = !paused && !site.isSuccessful && !checking
    val background by androidx.compose.animation.animateColorAsState(
        when {
            selected -> MaterialTheme.colorScheme.secondaryContainer
            current -> MaterialTheme.colorScheme.surfaceContainerHighest
            else -> tileColor
        },
        label = "row",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onLongPress()
            }, onLongClickLabel = selectLabel, role = Role.Button)
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { if (selecting) this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (selecting) SelectMark(selected) else PageAvatar(site.name(), site.id, dimmed = paused, icon = site.avatarIcon(), iconShape = site.iconShape, colors = site.colors)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                highlightMatches(site.name(), query),
                style = MaterialTheme.typography.titleMedium,
                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = if (largeText) 3 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Found by its address: show the address with the match, so the result explains itself.
            if (query.isNotBlank() && !matchesQuery(site.name(), query) && matchesQuery(site.url.displayPageAddress(), query)) {
                Text(
                    highlightMatches(site.url.displayPageAddress(), query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                val statusColor = if (failing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                when {
                    paused -> Icon(Icons.Outlined.Pause, null, Modifier.size(16.dp), tint = statusColor)
                    failing -> Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(16.dp), tint = statusColor)
                    else -> Unit
                }
                Text(
                    when {
                        checking -> stringResource(Res.string.home_row_checking)
                        paused -> stringResource(Res.string.home_row_paused)
                        failing -> checkFailureTitle(site.lastFailureCode)
                        row.snap == null -> stringResource(Res.string.home_row_waiting)
                        row.snapshotCount <= 1 -> stringResource(Res.string.home_row_no_changes)
                        else -> stringResource(Res.string.home_row_changed, getRelativeTimeLabel(row.snap.timestamp).replaceFirstChar { it.lowercase() })
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    maxLines = if (largeText) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        AnimatedVisibility(checking, enter = fadeIn(), exit = fadeOut()) { LoadingIndicator(Modifier.size(28.dp)) }
        if (!site.isRead && row.snapshotCount > 1 && !checking) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
        } else if (site.captureMode == CaptureMode.SCREENSHOT && !checking && row.snap != null) {
            CaptureThumb(row.snap.snapId, dimmed = paused)
        }
    }
}

/** The top of the latest capture, so a screenshot page is recognisable at a glance. */
@Composable
private fun CaptureThumb(snapId: String, dimmed: Boolean) {
    val shape = MaterialTheme.shapes.small
    com.bernaferrari.changedetection.ui.kit.CaptureTop(
        snapId,
        Modifier.size(width = 40.dp, height = 52.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        alpha = if (dimmed) 0.5f else 1f,
        decodeWidth = 160,
    )
}

/** The count wears the highlighter, the same mark changed text wears in the reader, a little wider than the digits. */
@Composable
private fun Modifier.highlighterStroke(layout: androidx.compose.ui.text.TextLayoutResult?, number: String): Modifier {
    val color = MaterialTheme.marks.highlight
    return drawBehind {
        val result = layout ?: return@drawBehind
        val start = result.layoutInput.text.text.indexOf(number)
        if (start < 0) return@drawBehind
        val first = result.getBoundingBox(start)
        val last = result.getBoundingBox(start + number.length - 1)
        val left = minOf(first.left, last.left) - 4.dp.toPx()
        val right = maxOf(first.right, last.right) + 4.dp.toPx()
        // Over the whole digit, like highlighted text in a change, so its ink always sits on the marker.
        val line = result.getLineForOffset(start)
        val baseline = result.getLineBaseline(line)
        val ascent = baseline - result.getLineTop(line)
        val top = baseline - ascent * 0.8f
        val bottom = baseline + ascent * 0.1f
        drawRoundRect(color, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(right - left, bottom - top), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
    }
}

/** The digits over the marker take the ink that reads on it, in light and dark. */
@Composable
private fun highlightedCount(text: String, count: Int): AnnotatedString {
    val number = com.bernaferrari.changedetection.util.localizedInteger(count)
    val start = text.indexOf(number)
    val ink = MaterialTheme.marks.onHighlight
    return androidx.compose.ui.text.buildAnnotatedString {
        append(text)
        if (start >= 0) addStyle(androidx.compose.ui.text.SpanStyle(color = ink), start, start + number.length)
    }
}
