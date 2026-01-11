package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    viewModel: LogsViewModel = koinViewModel()
) {
    val pagedItems = viewModel.pagedSnaps.collectAsLazyPagingItems()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.nav_logs)) }
            )
        }
    ) { padding ->
        if (pagedItems.itemCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LogsEmptyState()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    LogsHeader(
                        versionsCount = versionsCount,
                        siteCount = siteMap.size
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
                    }
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
                                    onDeleteClick = { viewModel.removeSnap(item.snap.snapId) }
                                )
                            }
                        }

                        is LogUiModel.Header -> {
                            // Date header
                            Text(
                                text = item.date,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        null -> {} // Loading state placeholder
                    }
                }
            }
        }
    }
}
