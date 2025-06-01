package com.bernaferrari.changedetection.ui.compose

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bernaferrari.changedetection.R
import com.bernaferrari.changedetection.extensions.convertTimestampToDate
import com.bernaferrari.changedetection.extensions.readableFileSize
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.Snap
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
    site: Site,
    lastSnap: Snap?,
    syncStatus: SyncStatus = SyncStatus.OK,
    onReloadClick: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State for time updates
    var lastSyncText by remember { mutableStateOf("") }
    var lastChangeText by remember { mutableStateOf("") }
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "sync_animation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Update time texts periodically
    LaunchedEffect(site.timestamp, syncStatus) {
        updateTimeText(site.timestamp).collect {
            lastSyncText = if (syncStatus == SyncStatus.ERROR) {
                "${site.timestamp.convertTimestampToDate()} – ${context.getString(R.string.error)}"
            } else if (syncStatus == SyncStatus.LOADING) {
                stringResource(R.string.syncing)
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
            lastChangeText = context.getString(R.string.nothing_yet)
        }
    }
    
    // Color scheme based on status and site state
    val (cardColor, backgroundBrush, reloadTint) = when {
        syncStatus == SyncStatus.ERROR && site.isSyncEnabled -> {
            val errorColor = Color(0xfff04a43.toInt())
            Triple(
                CardDefaults.cardColors(containerColor = errorColor),
                null,
                errorColor
            )
        }
        !site.isSyncEnabled -> {
            val greyColor = Color(ContextCompat.getColor(context, R.color.md_grey_700))
            Triple(
                CardDefaults.cardColors(containerColor = greyColor),
                null,
                greyColor
            )
        }
        else -> {
            val brush = Brush.linearGradient(
                colors = listOf(
                    Color(site.colors.first),
                    Color(site.colors.second)
                )
            )
            Triple(
                CardDefaults.cardColors(),
                brush,
                Color(site.colors.first)
            )
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = cardColor,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            if (backgroundBrush != null) {
                Spacer(
                    modifier = Modifier
                        .matchParentSize()
                        .background(backgroundBrush, RoundedCornerShape(8.dp))
                )
            }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header with title and reload button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val title = if (site.title.isNullOrBlank()) {
                            site.url.replaceFirst("^(http[s]?://www\\.|http[s]?://|www\\.)".toRegex(), "")
                        } else {
                            site.title ?: ""
                        }
                        
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (backgroundBrush != null) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = site.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backgroundBrush != null) Color.White.copy(alpha = 0.8f) 
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Reload button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onReloadClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.vector_anim_reload),
                            contentDescription = "Reload",
                            tint = reloadTint,
                            modifier = if (syncStatus == SyncStatus.LOADING) 
                                Modifier.rotate(rotation) else Modifier
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status indicators
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Last sync info
                    StatusIndicator(
                        icon = painterResource(R.drawable.vector_anim_sync),
                        text = lastSyncText,
                        backgroundColor = if (backgroundBrush != null) Color(site.colors.second)
                                        else Color(ContextCompat.getColor(context, R.color.md_grey_500)),
                        textColor = if (backgroundBrush != null) Color.White.copy(alpha = 0.9f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        isAnimating = syncStatus == SyncStatus.LOADING,
                        rotation = rotation,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Last change info
                    StatusIndicator(
                        icon = painterResource(R.drawable.ic_change_history_black_24dp),
                        text = lastChangeText,
                        backgroundColor = if (backgroundBrush != null) Color(site.colors.second)
                                        else Color(ContextCompat.getColor(context, R.color.md_grey_500)),
                        textColor = if (backgroundBrush != null) Color.White.copy(alpha = 0.9f)
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    icon: androidx.compose.ui.graphics.painter.Painter,
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
                painter = icon,
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

import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
private fun MainCardPreview() {
    MaterialTheme {
        Surface {
            MainCard(
                site = Site(
                    id = "1",
                    title = "TechCrunch",
                    url = "https://techcrunch.com",
                    timestamp = System.currentTimeMillis() - 1800000, // 30 min ago
                    isSuccessful = true,
                    isSyncEnabled = true,
                    colors = ColorGroup(0xFF2196F3.toInt(), 0xFF21CBF3.toInt())
                ),
                lastSnap = Snap(
                    siteId = "1",
                    timestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
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
    MaterialTheme {
        Surface {
            MainCard(
                site = Site(
                    id = "2",
                    title = "GitHub",
                    url = "https://github.com",
                    timestamp = System.currentTimeMillis(),
                    isSuccessful = true,
                    isSyncEnabled = true,
                    colors = ColorGroup(0xFF4CAF50.toInt(), 0xFF8BC34A.toInt())
                ),
                lastSnap = null,
                syncStatus = SyncStatus.LOADING,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainCardErrorPreview() {
    MaterialTheme {
        Surface {
            MainCard(
                site = Site(
                    id = "3",
                    title = "Example Site",
                    url = "https://example.com",
                    timestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    isSuccessful = false,
                    isSyncEnabled = true,
                    colors = ColorGroup(0xFFFF5722.toInt(), 0xFFFF9800.toInt())
                ),
                lastSnap = null,
                syncStatus = SyncStatus.ERROR,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainCardDisabledPreview() {
    MaterialTheme {
        Surface {
            MainCard(
                site = Site(
                    id = "4",
                    title = "Disabled Site",
                    url = "https://disabled.com",
                    timestamp = System.currentTimeMillis() - 86400000, // 1 day ago
                    isSuccessful = true,
                    isSyncEnabled = false,
                    colors = ColorGroup(0xFF9C27B0.toInt(), 0xFFE91E63.toInt())
                ),
                lastSnap = null,
                syncStatus = SyncStatus.OK,
                onReloadClick = { },
                onClick = { }
            )
        }
    }
}
