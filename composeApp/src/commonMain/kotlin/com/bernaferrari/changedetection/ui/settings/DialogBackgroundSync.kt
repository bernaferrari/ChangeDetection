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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Schedule
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

@OptIn(ExperimentalMaterial3Api::class)
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
    val spacing = MaterialTheme.spacing
    var draft by remember { mutableStateOf(uiState) }
    var initialized by remember { mutableStateOf(uiState.isLoaded) }
    LaunchedEffect(uiState.isLoaded) {
        if (uiState.isLoaded && !initialized) {
            draft = uiState
            initialized = true
        }
    }
    var valid by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = spacing.sm)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(bottom = spacing.md)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                if (!uiState.isLoaded || !initialized) {
                    SettingsLoadState(uiState.loadFailed, onRetry)
                } else {
                    BackgroundSyncHeader(
                        syncEnabled = draft.backgroundSync,
                        onToggle = { draft = draft.copy(backgroundSync = it) },
                    )

                    if (draft.backgroundSync && monitoringCapabilities.execution != MonitoringExecution.Unavailable) {
                        Column(
                            modifier = Modifier.padding(horizontal = spacing.md),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            BackgroundSyncSectionCard(
                                icon = Icons.Outlined.Schedule,
                                title = stringResource(Res.string.settings_sync_interval_label),
                            ) {
                                SyncIntervalInput(
                                    currentInterval = draft.syncInterval,
                                    onIntervalChanged = { draft = draft.copy(syncInterval = it) },
                                    onValidityChanged = { valid = it },
                                )
                            }
                            if (monitoringCapabilities.constraints) {
                                BackgroundSyncSectionCard(
                                    icon = Icons.Outlined.BatterySaver,
                                    title = stringResource(Res.string.settings_sync_constraints_title),
                                ) {
                                    SyncConstraints(
                                        requireWifi = draft.requireWifi,
                                        requireCharging = draft.requireCharging,
                                        requireBatteryNotLow = draft.requireBatteryNotLow,
                                        onWifiChange = { draft = draft.copy(requireWifi = it) },
                                        onChargingChange = { draft = draft.copy(requireCharging = it) },
                                        onBatteryChange = { draft = draft.copy(requireBatteryNotLow = it) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (failed) {
                Text(
                    stringResource(Res.string.schedule_save_failed),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = spacing.md),
                )
            }
            FilledTonalButton(
                enabled = uiState.isLoaded && initialized && monitoringCapabilities.execution != MonitoringExecution.Unavailable && (!draft.backgroundSync || valid) && !saving,
                onClick = {
                    saving = true
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
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.touchTarget)
                    .padding(horizontal = spacing.md),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(Res.string.save),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
