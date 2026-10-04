/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.bernaferrari.changedetection.ui.logs

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bernaferrari.changedetection.ui.kit.SectionHeader
import com.bernaferrari.changedetection.ui.kit.friendlyDay
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.previewLabel
import com.bernaferrari.changedetection.ui.kit.rememberVersionPreview
import com.bernaferrari.changedetection.util.BackHandler
import com.bernaferrari.changedetection.util.formatTime
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Every saved change, newest first, one sentence each. */
@Composable
fun ActivityScreen(
    onOpenChange: (siteId: String, snapId: String) -> Unit,
    onOpenPage: (siteId: String) -> Unit,
    viewModel: LogsViewModel = koinViewModel(),
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    BackHandler(enabled = searching) {
        viewModel.setSearchQuery("")
        searching = false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (searching) {
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { focus.requestFocus() }
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.setSearchQuery("")
                            searching = false
                        }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.home_search_close)) }
                    },
                    title = {
                        TextField(
                            value = query,
                            onValueChange = viewModel::setSearchQuery,
                            placeholder = { Text(stringResource(Res.string.logs_search_pages)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().focusRequester(focus),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    },
                    actions = {
                        if (query.isNotEmpty()) IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Outlined.Close, stringResource(Res.string.home_search_clear)) }
                    },
                )
            } else {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(Res.string.nav_activity)) },
                    actions = { IconButton(onClick = { searching = true }) { Icon(Icons.Outlined.Search, stringResource(Res.string.logs_search_pages)) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        ActivityFeed(
            items = items,
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
) {
    val failing = if (query.isBlank()) sites.values.filter { it.isSyncEnabled && !it.isSuccessful }.sortedBy { it.name() } else emptyList()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("activity-list"), contentPadding = PaddingValues(bottom = 32.dp)) {
            if (failing.isNotEmpty()) {
                item(key = "failures-header") { SectionHeader(stringResource(Res.string.home_section_attention), count = failing.size) }
                items(failing, key = { "failure-" + it.id }) { site ->
                    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                        Row(
                            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = { onOpenPage(site.id) }).heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(site.name(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(checkFailureTitle(site.lastFailureCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
            when {
                items.loadState.refresh is LoadState.Error -> item(key = "refresh-error") { ActivityLoadError(items::retry) }

                items.itemCount == 0 && items.loadState.refresh is LoadState.Loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
                }

                items.itemCount == 0 -> item(key = "empty") { EmptyActivity(searching = query.isNotBlank()) }
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
                        snap = model.snap,
                        site = sites[model.snap.siteId],
                        onOpen = { onOpenChange(model.snap.siteId, model.snap.snapId) },
                        onDelete = { onDelete(model.snap) },
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
        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ActivityRow(snap: Snap, site: Site?, onOpen: () -> Unit, onDelete: () -> Unit, canDelete: Boolean) {
    val preview = rememberVersionPreview(snap)
    var menu by remember { mutableStateOf(false) }
    val name = site?.name() ?: stringResource(Res.string.activity_deleted_page)
    val first = preview == ChangePreview.FirstVersion
    val deleteLabel = stringResource(Res.string.activity_delete_version)
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }, onLongClickLabel = deleteLabel, role = Role.Button).heightIn(min = 72.dp).padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageAvatar(name, snap.siteId, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (first) Res.string.activity_started else Res.string.activity_changed, name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (preview is ChangePreview.Crop) Icon(Icons.Outlined.Image, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(previewLabel(preview), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatTime(snap.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(Res.string.more_options)) }
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
        }
    }
}

@Composable
private fun EmptyActivity(searching: Boolean) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Timeline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(if (searching) Res.string.logs_search_empty_title else Res.string.activity_empty_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(if (searching) Res.string.logs_search_empty_subtitle else Res.string.activity_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 420.dp),
            textAlign = TextAlign.Center,
        )
    }
}
