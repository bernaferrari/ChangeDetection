/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.ui.theme.CornerRadius
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import com.bernaferrari.changedetection.ui.theme.spacing

/** A selectable monitored site in Turbo mode, kept separate from the screen state. */
@Composable
internal fun TurboSiteRow(
    site: Site,
    isSelected: Boolean,
    enabled: Boolean,
    isSyncing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }
    val gradient = remember(siteColor, siteColor2) {
        Brush.linearGradient(listOf(siteColor, siteColor2))
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) siteColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "turboRowBackground",
    )
    val cornerAnimation = spring<Dp>(
        dampingRatio = ExpressiveSpring.QUICK_DAMPING,
        stiffness = ExpressiveSpring.QUICK_STIFFNESS,
    )
    val topRadius by animateDpAsState(
        targetValue = if (isSelected) {
            CornerRadius.largePlus
        } else if (isFirst) {
            CornerRadius.medium
        } else {
            CornerRadius.xs
        },
        animationSpec = cornerAnimation,
        label = "turboRowTopRadius",
    )
    val bottomRadius by animateDpAsState(
        targetValue = if (isSelected) {
            CornerRadius.largePlus
        } else if (isLast) {
            CornerRadius.medium
        } else {
            CornerRadius.xs
        },
        animationSpec = cornerAnimation,
        label = "turboRowBottomRadius",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.BOUNCY_DAMPING,
            stiffness = ExpressiveSpring.BOUNCY_STIFFNESS,
        ),
        label = "turboRowScale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = spacing.xxs / 2)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = ExpressiveSpring.QUICK_DAMPING,
                    stiffness = ExpressiveSpring.QUICK_STIFFNESS,
                ),
            ),
        shape = RoundedCornerShape(topRadius, topRadius, bottomRadius, bottomRadius),
        color = backgroundColor,
        tonalElevation = if (isSelected) 3.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = isSelected,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                )
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(spacing.lg + spacing.md)
                    .clip(RoundedCornerShape(spacing.sm))
                    .background(gradient),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isSyncing,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "turboSyncIndicator",
                ) { syncing ->
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(spacing.md + spacing.xxs),
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                        )
                    } else {
                        Text(
                            text = site.title?.firstOrNull()?.uppercase() ?: "W",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.title?.takeIf { it.isNotBlank() } ?: site.url,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = site.url.replace(Regex("^https?://"), ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TurboSelectionIndicator(isSelected = isSelected, color = siteColor)
        }
    }
}

@Composable
private fun TurboSelectionIndicator(
    isSelected: Boolean,
    color: Color,
) {
    val spacing = MaterialTheme.spacing
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.Transparent,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "turboSelectionBackground",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else MaterialTheme.colorScheme.outline,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "turboSelectionBorder",
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "turboSelectionScale",
    )

    Box(
        modifier = Modifier
            .size(spacing.lg + spacing.xxs)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(spacing.md),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(spacing.lg)
                    .clip(CircleShape)
                    .background(borderColor.copy(alpha = 0.3f)),
            )
        }
    }
}
