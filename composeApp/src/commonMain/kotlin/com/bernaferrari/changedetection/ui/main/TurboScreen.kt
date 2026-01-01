package com.bernaferrari.changedetection.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

private data class IntervalOption(val seconds: Int, val label: String)

private val intervalOptions = listOf(
    IntervalOption(15, "15 sec"),
    IntervalOption(30, "30 sec"),
    IntervalOption(60, "1 min"),
    IntervalOption(120, "2 min"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurboScreen(
    sites: List<SiteAndLastSnap>,
    quickMonitorState: QuickMonitorState,
    onStartTurbo: (Set<String>, Int) -> Unit,
    onStopTurbo: () -> Unit
) {
    val isRunning = quickMonitorState.isActive
    var selectedIds by remember(quickMonitorState.siteIds) {
        mutableStateOf(if (quickMonitorState.isActive) quickMonitorState.siteIds else emptySet())
    }
    var intervalSeconds by remember { mutableIntStateOf(60) }
    var showHelpDialog by remember { mutableStateOf(false) }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(Res.string.turbo_help_title)) },
            text = {
                Text(stringResource(Res.string.turbo_help_text))
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(Res.string.got_it))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.turbo_mode),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = stringResource(Res.string.how_it_works),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (sites.isNotEmpty()) {
                StartStopFab(
                    isRunning = isRunning,
                    enabled = isRunning || selectedIds.isNotEmpty(),
                    onClick = {
                        if (isRunning) {
                            onStopTurbo()
                        } else {
                            onStartTurbo(selectedIds, intervalSeconds)
                        }
                    }
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        if (sites.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Hero section
                item {
                    TurboHero(
                        isRunning = isRunning,
                        selectedCount = selectedIds.size,
                        intervalSeconds = intervalSeconds,
                        onIntervalChange = { intervalSeconds = it },
                        canChangeInterval = !isRunning
                    )
                }

                // Sites header
                item {
                    Text(
                        text = if (isRunning) stringResource(Res.string.monitoring) else stringResource(Res.string.select_sites),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                // Sites
                items(sites) { siteAndSnap ->
                    val isSelected = selectedIds.contains(siteAndSnap.site.id)
                    val enabled = !isRunning

                    SiteRow(
                        site = siteAndSnap.site,
                        isSelected = isSelected,
                        enabled = enabled,
                        onClick = {
                            val newIds = selectedIds.toMutableSet()
                            if (isSelected) newIds.remove(siteAndSnap.site.id) else newIds.add(siteAndSnap.site.id)
                            selectedIds = newIds
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TurboHero(
    isRunning: Boolean,
    selectedCount: Int,
    intervalSeconds: Int,
    onIntervalChange: (Int) -> Unit,
    canChangeInterval: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        targetValue = if (isRunning)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS
        ),
        label = "containerColor"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated bolt icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                val infiniteTransition = rememberInfiniteTransition(label = "rings")
                val ring1 by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ring1"
                )
                val ring1Alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "ring1Alpha"
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(ring1)
                        .alpha(ring1Alpha)
                        .border(
                            width = 3.dp,
                            color = primaryColor,
                            shape = CircleShape
                        )
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                val boltScale by animateFloatAsState(
                    targetValue = if (isRunning) 1f else 0.9f,
                    animationSpec = tween(300),
                    label = "boltScale"
                )

                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (isRunning)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(44.dp)
                        .scale(boltScale)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isRunning) stringResource(Res.string.status_active) else stringResource(Res.string.status_inactive),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isRunning)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isRunning) {
            Text(
                text = pluralStringResource(Res.plurals.monitoring_sites, selectedCount, selectedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = stringResource(Res.string.select_sites_to_monitor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interval chip
        IntervalChip(
            intervalSeconds = intervalSeconds,
            onIntervalChange = onIntervalChange,
            enabled = canChangeInterval
        )
    }
}

@Composable
private fun IntervalChip(
    intervalSeconds: Int,
    onIntervalChange: (Int) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = intervalOptions.find { it.seconds == intervalSeconds }?.label ?: "1 min"

    Box {
        SuggestionChip(
            onClick = { if (enabled) expanded = true },
            label = {
                Text(
                    text = stringResource(Res.string.every_label, currentLabel),
                    style = MaterialTheme.typography.labelLarge
                )
            },
            enabled = enabled,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            border = null
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            intervalOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            fontWeight = if (option.seconds == intervalSeconds) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onIntervalChange(option.seconds)
                        expanded = false
                    },
                    trailingIcon = if (option.seconds == intervalSeconds) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun StartStopFab(
    isRunning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val contentColor = if (isRunning) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary

    val scale = if (isRunning) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        ).value
    } else 1f

    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = Modifier
            .width(160.dp)
            .scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) stringResource(Res.string.stop) else stringResource(Res.string.start),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.no_sites_yet),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.add_sites_to_monitor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SiteRow(
    site: Site,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val siteColor = Color(site.colors.first)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            siteColor.copy(alpha = 0.12f)
        else
            Color.Transparent,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS
        ),
        label = "bg"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isSelected) siteColor else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS
        ),
        label = "indicator"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onClick
                    )
                } else Modifier
            )
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .then(if (!enabled) Modifier.alpha(0.6f) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left indicator bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(indicatorColor)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = site.title.takeIf { !it.isNullOrBlank() } ?: site.url,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!site.title.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Checkmark only when selected
        val checkScale by animateFloatAsState(
            targetValue = if (isSelected) 1f else 0f,
            animationSpec = spring(
                dampingRatio = ExpressiveSpring.BOUNCY_DAMPING,
                stiffness = ExpressiveSpring.BOUNCY_STIFFNESS
            ),
            label = "checkScale"
        )

        if (isSelected || checkScale > 0f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = siteColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(checkScale)
            )
        }
    }
}
