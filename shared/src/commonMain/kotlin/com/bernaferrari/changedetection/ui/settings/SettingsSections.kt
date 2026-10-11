/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.background.monitoringCapabilities

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.History
import com.bernaferrari.changedetection.repo.supportsVisualMonitoring
import com.bernaferrari.changedetection.ui.components.monitoringCadenceLabel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.ui.components.SegmentPosition
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.dialog_cancel
import com.bernaferrari.changedetection.shared.generated.resources.done
import com.bernaferrari.changedetection.shared.generated.resources.settings_about_section
import com.bernaferrari.changedetection.shared.generated.resources.settings_about_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_action_contact
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_disabled
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_every
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_summary
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_browser_delay_summary
import com.bernaferrari.changedetection.shared.generated.resources.settings_browser_delay_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.settings_browser_delay_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_summary
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_page_text
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_screenshots
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_monitoring_section
import com.bernaferrari.changedetection.shared.generated.resources.settings_section_appearance
import com.bernaferrari.changedetection.shared.generated.resources.settings_version
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeviceSettingsSection(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (AppPalette) -> Unit,
    onRetryStorage: () -> Unit,
    writeFailed: Boolean = false,
    onRetryWrite: () -> Unit = {},
) {
    SettingsSectionHeader(title = stringResource(Res.string.settings_on_this_device))
    SettingsGroup {
        AppearanceSettingsRow(uiState.themeMode, uiState.appPalette, onThemeSelected, onPaletteSelected, writeFailed, onRetryWrite, SegmentPosition.First)
        HistoryStorageRow(uiState.storageBytes, uiState.storageLoadFailed, onRetryStorage, SegmentPosition.Last)
    }
    if (uiState.storageLoadFailed) TextButton(onClick = onRetryStorage, modifier = Modifier.padding(horizontal = 24.dp)) { Text(stringResource(Res.string.retry)) }
}

@Composable
private fun AppearanceSettingsRow(
    selectedTheme: ThemeMode,
    selectedPalette: AppPalette,
    onThemeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (AppPalette) -> Unit,
    writeFailed: Boolean,
    onRetryWrite: () -> Unit,
    position: SegmentPosition,
) {
    var expanded by remember { mutableStateOf(false) }
    SettingsActionRow(
        icon = Icons.Outlined.Palette,
        title = stringResource(Res.string.settings_theme_and_color),
        value = "${stringResource(selectedTheme.titleRes)} · ${stringResource(selectedPalette.titleRes)}",
        position = position,
        onClick = { expanded = true },
    )
    if (expanded) {
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(Res.string.settings_section_appearance)) },
            text = {
                Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                    if (writeFailed) SettingsWriteError(onRetryWrite)
                    ThemeSelector(selectedTheme, selectedPalette, onThemeSelected, onPaletteSelected, Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(Res.string.done)) } },
        )
    }
}

@Composable
internal fun MonitoringSettingsSection(
    uiState: SettingsUiState,
    onBackgroundSyncClick: () -> Unit,
    onBrowserDelayClick: () -> Unit,
    onComparisonMethodClick: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(Res.string.settings_monitoring_section),
    )
    SettingsGroup {
        SettingsActionRow(
            icon = Icons.Outlined.Sync,
            title = stringResource(if (monitoringCapabilities.execution == MonitoringExecution.Background) Res.string.settings_background_sync_title else Res.string.settings_automatic_checks_title),
            value = if (uiState.backgroundSync) {
                monitoringCadenceLabel(uiState.syncInterval)
            } else {
                stringResource(Res.string.settings_background_sync_disabled)
            },
            subtitle = stringResource(
                when (monitoringCapabilities.execution) {
                    MonitoringExecution.Background -> Res.string.settings_background_sync_summary
                    MonitoringExecution.WhileOpen -> Res.string.settings_checks_while_open
                    MonitoringExecution.Unavailable -> Res.string.settings_checks_unavailable
                },
            ),
            position = if (supportsVisualMonitoring) SegmentPosition.First else SegmentPosition.Only,
            onClick = onBackgroundSyncClick,
        )
        if (supportsVisualMonitoring) {
            SettingsActionRow(
                icon = Icons.Outlined.Schedule,
                title = stringResource(Res.string.settings_browser_delay_title),
                subtitle = stringResource(Res.string.settings_browser_delay_summary),
                value = formatDelay(uiState.browserDelayMs),
                position = SegmentPosition.Middle,
                onClick = onBrowserDelayClick,
            )
        }
        if (supportsVisualMonitoring) {
            SettingsActionRow(
                icon = Icons.Outlined.Code,
                title = stringResource(Res.string.settings_comparison_title),
                subtitle = stringResource(Res.string.settings_comparison_summary),
                value = stringResource(
                    if (uiState.comparisonMethod == ComparisonMethod.HTML) {
                        Res.string.settings_comparison_page_text
                    } else {
                        Res.string.settings_comparison_screenshots
                    },
                ),
                position = SegmentPosition.Last,
                onClick = onComparisonMethodClick,
            )
        }
    }
}

