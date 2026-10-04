/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.background.monitoringCapabilities

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.bernaferrari.changedetection.ui.kit.GroupLabel
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.shared.generated.resources.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.loading
import com.bernaferrari.changedetection.shared.generated.resources.save
import com.bernaferrari.changedetection.shared.generated.resources.schedule_save_failed
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_constraints_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_sync_interval_label
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Bottom-sheet route for background monitoring. The controls are factored into focused sections
 * so their state, semantics, and spacing can evolve independently from sheet presentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSyncDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackgroundSyncDialogContent(uiState, onDismiss, viewModel::saveMonitoringSettings, viewModel::retryLoading)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BackgroundSyncDialogContent(
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
    onSave: suspend (SettingsUiState) -> Unit,
    onRetry: () -> Unit = {},
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    val presets = listOf(15L, 30L, 60L, 180L, 360L, 720L, 1440L, 10080L)
    var draft by remember { mutableStateOf(uiState) }
    var custom by remember { mutableStateOf(uiState.syncInterval !in presets) }
    var initialized by remember { mutableStateOf(uiState.isLoaded) }
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded && !initialized) {
            draft = uiState
            custom = uiState.syncInterval !in presets
            initialized = true
        }
    }
    var valid by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val available = monitoringCapabilities.execution != MonitoringExecution.Unavailable

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.weight(1f, fill = false).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                Text(stringResource(Res.string.checks_title), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
                if (!uiState.isLoaded || !initialized) {
                    SettingsLoadState(uiState.loadFailed, onRetry)
                } else {
                    // One sentence that always says what the settings below add up to.
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
                            .toggleable(draft.backgroundSync, enabled = available && !saving, role = Role.Switch) { draft = draft.copy(backgroundSync = it) }
                            .semantics(mergeDescendants = true) {},
                        shape = MaterialTheme.shapes.large,
                        color = if (draft.backgroundSync) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (draft.backgroundSync) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(checksSentence(draft), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = draft.backgroundSync,
                                onCheckedChange = null,
                                enabled = available && !saving,
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedTrackColor = com.bernaferrari.changedetection.ui.theme.LocalMarks.current.highlight,
                                    checkedThumbColor = MaterialTheme.colorScheme.inverseSurface,
                                    checkedBorderColor = com.bernaferrari.changedetection.ui.theme.LocalMarks.current.highlight,
                                ),
                            )
                        }
                    }
                    if (draft.backgroundSync && available) {
                        GroupLabel(stringResource(Res.string.checks_every))
                        FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            presets.forEach { minutes ->
                                FilterChip(
                                    selected = !custom && draft.syncInterval == minutes,
                                    enabled = !saving,
                                    onClick = {
                                        custom = false
                                        valid = true
                                        draft = draft.copy(syncInterval = minutes)
                                    },
                                    label = { Text(presetLabel(minutes)) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                                )
                            }
                            FilterChip(
                                selected = custom,
                                enabled = !saving,
                                onClick = { custom = true },
                                label = { Text(stringResource(Res.string.checks_custom)) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                            )
                        }
                        if (custom) {
                            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                SyncIntervalInput(
                                    currentInterval = draft.syncInterval,
                                    onIntervalChanged = { draft = draft.copy(syncInterval = it) },
                                    onValidityChanged = { valid = it },
                                    enabled = !saving,
                                )
                            }
                        }
                        Text(
                            stringResource(Res.string.checks_own_schedule_note),
                            Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (monitoringCapabilities.constraints) {
                            GroupLabel(stringResource(Res.string.checks_only_when))
                            Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Column {
                                    GroupRow(stringResource(Res.string.checks_wifi), supporting = stringResource(Res.string.checks_wifi_body), icon = Icons.Outlined.Wifi, role = Role.Switch, checked = draft.requireWifi, enabled = !saving, onClick = { draft = draft.copy(requireWifi = !draft.requireWifi) }) {
                                        Switch(draft.requireWifi, onCheckedChange = null, enabled = !saving)
                                    }
                                    GroupRow(stringResource(Res.string.checks_charging), icon = Icons.Outlined.Power, showDivider = true, role = Role.Switch, checked = draft.requireCharging, enabled = !saving, onClick = { draft = draft.copy(requireCharging = !draft.requireCharging) }) {
                                        Switch(draft.requireCharging, onCheckedChange = null, enabled = !saving)
                                    }
                                    GroupRow(stringResource(Res.string.checks_battery), icon = Icons.Outlined.BatterySaver, showDivider = true, role = Role.Switch, checked = draft.requireBatteryNotLow, enabled = !saving, onClick = { draft = draft.copy(requireBatteryNotLow = !draft.requireBatteryNotLow) }) {
                                        Switch(draft.requireBatteryNotLow, onCheckedChange = null, enabled = !saving)
                                    }
                                }
                            }
                        }
                        Text(
                            stringResource(if (monitoringCapabilities.execution == MonitoringExecution.Background) Res.string.checks_android_note else Res.string.settings_checks_while_open),
                            Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (failed) {
                Text(stringResource(Res.string.schedule_save_failed), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
            }
            Button(
                enabled = uiState.isLoaded && initialized && available && (!draft.backgroundSync || valid) && !saving,
                onClick = {
                    saving = true
                    failed = false
                    scope.launch {
                        try {
                            onSave(draft)
                            onDismiss()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            failed = true
                        } finally {
                            saving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
            ) {
                Text(stringResource(if (saving) Res.string.saving else Res.string.save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun presetLabel(minutes: Long): String = when {
    minutes % 1440L == 0L && minutes / 1440L == 7L -> stringResource(Res.string.checks_weekly)
    minutes % 1440L == 0L -> stringResource(Res.string.checks_daily)
    minutes % 60L == 0L -> stringResource(Res.string.checks_hours_short, (minutes / 60L).toInt())
    else -> stringResource(Res.string.checks_minutes_short, minutes.toInt())
}

@Composable
internal fun checksSentence(state: SettingsUiState): String {
    if (monitoringCapabilities.execution == MonitoringExecution.Unavailable) return stringResource(Res.string.settings_checks_unavailable)
    if (!state.backgroundSync) return stringResource(Res.string.checks_sentence_off)
    val conditions = buildList {
        if (state.requireWifi) add(stringResource(Res.string.checks_cond_wifi))
        if (state.requireCharging) add(stringResource(Res.string.checks_cond_charging))
        if (state.requireBatteryNotLow) add(stringResource(Res.string.checks_cond_battery))
    }.takeIf { monitoringCapabilities.constraints }.orEmpty()
    val base = stringResource(Res.string.checks_sentence, com.bernaferrari.changedetection.ui.kit.cadence(state.syncInterval))
    return if (conditions.isEmpty()) "$base." else base + ", " + conditions.joinToString(", ") + "."
}
