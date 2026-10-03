/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.bernaferrari.changedetection.ui.turbo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.TURBO_DURATION_MINUTES
import com.bernaferrari.changedetection.background.TURBO_INTERVAL_SECONDS
import com.bernaferrari.changedetection.background.TurboEndReason
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.ChangePreviewBlock
import com.bernaferrari.changedetection.ui.kit.NewBadge
import com.bernaferrari.changedetection.ui.kit.PageAvatar
import com.bernaferrari.changedetection.ui.kit.SectionHeader
import com.bernaferrari.changedetection.ui.kit.domain
import com.bernaferrari.changedetection.ui.kit.name
import com.bernaferrari.changedetection.ui.kit.rememberLatestPreview
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.util.formatTime
import com.bernaferrari.changedetection.util.getCurrentTimeMillis
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** A clock that ticks once a second while a session is on screen. */
@Composable
private fun rememberNow(active: Boolean): Long {
    val now by produceState(getCurrentTimeMillis(), active) {
        while (active) {
            value = getCurrentTimeMillis()
            delay(1_000)
        }
    }
    return now
}

private fun TurboState.progress(now: Long): Float {
    val total = durationMinutes * 60_000f
    if (!isActive || total <= 0f) return 0f
    return (1f - remainingTimeMillis(now) / total).coerceIn(0f, 1f)
}

private fun TurboState.secondsToNextCheck(now: Long): Int {
    if (lastCheckTime <= 0) return intervalSeconds
    val next = lastCheckTime + intervalSeconds * 1_000L
    return ((next - now) / 1_000L).toInt().coerceIn(0, intervalSeconds)
}

@Composable
private fun minutesLeftLabel(state: TurboState, now: Long): String {
    val minutes = ((state.remainingTimeMillis(now) + 59_999) / 60_000).toInt()
    return if (minutes >= 60) {
        stringResource(Res.string.turbo_hours_left, minutes / 60, minutes % 60)
    } else {
        pluralStringResource(Res.plurals.turbo_minutes_left, minutes, minutes)
    }
}

@Composable
fun intervalLabel(seconds: Int): String = if (seconds < 60) {
    stringResource(Res.string.turbo_every_seconds_short, seconds)
} else {
    stringResource(Res.string.turbo_every_minutes_short, seconds / 60)
}

@Composable
fun durationLabel(minutes: Int): String = if (minutes < 60) {
    stringResource(Res.string.turbo_minutes_short, minutes)
} else {
    pluralStringResource(Res.plurals.turbo_hours_short, minutes / 60, minutes / 60)
}

