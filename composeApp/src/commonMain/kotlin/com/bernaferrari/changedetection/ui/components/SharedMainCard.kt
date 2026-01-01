package com.bernaferrari.changedetection.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.connection_error
import com.bernaferrari.changedetection.shared.generated.resources.last_change
import com.bernaferrari.changedetection.shared.generated.resources.last_sync
import com.bernaferrari.changedetection.shared.generated.resources.more_options
import org.jetbrains.compose.resources.stringResource
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.SharedTypography

enum class SyncStatus {
    LOADING, OK, ERROR
}

/**
 * Main card component for displaying a monitored site.
 * This version uses Compose Multiplatform resources for strings.
 */
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
    onOptionsClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    // Spring animation for touch response
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "card_scale",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Remember site colors and gradient to avoid redundant calculations
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }
    val gradient = remember(siteColor, siteColor2) {
        Brush.linearGradient(listOf(siteColor, siteColor2))
    }

    // Subtle tint with site color - using surfaceContainer as base
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val containerColor = remember(isQuickMonitored, siteColor, surfaceContainer) {
        if (isQuickMonitored) {
            Color.Unspecified // Let the Card use surfaceContainerHigh
        } else {
            lerp(surfaceContainer, siteColor, 0.04f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = remember(isQuickMonitored, siteColor, primaryColor) {
        if (isQuickMonitored) {
            primaryColor.copy(alpha = 0.5f)
        } else {
            siteColor.copy(alpha = 0.15f)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isQuickMonitored) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                containerColor
            }
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Site Icon / Indicator
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                // Initial
                Text(
                    text = site.title?.firstOrNull()?.uppercase() ?: "W",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.95f)
                )

                // Overlay for Status
                if (syncStatus != SyncStatus.OK || isQuickMonitored) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val rotation = if (syncStatus == SyncStatus.LOADING) {
                            val infinite = rememberInfiniteTransition(label = "status_spin")
                            infinite.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "status_rotation"
                            ).value
                        } else 0f

                        Icon(
                            imageVector = when {
                                syncStatus == SyncStatus.LOADING -> Icons.Filled.Refresh
                                syncStatus == SyncStatus.ERROR -> Icons.Filled.Warning
                                isQuickMonitored -> Icons.Filled.Star
                                else -> Icons.Filled.Refresh
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            // Right: Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header: Title and Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = site.title?.takeIf { it.isNotBlank() }
                                ?: site.url.replace(Regex("^https?://(www\\.)?"), ""),
                            style = SharedTypography.cardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = site.url.replace(Regex("^https?://"), ""),
                            style = SharedTypography.captionText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (onOptionsClick != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onOptionsClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Metadata Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Last Change
                    MetadataItem(
                        icon = Icons.Filled.Info,
                        text = stringResource(Res.string.last_change, lastChangeValue),
                        color = siteColor
                    )

                    // Size / Sync
                    MetadataItem(
                        icon = Icons.Filled.Refresh,
                        text = if (fileSizeValue.isNotEmpty()) fileSizeValue else stringResource(
                            Res.string.last_sync,
                            lastSyncValue
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (syncStatus == SyncStatus.ERROR) {
                    Text(
                        text = stringResource(Res.string.connection_error),
                        style = SharedTypography.captionText,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = SharedTypography.captionText,
            color = color,
            maxLines = 1
        )
    }
}
