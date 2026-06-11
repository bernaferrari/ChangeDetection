/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.material3.ripple
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.card_site_monitoring_active
import com.bernaferrari.changedetection.shared.generated.resources.card_last_sync
import com.bernaferrari.changedetection.shared.generated.resources.card_last_change
import com.bernaferrari.changedetection.shared.generated.resources.card_sync_failed
import com.bernaferrari.changedetection.shared.generated.resources.card_refresh_site
import com.bernaferrari.changedetection.shared.generated.resources.card_syncing
import org.jetbrains.compose.resources.stringResource

enum class SyncStatus {
    LOADING,
    OK,
    ERROR,
}

/**
 * M3 Expressive Split Card - rounded gradient header + 50/50 metadata footer.
 * Features a prominent refresh button with guaranteed full rotation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SharedMainCard(
    modifier: Modifier = Modifier,
    site: Site,
    lastSnap: Snap?,
    syncStatus: SyncStatus = SyncStatus.OK,
    isQuickMonitored: Boolean = false,
    lastSyncValue: String,
    lastChangeValue: String,
    fileSizeValue: String,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onRefreshClick: (() -> Unit)? = null,
    onOptionsClick: (() -> Unit)? = null,
) {
    var isPressed by remember { mutableStateOf(false) }
    val isLoading = syncStatus == SyncStatus.LOADING
    val isDark = isSystemInDarkTheme()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "card_scale",
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
    )

    // Rotation animation that always completes a full rotation
    val rotationAnimatable = remember { Animatable(0f) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            // Start spinning continuously
            while (true) {
                rotationAnimatable.animateTo(
                    targetValue = rotationAnimatable.value + 360f,
                    animationSpec = tween(800, easing = LinearEasing),
                )
            }
        } else {
            // Complete the current rotation to nearest 360
            val currentRotation = rotationAnimatable.value
            val remainder = currentRotation % 360f
            if (remainder > 0f) {
                // Finish the rotation
                val targetRotation = currentRotation + (360f - remainder)
                rotationAnimatable.animateTo(
                    targetValue = targetRotation,
                    animationSpec = tween(
                        durationMillis = ((360f - remainder) / 360f * 800f).toInt(),
                        easing = LinearEasing,
                    ),
                )
            }
        }
    }

    // Site colors
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }

    // Gradient
    val gradient = remember(siteColor, siteColor2) {
        Brush.linearGradient(
            colors = listOf(siteColor, siteColor2),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    // Display info
    val displayTitle = remember(site.title, site.url) {
        site.title?.takeIf { it.isNotBlank() }
            ?: site.url.replace(Regex("^https?://(www\\.)?"), "").substringBefore("/")
    }

    val displayDomain = remember(site.url) {
        site.url.replace(Regex("^https?://(www\\.)?"), "").substringBefore("/")
    }

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    // Update isPressed based on actual press state
    LaunchedEffect(pressed) {
        isPressed = pressed
    }

    // Border color based on theme
    val borderColor = if (isDark) {
        Color.Black.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    onOptionsClick?.invoke()
                },
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            // Edge-to-edge gradient header with top rounded corners only
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(gradient)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Title + Domain
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isQuickMonitored) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = "Turbo",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = displayDomain,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Refresh button - prominent and actionable
                    if (onRefreshClick != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(
                                    enabled = !isLoading,
                                    onClick = onRefreshClick,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(rotationAnimatable.value),
                            )
                        }
                    }
                }
            }

            // Metadata section with padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Last Sync row
                MetadataRow(
                    icon = if (syncStatus == SyncStatus.ERROR) Icons.Rounded.Error else Icons.Rounded.Sync,
                    label = "Last Sync",
                    value = if (syncStatus == SyncStatus.ERROR) "Sync failed" else lastSyncValue,
                    secondaryValue = fileSizeValue.takeIf { it.isNotBlank() && syncStatus != SyncStatus.ERROR },
                    accentColor = if (syncStatus == SyncStatus.ERROR) MaterialTheme.colorScheme.error else siteColor,
                    isError = syncStatus == SyncStatus.ERROR,
                )

                // Subtle divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )

                // Last Change row
                MetadataRow(
                    icon = Icons.Rounded.History,
                    label = "Last Change",
                    value = lastChangeValue,
                    secondaryValue = null,
                    accentColor = siteColor,
                )
            }
        }
    }
}

/**
 * Horizontal metadata row with icon, label, and value.
 */
@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String,
    secondaryValue: String?,
    accentColor: Color,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon with solid accent background and white icon for better contrast
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        // Label and value
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (secondaryValue != null) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        text = secondaryValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