/** The soft yellow card at the top of Home while a session runs. */
@Composable
fun TurboLiveCard(state: TurboState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val now = rememberNow(state.isActive)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.marks.highlightSoft,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoltBadge()
                Column(Modifier.weight(1f)) {
                    Text(pluralStringResource(Res.plurals.turbo_card_title_n, state.siteIds.size, state.siteIds.size), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(Res.string.turbo_card_status, state.secondsToNextCheck(now), minutesLeftLabel(state, now)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearWavyProgressIndicator(
                progress = { state.progress(now) },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun BoltBadge(size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(MaterialTheme.shapes.medium).background(MaterialTheme.marks.highlight),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.marks.onHighlight) }
}

/** Three decisions in one sheet: which pages, how often, for how long. */
@Composable
fun TurboSetupSheet(
    sites: List<SiteAndLastSnap>,
    preselected: Set<String>,
    state: TurboState,
    onDismiss: () -> Unit,
    onStart: (siteIds: Set<String>, intervalSeconds: Int, durationMinutes: Int) -> Unit,
) {
    val eligible = sites.filter { it.site.isSyncEnabled }
    val eligibleIds = eligible.mapTo(mutableSetOf()) { it.site.id }
    var selected by remember {
        mutableStateOf(
            preselected.intersect(eligibleIds).ifEmpty { state.siteIds.intersect(eligibleIds) }.ifEmpty {
                if (eligibleIds.size <= 3) eligibleIds else emptySet()
            },
        )
    }
    var interval by remember { mutableIntStateOf(state.intervalSeconds.takeIf { it in TURBO_INTERVAL_SECONDS } ?: 30) }
    var duration by remember { mutableIntStateOf(state.durationMinutes.takeIf { it in TURBO_DURATION_MINUTES } ?: 60) }
    val endsAt = remember(duration) { getCurrentTimeMillis() + duration * 60_000L }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp).testTag("turbo-setup")) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoltBadge()
                Text(stringResource(Res.string.turbo_mode), style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                stringResource(Res.string.turbo_sheet_body),
                Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!monitoringCapabilities.turbo) {
                Text(stringResource(Res.string.turbo_unavailable_description), Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            SheetLabel(stringResource(Res.string.turbo_sheet_pages))
            FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eligible.forEach { row ->
                    val on = row.site.id in selected
                    FilterChip(
                        selected = on,
                        onClick = { selected = if (on) selected - row.site.id else selected + row.site.id },
                        label = { Text(row.site.name(), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 200.dp)) },
                        leadingIcon = if (on) {
                            { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                        } else {
                            { PageAvatar(row.site.name(), row.site.id, size = 22.dp) }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.heightIn(min = 40.dp),
                    )
                }
            }
            SheetLabel(stringResource(Res.string.turbo_sheet_every))
            ConnectedChoice(TURBO_INTERVAL_SECONDS.sorted(), interval, { interval = it }) { intervalLabel(it) }
            SheetLabel(stringResource(Res.string.turbo_sheet_for))
            ConnectedChoice(TURBO_DURATION_MINUTES.sorted(), duration, { duration = it }) { durationLabel(it) }
            Button(
                onClick = { onStart(selected, interval, duration) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp).heightIn(min = 60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Icon(Icons.Rounded.Bolt, null)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (selected.isEmpty()) stringResource(Res.string.turbo_sheet_pick) else pluralStringResource(Res.plurals.turbo_sheet_start, selected.size, selected.size),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Row(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.BatteryAlert, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.turbo_sheet_caption, formatTime(endsAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(text, Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 10.dp), style = MaterialTheme.typography.titleSmall)
}

/** An M3 Expressive connected button group behaving as a single-choice control. */
@Composable
fun <T> ConnectedChoice(options: List<T>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, label: @Composable (T) -> String) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text(label(option), maxLines = 1, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** The live session: what is happening, how much time is left, and anything that changed. */
@Composable
fun TurboLiveScreen(
    state: TurboState,
    sites: List<SiteAndLastSnap>,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onRunAgain: () -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenChange: (String, String?) -> Unit,
) {
    val now = rememberNow(state.isActive)
    val sessionPages = sites.filter { it.site.id in state.siteIds }
    val changed = sessionPages.filter { row -> state.startTime > 0 && (row.snap?.timestamp ?: 0) >= state.startTime && row.snapshotCount > 1 }
    val quiet = sessionPages - changed.toSet()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(Res.string.a11y_navigate_back)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.isActive) {
                        FilledTonalButton(
                            onClick = onStop,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                        ) {
                            Icon(Icons.Rounded.Stop, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.turbo_stop_session))
                        }
                    } else {
                        FilledTonalButton(onClick = onRunAgain, modifier = Modifier.weight(1f).heightIn(min = 56.dp), enabled = sessionPages.isNotEmpty()) {
                            Icon(Icons.Rounded.Refresh, null)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(Res.string.turbo_run_again))
                        }
                        Button(onClick = onBack, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(Res.string.done)) }
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("turbo-live"), contentPadding = PaddingValues(bottom = 24.dp)) {
                item(key = "hero") {
                    Column(Modifier.padding(horizontal = 24.dp)) {
                        Row(
                            Modifier.clip(CircleShape).background(if (state.isActive) MaterialTheme.marks.highlight else MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Rounded.Bolt, null, Modifier.size(16.dp), tint = if (state.isActive) MaterialTheme.marks.onHighlight else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                if (state.isActive) pluralStringResource(Res.plurals.turbo_card_title_n, state.siteIds.size, state.siteIds.size) else endedLabel(state.endReason),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (state.isActive) MaterialTheme.marks.onHighlight else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.size(14.dp))
                        Text(
                            if (state.isActive) {
                                if (state.intervalSeconds < 60) stringResource(Res.string.turbo_live_every_seconds, state.intervalSeconds) else pluralStringResource(Res.plurals.turbo_live_every_minutes, state.intervalSeconds / 60, state.intervalSeconds / 60)
                            } else {
                                if (changed.isEmpty()) stringResource(Res.string.turbo_found_none) else pluralStringResource(Res.plurals.turbo_found, changed.size, changed.size)
                            },
                            style = MaterialTheme.typography.displaySmall,
                        )
                        if (state.isActive) {
                            LinearWavyProgressIndicator(
                                progress = { state.progress(now) },
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                Text(minutesLeftLabel(state, now), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(Res.string.turbo_ends_at, formatTime(state.deadlineTime)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text(
                                pluralStringResource(Res.plurals.turbo_summary_checks, state.successfulChecks + state.failedChecks, state.successfulChecks + state.failedChecks, sessionPages.size),
                                Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (changed.isNotEmpty()) {
                    item(key = "changed-h") { SectionHeader(stringResource(Res.string.turbo_changed), Modifier.padding(top = 16.dp)) }
                    items(changed, key = { "c" + it.site.id }) { row ->
                        TurboChangeCard(row, onOpen = { onOpenChange(row.site.id, row.snap?.snapId) }, modifier = Modifier.animateItem())
                    }
                }
                if (quiet.isNotEmpty()) {
                    item(key = "quiet-h") {
                        SectionHeader(
                            stringResource(if (state.isActive) Res.string.turbo_checking else Res.string.turbo_sheet_pages),
                            Modifier.padding(top = 16.dp),
                        )
                    }
                    items(quiet, key = { "q" + it.site.id }) { row ->
                        QuietRow(row, state, now, onClick = { onOpenPage(row.site.id) }, modifier = Modifier.animateItem())
                    }
                }
                if (state.isActive) {
                    item(key = "note") {
                        Text(
                            stringResource(Res.string.turbo_live_note),
                            Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun endedLabel(reason: TurboEndReason?): String = stringResource(
    when (reason) {
        TurboEndReason.Expired -> Res.string.turbo_ended_planned
        TurboEndReason.Stopped -> Res.string.turbo_ended_stopped
        TurboEndReason.Interrupted, TurboEndReason.SystemLimit -> Res.string.turbo_ended_interrupted
        TurboEndReason.StartFailed -> Res.string.turbo_ended_failed
        else -> Res.string.turbo_ended
    },
)

@Composable
private fun TurboChangeCard(row: SiteAndLastSnap, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val preview = rememberLatestPreview(row.site.id, row.snap?.snapId)
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PageAvatar(row.site.name(), row.site.id)
                Column(Modifier.weight(1f)) {
                    Text(row.site.name(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTime(row.snap?.timestamp ?: 0), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!row.site.isRead) NewBadge(text = stringResource(Res.string.badge_new))
            }
            ChangePreviewBlock(preview)
        }
    }
}

@Composable
private fun QuietRow(row: SiteAndLastSnap, state: TurboState, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lastCheck = row.site.timestamp.takeIf { it >= state.startTime && state.startTime > 0 }
    val seconds = lastCheck?.let { ((now - it) / 1_000).toInt().coerceAtLeast(0) }
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 72.dp).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageAvatar(row.site.name(), row.site.id)
        Column(Modifier.weight(1f)) {
            Text(row.site.name(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    !row.site.isSuccessful -> com.bernaferrari.changedetection.ui.components.checkFailureTitle(row.site.lastFailureCode)
                    seconds == null -> row.site.domain()
                    else -> stringResource(Res.string.turbo_row_quiet, seconds)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (!row.site.isSuccessful) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isActive) {
            CircularProgressIndicator(
                progress = { 1f - state.secondsToNextCheck(now) / state.intervalSeconds.toFloat() },
                modifier = Modifier.size(28.dp),
                strokeWidth = 3.dp,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            )
        }
    }
}
