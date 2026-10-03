/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.addedit.AddPageDialog
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.ui.logs.LogsScreen
import com.bernaferrari.changedetection.ui.detail.PageDetailScreen
import com.bernaferrari.changedetection.ui.settings.BackgroundSyncDialog
import com.bernaferrari.changedetection.navigation.*
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.components.SignalEmptyState
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.TurboScreen
import com.bernaferrari.changedetection.ui.main.ChangeRibbon
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionNavigationSuite
import com.bernaferrari.changedetection.ui.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.shared.generated.resources.Res
import com.bernaferrari.changedetection.shared.generated.resources.select_site_canvas_label
import com.bernaferrari.changedetection.shared.generated.resources.select_site_canvas_title
import com.bernaferrari.changedetection.shared.generated.resources.select_site_canvas_supporting
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Platform-specific actions that the [ChangeDetectionApp] needs to perform.
 */
data class PlatformActions(
    val openUrl: (String) -> Unit = {},
    val openEmail: (email: String, subject: String) -> Unit = { _, _ -> },
    val startTurbo: ((siteIds: Set<String>, intervalSeconds: Int, durationMinutes: Int) -> Unit)? = null,
    val stopTurbo: (() -> Unit)? = null,
    val requestNotifications: () -> Unit = {},
    val openNotificationSettings: () -> Unit = {},
    val appVersion: String = "",
)

