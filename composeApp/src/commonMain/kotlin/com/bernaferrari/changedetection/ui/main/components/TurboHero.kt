/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.background.TurboState
import com.bernaferrari.changedetection.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Setup exposes cadence and duration; an active session shows its bounded lifetime. */
@Composable
internal fun TurboHero(
    state: TurboState,
    intervalSeconds: Int,
    onIntervalChange: (Int) -> Unit,
    durationMinutes: Int = 180,
    onDurationChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (state.isActive) {
            SessionStatus(state)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.turbo_check_every), style = MaterialTheme.typography.titleMedium)
                IntervalSelector(intervalSeconds, onIntervalChange)
            }
        } else {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.turbo_closer_eye), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Outlined.Bolt, null)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.turbo_check_every), style = MaterialTheme.typography.bodyMedium)
                        TurboOptions(intervalOptions(), intervalSeconds, onIntervalChange, integrated = true)
                    }
                }
            }
        }
        if (!state.isActive) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.turbo_for_how_long), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TurboOptions(
                    options = listOf(15 to "15 min", 30 to "30 min", 60 to "1 h", 180 to "3 h").map { (minutes, label) ->
                        val hours = minutes >= 60
                        val quantity = if (hours) minutes / 60 else minutes
                        TurboOption(minutes, label, pluralStringResource(if (hours) Res.plurals.turbo_session_length_hours else Res.plurals.turbo_session_length_minutes, quantity, quantity))
                    },
                    selectedValue = durationMinutes,
                    onSelected = onDurationChange,
                )
                Text(stringResource(Res.string.turbo_schedule_continues), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SessionStatus(state: TurboState) {
    val remaining = rememberTurboRemaining(state)
    val durationMs = state.durationMinutes * 60_000L
    val clock = turboClock(remaining)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Timer, null)
                Text(stringResource(Res.string.turbo_running_title), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val type = if (maxWidth / LocalDensity.current.fontScale < 240.dp) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium
                    Text(clock, style = type.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Normal, maxLines = 1)
                }
                Text(stringResource(Res.string.turbo_session_remaining), style = MaterialTheme.typography.bodyMedium)
                val hours = state.durationMinutes >= 60
                val length = if (hours) state.durationMinutes / 60 else state.durationMinutes
                Text(pluralStringResource(if (hours) Res.plurals.turbo_session_length_hours else Res.plurals.turbo_session_length_minutes, length, length), style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = { remaining.toFloat() / durationMs },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                drawStopIndicator = {},
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(pluralStringResource(Res.plurals.turbo_saved_updates, state.updatesSaved, state.updatesSaved), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(pluralStringResource(Res.plurals.turbo_successful_checks, state.successfulChecks, state.successfulChecks), style = MaterialTheme.typography.bodySmall)
                    if (state.failedChecks > 0) Text(pluralStringResource(Res.plurals.turbo_failed_checks, state.failedChecks, state.failedChecks), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun IntervalSelector(intervalSeconds: Int, onIntervalChange: (Int) -> Unit) {
    val options = intervalOptions()
    TurboOptions(options, intervalSeconds, onIntervalChange)
}

private data class TurboOption(val value: Int, val label: String, val accessibleLabel: String)

@Composable
private fun intervalOptions(): List<TurboOption> = listOf(15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m").map { (seconds, label) ->
    val minutes = seconds >= 60
    val quantity = if (minutes) seconds / 60 else seconds
    TurboOption(seconds, label, pluralStringResource(if (minutes) Res.plurals.turbo_every_minutes else Res.plurals.turbo_every_seconds, quantity, quantity))
}

@Composable
private fun TurboOptions(options: List<TurboOption>, selectedValue: Int, onSelected: (Int) -> Unit, integrated: Boolean = false) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth / fontScale < 280.dp) 2 else 4
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (seconds, label, accessibleLabel) ->
                        val selected = seconds == selectedValue
                        val container by animateColorAsState(
                            when {
                                selected -> MaterialTheme.colorScheme.primaryContainer
                                integrated -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.06f)
                                else -> MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        )
                        val content by animateColorAsState(
                            if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else if (integrated) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                        )
                        val shape = MaterialTheme.shapes.medium
                        Surface(
                            modifier = Modifier.weight(1f).clip(shape).semantics { contentDescription = accessibleLabel }.selectable(
                                selected = selected,
                                role = Role.RadioButton,
                                onClick = { if (!selected) onSelected(seconds) },
                            ),
                            shape = shape,
                            color = container,
                            contentColor = content,
                            border = if (integrated && !selected) null else BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Reserve the marker space so choosing an option never moves its label.
                                Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(12.dp).alpha(if (selected) 1f else 0f))
                                Text(label, modifier = Modifier.clearAndSetSemantics {}, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Normal, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
