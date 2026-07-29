/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.logs

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import com.bernaferrari.changedetection.ui.theme.spacing
import com.bernaferrari.changedetection.util.getRelativeTimeLabel
import com.bernaferrari.changedetection.util.readableFileSize
import org.jetbrains.compose.resources.stringResource

/**
 * Position of an item in a list for expressive corner styling.
 * First items get top corners rounded, last items get bottom corners rounded.
 */
enum class ListItemPosition {
    FIRST, // First item - rounded top corners
    MIDDLE, // Middle items - no rounded corners
    LAST, // Last item - rounded bottom corners
    SINGLE, // Only item - all corners rounded
}

/**
 * Gets the appropriate corner shape based on item position.
 */
fun getListItemShape(position: ListItemPosition): RoundedCornerShape = when (position) {
    ListItemPosition.FIRST -> RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 4.dp,
        bottomEnd = 4.dp,
    )

    ListItemPosition.MIDDLE -> RoundedCornerShape(4.dp)
    ListItemPosition.LAST -> RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 4.dp,
        bottomStart = 24.dp,
        bottomEnd = 24.dp,
    )

    ListItemPosition.SINGLE -> RoundedCornerShape(24.dp)
}

@Composable
fun LogItem(
    snap: Snap,
    site: Site,
    position: ListItemPosition = ListItemPosition.MIDDLE,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val haptic = LocalHapticFeedback.current
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }
    val gradient = remember(siteColor, siteColor2) {
        Brush.linearGradient(listOf(siteColor, siteColor2))
    }
    val timeLabel = getRelativeTimeLabel(snap.timestamp)
    val shape = getListItemShape(position)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.BOUNCY_DAMPING,
            stiffness = ExpressiveSpring.BOUNCY_STIFFNESS,
        ),
        label = "logItemScale",
    )

    val backgroundColor by animateColorAsState(
        targetValue = siteColor.copy(alpha = 0.08f),
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
        label = "logItemBg",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 16.dp)
            .padding(vertical = 2.dp),
        color = backgroundColor,
        shape = shape,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            // Gradient color indicator - larger and more prominent
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = getTitle(site).firstOrNull()?.uppercase() ?: "W",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getTitle(site),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = snap.contentSize.toLong().readableFileSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteClick()
                },
                modifier = Modifier.size(spacing.touchTarget),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(Res.string.delete_snapshot_title),
                    modifier = Modifier.size(18.dp),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.logs_hero_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.logs_hero_subtitle, versionsCount, siteCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

/**
 * Stylish date header chip for section dividers.
 */
@Composable
fun DateHeader(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// Helper to keep logic shared
internal fun getTitle(site: Site?): String {
    if (site == null) return ""

    return if (site.title.isNullOrBlank()) {
        site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
    } else {
        site.title
    }
}
