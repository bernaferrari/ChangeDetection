/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.card_more_options
import com.bernaferrari.changedetection.shared.generated.resources.site_status_changed
import com.bernaferrari.changedetection.shared.generated.resources.site_status_checking
import com.bernaferrari.changedetection.shared.generated.resources.site_status_error
import com.bernaferrari.changedetection.shared.generated.resources.site_status_unchanged
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

/**
 * The monitoring list's primary row. It deliberately keeps the site, its status, and the one
 * overflow action legible at a glance instead of turning each row into a miniature dashboard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SiteRow(
    site: Site,
    status: SiteStatus,
    isQuickMonitored: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val title = remember(site.title, site.url) { site.displayTitle() }
    val domain = remember(site.url) { site.displayDomain() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = spacing.touchTarget + spacing.sm)
                .padding(start = spacing.sm, end = spacing.xxs, top = spacing.xs, bottom = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteIdentity(
                site = site,
                title = title,
                modifier = Modifier.size(40.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isQuickMonitored) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .padding(start = spacing.xs)
                                .size(16.dp),
                        )
                    }
                }
                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            StatusBadge(status = status)

            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(spacing.touchTarget),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(Res.string.card_more_options, title),
                )
            }
        }
    }
}

@Composable
private fun SiteIdentity(
    site: Site,
    title: String,
    modifier: Modifier = Modifier,
) {
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val containerColor = siteColor.copy(alpha = 0.18f)
    val contentColor = siteColor.copy(alpha = 0.94f)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.firstOrNull()?.uppercase().orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

@Composable
private fun StatusBadge(status: SiteStatus) {
    val visuals = status.visuals()
    val label = when (status) {
        SiteStatus.Checking -> stringResource(Res.string.site_status_checking)
        SiteStatus.Error -> stringResource(Res.string.site_status_error)
        is SiteStatus.Changed -> stringResource(Res.string.site_status_changed, status.ago)
        is SiteStatus.Unchanged -> stringResource(Res.string.site_status_unchanged, status.lastCheck)
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = visuals.containerColor,
        contentColor = visuals.contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (status == SiteStatus.Checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = visuals.contentColor,
                    strokeWidth = 1.5.dp,
                )
            } else {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SiteStatus.visuals(): StatusVisuals = when (this) {
    SiteStatus.Checking -> StatusVisuals(
        icon = Icons.Rounded.History,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )

    SiteStatus.Error -> StatusVisuals(
        icon = Icons.Rounded.Error,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )

    is SiteStatus.Changed -> StatusVisuals(
        icon = Icons.Rounded.History,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )

    is SiteStatus.Unchanged -> StatusVisuals(
        icon = Icons.Rounded.CheckCircle,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class StatusVisuals(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)

private fun Site.displayTitle(): String = title?.takeIf { it.isNotBlank() } ?: displayDomain()

private fun Site.displayDomain(): String = url
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore("/")
    .ifBlank { url }
