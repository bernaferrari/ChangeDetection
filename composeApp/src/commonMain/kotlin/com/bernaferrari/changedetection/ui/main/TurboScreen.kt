/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.TurboEndReason
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.components.ListSkeleton
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.main.components.TurboHero
import com.bernaferrari.changedetection.ui.main.components.TurboSiteRow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TurboScreen(
    sites: List<SiteAndLastSnap>,
    quickMonitorState: QuickMonitorState,
    isLoading: Boolean = false,
    initialSiteId: String? = null,
    onBack: (() -> Unit)? = null,
    onAddSite: () -> Unit,
    onStartTurbo: (siteIds: Set<String>, intervalSeconds: Int, durationMinutes: Int) -> Unit,
    onStopTurbo: () -> Unit,
    onOpenSite: (String) -> Unit,
    turboAvailable: Boolean = monitoringCapabilities.turbo,
) {
    val running = quickMonitorState.isActive
    val eligibleIds = sites.filter { it.site.isSyncEnabled }.mapTo(mutableSetOf()) { it.site.id }
    val initialSelection = if (!running && initialSiteId != null) setOf(initialSiteId).intersect(eligibleIds) else quickMonitorState.siteIds.ifEmpty { eligibleIds }
    var selectedIds by remember(initialSiteId, quickMonitorState.siteIds) { mutableStateOf(initialSelection) }
    var initialized by remember { mutableStateOf(sites.isNotEmpty()) }
    var intervalSeconds by remember(quickMonitorState.intervalSeconds) { mutableIntStateOf(quickMonitorState.intervalSeconds) }
    var durationMinutes by remember(quickMonitorState.durationMinutes) { mutableIntStateOf(quickMonitorState.durationMinutes) }
    var editingPages by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val selecting = !running || editingPages

    LaunchedEffect(isLoading, eligibleIds, running) {
        if (!isLoading && !initialized && sites.isNotEmpty()) {
            selectedIds = initialSelection.intersect(eligibleIds)
            initialized = true
        } else if (!running) {
            selectedIds = selectedIds.intersect(eligibleIds)
        }
    }
    LaunchedEffect(running, quickMonitorState.siteIds, quickMonitorState.intervalSeconds) {
        if (running) {
            selectedIds = quickMonitorState.siteIds
            intervalSeconds = quickMonitorState.intervalSeconds
        } else {
            editingPages = false
        }
    }
    val selectPages: (Set<String>) -> Unit = { ids ->
        selectedIds = ids.intersect(eligibleIds)
        if (running) {
            if (selectedIds.isEmpty()) onStopTurbo() else onStartTurbo(selectedIds, intervalSeconds, durationMinutes)
        }
    }

    // Frequent cadence and session controls use restrained Material motion.
    MaterialTheme(motionScheme = MotionScheme.standard()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.turbo_mode)) },
                    navigationIcon = { onBack?.let { IconButton(onClick = it) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back)) } } },
                    actions = {
                        IconButton(onClick = { showHelp = true }) {
                            Icon(Icons.AutoMirrored.Outlined.HelpOutline, stringResource(Res.string.how_it_works))
                        }
                    },
                )
            },
            bottomBar = {
                if (running || (turboAvailable && sites.isNotEmpty() && !isLoading)) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(
                                Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Button(
                                    onClick = { if (running) onStopTurbo() else onStartTurbo(selectedIds, intervalSeconds, durationMinutes) },
                                    enabled = running || selectedIds.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                                    shape = MaterialTheme.shapes.large,
                                    colors = if (running) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        )
                                    } else {
                                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                                    },
                                ) {
                                    Icon(if (running) Icons.Rounded.Stop else Icons.Rounded.Bolt, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(if (running) Res.string.turbo_stop_session else Res.string.turbo_mode_button_start))
                                }
                            }
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->
            when {
                !turboAvailable && !running -> SharedEmptyState(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(Res.string.turbo_unavailable_title),
                    description = stringResource(Res.string.turbo_unavailable_description),
                    modifier = Modifier.fillMaxSize().padding(padding),
                )

                isLoading -> ListSkeleton(Modifier.fillMaxSize().padding(padding))

                sites.isEmpty() -> SharedEmptyState(
                    icon = Icons.Rounded.Bolt,
                    title = stringResource(Res.string.no_sites_yet),
                    description = stringResource(Res.string.add_sites_to_monitor),
                    modifier = Modifier.padding(padding),
                    primaryActionLabel = stringResource(Res.string.add_first_site),
                    onPrimaryAction = onAddSite,
                )

                else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    val visibleSites = if (selecting) sites else sites.filter { it.site.id in quickMonitorState.siteIds }
                    LazyColumn(
                        Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("turbo-content"),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (!running && quickMonitorState.endReason != null) {
                            item(key = "outcome") { SessionOutcome(quickMonitorState.endReason) }
                        }
                        item(key = "session") {
                            TurboHero(
                                state = quickMonitorState,
                                intervalSeconds = intervalSeconds,
                                durationMinutes = durationMinutes,
                                onDurationChange = { durationMinutes = it },
                                onIntervalChange = {
                                    intervalSeconds = it
                                    if (running) onStartTurbo(selectedIds, it, durationMinutes)
                                },
                            )
                        }
                        item(key = "pages-heading") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    pluralStringResource(Res.plurals.turbo_mode_selected_sites, selectedIds.size, selectedIds.size),
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (running) {
                                    TextButton(onClick = { editingPages = !editingPages }) {
                                        Text(stringResource(if (editingPages) Res.string.done else Res.string.edit))
                                    }
                                } else {
                                    TextButton(onClick = { selectPages(if (selectedIds == eligibleIds) emptySet() else eligibleIds) }) {
                                        Text(stringResource(if (selectedIds == eligibleIds) Res.string.clear_all else Res.string.select_all))
                                    }
                                }
                            }
                        }
                        itemsIndexed(visibleSites, key = { _, item -> item.site.id }) { index, item ->
                            TurboSiteRow(
                                item = item,
                                selected = item.site.id in selectedIds,
                                selecting = selecting,
                                sessionStarted = if (running) quickMonitorState.startTime else 0,
                                position = when {
                                    visibleSites.size == 1 -> SegmentPosition.Only
                                    index == 0 -> SegmentPosition.First
                                    index == visibleSites.lastIndex -> SegmentPosition.Last
                                    else -> SegmentPosition.Middle
                                },
                                onToggle = { selectPages(if (item.site.id in selectedIds) selectedIds - item.site.id else selectedIds + item.site.id) },
                                onOpen = { onOpenSite(item.site.id) },
                            )
                        }
                    }
                }
            }
        }
        if (showHelp) {
            AlertDialog(
                onDismissRequest = { showHelp = false },
                title = { Text(stringResource(Res.string.turbo_help_title)) },
                text = { Text(stringResource(Res.string.turbo_help_text).replace("**", ""), Modifier.verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = { showHelp = false }) { Text(stringResource(Res.string.got_it)) } },
            )
        }
    }
}

@Composable
private fun SessionOutcome(reason: TurboEndReason) {
    val message = when (reason) {
        TurboEndReason.Stopped -> Res.string.turbo_session_stopped
        TurboEndReason.NoSites -> Res.string.turbo_session_no_sites
        TurboEndReason.Expired -> Res.string.turbo_session_finished
        TurboEndReason.Interrupted -> Res.string.turbo_session_interrupted
        TurboEndReason.SystemLimit -> Res.string.turbo_session_system_limit
        TurboEndReason.StartFailed -> Res.string.turbo_session_start_failed
    }
    Text(
        stringResource(message),
        Modifier.padding(top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
