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
import androidx.compose.material.icons.outlined.Description
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
import com.bernaferrari.changedetection.ui.logs.ActivityScreen
import com.bernaferrari.changedetection.ui.detail.PageDetailScreen
import com.bernaferrari.changedetection.ui.settings.BackgroundSyncDialog
import com.bernaferrari.changedetection.navigation.*
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.home.HomeScreen
import com.bernaferrari.changedetection.ui.turbo.TurboLiveScreen
import com.bernaferrari.changedetection.ui.turbo.TurboSetupSheet
import com.bernaferrari.changedetection.ui.main.MainViewModel
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
    val openNotificationSettings: (() -> Unit)? = null,
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
    var turboSetup by remember { mutableStateOf<Set<String>?>(null) }
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

    val navSites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
    val unreadCount = navSites.orEmpty().count { !it.site.isRead && it.snap != null && it.snapshotCount > 1 }
    ChangeDetectionNavigationSuite(
        backStack = backStack,
        homeBadge = unreadCount,
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
                    HomeScreen(
                        viewModel = mainViewModel,
                        selectedSiteId = (backStack.lastOrNull() as? PageDetailDestination)?.siteId ?: (backStack.lastOrNull() as? ComparisonDestination)?.siteId,
                        onAddPage = { url -> openAddPage(url) },
                        onOpenPage = { id -> backStack.openPage(id) },
                        onOpenChange = { id, snapId -> backStack.openComparison(id, snapId) },
                        onEditPage = { id -> backStack.add(EditSiteDestination(siteId = id)) },
                        onSetUpTurbo = { ids -> turboSetup = ids },
                        onOpenTurbo = { backStack.add(TurboDestination) },
                        onHowItWorks = { backStack.add(HowItWorksDestination) },
                        onEnableNotifications = platformActions.requestNotifications,
                    )
                }

                entry<TurboDestination> {
                    TurboRoute(
                        mainViewModel = mainViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onStop = { platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor() },
                        onRunAgain = { ids -> turboSetup = ids },
                        onOpenPage = { backStack.openPage(it) },
                        onOpenChange = { id, snapId -> backStack.openComparison(id, snapId) },
                    )
                }

                entry<LogsDestination> {
                    ActivityScreen(
                        onOpenChange = { siteId, snapId -> backStack.openComparison(siteId, snapId) },
                        onOpenPage = { backStack.openPage(it) },
                    )
                }

                entry<SettingsDestination> {
                    CenteredScreen {
                        SettingsScreen(
                            onAboutClick = { backStack.add(AboutDestination) },
                            onHowItWorksClick = { backStack.add(HowItWorksDestination) },
                            onContactClick = {
                                platformActions.openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                            },
                            onNotificationSettings = platformActions.openNotificationSettings,
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
                        onTurbo = { turboSetup = setOf(key.siteId) },
                        onSchedule = { showSchedule = true },
                        onOpenUrl = platformActions.openUrl,
                        onEnableNotifications = platformActions.requestNotifications,
                    )
                    if (showSchedule) BackgroundSyncDialog(onDismiss = { showSchedule = false })
                }

                entry<PageTurboDestination>(metadata = forwardTransitionMetadata) {
                    TurboRoute(
                        mainViewModel = mainViewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onStop = { platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor() },
                        onRunAgain = { ids -> turboSetup = ids },
                        onOpenPage = { backStack.openPage(it) },
                        onOpenChange = { id, snapId -> backStack.openComparison(id, snapId) },
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

    turboSetup?.let { preselected ->
        val sites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
        val turboState by mainViewModel.quickMonitorState.collectAsStateWithLifecycle()
        TurboSetupSheet(
            sites = sites.orEmpty(),
            preselected = preselected,
            state = turboState,
            onDismiss = { turboSetup = null },
            onStart = { ids, interval, duration ->
                turboSetup = null
                platformActions.startTurbo?.invoke(ids, interval, duration) ?: mainViewModel.startQuickMonitor(ids, interval, duration)
                if (backStack.lastOrNull() !is TurboDestination) backStack.add(TurboDestination)
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
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Outlined.Description,
                null,
                Modifier.padding(bottom = 8.dp).widthIn(max = 64.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
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

@Composable
private fun TurboRoute(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
    onStop: () -> Unit,
    onRunAgain: (Set<String>) -> Unit,
    onOpenPage: (String) -> Unit,
    onOpenChange: (String, String?) -> Unit,
) {
    val sites by mainViewModel.sitesFlow.collectAsStateWithLifecycle()
    val state by mainViewModel.quickMonitorState.collectAsStateWithLifecycle()
    TurboLiveScreen(
        state = state,
        sites = sites.orEmpty(),
        onBack = onBack,
        onStop = onStop,
        onRunAgain = { onRunAgain(state.siteIds) },
        onOpenPage = onOpenPage,
        onOpenChange = onOpenChange,
    )
}
