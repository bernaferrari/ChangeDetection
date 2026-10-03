/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.repo.SiteAndLastSnap
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.util.displayPageTitle
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun rememberTurboRemaining(state: TurboState): Long {
    var remaining by remember(state.startTime, state.deadlineTime, state.remainingTimeSource) { mutableLongStateOf(state.remainingTimeMillis()) }
    LaunchedEffect(state.isActive, state.startTime, state.deadlineTime, state.remainingTimeSource) {
        if (state.isActive) {
            while (true) {
                remaining = state.remainingTimeMillis()
                delay(1000)
            }
        } else {
            remaining = 0
        }
    }
    return remaining
}

internal fun turboClock(remaining: Long): String {
    val seconds = (remaining.coerceAtLeast(0) + 999) / 1000
    return "${seconds / 3600}:${((seconds / 60) % 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
}

/** A running session stays discoverable even when its pages are filtered out. */
@Composable
internal fun ActiveTurboCard(state: TurboState, sites: List<SiteAndLastSnap>, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val clock = turboClock(rememberTurboRemaining(state))
    val page = state.siteIds.singleOrNull()?.let { id -> sites.firstOrNull { it.site.id == id }?.site }
    val supporting = page?.displayPageTitle() ?: pluralStringResource(Res.plurals.turbo_mode_selected_sites, state.siteIds.size, state.siteIds.size)
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val identity: @Composable () -> Unit = {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bolt, null, Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(Res.string.turbo_running_title), style = MaterialTheme.typography.titleMedium)
                        Text(supporting, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
            }
            val timer: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(Res.string.turbo_session_remaining), style = MaterialTheme.typography.labelSmall)
                    Text(clock, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
                }
            }
            if (maxWidth / LocalDensity.current.fontScale < 300.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    identity()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        timer()
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { identity() }
                    timer()
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp))
                }
            }
        }
    }
}
