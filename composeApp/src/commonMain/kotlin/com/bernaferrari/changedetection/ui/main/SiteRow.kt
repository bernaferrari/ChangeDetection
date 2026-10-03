/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import com.bernaferrari.changedetection.shared.generated.resources.monitor_filter_new
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_turbo_indicator
import com.bernaferrari.changedetection.shared.generated.resources.card_more_options
import com.bernaferrari.changedetection.shared.generated.resources.last_successful_check
import com.bernaferrari.changedetection.shared.generated.resources.saved_change_needs_review
import com.bernaferrari.changedetection.shared.generated.resources.site_status_waiting
import com.bernaferrari.changedetection.shared.generated.resources.site_status_changed
import com.bernaferrari.changedetection.shared.generated.resources.site_status_checking
import com.bernaferrari.changedetection.shared.generated.resources.site_status_error
import com.bernaferrari.changedetection.shared.generated.resources.site_status_paused
import com.bernaferrari.changedetection.shared.generated.resources.site_status_unchanged
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.displayPageAddress
import com.bernaferrari.changedetection.util.displayPageTitle
import org.jetbrains.compose.resources.stringResource

/**
 * The monitoring list's primary row. It deliberately keeps the site, its status, and the one
 * destination legible at a glance instead of turning each row into a miniature dashboard.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SiteRow(
    site: Site,
    status: SiteStatus,
    isQuickMonitored: Boolean,
    hasUnreadChange: Boolean = false,
    isSelected: Boolean = false,
    selectionSemanticsEnabled: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: com.bernaferrari.changedetection.ui.components.SegmentPosition = com.bernaferrari.changedetection.ui.components.SegmentPosition.Only,
) {
    val spacing = MaterialTheme.spacing
    val title = remember(site.title, site.url) { site.displayPageTitle() }
    val domain = remember(site.url) { runCatching { io.ktor.http.Url(site.url).host.removePrefix("www.") }.getOrDefault(site.url.displayPageAddress()) }
    val turboDescription = stringResource(Res.string.a11y_turbo_indicator)
    val optionsLabel = stringResource(Res.string.card_more_options, title)
    val statusLabel = when (status) {
        SiteStatus.Waiting -> stringResource(Res.string.site_status_waiting)

        SiteStatus.Checking -> stringResource(Res.string.site_status_checking)

        SiteStatus.Error -> if (site.lastFailureCode != null) com.bernaferrari.changedetection.ui.components.checkFailureTitle(site.lastFailureCode) else stringResource(Res.string.site_status_error)

        SiteStatus.Paused -> stringResource(Res.string.site_status_paused)

        is SiteStatus.Changed -> {
            val checkedAt = site.lastSuccessfulCheck ?: site.timestamp.takeIf { it > 0 }
            if (checkedAt != null) {
                stringResource(Res.string.site_status_unchanged, com.bernaferrari.changedetection.util.getRelativeTimeLabel(checkedAt))
            } else {
                stringResource(Res.string.site_status_changed, status.ago)
            }
        }

        is SiteStatus.Unchanged -> stringResource(Res.string.site_status_unchanged, status.lastCheck)
    }
    val unreadLabel = if (hasUnreadChange && status !is SiteStatus.Changed) stringResource(Res.string.saved_change_needs_review) else null
    val lastSuccessLabel = site.lastSuccessfulCheck?.takeIf { status == SiteStatus.Error }?.let {
        stringResource(Res.string.last_successful_check, com.bernaferrari.changedetection.util.getRelativeTimeLabel(it))
    }
    val titleColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val supportingColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val attentionColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val shape = com.bernaferrari.changedetection.ui.components.segmentShape(position)
    val containerColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "pageSelection",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .semantics {
                role = Role.Button
                if (selectionSemanticsEnabled) selected = isSelected
                stateDescription = listOfNotNull(statusLabel, unreadLabel, lastSuccessLabel, turboDescription.takeIf { isQuickMonitored }).joinToString(". ")
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClickLabel = optionsLabel,
                onLongClick = onLongClick,
            ),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 68.dp)
                .padding(start = 16.dp, end = 8.dp, top = 18.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SiteIdentity(
                title = title,
                modifier = Modifier.size(44.dp),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = if (hasUnreadChange || status is SiteStatus.Changed) FontWeight.SemiBold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isQuickMonitored) {
                        Surface(
                            modifier = Modifier.padding(start = spacing.xs),
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                modifier = Modifier.padding(4.dp).size(14.dp),
                            )
                        }
                    }
                }
                if (!title.equals(domain, ignoreCase = true)) {
                    Text(domain, style = MaterialTheme.typography.bodySmall, color = supportingColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val statusVisuals = status.visuals()
                val statusColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    status is SiteStatus.Changed -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> statusVisuals.contentColor
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (status == SiteStatus.Error || status == SiteStatus.Paused || status == SiteStatus.Checking) {
                        Icon(
                            imageVector = statusVisuals.icon,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = statusColor,
                        )
                    }
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (lastSuccessLabel != null) {
                    Text(lastSuccessLabel, style = MaterialTheme.typography.bodySmall, color = supportingColor)
                }
                if (unreadLabel != null) {
                    Text(unreadLabel, style = MaterialTheme.typography.bodySmall, color = attentionColor)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (hasUnreadChange || status is SiteStatus.Changed) {
                    Text(
                        stringResource(Res.string.monitor_filter_new),
                        style = MaterialTheme.typography.labelSmall,
                        color = attentionColor,
                    )
                }
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = optionsLabel,
                        tint = supportingColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SiteIdentity(
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = when (title.hashCode().mod(3)) {
            0 -> MaterialTheme.colorScheme.primaryContainer
            1 -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = when (title.hashCode().mod(3)) {
            0 -> MaterialTheme.colorScheme.onPrimaryContainer
            1 -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurface
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun SiteStatus.visuals(): StatusVisuals = when (this) {
    SiteStatus.Waiting -> StatusVisuals(
        icon = Icons.Rounded.History,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    SiteStatus.Checking -> StatusVisuals(
        icon = Icons.Rounded.History,
        contentColor = MaterialTheme.colorScheme.primary,
    )

    SiteStatus.Error -> StatusVisuals(
        icon = Icons.Rounded.Error,
        contentColor = MaterialTheme.colorScheme.error,
    )

    SiteStatus.Paused -> StatusVisuals(
        icon = Icons.Rounded.PauseCircle,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    is SiteStatus.Changed -> StatusVisuals(
        icon = Icons.Rounded.History,
        contentColor = MaterialTheme.colorScheme.primary,
    )

    is SiteStatus.Unchanged -> StatusVisuals(
        icon = Icons.Rounded.CheckCircle,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private data class StatusVisuals(
    val icon: ImageVector,
    val contentColor: Color,
)
