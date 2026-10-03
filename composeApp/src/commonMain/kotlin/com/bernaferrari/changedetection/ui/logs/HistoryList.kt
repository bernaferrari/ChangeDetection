/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import androidx.paging.compose.itemContentType
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import org.jetbrains.compose.resources.stringResource

/** Keeps loaded history visible while additional pages load or await a retry. */
@Composable
internal fun HistoryList(
    pagedItems: LazyPagingItems<LogUiModel>,
    siteMap: Map<String, Site>,
    versionsCount: Int,
    changedSiteCount: Int,
    onSnapClick: (String, String) -> Unit,
    onSnapOpenInBrowser: (String, String) -> Unit,
    onDelete: (Snap) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    loadPreview: (suspend (Snap) -> HistoryPreviewContent)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .fillMaxHeight().testTag("history-list"),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            if (pagedItems.loadState.refresh is LoadState.Error) {
                item(key = "refresh-history-error") {
                    Surface(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.logs_refresh_failed), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = pagedItems::retry, modifier = Modifier.align(Alignment.End)) { Text(stringResource(Res.string.retry)) }
                        }
                    }
                }
            }
            item(key = "history-pulse") {
                if (searchQuery.isBlank()) {
                    LogsPulse(
                        versionsCount = versionsCount,
                        siteCount = changedSiteCount,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp),
                    )
                } else {
                    Text(stringResource(Res.string.logs_search_results), Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                }
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
                            LogItem(
                                position = com.bernaferrari.changedetection.ui.components.segmentPosition(
                                    index == 0 || pagedItems.peek(index - 1) !is LogUiModel.Item,
                                    index == pagedItems.itemCount - 1 || pagedItems.peek(index + 1) !is LogUiModel.Item,
                                ),
                                snap = item.snap,
                                loadPreview = loadPreview,
                                site = site,
                                onClick = { onSnapClick(site.id, item.snap.snapId) },
                                onDeleteClick = { onDelete(item.snap) },
                                onOpenInBrowser = { onSnapOpenInBrowser(item.snap.snapId, site.url) },
                            )
                        }
                    }

                    is LogUiModel.Header -> DateHeader(date = item.date)

                    null -> Unit
                }
            }

            if (pagedItems.loadState.append is LoadState.Error && pagedItems.loadState.refresh !is LoadState.Error) {
                item(key = "retry-history") {
                    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(Res.string.logs_more_failed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = pagedItems::retry) { Text(stringResource(Res.string.retry)) }
                    }
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
