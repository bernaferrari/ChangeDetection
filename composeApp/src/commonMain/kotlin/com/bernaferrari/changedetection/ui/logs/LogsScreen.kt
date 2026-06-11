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
import org.koin.compose.viewmodel.koinViewModel
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    onNavigateUp: () -> Unit,
    onSnapClick: (snapId: String, url: String) -> Unit,
    viewModel: LogsViewModel = koinViewModel(),
) {
    val pagedItems = viewModel.pagedSnaps.collectAsLazyPagingItems()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.nav_logs),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (pagedItems.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LogsEmptyState()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
            ) {
                item {
                    LogsHeader(
                        versionsCount = versionsCount,
                        siteCount = siteMap.size,
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
                    val item = pagedItems[index]
                    when (item) {
                        is LogUiModel.Item -> {
                            val site = siteMap[item.snap.siteId]
                            if (site != null) {
                                val position = when {
                                    pagedItems.itemCount == 1 -> ListItemPosition.SINGLE
                                    index == 0 -> ListItemPosition.FIRST
                                    index == pagedItems.itemCount - 1 -> ListItemPosition.LAST
                                    else -> ListItemPosition.MIDDLE
                                }
                                LogItem(
                                    snap = item.snap,
                                    site = site,
                                    position = position,
                                    onClick = { onSnapClick(item.snap.snapId, site.url) },
                                    onDeleteClick = { viewModel.removeSnap(item.snap.snapId) },
                                )
                            }
                        }

                        is LogUiModel.Header -> {
                            DateHeader(date = item.date)
                        }

                        null -> {} // Loading state placeholder
                    }
                }
            }
        }
    }
}
