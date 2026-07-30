/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.ui.logs.LogsScreen
import com.bernaferrari.changedetection.navigation.*
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.components.SharedEmptyState
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.TurboScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.select_site_to_view_details
import com.bernaferrari.changedetection.shared.generated.resources.select_sites
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Platform-specific actions that the [ChangeDetectionApp] needs to perform.
 */
data class PlatformActions(
    val openUrl: (String) -> Unit = {},
    val openEmail: (email: String, subject: String) -> Unit = { _, _ -> },
    val startTurbo: (siteIds: Set<String>, intervalSeconds: Int) -> Unit = { _, _ -> },
    val stopTurbo: () -> Unit = {},
    val requestNotifications: () -> Unit = {},
    val appVersion: String = "",
)

@Composable
fun ChangeDetectionRootApp(
    windowSizeClass: WindowSizeClass,
    platformActions: PlatformActions = PlatformActions(),
    initialSiteId: String? = null,
    initialUrl: String? = null,
    onInitialUrlHandled: () -> Unit = {},
) {
    val themeViewModel: ThemeViewModel = koinViewModel()

    ChangeDetectionTheme(
        darkTheme = themeViewModel.shouldUseDarkTheme(),
        dynamicColor = themeViewModel.shouldUseDynamicColor(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ChangeDetectionApp(
                windowSizeClass = windowSizeClass,
                platformActions = platformActions,
                initialSiteId = initialSiteId,
                initialUrl = initialUrl,
                onInitialUrlHandled = onInitialUrlHandled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChangeDetectionApp(
    windowSizeClass: WindowSizeClass,
    platformActions: PlatformActions,
    initialSiteId: String? = null,
    initialUrl: String? = null,
    onInitialUrlHandled: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(MainDestination)
    val mainViewModel: MainViewModel = koinViewModel()

    // Handle deep link from notification
    androidx.compose.runtime.LaunchedEffect(initialSiteId) {
        if (initialSiteId != null) {
            backStack.add(ComparisonDestination(siteId = initialSiteId))
        }
    }
    androidx.compose.runtime.LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            backStack.add(AddEditDestination(initialUrl = initialUrl))
            onInitialUrlHandled()
        }
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    ChangeDetectionBottomNavigation(
        backStack = backStack,
        useNavigationRail = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded,
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MainDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            SharedEmptyState(
                                icon = Icons.Outlined.Visibility,
                                title = stringResource(Res.string.select_sites),
                                description = stringResource(Res.string.select_site_to_view_details),
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    ),
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        onAddSite = { backStack.add(AddEditDestination()) },
                        onOpenTurbo = { backStack.add(TurboDestination) },
                        onLearnHowItWorks = { backStack.add(HowItWorksDestination) },
                        onSiteClick = { site -> backStack.add(ComparisonDestination(siteId = site.id)) },
                        onSiteEdit = { site -> backStack.add(EditSiteDestination(siteId = site.id)) },
                    )
                }

                entry<TurboDestination> {
                    val sites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
                    val quickMonitorState by mainViewModel.quickMonitorState.collectAsStateWithLifecycle()

                    TurboScreen(
                        sites = sites ?: emptyList(),
                        quickMonitorState = quickMonitorState,
                        isLoading = sites == null,
                        onAddSite = { backStack.add(AddEditDestination()) },
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onStartTurbo = { selectedUrl, interval ->
                            platformActions.startTurbo(selectedUrl, interval)
                        },
                        onStopTurbo = {
                            platformActions.stopTurbo()
                        },
                    )
                }

                entry<LogsDestination> {
                    LogsScreen(
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSnapClick = { snapId, url ->
                            backStack.add(BrowserDestination(snapId = snapId, url = url))
                        },
                    )
                }

                entry<SettingsDestination> {
                    SettingsScreen(
                        onAboutClick = { backStack.add(AboutDestination) },
                        onHowItWorksClick = { backStack.add(HowItWorksDestination) },
                        onContactClick = {
                            platformActions.openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                        },
                        appVersion = platformActions.appVersion,
                    )
                }

                // Full-screen destinations (no bottom navigation)
                entry<AddEditDestination> { key ->
                    AddEditFlow(
                        selectedUrl = key.initialUrl,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSaveComplete = {
                            platformActions.requestNotifications()
                            backStack.removeLastOrNull()
                        },
                    )
                }

                entry<EditSiteDestination> { key ->
                    AddEditFlow(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSaveComplete = { backStack.removeLastOrNull() },
                    )
                }

                entry<AboutDestination> {
                    AboutScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onSocialClick = { url ->
                            platformActions.openUrl(url)
                        },
                        onGitHubClick = { url ->
                            platformActions.openUrl(url)
                        },
                        onContactClick = {
                            platformActions.openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                        },
                        onLicenseUrlClick = { url ->
                            platformActions.openUrl(url)
                        },
                    )
                }

                entry<HowItWorksDestination> {
                    HowItWorksScreen(
                        onBackClick = { backStack.removeLastOrNull() },
                    )
                }

                entry<BrowserDestination> { key ->
                    LocalBrowserScreen(
                        snapId = key.snapId,
                        url = key.url,
                        onDismiss = { backStack.removeLastOrNull() },
                    )
                }

                entry<ComparisonDestination>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
                    ComparisonScreen(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSiteEdit = { id -> backStack.add(EditSiteDestination(siteId = id)) },
                        onSiteDelete = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }
}
