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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.ui.design.CanvasEmptyState
import com.bernaferrari.changedetection.ui.design.CanvasHero
import com.bernaferrari.changedetection.ui.design.InsightCard
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
            LogsTimelineList(
                versionsCount = versionsCount,
                siteCount = siteMap.size,
                snapsPagingData = snapsPagingData,
                siteMap = siteMap,
                onSnapClick = onSnapClick,
                onDeleteRequest = { snap -> showDeleteDialog = snap }
            )
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
private fun LogsTimelineList(
    versionsCount: Int,
    siteCount: Int,
    snapsPagingData: LazyPagingItems<Snap>,
    siteMap: Map<String, Site>,
    onSnapClick: (String, String) -> Unit,
    onDeleteRequest: (Snap) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LogsHero(
                versionsCount = versionsCount,
                siteCount = siteCount
            )
        }
        item {
            LogsStatsRow(
                versionsCount = versionsCount,
                siteCount = siteCount
            )
        }
        item {
            LogsSectionLabel(text = stringResource(id = R.string.logs_section_history))
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
                        onLongClick = { onDeleteRequest(it) }
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

@Composable
private fun LogsHero(
    versionsCount: Int,
    siteCount: Int
) {
    CanvasHero(
        modifier = Modifier.fillMaxWidth(),
        badge = stringResource(id = R.string.logs_hero_badge),
        title = stringResource(id = R.string.logs_hero_title),
        subtitle = stringResource(id = R.string.logs_hero_subtitle, versionsCount, siteCount)
    )
}

@Composable
private fun LogsStatsRow(
    versionsCount: Int,
    siteCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.History,
            label = stringResource(id = R.string.logs_stats_changes_label),
            value = versionsCount.toString(),
            supportingText = stringResource(id = R.string.logs_stats_changes_supporting)
        )
        InsightCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.Language,
            label = stringResource(id = R.string.logs_stats_sites_label),
            value = siteCount.toString(),
            supportingText = stringResource(id = R.string.logs_stats_sites_supporting)
        )
    }
}

@Composable
private fun LogsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun LogsEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CanvasEmptyState(
            icon = Icons.Filled.History,
            title = stringResource(id = R.string.logs_empty_title),
            subtitle = stringResource(id = R.string.logs_empty_subtitle),
            actionLabel = null,
            onAction = null
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
    val initials = getTitle(site).take(1).uppercase()
    val dateLabel = dateTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    role = Role.Button
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(site.colors.first),
                                    Color(site.colors.second)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getTitle(site),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = site.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = snap.contentSize.readableFileSize(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = snap.timestamp.convertTimestampToDate(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
