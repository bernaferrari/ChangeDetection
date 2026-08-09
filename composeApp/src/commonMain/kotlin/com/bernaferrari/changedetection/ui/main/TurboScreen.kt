/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_text
import com.bernaferrari.changedetection.shared.generated.resources.turbo_help_title
import com.bernaferrari.changedetection.shared.generated.resources.turbo_mode
import com.bernaferrari.changedetection.shared.generated.resources.turbo_select_pages
import com.bernaferrari.changedetection.shared.generated.resources.turbo_select_pages_supporting
import com.bernaferrari.changedetection.shared.generated.resources.clear_all
import com.bernaferrari.changedetection.shared.generated.resources.select_all
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.components.AppLoadingIndicator
import com.bernaferrari.changedetection.ui.main.components.TurboHero
import com.bernaferrari.changedetection.ui.main.components.TurboSiteRow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TurboScreen(
    sites: List<SiteAndLastSnap>,
    quickMonitorState: QuickMonitorState,
    isLoading: Boolean = false,
    onAddSite: () -> Unit,
    onStartTurbo: (siteIds: Set<String>, intervalSeconds: Int) -> Unit,
    onStopTurbo: () -> Unit,
) {
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        stringResource(Res.string.turbo_mode),
                        fontWeight = FontWeight.Bold,
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
                    AppLoadingIndicator()
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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    val visibleSites = if (isRunning) {
                        sites.filter { selectedIds.contains(it.site.id) }
                    } else {
                        sites
                    }
                    val toggleAll = {
                        selectedIds = if (selectedIds.size == sites.size) {
                            emptySet()
                        } else {
                            sites.mapTo(mutableSetOf()) { it.site.id }
                        }
                    }
                    val onAction = {
                        if (isRunning) onStopTurbo() else onStartTurbo(selectedIds, intervalSeconds)
                    }
                    val onSiteToggle: (String) -> Unit = { siteId ->
                        val newIds = selectedIds.toMutableSet()
                        if (siteId in newIds) newIds.remove(siteId) else newIds.add(siteId)
                        selectedIds = newIds
                    }

                    if (maxWidth >= 900.dp) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = 1120.dp)
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            TurboHero(
                                isRunning = isRunning,
                                isSyncing = isNetworkSyncing,
                                intervalSeconds = intervalSeconds,
                                selectedCount = selectedIds.size,
                                onIntervalChange = { intervalSeconds = it },
                                onAction = onAction,
                                modifier = Modifier.weight(0.92f),
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1.08f).fillMaxHeight(),
                                contentPadding = PaddingValues(bottom = 32.dp),
                            ) {
                                item {
                                    TurboSelectionHeader(
                                        isRunning = isRunning,
                                        allSelected = selectedIds.size == sites.size,
                                        onToggleAll = toggleAll,
                                    )
                                }
                                turboSiteItems(
                                    sites = visibleSites,
                                    selectedIds = selectedIds,
                                    isRunning = isRunning,
                                    onSiteToggle = onSiteToggle,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .widthIn(max = 720.dp)
                                .fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                        ) {
                            item {
                                TurboHero(
                                    isRunning = isRunning,
                                    isSyncing = isNetworkSyncing,
                                    intervalSeconds = intervalSeconds,
                                    selectedCount = selectedIds.size,
                                    onIntervalChange = { intervalSeconds = it },
                                    onAction = onAction,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            item {
                                TurboSelectionHeader(
                                    isRunning = isRunning,
                                    allSelected = selectedIds.size == sites.size,
                                    onToggleAll = toggleAll,
                                    modifier = Modifier.padding(top = 12.dp),
                                )
                            }
                            turboSiteItems(
                                sites = visibleSites,
                                selectedIds = selectedIds,
                                isRunning = isRunning,
                                onSiteToggle = onSiteToggle,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TurboSelectionHeader(
    isRunning: Boolean,
    allSelected: Boolean,
    onToggleAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isRunning) stringResource(Res.string.monitoring) else stringResource(Res.string.turbo_select_pages),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!isRunning) {
            TextButton(onClick = onToggleAll) {
                Text(stringResource(if (allSelected) Res.string.clear_all else Res.string.select_all))
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.turboSiteItems(
    sites: List<SiteAndLastSnap>,
    selectedIds: Set<String>,
    isRunning: Boolean,
    onSiteToggle: (String) -> Unit,
) {
    itemsIndexed(sites, key = { _, item -> item.site.id }) { index, siteAndSnap ->
        val isSelected = siteAndSnap.site.id in selectedIds
        TurboSiteRow(
            site = siteAndSnap.site,
            isSelected = isSelected,
            enabled = !isRunning,
            isSyncing = isRunning && siteAndSnap.isSyncing,
            position = when {
                sites.size == 1 -> SiteRowPosition.Single
                index == 0 -> SiteRowPosition.First
                index == sites.lastIndex -> SiteRowPosition.Last
                else -> SiteRowPosition.Middle
            },
            onClick = { onSiteToggle(siteAndSnap.site.id) },
            modifier = Modifier.padding(horizontal = 4.dp),
        )
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
