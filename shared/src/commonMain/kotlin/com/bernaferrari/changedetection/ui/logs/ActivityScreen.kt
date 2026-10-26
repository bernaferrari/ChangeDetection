@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.bernaferrari.changedetection.ui.logs

import com.bernaferrari.changedetection.util.displayPageAddress
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.avatarIcon
import com.bernaferrari.changedetection.ui.kit.SectionHeader
import com.bernaferrari.changedetection.ui.kit.friendlyDay
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.previewLabel
import com.bernaferrari.changedetection.ui.kit.placeholderPulse
import com.bernaferrari.changedetection.ui.kit.rememberVersionPreview
import com.bernaferrari.changedetection.util.BackHandler
import com.bernaferrari.changedetection.util.formatTime
import kotlinx.coroutines.launch
import com.bernaferrari.changedetection.localization.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Every saved change, newest first, one sentence each. */
@Composable
fun ActivityScreen(
    onOpenChange: (siteId: String, snapId: String) -> Unit,
    onOpenPage: (siteId: String) -> Unit,
    viewModel: LogsViewModel = koinViewModel(),
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    topBarState: androidx.compose.material3.TopAppBarState = androidx.compose.material3.rememberTopAppBarState(),
) {
    val items = viewModel.pagedSnaps.collectAsLazyPagingItems()
    val sites by viewModel.siteMap.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(query.isNotBlank()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var deleting by remember { mutableStateOf(emptySet<String>()) }
    val deletedText = stringResource(Res.string.snapshot_deleted)
    val deleteFailedText = stringResource(Res.string.snapshot_delete_failed)
    val restoreFailedText = stringResource(Res.string.snapshot_restore_failed)
    val retryText = stringResource(Res.string.retry)
    val undoText = stringResource(Res.string.undo)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(state = topBarState)
    // Nothing to search until there is activity.
    val canSearch = items.itemCount > 0 || query.isNotBlank()
    BackHandler(enabled = searching) {
        viewModel.setSearchQuery("")
        searching = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            com.bernaferrari.changedetection.ui.kit.TopBarSwitcher(searching) { searchMode ->
                if (searchMode) {
                    com.bernaferrari.changedetection.ui.navigation.HideNativeBar()
                    com.bernaferrari.changedetection.ui.kit.SearchTopBar(query, viewModel::setSearchQuery, stringResource(Res.string.logs_search_pages)) {
                        viewModel.setSearchQuery("")
                        searching = false
                    }
                } else {
                    if (com.bernaferrari.changedetection.ui.navigation.usesNativeNavigation()) {
                        com.bernaferrari.changedetection.ui.navigation.AppTopBar(
                            title = stringResource(Res.string.nav_activity),
                            onBack = null,
                            actions = if (canSearch) listOf(com.bernaferrari.changedetection.ui.navigation.BarAction(stringResource(Res.string.logs_search_pages), Icons.Outlined.Search, "magnifyingglass") { searching = true }) else emptyList(),
                        )
                    } else {
                        LargeFlexibleTopAppBar(
                            title = { Text(stringResource(Res.string.nav_activity)) },
                            actions = { if (canSearch) IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, stringResource(Res.string.logs_search_pages)) } },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        ActivityFeed(
            items = items,
            listState = listState,
            sites = sites,
            query = query,
            deleting = deleting,
            modifier = Modifier.padding(padding),
            onOpenChange = onOpenChange,
            onOpenPage = onOpenPage,
            onDelete = { snap ->
                if (snap.snapId in deleting) return@ActivityFeed
                deleting = deleting + snap.snapId
                scope.launch {
                    try {
                        if (viewModel.removeSnap(snap).await()) {
                            val undo = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
                            if (undo) {
                                while (!viewModel.undoRemoveSnap(snap).await()) {
                                    val retry = snackbar.showSnackbar(restoreFailedText, retryText, duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed
                                    if (!retry) break
                                }
                            }
                        } else {
                            snackbar.showSnackbar(deleteFailedText)
                        }
                    } finally {
                        viewModel.discardPendingDeletion(snap)
                        deleting = deleting - snap.snapId
                    }
                }
            },
        )
    }
}

@Composable
internal fun ActivityFeed(
    items: LazyPagingItems<LogUiModel>,
    sites: Map<String, Site>,
    query: String,
    onOpenChange: (String, String) -> Unit,
    onOpenPage: (String) -> Unit,
    onDelete: (Snap) -> Unit,
    modifier: Modifier = Modifier,
    deleting: Set<String> = emptySet(),
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
) {
    val failing = if (query.isBlank()) sites.values.filter { it.isSyncEnabled && !it.isSuccessful }.sortedBy { it.name() } else emptyList()
    if (failing.isEmpty() && items.itemCount == 0 && items.loadState.refresh is LoadState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            com.bernaferrari.changedetection.ui.components.DelayedLoadingIndicator()
        }
        return
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("activity-list"), state = listState, contentPadding = PaddingValues(bottom = 32.dp)) {
            if (failing.isNotEmpty()) {
                item(key = "failures-header") { SectionHeader(stringResource(Res.string.home_section_attention), count = failing.size) }
                itemsIndexed(failing, key = { _, it -> "failure-" + it.id }) { index, site ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 1.dp).clip(com.bernaferrari.changedetection.ui.kit.segmentShape(index, failing.size))
                            .background(com.bernaferrari.changedetection.ui.kit.tileColor)
                            .clickable(role = Role.Button, onClick = { onOpenPage(site.id) }).heightIn(min = 72.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(site.name(), style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(checkFailureTitle(site.lastFailureCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            when {
                items.loadState.refresh is LoadState.Error -> item(key = "refresh-error") { ActivityLoadError(items::retry) }

                // Pending history sits below the current failures; the empty state owns the viewport.
                items.itemCount == 0 && items.loadState.refresh is LoadState.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        com.bernaferrari.changedetection.ui.components.DelayedLoadingIndicator()
                    }
                }

                items.itemCount == 0 -> item(key = "empty") {
                    Box(if (failing.isEmpty()) Modifier.fillParentMaxSize() else Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyActivity(searching = query.isNotBlank())
                    }
                }
            }
            items(count = items.itemCount, key = { index ->
                when (val model = items.peek(index)) {
                    is LogUiModel.Item -> "s" + model.snap.snapId
                    is LogUiModel.Header -> "h" + model.date
                    null -> "p$index"
                }
            }) { index ->
                when (val model = items[index]) {
                    is LogUiModel.Header -> DayHeader(model.date, if (index + 1 < items.itemCount) items.peek(index + 1) else null)

                    is LogUiModel.Item -> ActivityRow(
                        shape = run {
                            val first = index == 0 || items.peek(index - 1) is LogUiModel.Header
                            val last = index + 1 >= items.itemCount || items.peek(index + 1) is LogUiModel.Header
                            val position = if (first) 0 else 1
                            com.bernaferrari.changedetection.ui.kit.segmentShape(position, if (last) position + 1 else position + 2)
                        },
                        snap = model.snap,
                        site = sites[model.snap.siteId],
                        onOpen = { onOpenChange(model.snap.siteId, model.snap.snapId) },
                        onDelete = { onDelete(model.snap) },
                        query = query,
                        canDelete = model.snap.snapId !in deleting,
                    )

                    null -> Box(Modifier.fillMaxWidth().heightIn(min = 72.dp))
                }
            }
            when (items.loadState.append) {
                is LoadState.Error -> item(key = "append-error") { ActivityLoadError(items::retry) }

                is LoadState.Loading -> item(key = "append-loading") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { LoadingIndicator(Modifier.size(32.dp)) }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun ActivityLoadError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.logs_error_loading), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
    }
}

@Composable
private fun DayHeader(date: String, next: LogUiModel?) {
    val timestamp = (next as? LogUiModel.Item)?.snap?.timestamp
    Text(
        if (timestamp != null) friendlyDay(timestamp) else date,
        Modifier.padding(start = 24.dp, end = 20.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
internal fun ActivityRow(shape: androidx.compose.ui.graphics.Shape, snap: Snap, site: Site?, onOpen: () -> Unit, onDelete: () -> Unit, canDelete: Boolean, preview: ChangePreview? = rememberVersionPreview(snap), query: String = "") {
    var menu by remember { mutableStateOf(false) }
    val name = site?.name() ?: stringResource(Res.string.activity_deleted_page)
    val first = preview == ChangePreview.FirstVersion
    val deleteLabel = stringResource(Res.string.activity_delete_version)
    Box(Modifier.padding(horizontal = 16.dp, vertical = 1.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(shape).background(com.bernaferrari.changedetection.ui.kit.tileColor)
                .combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onLongClickLabel = deleteLabel, role = Role.Button)
                .heightIn(min = 72.dp).testTag("activity-row-${snap.snapId}").padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageAvatar(name, snap.siteId, size = 40.dp, icon = site?.avatarIcon(), iconShape = site?.iconShape ?: com.bernaferrari.changedetection.repo.PageIconShape.AUTOMATIC, colors = site?.colors ?: (0 to 0))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(com.bernaferrari.changedetection.ui.kit.highlightMatches(name, query), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Found by its address: show the address with the match, so the result explains itself.
                val address = site?.url?.displayPageAddress()
                if (address != null && query.isNotBlank() && !com.bernaferrari.changedetection.ui.kit.matchesQuery(name, query) && com.bernaferrari.changedetection.ui.kit.matchesQuery(address, query)) {
                    Text(
                        com.bernaferrari.changedetection.ui.kit.highlightMatches(address, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val subtitle = if (first) stringResource(Res.string.activity_started_short) else previewLabel(preview)
                if (!first && preview == null) {
                    // Holds the summary's line quietly while it is worked out, like the cards on Home.
                    val line = with(androidx.compose.ui.platform.LocalDensity.current) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
                    Box(Modifier.height(line), contentAlignment = Alignment.CenterStart) {
                        Box(Modifier.size(width = 96.dp, height = 12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(50)).then(Modifier.placeholderPulse()))
                    }
                } else if (subtitle.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (preview is ChangePreview.Crop) Icon(Icons.Outlined.Image, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Text(formatTime(snap.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.activity_delete_version)) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                enabled = canDelete,
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun EmptyActivity(searching: Boolean) {
    // The bottom padding lifts the group to the optical centre; true centre reads as sagging.
    Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(80.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(if (searching) Icons.Outlined.Search else Icons.Outlined.Timeline, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(if (searching) Res.string.logs_search_empty_title else Res.string.activity_empty_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(if (searching) Res.string.logs_search_empty_subtitle else Res.string.activity_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
            textAlign = TextAlign.Center,
        )
    }
}
