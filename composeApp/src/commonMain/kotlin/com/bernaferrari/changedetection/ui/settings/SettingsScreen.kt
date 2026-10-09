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
    var showDelay by remember { mutableStateOf(false) }
    var showMethod by remember { mutableStateOf(false) }
    var showAppearance by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, scrolledContainerColor = MaterialTheme.colorScheme.surface),
            )
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
                            GroupRow(
                                title = stringResource(Res.string.settings_default_detection),
                                supporting = stringResource(if (uiState.comparisonMethod == ComparisonMethod.HTML) Res.string.mode_text else Res.string.mode_screenshot),
                                icon = Icons.Outlined.Code,
                                showDivider = true,
                                onClick = { showMethod = true },
                            )
                            GroupRow(
                                title = stringResource(Res.string.settings_screenshot_wait),
                                supporting = stringResource(Res.string.settings_screenshot_wait_value, (uiState.browserDelayMs / 1000).toInt()),
                                icon = Icons.Outlined.Schedule,
                                showDivider = true,
                                onClick = { showDelay = true },
                            )
                        }
                    }
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
                        GroupRow(
                            title = stringResource(Res.string.settings_section_appearance),
                            supporting = stringResource(uiState.themeMode.titleRes) + " · " + stringResource(uiState.appPalette.titleRes),
                            icon = Icons.Outlined.Palette,
                            showDivider = onNotificationSettings != null,
                            onClick = { showAppearance = true },
                        )
                        GroupRow(
                            title = stringResource(Res.string.settings_saved_versions),
                            supporting = when {
                                uiState.storageLoadFailed -> stringResource(Res.string.retry)
                                uiState.storageBytes == null -> stringResource(Res.string.loading)
                                else -> stringResource(Res.string.settings_storage_value, formatStorage(uiState.storageBytes!!))
                            },
                            icon = Icons.Outlined.Storage,
                            showDivider = true,
                            onClick = if (uiState.storageLoadFailed) viewModel::retryStorageLoading else null,
                        )
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
    if (showDelay) {
        BrowserDelayChoiceDialog(selectedDelay = uiState.browserDelayMs, onDismiss = { showDelay = false }, onConfirm = {
            viewModel.updateBrowserDelay(it)
            showDelay = false
        })
    }
    if (showMethod) {
        ComparisonMethodChoiceDialog(selectedMethod = uiState.comparisonMethod, onDismiss = { showMethod = false }, onConfirm = {
            viewModel.updateComparisonMethod(it)
            showMethod = false
        })
    }
    if (showAppearance) {
        AppearanceSheet(uiState.themeMode, uiState.appPalette, viewModel::updateThemeMode, viewModel::updateAppPalette) { showAppearance = false }
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

private fun formatStorage(bytes: Long): String = when {
    bytes == 0L -> "0 MB"
    bytes < 1024 * 1024 -> "<1 MB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${(bytes * 10 / (1024L * 1024 * 1024)) / 10.0} GB"
}

/** Theme as three connected buttons; color as the brand look, wallpaper colors, or a seed swatch. */
@Composable
internal fun AppearanceSheet(
    theme: ThemeMode,
    palette: AppPalette,
    onTheme: (ThemeMode) -> Unit,
    onPalette: (AppPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 32.dp).selectableGroup()) {
            Text(stringResource(Res.string.settings_section_appearance), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.headlineSmall)
            GroupLabel(stringResource(Res.string.settings_theme_label))
            ConnectedChoice(ThemeMode.entries, theme, onTheme) { stringResource(it.titleRes) }
            GroupLabel(stringResource(Res.string.settings_palette_label))
            PaletteRow(AppPalette.DEFAULT, palette, Icons.Outlined.Palette, stringResource(Res.string.palette_highlighter_body), onPalette)
            if (supportsDynamicColors()) {
                PaletteRow(AppPalette.DYNAMIC, palette, Icons.Outlined.AutoAwesome, stringResource(Res.string.palette_dynamic_body), onPalette)
            }
            Text(stringResource(Res.string.palette_more), Modifier.padding(start = 24.dp, top = 16.dp, bottom = 10.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppPalette.entries.filter { it != AppPalette.DEFAULT && it != AppPalette.DYNAMIC }.forEach { option ->
                    val selected = option == palette
                    val name = stringResource(option.titleRes)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(option.seedColor)
                            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton) { onPalette(option) }
                            .semantics {
                                this.selected = selected
                                contentDescription = name
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, null, tint = if (option.seedColor.luminance() > 0.179f) Color.Black else Color.White)
                    }
                }
            }
            Text(
                stringResource(Res.string.palette_note),
                Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PaletteRow(option: AppPalette, current: AppPalette, icon: androidx.compose.ui.graphics.vector.ImageVector, body: String, onSelect: (AppPalette) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = option == current, role = Role.RadioButton) { onSelect(option) }.padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (option == AppPalette.DEFAULT) MaterialTheme.marks.highlight else MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (option == AppPalette.DEFAULT) MaterialTheme.marks.onHighlight else MaterialTheme.colorScheme.onSurface) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(option.titleRes), style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = option == current, onClick = null)
    }
}