@Composable
fun ChangeDetectionRootApp(
    platformActions: PlatformActions = PlatformActions(),
    initialSiteId: String? = null,
    initialSnapId: String? = null,
    initialUrl: String? = null,
    onInitialSiteIdHandled: () -> Unit = {},
    onInitialSnapIdHandled: () -> Unit = {},
    onInitialUrlHandled: () -> Unit = {},
) {
    val themeViewModel: ThemeViewModel = koinViewModel()
    val appPalette by themeViewModel.appPalette.collectAsStateWithLifecycle()

    ChangeDetectionTheme(
        darkTheme = themeViewModel.shouldUseDarkTheme(),
        dynamicColor = appPalette.usesSystemColors,
        seedColor = appPalette.seedColor,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            ChangeDetectionApp(
                platformActions = platformActions,
                initialSiteId = initialSiteId,
                initialSnapId = initialSnapId,
                initialUrl = initialUrl,
                onInitialSiteIdHandled = onInitialSiteIdHandled,
                onInitialSnapIdHandled = onInitialSnapIdHandled,
                onInitialUrlHandled = onInitialUrlHandled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChangeDetectionApp(
    platformActions: PlatformActions,
    initialSiteId: String? = null,
    initialSnapId: String? = null,
    initialUrl: String? = null,
    onInitialSiteIdHandled: () -> Unit = {},
    onInitialSnapIdHandled: () -> Unit = {},
    onInitialUrlHandled: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(appNavSavedStateConfiguration, MainDestination)
    val mainViewModel: MainViewModel = koinViewModel()
    var addPageSession by remember { mutableIntStateOf(0) }
    var addPageUrl by remember { mutableStateOf<String?>(null) }

    fun openAddPage(url: String? = null) {
        addPageUrl = url
        addPageSession += 1
    }

    // Handle deep link from notification
    androidx.compose.runtime.LaunchedEffect(initialSiteId, initialSnapId) {
        if (!initialSiteId.isNullOrBlank()) {
            backStack.openComparison(initialSiteId, initialSnapId)
            onInitialSiteIdHandled()
            onInitialSnapIdHandled()
        }
    }
    androidx.compose.runtime.LaunchedEffect(initialUrl) {
        if (!initialUrl.isNullOrBlank()) {
            openAddPage(initialUrl)
            onInitialUrlHandled()
        }
    }
    val paneScaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfoV2())
    val singlePaneLayout = paneScaffoldDirective.maxHorizontalPartitions == 1
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = paneScaffoldDirective,
    )
    val forwardTransitionMetadata = rememberForwardTransitionMetadata()
    val comparisonMetadata = remember(singlePaneLayout, forwardTransitionMetadata) {
        val paneMetadata = ListDetailSceneStrategy.detailPane()
        if (singlePaneLayout) paneMetadata + forwardTransitionMetadata else paneMetadata
    }

    @Composable
    fun CenteredScreen(content: @Composable () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                content()
            }
        }
    }

    ChangeDetectionNavigationSuite(
        backStack = backStack,
        retainNavigationForDetail = !singlePaneLayout && (backStack.lastOrNull() is ComparisonDestination || backStack.lastOrNull() is PageDetailDestination),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = rememberAppEntryDecorators(),
            entryProvider = entryProvider {
                entry<MainDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            MonitorDetailPlaceholder(modifier = Modifier.fillMaxSize())
                        },
                    ),
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        onEnableNotifications = platformActions.requestNotifications,
                        onNotificationSettings = platformActions.openNotificationSettings,
                        selectedSiteId = (backStack.lastOrNull() as? PageDetailDestination)?.siteId ?: (backStack.lastOrNull() as? ComparisonDestination)?.siteId,
                        onAddSite = { openAddPage() },
                        onLearnHowItWorks = { backStack.add(HowItWorksDestination) },
                        onOpenTurbo = { backStack.add(TurboDestination) },
                        onSiteClick = { site -> backStack.openPage(site.id) },
                        onSiteEdit = { site -> backStack.add(EditSiteDestination(siteId = site.id)) },
                    )
                }

                entry<TurboDestination> {
                    val sites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
                    val quickMonitorState by mainViewModel.quickMonitorState.collectAsStateWithLifecycle()

                    TurboScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onOpenSite = { backStack.openPage(it) },
                        sites = sites ?: emptyList(),
                        quickMonitorState = quickMonitorState,
                        isLoading = sites == null,
                        onAddSite = { openAddPage() },
                        onStartTurbo = { selectedUrl, interval, duration ->
                            platformActions.startTurbo?.invoke(selectedUrl, interval, duration)
                                ?: mainViewModel.startQuickMonitor(selectedUrl, interval, duration)
                        },
                        onStopTurbo = {
                            platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor()
                        },
                    )
                }

                entry<LogsDestination> {
                    CenteredScreen {
                        LogsScreen(
                            onSnapClick = { siteId, snapId ->
                                backStack.openComparison(siteId, snapId)
                            },
                            onSnapOpenInBrowser = { snapId, url ->
                                backStack.add(BrowserDestination(snapId = snapId, url = url))
                            },
                        )
                    }
                }

                entry<SettingsDestination> {
                    CenteredScreen {
                        SettingsScreen(
                            onAboutClick = { backStack.add(AboutDestination) },
                            onHowItWorksClick = { backStack.add(HowItWorksDestination) },
                            onContactClick = {
                                platformActions.openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                            },
                            appVersion = platformActions.appVersion,
                        )
                    }
                }

                entry<PageDetailDestination>(metadata = comparisonMetadata) { key ->
                    var showSchedule by remember { mutableStateOf(false) }
                    PageDetailScreen(
                        siteId = key.siteId,
                        viewModel = mainViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onCompare = { snapId -> backStack.openComparison(key.siteId, snapId) },
                        onHistory = { snapId -> backStack.openComparison(key.siteId, snapId, showHistory = true) },
                        onEdit = { backStack.add(EditSiteDestination(key.siteId)) },
                        onTurbo = { backStack.add(PageTurboDestination(key.siteId)) },
                        onSchedule = { showSchedule = true },
                        onOpenUrl = platformActions.openUrl,
                    )
                    if (showSchedule) BackgroundSyncDialog(onDismiss = { showSchedule = false })
                }

                entry<PageTurboDestination>(metadata = forwardTransitionMetadata) { key ->
                    val sites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
                    val state by mainViewModel.quickMonitorState.collectAsStateWithLifecycle()
                    TurboScreen(
                        sites = sites.orEmpty(), quickMonitorState = state, isLoading = sites == null,
                        initialSiteId = key.siteId, onBack = { backStack.removeLastOrNull() },
                        onAddSite = { openAddPage() },
                        onOpenSite = { backStack.openPage(it) },
                        onStartTurbo = { ids, interval, duration ->
                            platformActions.startTurbo?.invoke(ids, interval, duration) ?: mainViewModel.startQuickMonitor(ids, interval, duration)
                        },
                        onStopTurbo = { platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor() },
                    )
                }

                entry<EditSiteDestination>(metadata = forwardTransitionMetadata) { key ->
                    AddEditFlow(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSaveComplete = { backStack.removeLastOrNull() },
                    )
                }

                entry<AboutDestination>(metadata = forwardTransitionMetadata) {
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

                entry<HowItWorksDestination>(metadata = forwardTransitionMetadata) {
                    HowItWorksScreen(
                        onBackClick = { backStack.removeLastOrNull() },
                    )
                }

                entry<BrowserDestination>(metadata = forwardTransitionMetadata) { key ->
                    LocalBrowserScreen(
                        snapId = key.snapId,
                        url = key.url,
                        onDismiss = { backStack.removeLastOrNull() },
                    )
                }

                entry<ComparisonDestination>(metadata = comparisonMetadata) { key ->
                    ComparisonScreen(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        showNavigateUp = singlePaneLayout,
                        initialSnapId = key.initialSnapId,
                        showHistoryInitially = key.showHistory,
                        onSiteEdit = { id -> backStack.add(EditSiteDestination(siteId = id)) },
                        onSiteDelete = { backStack.removeLastOrNull() },
                    )
                }
            },
        )
    }

    if (addPageSession > 0) {
        AddPageDialog(
            sessionKey = addPageSession,
            selectedUrl = addPageUrl,
            onDismiss = {
                addPageSession = 0
                addPageUrl = null
            },
            onSaveComplete = {
                platformActions.requestNotifications()
                addPageSession = 0
                addPageUrl = null
            },
            onOpenSite = { siteId ->
                backStack.openPage(siteId)
            },
        )
    }
}

@Composable
private fun MonitorDetailPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(48.dp).widthIn(max = 480.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ChangeRibbon(colors = emptyList(), isListening = false, modifier = Modifier.widthIn(max = 320.dp))
            androidx.compose.material3.Text(
                text = stringResource(Res.string.select_site_canvas_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.material3.Text(
                text = stringResource(Res.string.select_site_canvas_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            androidx.compose.material3.Text(
                text = stringResource(Res.string.select_site_canvas_supporting),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
