/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.a11y_turbo_indicator
import com.bernaferrari.changedetection.shared.generated.resources.site_status_changed
import com.bernaferrari.changedetection.shared.generated.resources.site_status_checking
import com.bernaferrari.changedetection.shared.generated.resources.site_status_error
import com.bernaferrari.changedetection.shared.generated.resources.site_status_paused
import com.bernaferrari.changedetection.shared.generated.resources.site_status_unchanged
import com.bernaferrari.changedetection.ui.theme.spacing
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
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: SiteRowPosition = SiteRowPosition.Single,
) {
    val spacing = MaterialTheme.spacing
    val title = remember(site.title, site.url) { site.displayTitle() }
    val domain = remember(site.url) { site.displayDomain() }
    val turboDescription = stringResource(Res.string.a11y_turbo_indicator)
    val statusLabel = when (status) {
        SiteStatus.Checking -> stringResource(Res.string.site_status_checking)
        SiteStatus.Error -> stringResource(Res.string.site_status_error)
        SiteStatus.Paused -> stringResource(Res.string.site_status_paused)
        is SiteStatus.Changed -> stringResource(Res.string.site_status_changed, status.ago)
        is SiteStatus.Unchanged -> stringResource(Res.string.site_status_unchanged, status.lastCheck)
    }
    val restingContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "sitePressScale",
    )
    val selectionTransition = updateTransition(isSelected, label = "siteSelection")
    val restingRadii = position.cornerRadii()
    val selectionShapeSpec = MaterialTheme.motionScheme.fastSpatialSpec<androidx.compose.ui.unit.Dp>()
    val shape = RoundedCornerShape(
        topStart = selectionTransition.animatedRadius("topStart", restingRadii.topStart, selectionShapeSpec),
        topEnd = selectionTransition.animatedRadius("topEnd", restingRadii.topEnd, selectionShapeSpec),
        bottomEnd = selectionTransition.animatedRadius("bottomEnd", restingRadii.bottomEnd, selectionShapeSpec),
        bottomStart = selectionTransition.animatedRadius("bottomStart", restingRadii.bottomStart, selectionShapeSpec),
    )
    val selectionColorSpec = MaterialTheme.motionScheme.fastEffectsSpec<Color>()
    val containerColor by selectionTransition.animateColor(
        transitionSpec = { selectionColorSpec },
        label = "siteContainerColor",
    ) { selected ->
        if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            restingContainerColor
        }
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(shape)
            .semantics {
                role = Role.Button
                selected = isSelected
                stateDescription = if (isQuickMonitored) {
                    "$statusLabel. $turboDescription"
                } else {
                    statusLabel
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        shape = shape,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 68.dp)
                .padding(start = spacing.sm, end = spacing.sm, top = spacing.sm, bottom = spacing.sm),
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
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
                                contentDescription = turboDescription,
                                modifier = Modifier.padding(4.dp).size(14.dp),
                            )
                        }
                    }
                }
                val statusVisuals = status.visuals()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = statusVisuals.icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = statusVisuals.contentColor,
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusVisuals.contentColor,
                        maxLines = 1,
                    )
                    Text(
                        text = "· $domain",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
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

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = siteColor.copy(alpha = 0.16f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = siteColor,
            )
        }
    }
}

@Composable
private fun SiteStatus.visuals(): StatusVisuals = when (this) {
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
        contentColor = MaterialTheme.colorScheme.tertiary,
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

enum class SiteRowPosition {
    Single,
    First,
    Middle,
    Last,
}

@Composable
private fun androidx.compose.animation.core.Transition<Boolean>.animatedRadius(
    label: String,
    restingRadius: androidx.compose.ui.unit.Dp,
    animationSpec: FiniteAnimationSpec<androidx.compose.ui.unit.Dp>,
) = animateDp(
    transitionSpec = { animationSpec },
    label = label,
) { selected ->
    if (selected) 24.dp else restingRadius
}.value

private data class SiteCornerRadii(
    val topStart: androidx.compose.ui.unit.Dp,
    val topEnd: androidx.compose.ui.unit.Dp,
    val bottomEnd: androidx.compose.ui.unit.Dp,
    val bottomStart: androidx.compose.ui.unit.Dp,
)

private fun SiteRowPosition.cornerRadii(): SiteCornerRadii = when (this) {
    SiteRowPosition.Single -> SiteCornerRadii(24.dp, 24.dp, 24.dp, 24.dp)
    SiteRowPosition.First -> SiteCornerRadii(24.dp, 24.dp, 7.dp, 7.dp)
    SiteRowPosition.Middle -> SiteCornerRadii(7.dp, 7.dp, 7.dp, 7.dp)
    SiteRowPosition.Last -> SiteCornerRadii(7.dp, 7.dp, 24.dp, 24.dp)
}

private fun Site.displayTitle(): String = title?.takeIf { it.isNotBlank() } ?: displayDomain()

private fun Site.displayDomain(): String = url
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore("/")
    .ifBlank { url }
