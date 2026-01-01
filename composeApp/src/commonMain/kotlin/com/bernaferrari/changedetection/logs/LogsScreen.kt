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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bernaferrari.changedetection.ui.logs.LogItem
import com.bernaferrari.changedetection.ui.logs.LogsHeader
import com.bernaferrari.changedetection.ui.logs.LogsEmptyState
import com.bernaferrari.changedetection.ui.logs.LogsViewModel
import com.bernaferrari.changedetection.util.formatTimestamp
import com.bernaferrari.changedetection.util.readableFileSize
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LogsScreen(
    onSnapClick: (String, String) -> Unit,
    viewModel: LogsViewModel = koinViewModel()
) {
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val snapsPagingData = viewModel.pagedSnaps.collectAsLazyPagingItems()
    var showDeleteDialog by remember { mutableStateOf<Snap?>(null) }

    when {
        snapsPagingData.loadState.refresh is LoadState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        snapsPagingData.loadState.refresh is LoadState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(id = R.string.logs_error_loading))
            }
        }

        snapsPagingData.itemCount == 0 -> {
            LogsEmptyState()
        }

        else -> {
            LogsContent(
                versionsCount = versionsCount,
                siteCount = siteMap.size,
                snapsPagingData = snapsPagingData,
                siteMap = siteMap,
                onSnapClick = onSnapClick,
                onDeleteRequest = { snap -> showDeleteDialog = snap }
            )
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

@Composable
private fun LogsContent(
    versionsCount: Int,
    siteCount: Int,
    snapsPagingData: LazyPagingItems<Snap>,
    siteMap: Map<String, Site>,
    onSnapClick: (String, String) -> Unit,
    onDeleteRequest: (Snap) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Header
        item {
            LogsHeader(
                versionsCount = versionsCount,
                siteCount = siteCount
            )
        }

        // Section label
        item {
            Text(
                text = stringResource(id = R.string.logs_section_history),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // Log items
        items(
            count = snapsPagingData.itemCount,
            key = snapsPagingData.itemKey { it.snapId }
        ) { index ->
            val snap = snapsPagingData[index]
            snap?.let {
                val site = siteMap[it.siteId]
                if (site != null) {
                    LogItem(
                        snap = it,
                        site = site,
                        onClick = { onSnapClick(it.snapId, site.url) },
                        onDeleteClick = { onDeleteRequest(it) }
                    )
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}
