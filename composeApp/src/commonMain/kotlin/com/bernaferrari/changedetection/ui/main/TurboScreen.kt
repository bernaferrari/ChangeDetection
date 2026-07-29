/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.bernaferrari.changedetection.repo.Site
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.add_sites_to_monitor
import com.bernaferrari.changedetection.shared.generated.resources.add_first_site
import com.bernaferrari.changedetection.shared.generated.resources.got_it
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.label_interval
import com.bernaferrari.changedetection.shared.generated.resources.monitoring
import com.bernaferrari.changedetection.shared.generated.resources.no_sites_yet
import com.bernaferrari.changedetection.shared.generated.resources.select_sites
import com.bernaferrari.changedetection.shared.generated.resources.start
import com.bernaferrari.changedetection.shared.generated.resources.stop
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_text
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_title
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode
import com.bernaferrari.changedetection.shared.generated.resources.clear_all
import com.bernaferrari.changedetection.shared.generated.resources.select_all
import com.bernaferrari.changedetection.shared.generated.resources.turbo_active_sites
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode_selected_sites
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.theme.CornerRadius
import com.bernaferrari.changedetection.ui.theme.ExpressiveSpring
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

private data class IntervalOption(val seconds: Int, val label: String)

private val intervalOptions = listOf(
    IntervalOption(15, "15s"),
    IntervalOption(30, "30s"),
    IntervalOption(60, "1m"),
    IntervalOption(120, "2m"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurboScreen(
    sites: List<SiteAndLastSnap>,
    quickMonitorState: QuickMonitorState,
    isLoading: Boolean = false,
    onAddSite: () -> Unit,
    onStartTurbo: (siteIds: Set<String>, intervalSeconds: Int) -> Unit,
    onStopTurbo: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val isRunning = quickMonitorState.isActive
    var selectedIds by remember {
        mutableStateOf(quickMonitorState.siteIds.ifEmpty { sites.mapTo(mutableSetOf()) { it.site.id } })
    }

    // Sync selectedIds when turbo starts (but don't clear when it stops)
    LaunchedEffect(quickMonitorState.isActive, quickMonitorState.siteIds) {
        if (quickMonitorState.isActive) {
            selectedIds = quickMonitorState.siteIds
        }
    }
    LaunchedEffect(sites, isRunning) {
        if (!isRunning && selectedIds.isEmpty()) {
            selectedIds = sites.mapTo(mutableSetOf()) { it.site.id }
        }
    }
    var intervalSeconds by remember { mutableIntStateOf(quickMonitorState.intervalSeconds) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(isRunning, quickMonitorState.intervalSeconds) {
        if (isRunning) {
            intervalSeconds = quickMonitorState.intervalSeconds
        }
    }

    // Derived state: Is any selected site currently syncing?
    val isNetworkSyncing = remember(sites, selectedIds) {
        sites.any { selectedIds.contains(it.site.id) && it.isSyncing }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = { Text(stringResource(Res.string.turbo_help_title)) },
            text = {
                val helpText = stringResource(Res.string.turbo_help_text)
                val annotatedText = remember(helpText) {
                    buildAnnotatedString {
                        var currentIndex = 0
                        val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
                        boldRegex.findAll(helpText).forEach { match ->
                            // Append text before the bold part
                            append(helpText.substring(currentIndex, match.range.first))

                            // Append the bold part
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(match.groupValues[1])
                            }

                            currentIndex = match.range.last + 1
                        }
                        // Append remaining text
                        if (currentIndex < helpText.length) {
                            append(helpText.substring(currentIndex))
                        }
                    }
                }
                Text(text = annotatedText)
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(Res.string.got_it))
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.turbo_mode),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(Res.string.how_it_works),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        floatingActionButton = {},
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            sites.isEmpty() -> {
                SharedEmptyState(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(Res.string.no_sites_yet),
                    description = stringResource(Res.string.add_sites_to_monitor),
                    modifier = Modifier.padding(padding),
                    primaryActionLabel = stringResource(Res.string.add_first_site),
                    onPrimaryAction = onAddSite,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                ) {
                    // Hero section with circular progress
                    item {
                        TurboHero(
                            isRunning = isRunning,
                            isSyncing = isNetworkSyncing,
                            intervalSeconds = intervalSeconds,
                            selectedCount = selectedIds.size,
                            onIntervalChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                intervalSeconds = it
                            },
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isRunning) {
                                    onStopTurbo()
                                } else if (selectedIds.isNotEmpty()) {
                                    onStartTurbo(selectedIds, intervalSeconds)
                                }
                            },
                            canToggle = isRunning || selectedIds.isNotEmpty(),
                        )
                    }

                    // Sites section header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (isRunning) stringResource(Res.string.monitoring) else stringResource(Res.string.select_sites),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            if (!isRunning) {
                                TextButton(
                                    onClick = {
                                        selectedIds = if (selectedIds.size == sites.size) {
                                            emptySet()
                                        } else {
                                            sites.mapTo(mutableSetOf()) { it.site.id }
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(
                                            if (selectedIds.size == sites.size) Res.string.clear_all else Res.string.select_all,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    // Sites list with individual row styling
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            val visibleSites = if (isRunning) {
                                sites.filter { selectedIds.contains(it.site.id) }
                            } else {
                                sites
                            }

                            visibleSites.forEachIndexed { index, siteAndSnap ->
                                val isSelected = selectedIds.contains(siteAndSnap.site.id)
                                val enabled = !isRunning
                                val isFirst = index == 0
                                val isLast = index == visibleSites.size - 1

                                SiteRowCard(
                                    site = siteAndSnap.site,
                                    isSelected = isSelected,
                                    enabled = enabled,
                                    isSyncing = isRunning && siteAndSnap.isSyncing,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        val newIds = selectedIds.toMutableSet()
                                        if (isSelected) newIds.remove(siteAndSnap.site.id) else newIds.add(siteAndSnap.site.id)
                                        selectedIds = newIds
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SiteRowCard(
    site: Site,
    isSelected: Boolean,
    enabled: Boolean,
    isSyncing: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    val siteColor = remember(site.colors.first) { Color(site.colors.first) }
    val siteColor2 = remember(site.colors.second) { Color(site.colors.second) }
    val gradient = remember(siteColor, siteColor2) {
        androidx.compose.ui.graphics.Brush.linearGradient(listOf(siteColor, siteColor2))
    }

    // Background color when selected
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            siteColor.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "rowBg",
    )

    // Animate corner radii
    val animationSpec = spring<Dp>(
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
        animationSpec = animationSpec,
        label = "topRadius",
    )

    val bottomRadius by animateDpAsState(
        targetValue = if (isSelected) {
            CornerRadius.largePlus
        } else if (isLast) {
            CornerRadius.medium
        } else {
            CornerRadius.xs
        },
        animationSpec = animationSpec,
        label = "bottomRadius",
    )

    val shape = RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.BOUNCY_DAMPING,
            stiffness = ExpressiveSpring.BOUNCY_STIFFNESS,
        ),
        label = "scale",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(vertical = 2.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = ExpressiveSpring.QUICK_DAMPING,
                    stiffness = ExpressiveSpring.QUICK_STIFFNESS,
                ),
            ),
        shape = shape,
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Gradient color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = isSyncing,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                    },
                    label = "sync_indicator",
                ) { syncing ->
                    if (syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
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

            // Site info
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

            // Circular selection indicator
            CircularCheckIndicator(
                isSelected = isSelected,
                color = siteColor,
            )
        }
    }
}

@Composable
private fun TurboHero(
    isRunning: Boolean,
    isSyncing: Boolean,
    intervalSeconds: Int,
    selectedCount: Int,
    onIntervalChange: (Int) -> Unit,
    onToggle: () -> Unit,
    canToggle: Boolean,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isRunning) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.GENTLE_DAMPING,
            stiffness = ExpressiveSpring.GENTLE_STIFFNESS,
        ),
        label = "containerColor",
    )

    // Current interval label
    val currentLabel = intervalOptions.find { it.seconds == intervalSeconds }?.label ?: "15s"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Outer Hero Container - Entire 120.dp area is now clickable
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            // Inner container (Circle-in-circle effect)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 3.dp,
                    )
                } else if (isRunning) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    // Animated interval display
                    AnimatedContent(
                        targetState = currentLabel,
                        transitionSpec = {
                            slideInVertically { -it } + fadeIn() togetherWith
                                slideOutVertically { it } + fadeOut()
                        },
                        label = "interval_animation",
                    ) { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isRunning) {
                pluralStringResource(
                    Res.plurals.turbo_active_sites,
                    selectedCount,
                    selectedCount,
                    currentLabel,
                )
            } else {
                pluralStringResource(
                    Res.plurals.turbo_mode_selected_sites,
                    selectedCount,
                    selectedCount,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.label_interval),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            intervalOptions.forEach { option ->
                val selected = intervalSeconds == option.seconds
                Surface(
                    onClick = { onIntervalChange(option.seconds) },
                    enabled = !isRunning,
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Start/Stop button
        StartStopFab(
            isRunning = isRunning,
            enabled = canToggle,
            onClick = onToggle,
        )
    }
}

@Composable
private fun StartStopFab(
    isRunning: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    if (isRunning) {
        // Outlined style for Stop
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .width(160.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.stop),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    } else {
        // Filled FAB for Start
        FloatingActionButton(
            onClick = onClick,
            containerColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(160.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.start),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    SharedEmptyState(
        icon = Icons.Rounded.Bolt,
        title = stringResource(Res.string.no_sites_yet),
        description = stringResource(Res.string.add_sites_to_monitor),
        modifier = modifier,
    )
}

/**
 * Circular check indicator - replaces checkbox with a more expressive circle.
 */
@Composable
private fun CircularCheckIndicator(
    isSelected: Boolean,
    color: Color,
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color else Color.Transparent,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "circleBg",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else MaterialTheme.colorScheme.outline,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "circleBorder",
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = ExpressiveSpring.QUICK_DAMPING,
            stiffness = ExpressiveSpring.QUICK_STIFFNESS,
        ),
        label = "circleScale",
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(
                if (!isSelected) {
                    Modifier.background(
                        color = Color.Transparent,
                        shape = CircleShape,
                    )
                } else {
                    Modifier
                },
            )
            .then(
                if (!isSelected) {
                    Modifier.padding(2.dp)
                        .background(
                            color = Color.Transparent,
                            shape = CircleShape,
                        )
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!isSelected) {
            // Empty circle border
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(borderColor.copy(alpha = 0.3f)),
                )
            }
        } else {
            // Checkmark
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
