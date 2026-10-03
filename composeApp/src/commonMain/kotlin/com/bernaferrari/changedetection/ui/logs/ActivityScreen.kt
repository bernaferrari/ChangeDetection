/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)

package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.bernaferrari.changedetection.preview.ChangePreview
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.checkFailureTitle
import com.bernaferrari.changedetection.ui.kit.PageAvatar
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
    val deletedText = stringResource(Res.string.snapshot_deleted)
    val undoText = stringResource(Res.string.undo)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    BackHandler(enabled = searching) {
        viewModel.setSearchQuery("")
        searching = false
    }
    val failing = sites.values.filter { it.isSyncEnabled && !it.isSuccessful }

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
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> LoadingIndicator(Modifier.padding(48.dp))

                items.itemCount == 0 -> EmptyActivity(searching = query.isNotBlank())

                else -> LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("activity-list"), contentPadding = PaddingValues(bottom = 32.dp)) {
                    if (failing.isNotEmpty() && query.isBlank()) {
                        item(key = "failing") {
                            Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer) {
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    failing.take(4).forEach { site ->
                                        Row(
                                            Modifier.fillMaxWidth().combinedClickable(onClick = { onOpenPage(site.id) }).heightIn(min = 48.dp).padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            Icon(Icons.Outlined.ErrorOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                                            Text(site.name(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                            Text(checkFailureTitle(site.lastFailureCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
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
                            is LogUiModel.Header -> DayHeader(model.date, items.peek(index + 1))

                            is LogUiModel.Item -> ActivityRow(
                                snap = model.snap,
                                site = sites[model.snap.siteId],
                                onOpen = { onOpenChange(model.snap.siteId, model.snap.snapId) },
                                onDelete = {
                                    val result = viewModel.removeSnap(model.snap)
                                    scope.launch {
                                        val undo = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed
                                        if (undo) viewModel.undoRemoveSnap(model.snap) else viewModel.discardPendingDeletion(model.snap)
                                        result.await()
                                    }
                                },
                            )

                            null -> Box(Modifier.fillMaxWidth().heightIn(min = 72.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: String, next: LogUiModel?) {
    val timestamp = (next as? LogUiModel.Item)?.snap?.timestamp
    Text(
        if (timestamp != null) friendlyDay(timestamp) else date,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ActivityRow(snap: Snap, site: Site?, onOpen: () -> Unit, onDelete: () -> Unit) {
    val preview = rememberVersionPreview(snap)
    var menu by remember { mutableStateOf(false) }
    val name = site?.name() ?: stringResource(Res.string.activity_deleted_page)
    val first = preview == ChangePreview.FirstVersion
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = { menu = true }).heightIn(min = 72.dp).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageAvatar(name, snap.siteId, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (first) Res.string.activity_started else Res.string.activity_changed, name),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (preview is ChangePreview.Crop) Icon(Icons.Outlined.Image, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(previewLabel(preview), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(formatTime(snap.timestamp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.activity_delete_version)) },
                leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
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
    Column(Modifier.padding(32.dp).widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Outlined.Timeline, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(if (searching) Res.string.logs_search_empty_title else Res.string.activity_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(if (searching) Res.string.logs_search_empty_subtitle else Res.string.activity_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
