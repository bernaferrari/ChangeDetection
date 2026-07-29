/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.add_sites_to_monitor
import com.bernaferrari.changedetection.shared.generated.resources.add_first_site
import com.bernaferrari.changedetection.shared.generated.resources.got_it
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.monitoring
import com.bernaferrari.changedetection.shared.generated.resources.no_sites_yet
import com.bernaferrari.changedetection.shared.generated.resources.select_sites
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_text
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_title
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode
import com.bernaferrari.changedetection.shared.generated.resources.clear_all
import com.bernaferrari.changedetection.shared.generated.resources.select_all
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.main.components.TurboHero
import com.bernaferrari.changedetection.ui.main.components.TurboSiteRow
import org.jetbrains.compose.resources.stringResource

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

                                TurboSiteRow(
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
private fun EmptyState(modifier: Modifier = Modifier) {
    SharedEmptyState(
        icon = Icons.Rounded.Bolt,
        title = stringResource(Res.string.no_sites_yet),
        description = stringResource(Res.string.add_sites_to_monitor),
        modifier = modifier,
    )
}
