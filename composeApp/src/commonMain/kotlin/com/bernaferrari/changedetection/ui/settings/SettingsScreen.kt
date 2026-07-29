/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.settings_title
import com.bernaferrari.changedetection.ui.theme.spacing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings route. Feature sections own their visual details so the route remains a concise map
 * of the settings experience.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onAboutClick: () -> Unit = {},
    onHowItWorksClick: () -> Unit = {},
    onContactClick: () -> Unit = {},
    appVersion: String = "",
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBackgroundSyncDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val spacing = MaterialTheme.spacing

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = WindowInsets(0),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = spacing.md),
        ) {
            item(key = "appearance") {
                AppearanceSettingsSection(
                    selectedTheme = uiState.themeMode,
                    onThemeSelected = viewModel::updateThemeMode,
                )
            }
            item(key = "monitoring") {
                MonitoringSettingsSection(
                    uiState = uiState,
                    onBackgroundSyncClick = { showBackgroundSyncDialog = true },
                    onBrowserDelaySelected = viewModel::updateBrowserDelay,
                    onComparisonMethodSelected = viewModel::updateComparisonMethod,
                )
            }
            item(key = "about") {
                AboutSettingsSection(
                    appVersion = appVersion,
                    onHowItWorksClick = onHowItWorksClick,
                    onAboutClick = onAboutClick,
                    onContactClick = onContactClick,
                )
            }
            item(key = "bottom-spacer") {
                Spacer(modifier = Modifier.height(spacing.xl + spacing.touchTarget))
            }
        }
    }

    if (showBackgroundSyncDialog) {
        BackgroundSyncDialog(
            onDismiss = { showBackgroundSyncDialog = false },
            viewModel = viewModel,
        )
    }
}
