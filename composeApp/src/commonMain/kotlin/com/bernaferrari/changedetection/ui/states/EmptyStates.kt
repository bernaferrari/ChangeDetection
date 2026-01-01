package com.bernaferrari.changedetection.ui.states

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun EmptyStateScreen(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    showAnimation: Boolean = true,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (showAnimation) {
            kotlinx.coroutines.delay(300)
            visible = true
        } else {
            visible = true
        }
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + 
                slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Icon
            EmptyStateIcon(icon = icon)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400, delayMillis = 300)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "title_animation"
            ) { titleText ->
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            AnimatedContent(
                targetState = description,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400, delayMillis = 500)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "description_animation"
            ) { descText ->
                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons
            var buttonsVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(visible) {
                if (visible) {
                    kotlinx.coroutines.delay(800)
                    buttonsVisible = true
                }
            }
            
            AnimatedVisibility(
                visible = buttonsVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Action
                    if (actionText != null && onActionClick != null) {
                        Button(
                            onClick = onActionClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = actionText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                    
                    // Secondary Action
                    if (secondaryActionText != null && onSecondaryActionClick != null) {
                        OutlinedButton(
                            onClick = onSecondaryActionClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = secondaryActionText,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "empty_state_float")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_offset"
    )
    
    // Pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Card(
        modifier = modifier
            .size(100.dp)
            .offset(y = floatingOffset.dp)
            .scale(pulseScale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Specific Empty States
@Composable
fun NoSitesEmptyState(
    onAddFirstSite: () -> Unit,
    onImportSites: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        icon = Icons.Default.Visibility,
        title = "Start Monitoring",
        description = "Add your first website to begin tracking changes. Monitor prices, availability, news updates, and more with intelligent change detection.",
        actionText = "Add Your First Site",
        onActionClick = onAddFirstSite,
        secondaryActionText = "Import Sites",
        onSecondaryActionClick = onImportSites,
        modifier = modifier
    )
}

@Composable
fun NoSnapshotsEmptyState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        icon = Icons.Default.History,
        title = "No History Yet",
        description = "This site hasn't been monitored yet. Snapshots will appear here once the first scan completes. You can manually trigger a sync or wait for the automatic check.",
        actionText = "Check for Changes Now",
        onActionClick = onRefresh,
        modifier = modifier
    )
}

@Composable
fun SearchEmptyState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        icon = Icons.Default.SearchOff,
        title = "No Results Found",
        description = "No sites match \"$searchQuery\". Try different keywords or check your spelling. You can also add a new site if you're looking for something specific.",
        actionText = "Clear Search",
        onActionClick = onClearSearch,
        modifier = modifier
    )
}

@Composable
fun ErrorStateScreen(
    error: ErrorState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateScreen(
        icon = error.icon,
        title = error.title,
        description = error.description,
        actionText = error.primaryActionText,
        onActionClick = onRetry,
        secondaryActionText = "Dismiss",
        onSecondaryActionClick = onDismiss,
        modifier = modifier
    )
}

// Error State Types
sealed class ErrorState(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val primaryActionText: String
) {
    object NetworkError : ErrorState(
        icon = Icons.Default.CloudOff,
        title = "Connection Failed",
        description = "Unable to connect to the internet. Check your network connection and try again. Some features may be limited in offline mode.",
        primaryActionText = "Retry"
    )
    
    object ServerError : ErrorState(
        icon = Icons.Default.Error,
        title = "Server Unavailable",
        description = "The monitoring service is temporarily unavailable. This may be due to maintenance or high traffic. Please try again in a few moments.",
        primaryActionText = "Try Again"
    )
    
    object SyncError : ErrorState(
        icon = Icons.Default.SyncProblem,
        title = "Sync Failed",
        description = "Unable to synchronize your sites. Some data may be outdated. Check your internet connection and sync settings.",
        primaryActionText = "Sync Now"
    )
    
    object PermissionError : ErrorState(
        icon = Icons.Default.Block,
        title = "Permission Required",
        description = "This feature requires additional permissions to work properly. Please grant the necessary permissions in your device settings.",
        primaryActionText = "Grant Permission"
    )
    
    object StorageError : ErrorState(
        icon = Icons.Default.Storage,
        title = "Storage Full",
        description = "Your device is running low on storage space. Free up some space or clean old snapshots to continue monitoring.",
        primaryActionText = "Manage Storage"
    )
}

// Loading States
@Composable
fun LoadingStateScreen(
    title: String = "Loading...",
    description: String = "Please wait while we prepare your content.",
    showProgress: Boolean = false,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated loading icon
        val infiniteTransition = rememberInfiniteTransition(label = "loading_rotation")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        
        Card(
            modifier = Modifier
                .size(80.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (showProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}