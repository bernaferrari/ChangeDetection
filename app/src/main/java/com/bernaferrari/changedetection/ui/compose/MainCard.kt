package com.bernaferrari.changedetection.ui.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.ColorGroup
import com.bernaferrari.changedetection.repo.ConstraintsRequired
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
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
    onReloadLongClick: () -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val haptics = LocalHapticFeedback.current

    var lastSyncLabel by remember { mutableStateOf("") }
    var lastChangeLabel by remember { mutableStateOf("") }

    val syncingLabel = stringResource(R.string.syncing)
    val errorLabel = stringResource(R.string.error)
    val nothingYetLabel = stringResource(R.string.nothing_yet)
    val lastSyncTitle = stringResource(R.string.last_sync)
    val lastChangeTitle = stringResource(R.string.last_change)

    LaunchedEffect(site.timestamp, syncStatus, lastSnap?.contentSize) {
        updateTimeText(site.timestamp).collect {
            lastSyncLabel = when {
                syncStatus == SyncStatus.ERROR ->
                    "${site.timestamp.convertTimestampToDate()} • $errorLabel"
                syncStatus == SyncStatus.LOADING -> syncingLabel
                else -> {
                    val size = lastSnap?.contentSize?.readableFileSize().orEmpty()
                    if (size.isNotEmpty()) "${site.timestamp.convertTimestampToDate()} • $size"
                    else site.timestamp.convertTimestampToDate()
                }
            }
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

    val accentStart = Color(site.colors.first)
    val accentEnd = Color(site.colors.second)
    val gradient = listOf(accentStart, accentEnd)

    val statusChip = when (syncStatus) {
        SyncStatus.LOADING -> StatusChipData(
            icon = Icons.Outlined.Sync,
            label = stringResource(R.string.syncing),
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            animate = true
        )
        SyncStatus.ERROR -> StatusChipData(
            icon = Icons.Filled.CheckCircle,
            label = stringResource(R.string.error),
            container = MaterialTheme.colorScheme.errorContainer,
            content = MaterialTheme.colorScheme.onErrorContainer
        )
        SyncStatus.OK -> StatusChipData(
            icon = Icons.Filled.CheckCircle,
            label = stringResource(R.string.all_good),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    val hasRecentChange = lastSnap?.let {
        System.currentTimeMillis() - it.timestamp < 24 * 60 * 60 * 1000L
    } == true

    val border = if (isQuickMonitored) {
        BorderStroke(1.25.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            // Hero banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(gradient),
                        RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.align(Alignment.CenterStart),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = site.title.takeIf { !it.isNullOrBlank() }
                                    ?: site.url.trimStart('h', 't', 'p', 's', ':', '/', '/').removePrefix("www."),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = site.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        ReloadButton(
                            isQuickMonitored = isQuickMonitored,
                            gradient = gradient,
                            isLoading = syncStatus == SyncStatus.LOADING,
                            onClick = onReloadClick,
                            onLongClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReloadLongClick()
                            },
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusAssistChip(data = statusChip)

                        if (isQuickMonitored) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(text = stringResource(R.string.quick_monitor_active)) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    disabledLeadingIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }

                        if (hasRecentChange) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(text = stringResource(R.string.recent_changes)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = Color.White.copy(alpha = 0.2f),
                                    disabledLabelColor = Color.White,
                                    disabledLeadingIconContentColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    InfoColumn(
                        icon = Icons.Outlined.Sync,
                        title = lastSyncTitle,
                        value = lastSyncLabel,
                        isLoading = syncStatus == SyncStatus.LOADING,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                    InfoColumn(
                        icon = Icons.Filled.History,
                        title = lastChangeTitle,
                        value = lastChangeLabel,
                        isLoading = false,
                        modifier = Modifier.fillMaxWidth(0.5f)
                    )
                }

                if (!site.notes.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = site.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class StatusChipData(
    val icon: ImageVector,
    val label: String,
    val container: Color,
    val content: Color,
    val animate: Boolean = false
)

@Composable
private fun StatusAssistChip(data: StatusChipData) {
    val rotation = if (data.animate) {
        val infinite = rememberInfiniteTransition(label = "status_rotation")
        val animated by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "status_spin"
        )
        animated
    } else 0f

    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = data.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .let { if (data.animate) it.rotate(rotation) else it }
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = data.container,
            disabledLabelColor = data.content,
            disabledLeadingIconContentColor = data.content
        )
    )
}

@Composable
private fun InfoColumn(
    icon: ImageVector,
    title: String,
    value: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = if (isLoading) {
        val infinite = rememberInfiniteTransition(label = "info_spin")
        val animated by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "info_rotation"
        )
        animated
    } else 0f

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .let { if (isLoading) it.rotate(rotation) else it },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReloadButton(
    isQuickMonitored: Boolean,
    gradient: List<Color>,
    isLoading: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation = if (isLoading) {
        val infinite = rememberInfiniteTransition(label = "reload_spin")
        val animated by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "reload_rotation"
        )
        animated
    } else 0f

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (isQuickMonitored) 0.25f else 0.18f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = CircleShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isQuickMonitored) Icons.Filled.Bolt else Icons.Outlined.Sync,
            contentDescription = if (isQuickMonitored) "Stop quick monitoring" else "Reload site",
            tint = if (isQuickMonitored) Color.White else gradient.first(),
            modifier = Modifier
                .size(22.dp)
                .let { if (isLoading) it.rotate(rotation) else it }
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
        Surface {
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
                    notes = "Watching IPO coverage.",
                    colors = ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
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
                isQuickMonitored = true,
                onReloadClick = {},
                onReloadLongClick = {},
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MainCardLoadingPreview() {
    ChangeDetectionTheme {
        Surface {
            MainCard(
                site = Site(
                    title = "Government Portal",
                    url = "https://gov.example",
                    timestamp = System.currentTimeMillis(),
                    id = "2",
                    isSuccessful = true,
                    isBrowser = true,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    notes = "Requires login",
                    colors = ColorGroup(0xFF009688.toInt(), 0xFF4DB6AC.toInt()),
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = null,
                syncStatus = SyncStatus.LOADING,
                onReloadClick = {},
                onReloadLongClick = {},
                onClick = {}
            )
        }
    }
}
