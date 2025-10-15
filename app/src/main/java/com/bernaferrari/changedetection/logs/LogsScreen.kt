package com.bernaferrari.changedetection.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.compose.SectionHeader
import com.bernaferrari.changedetection.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LogsScreen(
    onSnapClick: (String, String) -> Unit,
    viewModel: LogsViewModel = hiltViewModel()
) {
    val versionsCount by viewModel.versionsCount.collectAsStateWithLifecycle()
    val siteMap by viewModel.siteMap.collectAsStateWithLifecycle()
    val snapsPagingData = viewModel.pagedSnaps.collectAsLazyPagingItems()
    var showDeleteDialog by remember { mutableStateOf<Snap?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
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
                EmptyLogsComponent()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader(
                            title = stringResource(id = R.string.logs_last_changes_title),
                            subtitle = stringResource(
                                id = R.plurals.logs_changes_detected,
                                versionsCount,
                                versionsCount
                            )
                        )
                    }

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
                                    onLongClick = { showDeleteDialog = it }
                                )
                            }
                        }
                    }

                    if (snapsPagingData.loadState.append is LoadState.Loading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    showDeleteDialog?.let { snap ->
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(snap.timestamp),
            ZoneId.systemDefault()
        )
        val formattedDate = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format(dateTime)
        val sizeLabel = snap.contentSize.readableFileSize()

        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
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
                    Text(stringResource(id = R.string.remove))
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
private fun LogItem(
    snap: Snap,
    site: Site,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(snap.timestamp),
        ZoneId.systemDefault()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button
                )
        ) {
            // Gradient background
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(site.colors.first),
                                Color(site.colors.second)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(48.dp)
                ) {
                    Text(
                        text = dateTime.dayOfMonth.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dateTime.format(DateTimeFormatter.ofPattern("MMM")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getTitle(site),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = getLogsSubtitle(snap),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Size indicator
                Text(
                    text = snap.contentSize.readableFileSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun EmptyLogsComponent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.logs_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.logs_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

internal fun getTitle(site: Site?): String {
    if (site == null) return ""

    return if (site.title.isNullOrBlank()) {
        site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
    } else {
        site.title
    }
}

internal fun getLogsSubtitle(lastSnap: Snap): String =
    "${lastSnap.contentSize.readableFileSize()} • ${lastSnap.timestamp.convertTimestampToDate()}"
