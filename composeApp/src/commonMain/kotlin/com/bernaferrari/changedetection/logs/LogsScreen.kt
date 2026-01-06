package com.bernaferrari.changedetection.logs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.logs.ListItemPosition
import com.bernaferrari.changedetection.ui.logs.LogItem
import com.bernaferrari.changedetection.ui.logs.LogsHeader
import com.bernaferrari.changedetection.ui.logs.LogUiModel
import com.bernaferrari.changedetection.ui.logs.LogsViewModel
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.readableFileSize
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LogsScreen(
    onSnapClick: (String, String) -> Unit,
    viewModel: LogsViewModel = koinViewModel()
) {
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val snapsPagingData = viewModel.pagedSnaps.collectAsLazyPagingItems()
    var showDeleteDialog by remember { mutableStateOf<Snap?>(null) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.logs_hero_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { paddingValues ->
        when {
            snapsPagingData.loadState.refresh is LoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            }

            snapsPagingData.loadState.refresh is LoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.logs_error_loading))
                }
            }

            snapsPagingData.itemCount == 0 -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    SharedEmptyState(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.logs_empty_title),
                        description = stringResource(R.string.logs_empty_subtitle)
                    )
                }
            }

            else -> {
                LogsContent(
                    paddingValues = paddingValues,
                    versionsCount = versionsCount,
                    siteCount = siteMap.size,
                    snapsPagingData = snapsPagingData,
                    siteMap = siteMap,
                    onSnapClick = onSnapClick,
                    onDeleteRequest = { snap -> showDeleteDialog = snap }
                )
            }
        }
    }

    showDeleteDialog?.let { snap ->
        val formattedDate = formatTimestamp(snap.timestamp)
        val sizeLabel = snap.contentSize.toLong().readableFileSize()

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(id = R.string.logs_remove_snapshot_title)) },
            text = {
                Text(stringResource(id = R.string.logs_remove_snapshot_body, sizeLabel, formattedDate))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeSnap(snap.snapId)
                        showDeleteDialog = null
                    }
                ) {
                    Text(
                        stringResource(id = R.string.remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogsContent(
    paddingValues: PaddingValues,
    versionsCount: Int,
    siteCount: Int,
    snapsPagingData: LazyPagingItems<LogUiModel>,
    siteMap: Map<String, Site>,
    onSnapClick: (String, String) -> Unit,
    onDeleteRequest: (Snap) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
    ) {
        // Section label
        item {
            Text(
                text = stringResource(id = R.string.logs_section_history),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp)
            )
        }

        // Log items
        // Log items
        items(
            count = snapsPagingData.itemCount,
            key = snapsPagingData.itemKey {
                when (it) {
                    is LogUiModel.Item -> "item_${it.snap.snapId}"
                    is LogUiModel.Header -> "header_${it.date}"
                }
            }
        ) { index ->
            val item = snapsPagingData[index]

            when (item) {
                is LogUiModel.Header -> {
                    // Inline date header for timeline separators
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            end = 24.dp,
                            top = 24.dp,
                            bottom = 8.dp
                        )
                    )
                }

                is LogUiModel.Item -> {
                    val snap = item.snap
                    val site = siteMap[snap.siteId]

                    if (site != null) {
                        // Calculate item position
                        val prev = if (index > 0) snapsPagingData.peek(index - 1) else null
                        val next = if (index < snapsPagingData.itemCount - 1) snapsPagingData.peek(index + 1) else null

                        val isFirst = prev is LogUiModel.Header || prev == null
                        val isLast = next is LogUiModel.Header || next == null

                        val position = when {
                            isFirst && isLast -> ListItemPosition.SINGLE
                            isFirst -> ListItemPosition.FIRST
                            isLast -> ListItemPosition.LAST
                            else -> ListItemPosition.MIDDLE
                        }

                        LogItem(
                            snap = snap,
                            site = site,
                            position = position,
                            onClick = { onSnapClick(snap.snapId, site.url) },
                            onDeleteClick = { onDeleteRequest(snap) }
                        )
                    }
                }

                null -> {
                    // Placeholder or error, do nothing or show placeholder
                }
            }
        }

        if (snapsPagingData.loadState.append is LoadState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
