/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.done
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val spacing = MaterialTheme.spacing

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            BackgroundSyncHeader(
                syncEnabled = uiState.backgroundSync,
                onToggle = viewModel::updateBackgroundSync,
            )

            if (uiState.backgroundSync) {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    BackgroundSyncSectionCard(
                        icon = Icons.Outlined.Schedule,
                        title = stringResource(Res.string.settings_sync_interval_label),
                    ) {
                        SyncIntervalInput(
                            currentInterval = uiState.syncInterval,
                            onIntervalChanged = viewModel::updateSyncInterval,
                        )
                    }
                    BackgroundSyncSectionCard(
                        icon = Icons.Outlined.BatterySaver,
                        title = stringResource(Res.string.settings_sync_constraints_title),
                    ) {
                        SyncConstraints(
                            requireWifi = uiState.requireWifi,
                            requireCharging = uiState.requireCharging,
                            requireBatteryNotLow = uiState.requireBatteryNotLow,
                            requireDeviceIdle = uiState.requireDeviceIdle,
                            onWifiChange = viewModel::updateWifiConstraint,
                            onChargingChange = viewModel::updateChargingConstraint,
                            onBatteryChange = viewModel::updateBatteryNotLowConstraint,
                            onIdleChange = viewModel::updateDeviceIdleConstraint,
                        )
                    }
                }
            }

            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = spacing.touchTarget)
                    .padding(horizontal = spacing.md),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = stringResource(Res.string.done),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
