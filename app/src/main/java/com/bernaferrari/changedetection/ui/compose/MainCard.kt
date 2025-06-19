package com.bernaferrari.changedetection.ui.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import kotlin.math.abs

enum class SyncStatus {
    LOADING, OK, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainCard(
    modifier: Modifier = Modifier,
    site: Site,
    lastSnap: Snap?,
    syncStatus: SyncStatus = SyncStatus.OK,
    onReloadClick: () -> Unit,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current

    // State for time updates
    var lastSyncText by remember { mutableStateOf("") }
    var lastChangeText by remember { mutableStateOf("") }

    // Enhanced animations - keep spinning while loading
    val infiniteTransition = rememberInfiniteTransition(label = "sync_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Determine if should be spinning based on sync status
    val isSpinning = syncStatus == SyncStatus.LOADING

    // Get string resources
    val syncingText = stringResource(R.string.syncing)
    val errorText = stringResource(R.string.error)
    val nothingYetText = stringResource(R.string.nothing_yet)
    val lastSyncTitle = stringResource(R.string.last_sync)
    val lastChangeTitle = stringResource(R.string.last_change)

    // Update time texts periodically
    LaunchedEffect(site.timestamp, syncStatus) {
        updateTimeText(site.timestamp).collect {
            lastSyncText = if (syncStatus == SyncStatus.ERROR) {
                "${site.timestamp.convertTimestampToDate()} – $errorText"
            } else if (syncStatus == SyncStatus.LOADING) {
                syncingText
            } else {
                "${site.timestamp.convertTimestampToDate()} – ${lastSnap?.contentSize?.readableFileSize() ?: ""}"
            }
        }
    }

    LaunchedEffect(lastSnap?.timestamp) {
        if (lastSnap != null) {
            updateTimeText(lastSnap.timestamp).collect {
                lastChangeText = lastSnap.timestamp.convertTimestampToDate()
            }
        } else {
            lastChangeText = nothingYetText
        }
    }

    // Enhanced color scheme matching original design
    val (gradientColors, reloadIconColor, separatorColor) = when {
        syncStatus == SyncStatus.ERROR && site.isSyncEnabled -> {
            val errorColor = Color(0xFFFF5449)
            Triple(
                listOf(errorColor, errorColor.copy(red = 0.8f)),
                Color.White,
                Color(0x20000000)
            )
        }

        !site.isSyncEnabled -> {
            val disabledColor = Color(ContextCompat.getColor(context, R.color.md_grey_600))
            Triple(
                listOf(disabledColor, disabledColor),
                Color.White,
                Color(0x20000000)
            )
        }

        else -> {
            val primaryColor = Color(site.colors.first)
            val secondaryColor = Color(site.colors.second)
            Triple(
                listOf(primaryColor, secondaryColor),
                Color.White,
                Color(0x20000000)
            )
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            // Gradient header section matching original design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(colors = gradientColors),
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val title = if (site.title.isNullOrBlank()) {
                                site.url.replaceFirst(
                                    "^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(),
                                    ""
                                )
                            } else {
                                site.title
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = site.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Reload button matching original design
                        Surface(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    onReloadClick()
                                },
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = "Reload",
                                    tint = gradientColors.first(),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .let {
                                            if (isSpinning) it.rotate(rotation) else it
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Status section matching original layout
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Last sync status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .let {
                                        if (isSpinning) it.rotate(rotation) else it
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lastSyncTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = lastSyncText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Separator line matching original
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = separatorColor
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Last change status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = lastChangeTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = lastChangeText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

}

// Helper data class for multiple return values
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

@Composable
private fun StatusIndicator(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false,
    rotation: Float = 0f
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .let { if (isAnimating) it.rotate(rotation) else it }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RotatingStatusIndicator(
    imageVector: ImageVector,
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = false,
    rotation: Float = 0f,
    onIconClick: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable { onIconClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .let { if (isAnimating) it.rotate(rotation) else it }
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun updateTimeText(timestamp: Long): Flow<Unit> = flow {
    while (true) {
        emit(Unit)

        val delay = abs(System.currentTimeMillis() - timestamp)
        val seconds = delay / 1000
        val min = seconds / 60
        val hours = min / 60

        val timeToWait = when {
            hours / 24 > 0 -> 3600 * 24
            hours > 0 -> 3600
            min > 0 -> 60
            else -> 30
        }

        val ttl = (timeToWait * 1000).toFloat()
        val finalTimeToWait = ((1 - ((delay / ttl) % 1)) * ttl).toLong()

        delay(finalTimeToWait)
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
                    timestamp = System.currentTimeMillis() - 1800000, // 30 min ago (last sync)
                    id = "1",
                    isSuccessful = true,
                    isBrowser = false,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    notes = "",
                    colors = ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt()),
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = Snap(
                    siteId = "1",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago (last change)
                    contentSize = 15420,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                syncStatus = SyncStatus.OK,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainCardLoadingPreview() {
    ChangeDetectionTheme {
        Surface {
            MainCard(
                site = Site(
                    title = "GitHub",
                    url = "https://github.com",
                    timestamp = System.currentTimeMillis() - 600000, // 10 min ago (last sync)
                    id = "2",
                    isSuccessful = true,
                    isBrowser = false,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    notes = "",
                    colors = ColorGroup(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt()),
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = Snap(
                    siteId = "2",
                    timestamp = System.currentTimeMillis() - 900000, // 15 min ago (last change)
                    contentSize = 8750,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                syncStatus = SyncStatus.LOADING,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MainCardErrorPreview() {
    ChangeDetectionTheme {
        Surface {
            MainCard(
                site = Site(
                    title = "Example Site",
                    url = "https://example.com",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago (last sync)
                    id = "3",
                    isSuccessful = false,
                    isBrowser = false,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = true,
                    isNotificationEnabled = true,
                    notes = "",
                    colors = ColorGroup(0xFFFF5722.toInt(), 0xFFFF9800.toInt()),
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = Snap(
                    siteId = "3",
                    timestamp = System.currentTimeMillis() - 10800000, // 3 hours ago (last change)
                    contentSize = 12340,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                syncStatus = SyncStatus.ERROR,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun MainCardDisabledPreview() {
    ChangeDetectionTheme {
        Surface {
            MainCard(
                site = Site(
                    title = "Disabled Site",
                    url = "https://disabled.com",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago (last sync)
                    id = "4",
                    isSuccessful = true,
                    isBrowser = false,
                    threshold = 0,
                    isRead = false,
                    isSyncEnabled = false,
                    isNotificationEnabled = true,
                    notes = "",
                    colors = ColorGroup(0xFF9C27B0.toInt(), 0xFFE91E63.toInt()),
                    constraints = ConstraintsRequired(false, false, false, false)
                ),
                lastSnap = Snap(
                    siteId = "4",
                    timestamp = System.currentTimeMillis() - 172800000, // 2 days ago (last change)
                    contentSize = 9876,
                    contentType = "text/html",
                    contentCharset = "UTF-8"
                ),
                syncStatus = SyncStatus.OK,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}
