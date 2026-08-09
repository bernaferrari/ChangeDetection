/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.paging.LoadState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Delete
import com.bernaferrari.changedetection.ui.components.ConfirmDialog
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogsScreen(
    onSnapClick: (snapId: String, url: String) -> Unit,
    viewModel: LogsViewModel = koinViewModel(),
) {
    val pagedItems = viewModel.pagedSnaps.collectAsLazyPagingItems()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var snapPendingDeletion by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.nav_logs),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (pagedItems.loadState.refresh is LoadState.Loading && pagedItems.itemCount == 0) {
            LogsLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
        } else if (pagedItems.loadState.refresh is LoadState.Error && pagedItems.itemCount == 0) {
            SharedEmptyState(
                icon = Icons.Default.ErrorOutline,
                title = stringResource(Res.string.logs_load_failed),
                description = (pagedItems.loadState.refresh as LoadState.Error).error.message.orEmpty(),
                modifier = Modifier.fillMaxSize().padding(padding),
                primaryActionLabel = stringResource(Res.string.retry),
                onPrimaryAction = pagedItems::retry,
            )
        } else if (pagedItems.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LogsEmptyState()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    item(key = "history-pulse") {
                        LogsPulse(
                            versionsCount = versionsCount,
                            siteCount = siteMap.size,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                        )
                    }
                    items(
                        count = pagedItems.itemCount,
                        key = pagedItems.itemKey { item ->
                            when (item) {
                                is LogUiModel.Item -> item.snap.snapId
                                is LogUiModel.Header -> item.date
                            }
                        },
                        contentType = pagedItems.itemContentType { item ->
                            when (item) {
                                is LogUiModel.Item -> "item"
                                is LogUiModel.Header -> "header"
                            }
                        },
                    ) { index ->
                        when (val item = pagedItems[index]) {
                            is LogUiModel.Item -> {
                                val site = siteMap[item.snap.siteId]
                                if (site != null) {
                                    val previous = if (index > 0) pagedItems.peek(index - 1) else null
                                    val next = if (index + 1 < pagedItems.itemCount) pagedItems.peek(index + 1) else null
                                    LogItem(
                                        snap = item.snap,
                                        site = site,
                                        onClick = { onSnapClick(item.snap.snapId, site.url) },
                                        onDeleteClick = { snapPendingDeletion = item.snap.snapId },
                                        position = when {
                                            previous !is LogUiModel.Item && next !is LogUiModel.Item -> SegmentPosition.Only
                                            previous !is LogUiModel.Item -> SegmentPosition.First
                                            next !is LogUiModel.Item -> SegmentPosition.Last
                                            else -> SegmentPosition.Middle
                                        },
                                    )
                                }
                            }

                            is LogUiModel.Header -> DateHeader(date = item.date)

                            null -> Unit
                        }
                    }

                    if (pagedItems.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AppLoadingIndicator(size = 24.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    snapPendingDeletion?.let { snapId ->
        ConfirmDialog(
            visible = true,
            onDismiss = { snapPendingDeletion = null },
            onConfirm = { viewModel.removeSnap(snapId) },
            title = stringResource(Res.string.delete_snapshot_title),
            message = stringResource(Res.string.delete_snapshot_message),
            icon = Icons.Outlined.Delete,
            confirmText = stringResource(Res.string.delete),
            isDestructive = true,
        )
    }
}