@Composable
internal fun BrowserDelayChoiceDialog(
    selectedDelay: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var pendingDelay by remember(selectedDelay) { mutableLongStateOf(selectedDelay) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_browser_delay_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Text(
                    stringResource(Res.string.settings_browser_delay_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BrowserDelaySelector(selectedDelay = pendingDelay, onDelaySelected = { pendingDelay = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingDelay) }) {
                Text(stringResource(Res.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
internal fun ComparisonMethodChoiceDialog(
    selectedMethod: ComparisonMethod,
    onDismiss: () -> Unit,
    onConfirm: (ComparisonMethod) -> Unit,
) {
    var pendingMethod by remember(selectedMethod) { mutableStateOf(selectedMethod) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_comparison_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                Text(
                    stringResource(Res.string.settings_comparison_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ComparisonMethodSelector(selectedMethod = pendingMethod, onMethodSelected = { pendingMethod = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingMethod) }) {
                Text(stringResource(Res.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

@Composable
internal fun AboutSettingsSection(
    appVersion: String,
    onHowItWorksClick: () -> Unit,
    onAboutClick: () -> Unit,
    onContactClick: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(Res.string.settings_help_section),
    )
    SettingsGroup {
        SettingsActionRow(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = stringResource(Res.string.how_it_works),
            position = SegmentPosition.First,
            onClick = onHowItWorksClick,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Info,
            title = stringResource(Res.string.settings_about_title),
            position = SegmentPosition.Middle,
            onClick = onAboutClick,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Email,
            title = stringResource(Res.string.settings_action_contact),
            position = SegmentPosition.Last,
            onClick = onContactClick,
        )
    }
    Text(
        stringResource(Res.string.settings_version) + " · " + appVersion.ifBlank { "—" },
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 20.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BrowserDelaySelector(
    selectedDelay: Long,
    onDelaySelected: (Long) -> Unit,
) {
    val delays = listOf(2000L, 5000L, 10000L, 15000L)
    com.bernaferrari.changedetection.ui.kit.ConnectedChoice(delays, selectedDelay, onDelaySelected, Modifier) { formatDelay(it) }
}

private fun formatDelay(delayMs: Long): String {
    val seconds = (delayMs / 1000L).coerceAtLeast(1L)
    return "${seconds}s"
}

@Composable
private fun HistoryStorageRow(bytes: Long?, loadFailed: Boolean, onRetry: () -> Unit, position: SegmentPosition) {
    var expanded by remember { mutableStateOf(false) }
    val usage = if (loadFailed) {
        stringResource(Res.string.storage_history_load_failed)
    } else if (bytes == null) {
        stringResource(Res.string.loading)
    } else {
        stringResource(Res.string.storage_history_usage, if (bytes in 1..1_048_575) "<1" else (bytes / 1_048_576L).toString())
    }
    SettingsActionRow(
        icon = Icons.Outlined.History,
        title = stringResource(Res.string.storage_history_title),
        subtitle = usage,
        position = position,
        onClick = { expanded = true },
    )
    if (expanded) HistoryStorageDialog(bytes, loadFailed, onRetry, onDismiss = { expanded = false })
}

@Composable
internal fun HistoryStorageDialog(bytes: Long?, loadFailed: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    val usage = when {
        loadFailed -> stringResource(Res.string.storage_history_load_failed)
        bytes == null -> stringResource(Res.string.loading)
        else -> stringResource(Res.string.storage_history_usage, if (bytes in 1..1_048_575) "<1" else com.bernaferrari.changedetection.util.localizedNumber((bytes / 1_048_576L).toDouble(), maxFractionDigits = 0))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.storage_history_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(usage, style = MaterialTheme.typography.titleMedium)
                if (loadFailed) TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
                Text(
                    stringResource(Res.string.storage_history_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.done)) } },
    )
}
