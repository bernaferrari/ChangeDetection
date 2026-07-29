/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.delete_snapshot_title
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.logs_empty_title
import com.bernaferrari.changedetection.shared.generated.resources.logs_hero_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.logs_section_history
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import com.bernaferrari.changedetection.util.readableFileSize
import org.jetbrains.compose.resources.stringResource

@Composable
fun LogItem(
    snap: Snap,
    site: Site,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val title = remember(site.title, site.url) { getTitle(site) }
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(start = spacing.sm, end = spacing.xxs, top = spacing.xs, bottom = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(siteColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = siteColor.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
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
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(spacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.delete_snapshot_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
fun LogsHeader(
    versionsCount: Int,
    siteCount: Int,
) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(
            text = stringResource(Res.string.logs_section_history),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(Res.string.logs_hero_subtitle, versionsCount, siteCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.lg, vertical = MaterialTheme.spacing.sm),
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
