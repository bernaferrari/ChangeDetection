/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)

package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bernaferrari.changedetection.background.MonitoringExecution
import com.bernaferrari.changedetection.background.monitoringCapabilities
import com.bernaferrari.changedetection.model.AppPalette
import com.bernaferrari.changedetection.model.ThemeMode
import com.bernaferrari.changedetection.repo.ComparisonMethod
import com.bernaferrari.changedetection.repo.supportsVisualMonitoring
import com.bernaferrari.changedetection.shared.generated.resources.*
import com.bernaferrari.changedetection.ui.kit.GroupLabel
import com.bernaferrari.changedetection.ui.kit.GroupRow
import com.bernaferrari.changedetection.ui.kit.ListGroup
import com.bernaferrari.changedetection.ui.kit.groupTile
import com.bernaferrari.changedetection.ui.kit.cadence
import com.bernaferrari.changedetection.ui.theme.marks
import com.bernaferrari.changedetection.ui.theme.supportsDynamicColors
import com.bernaferrari.changedetection.ui.kit.ConnectedChoice
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Every row shows its current value, so the whole setup can be read without opening anything. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onAboutClick: () -> Unit = {},
    onHowItWorksClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    onNotificationSettings: (() -> Unit)? = null,
    appVersion: String = "",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val writeFailed by viewModel.writeFailed.collectAsStateWithLifecycle()
    var showChecks by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (com.bernaferrari.changedetection.ui.navigation.usesNativeNavigation()) {
                com.bernaferrari.changedetection.ui.navigation.AppTopBar(title = stringResource(Res.string.settings_title), onBack = null)
            } else {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(Res.string.settings_title)) },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
                )
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (!uiState.isLoaded) {
                SettingsLoadState(uiState.loadFailed, viewModel::retryLoading)
                return@Box
            }
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
                if (writeFailed) item(key = "write-error") { SettingsWriteError(viewModel::retryPreferenceWrite) }
                item(key = "checking") {
                    GroupLabel(stringResource(Res.string.settings_group_checking), Modifier.padding(top = 0.dp))
                    ListGroup {
                        GroupRow(
                            title = stringResource(Res.string.checks_title),
                            supporting = when {
                                monitoringCapabilities.execution == MonitoringExecution.Unavailable -> stringResource(Res.string.settings_checks_unavailable)
                                !uiState.backgroundSync -> stringResource(Res.string.settings_background_sync_disabled)
                                else -> settingsChecksSummary(uiState)
                            },
                            icon = Icons.Outlined.Sync,
                            onClick = { showChecks = true },
                        )
                        if (supportsVisualMonitoring) {
                            // Small choices apply in place, like Appearance: no dialog, no Done.
                            InlineChoice(
                                title = stringResource(Res.string.settings_default_detection),
                                body = stringResource(Res.string.settings_capture_body),
                                options = ComparisonMethod.entries,
                                selected = uiState.comparisonMethod,
                                onSelect = viewModel::updateComparisonMethod,
                            ) { stringResource(if (it == ComparisonMethod.HTML) Res.string.mode_text else Res.string.mode_screenshot) }
                            ScreenshotWaitChoice(uiState.browserDelayMs, viewModel::updateBrowserDelay)
                        }
                    }
                }
                item(key = "appearance") {
                    GroupLabel(stringResource(Res.string.settings_section_appearance))
                    AppearanceControls(uiState.themeMode, uiState.appPalette, viewModel::updateThemeMode, viewModel::updateAppPalette)
                }
                item(key = "app") {
                    GroupLabel(stringResource(Res.string.settings_group_app))
                    ListGroup {
                        if (onNotificationSettings != null) {
                            GroupRow(
                                title = stringResource(Res.string.settings_notifications),
                                supporting = stringResource(Res.string.settings_notifications_body),
                                icon = Icons.Outlined.Notifications,
                                onClick = onNotificationSettings,
                            )
                        }
                        SavedVersionsRow(uiState, viewModel::retryStorageLoading, showDivider = onNotificationSettings != null)
                    }
                }
                item(key = "help") {
                    GroupLabel(stringResource(Res.string.settings_help_section))
                    ListGroup {
                        GroupRow(stringResource(Res.string.how_it_works), icon = Icons.AutoMirrored.Outlined.HelpOutline, onClick = onHowItWorksClick)
                        GroupRow(stringResource(Res.string.about_title), supporting = stringResource(Res.string.settings_about_value, appVersion), icon = Icons.Outlined.Info, showDivider = true, onClick = onAboutClick)
                        GroupRow(stringResource(Res.string.settings_feedback), icon = Icons.Outlined.MailOutline, showDivider = true, onClick = onContactClick)
                    }
                }
                item(key = "privacy") {
                    Row(Modifier.fillMaxWidth().padding(top = 28.dp, start = 32.dp, end = 32.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VerifiedUser, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(Res.string.settings_privacy_line), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showChecks) BackgroundSyncDialog(onDismiss = { showChecks = false }, viewModel = viewModel)
}

/** A titled tile with a one-line explanation and a connected button group that applies on tap. */
@Composable
private fun <T> InlineChoice(title: String, body: String, options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    Column(Modifier.groupTile().padding(vertical = 16.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ConnectedChoice(options, selected, onSelect, label = label)
    }
}

@Composable
private fun settingsChecksSummary(state: SettingsUiState): String {
    val conditions = if (monitoringCapabilities.constraints) {
        listOfNotNull(
            stringResource(Res.string.checks_wifi).takeIf { state.requireWifi },
            stringResource(Res.string.checks_charging).takeIf { state.requireCharging },
            stringResource(Res.string.checks_battery).takeIf { state.requireBatteryNotLow },
        )
    } else {
        emptyList()
    }
    return (listOf(cadence(state.syncInterval).replaceFirstChar { it.uppercase() }) + conditions).joinToString(" · ")
}

/**
 * Appearance, right on the Settings page: the theme as three connected buttons and every palette as one row
 * of swatches (wallpaper colors first, marked with a sparkle). Choices apply instantly; the app is the preview.
 */
@Composable
internal fun AppearanceControls(
    theme: ThemeMode,
    palette: AppPalette,
    onTheme: (ThemeMode) -> Unit,
    onPalette: (AppPalette) -> Unit,
) {
    ListGroup {
        Column(Modifier.groupTile().padding(vertical = 16.dp).selectableGroup()) {
            ConnectedChoice(ThemeMode.entries, theme, onTheme) { stringResource(it.titleRes) }
        }
        Column(Modifier.groupTile().padding(vertical = 16.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.settings_palette_label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(palette.titleRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppPalette.entries
                    .filter { it != AppPalette.DYNAMIC || supportsDynamicColors() }
                    .sortedBy { if (it == AppPalette.DYNAMIC) 0 else 1 }
                    .forEach { option ->
                        com.bernaferrari.changedetection.ui.components.PaletteSwatch(
                            colors = com.bernaferrari.changedetection.ui.components.paletteSwatchColors(option),
                            selected = option == palette,
                            label = stringResource(option.titleRes),
                            onClick = { onPalette(option) },
                            icon = if (option == AppPalette.DYNAMIC) Icons.Outlined.AutoAwesome else null,
                        )
                    }
            }
        }
    }
}
