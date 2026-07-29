/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.how_it_works
import com.bernaferrari.changedetection.shared.generated.resources.settings_about_section
import com.bernaferrari.changedetection.shared.generated.resources.settings_about_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_action_contact
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_disabled
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_every
import com.bernaferrari.changedetection.shared.generated.resources.settings_background_sync_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_browser_delay_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.settings_browser_delay_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_subtitle
import com.bernaferrari.changedetection.shared.generated.resources.settings_comparison_title
import com.bernaferrari.changedetection.shared.generated.resources.settings_monitoring_section
import com.bernaferrari.changedetection.shared.generated.resources.settings_section_appearance
import com.bernaferrari.changedetection.shared.generated.resources.settings_version
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AppearanceSettingsSection(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(Res.string.settings_section_appearance),
        icon = Icons.Outlined.Palette,
    )
    SettingsGroup {
        ThemeSelector(
            selectedTheme = selectedTheme,
            onThemeSelected = onThemeSelected,
        )
    }
}

@Composable
internal fun MonitoringSettingsSection(
    uiState: SettingsUiState,
    onBackgroundSyncClick: () -> Unit,
    onBrowserDelaySelected: (Long) -> Unit,
    onComparisonMethodSelected: (ComparisonMethod) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(Res.string.settings_monitoring_section),
        icon = Icons.Outlined.Sync,
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
            onClick = onBackgroundSyncClick,
        )
        SettingsDivider()
        SettingsPreferenceBlock(
            icon = Icons.Outlined.Schedule,
            title = stringResource(Res.string.settings_browser_delay_title),
            subtitle = stringResource(Res.string.settings_browser_delay_subtitle),
        ) {
            BrowserDelaySelector(
                selectedDelay = uiState.browserDelayMs,
                onDelaySelected = onBrowserDelaySelected,
            )
        }
        SettingsDivider()
        SettingsPreferenceBlock(
            icon = Icons.Outlined.Code,
            title = stringResource(Res.string.settings_comparison_title),
            subtitle = stringResource(Res.string.settings_comparison_subtitle),
        ) {
            ComparisonMethodSelector(
                selectedMethod = uiState.comparisonMethod,
                onMethodSelected = onComparisonMethodSelected,
            )
        }
    }
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
        icon = Icons.Outlined.Info,
    )
    SettingsGroup {
        SettingsActionRow(
            icon = Icons.AutoMirrored.Outlined.HelpOutline,
            title = stringResource(Res.string.how_it_works),
            onClick = onHowItWorksClick,
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Outlined.Info,
            title = stringResource(Res.string.settings_about_title),
            onClick = onAboutClick,
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Outlined.Code,
            title = stringResource(Res.string.settings_version),
            value = appVersion.ifBlank { "—" },
            showChevron = false,
        )
        SettingsDivider()
        SettingsActionRow(
            icon = Icons.Outlined.Email,
            title = stringResource(Res.string.settings_action_contact),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            delays.forEach { delay ->
                val isSelected = selectedDelay == delay
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = spacing.touchTarget - spacing.xxs * 2)
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        )
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onDelaySelected(delay) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatDelay(delay),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

private fun formatDelay(delayMs: Long): String {
    val seconds = (delayMs / 1000L).coerceAtLeast(1L)
    return "${seconds}s"
}
