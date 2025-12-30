package com.bernaferrari.changedetection.ui.compose

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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.lerp
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ripple
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.TypographyTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class SyncStatus {
    LOADING, OK, ERROR
}

@Composable
fun MainCard(
    modifier: Modifier = Modifier,
    site: Site,
    lastSnap: Snap?,
    syncStatus: SyncStatus = SyncStatus.OK,
    isQuickMonitored: Boolean = false,
    onReloadClick: () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onOptionsClick: (() -> Unit)? = null,
    onQuickMonitorClick: (() -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
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

    var lastSyncLabel by remember { mutableStateOf("") }
    var lastChangeLabel by remember { mutableStateOf("") }
    var fileSizeLabel by remember { mutableStateOf("") }

    val errorLabel = stringResource(R.string.error)
    val nothingYetLabel = stringResource(R.string.nothing_yet)
    val syncingLabel = stringResource(R.string.syncing)

    LaunchedEffect(site.timestamp, syncStatus, lastSnap?.contentSize) {
        updateTimeText(site.timestamp).collect {
            lastSyncLabel = when (syncStatus) {
                SyncStatus.ERROR -> "$errorLabel • ${site.timestamp.convertTimestampToDate()}"
                SyncStatus.LOADING -> syncingLabel
                else -> site.timestamp.convertTimestampToDate()
            }
            fileSizeLabel = lastSnap?.contentSize?.readableFileSize().orEmpty()
        }
    }

    LaunchedEffect(lastSnap?.timestamp) {
        if (lastSnap == null) {
            lastChangeLabel = nothingYetLabel
        } else {
            updateTimeText(lastSnap.timestamp).collect {
                lastChangeLabel = lastSnap.timestamp.convertTimestampToDate()
            }
        }
    }

    val siteColor = Color(site.colors.first)
    val siteColor2 = Color(site.colors.second)
    val gradient = Brush.linearGradient(listOf(siteColor, siteColor2))

    // Subtlety: Tint the background slightly with the site color
    val containerColor = if (isQuickMonitored) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        // Very subtle tint
        val base = MaterialTheme.colorScheme.surfaceContainer
        lerp(base, siteColor, 0.04f)
    }

    val borderColor = if (isQuickMonitored) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        // Subtle colored border
        siteColor.copy(alpha = 0.15f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        waitForUpOrCancellation()
                        isPressed = false
                    }
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = siteColor),  // M3 Expressive: Colored ripple
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
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
                                syncStatus == SyncStatus.LOADING -> Icons.Outlined.Sync
                                syncStatus == SyncStatus.ERROR -> Icons.Filled.Error
                                isQuickMonitored -> Icons.Filled.Bolt
                                else -> Icons.Outlined.Sync
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
                            text = site.title.takeIf { !it.isNullOrBlank() }
                                ?: site.url.replace(Regex("^https?://(www\\.)?"), ""),
                            style = TypographyTokens.cardTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = site.url.replace(Regex("^https?://"), ""),
                            style = TypographyTokens.captionText,
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
                                .combinedClickable(onClick = { onOptionsClick() }),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options),
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
                        icon = Icons.Filled.History,
                        text = lastChangeLabel,
                        color = siteColor // Use site color for emphasis
                    )

                    VerticalDivider(
                        modifier = Modifier.height(12.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Size / Sync
                    MetadataItem(
                        icon = if (fileSizeLabel.isNotEmpty()) Icons.AutoMirrored.Filled.TrendingUp else Icons.Outlined.Sync,
                        text = if (fileSizeLabel.isNotEmpty()) fileSizeLabel else lastSyncLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (syncStatus == SyncStatus.ERROR) {
                    Text(
                        text = stringResource(R.string.missing_internet),
                        style = TypographyTokens.captionText,
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
            style = TypographyTokens.captionText,
            color = color,
            maxLines = 1
        )
    }
}

private fun updateTimeText(timestamp: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        val diffMillis = kotlin.math.abs(System.currentTimeMillis() - timestamp)
        val seconds = diffMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        val interval = when {
            hours >= 24 -> 3_600L * 24
            hours > 0 -> 3_600L
            minutes > 0 -> 60L
            else -> 30L
        }
        delay(interval * 1000)
    }
}

@Preview(showBackground = true)
@Composable
private fun MainCardPreview() {
    ChangeDetectionTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            MainCard(
                site = Site(
                    title = "TechCrunch",
                    url = "https://techcrunch.com",
                    timestamp = System.currentTimeMillis() - 1800000,
                    id = "1",
                    isSuccessful = true,
                    isBrowser = false,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    notes = "",
                    colors = ColorGroup(
                        0xFFFB1F0C.toInt(),
                        0xFFFB1F0C.toInt()
                    ), // TechCrunch Green-ish? No, Green is usually TC... wait TC is Green. Let's use generic.
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = Snap(
                    siteId = "1",
                    timestamp = System.currentTimeMillis() - 3600000,
                    contentSize = 15420,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                syncStatus = SyncStatus.OK,
                isQuickMonitored = false,
                onReloadClick = {},
                onQuickMonitorClick = {},
                onOptionsClick = {},
                onClick = {}
            )
        }
    }
}
