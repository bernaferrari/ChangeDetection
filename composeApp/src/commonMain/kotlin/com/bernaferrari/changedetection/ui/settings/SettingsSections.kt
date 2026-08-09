/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
internal fun AppearanceSettingsSection(
    selectedTheme: ThemeMode,
    selectedPalette: AppPalette,
    onThemeSelected: (ThemeMode) -> Unit,
    onPaletteSelected: (AppPalette) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(Res.string.settings_section_appearance),
    )
    ThemeSelector(
        selectedTheme = selectedTheme,
        selectedPalette = selectedPalette,
        onThemeSelected = onThemeSelected,
        onPaletteSelected = onPaletteSelected,
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.md),
    )
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
            title = stringResource(Res.string.settings_background_sync_title),
            value = if (uiState.backgroundSync) {
                stringResource(Res.string.settings_background_sync_every, uiState.syncInterval.toInt())
            } else {
                stringResource(Res.string.settings_background_sync_disabled)
            },
            subtitle = stringResource(Res.string.settings_background_sync_summary),
            position = SegmentPosition.First,
            onClick = onBackgroundSyncClick,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Schedule,
            title = stringResource(Res.string.settings_browser_delay_title),
            subtitle = stringResource(Res.string.settings_browser_delay_summary),
            value = formatDelay(uiState.browserDelayMs),
            position = SegmentPosition.Middle,
            onClick = onBrowserDelayClick,
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
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
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
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
        title = stringResource(Res.string.settings_about_section),
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
            icon = Icons.Outlined.Code,
            title = stringResource(Res.string.settings_version),
            value = appVersion.ifBlank { "—" },
            showChevron = false,
            position = SegmentPosition.Middle,
        )
        SettingsActionRow(
            icon = Icons.Outlined.Email,
            title = stringResource(Res.string.settings_action_contact),
            position = SegmentPosition.Last,
            onClick = onContactClick,
        )
    }
}

@Composable
private fun BrowserDelaySelector(
    selectedDelay: Long,
    onDelaySelected: (Long) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val delays = listOf(2000L, 5000L, 10000L, 15000L)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        delays.forEachIndexed { index, delay ->
            val isSelected = selectedDelay == delay
            ToggleButton(
                checked = isSelected,
                onCheckedChange = { checked -> if (checked) onDelaySelected(delay) },
                modifier = Modifier.weight(1f).heightIn(min = spacing.touchTarget),
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    delays.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.toggleButtonColors(
                    checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(
                    text = formatDelay(delay),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun formatDelay(delayMs: Long): String {
    val seconds = (delayMs / 1000L).coerceAtLeast(1L)
    return "${seconds}s"
}
