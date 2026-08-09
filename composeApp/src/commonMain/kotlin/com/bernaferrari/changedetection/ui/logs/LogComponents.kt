/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.delete
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.logs_metric_changes
import com.bernaferrari.changedetection.shared.generated.resources.logs_metric_sites
import com.bernaferrari.changedetection.shared.generated.resources.loading
import com.bernaferrari.changedetection.shared.generated.resources.more_options
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.segmentShape
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import com.bernaferrari.changedetection.util.readableFileSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun LogsPulse(
    versionsCount: Int,
    siteCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        HistoryMetric(
            value = versionsCount,
            label = stringResource(Res.string.logs_metric_changes),
        )
        HistoryMetric(
            value = siteCount,
            label = stringResource(Res.string.logs_metric_sites),
        )
    }
}

@Composable
private fun HistoryMetric(
    value: Int,
    label: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun LogItem(
    snap: Snap,
    site: Site,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    position: SegmentPosition,
) {
    val spacing = MaterialTheme.spacing
    val title = remember(site.title, site.url) { getTitle(site) }
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = spacing.lg,
                end = spacing.lg,
                top = if (position == SegmentPosition.First || position == SegmentPosition.Only) 0.dp else 2.dp,
            )
            .clip(segmentShape(position))
            .clickable(onClick = onClick),
        shape = segmentShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.defaultMinSize(minHeight = 68.dp).padding(start = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.large,
                color = siteColor.copy(alpha = 0.16f),
                contentColor = siteColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = title.firstOrNull()?.uppercase().orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${getRelativeTimeLabel(snap.timestamp)} · ${snap.contentSize.toLong().readableFileSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(spacing.touchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(Res.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.delete)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDeleteClick()
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun LogsLoadingState(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val loadingDescription = stringResource(Res.string.loading)
    Box(
        modifier = modifier.semantics { contentDescription = loadingDescription },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .clearAndSetSemantics { }
                .padding(top = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            repeat(4) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = spacing.lg),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {}
            }
        }
    }
}

@Composable
fun LogsEmptyState() {
    SharedEmptyState(
        icon = Icons.Rounded.History,
        title = stringResource(Res.string.logs_empty_title),
        description = stringResource(Res.string.logs_empty_subtitle),
    )
}

@Composable
fun DateHeader(date: String) {
    Text(
        text = date,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = MaterialTheme.spacing.lg,
            end = MaterialTheme.spacing.lg,
            top = MaterialTheme.spacing.lg,
            bottom = MaterialTheme.spacing.xs,
        ),
    )
}

internal fun getTitle(site: Site?): String {
    if (site == null) return ""

    return if (site.title.isNullOrBlank()) {
        site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
    } else {
        site.title
    }
}
