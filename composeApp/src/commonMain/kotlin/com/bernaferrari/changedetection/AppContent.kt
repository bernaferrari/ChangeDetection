/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.bernaferrari.changedetection

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.addedit.AddPageDialog
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.ui.logs.LogsScreen
import com.bernaferrari.changedetection.navigation.*
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.components.SignalEmptyState
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.TurboScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionNavigationSuite
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
    val startTurbo: ((siteIds: Set<String>, intervalSeconds: Int) -> Unit)? = null,
    val stopTurbo: (() -> Unit)? = null,
    val requestNotifications: () -> Unit = {},
    val appVersion: String = "",
)

@Composable
fun ChangeDetectionRootApp(
    platformActions: PlatformActions = PlatformActions(),
    initialSiteId: String? = null,
    initialUrl: String? = null,
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
                initialUrl = initialUrl,
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
    initialUrl: String? = null,
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
    androidx.compose.runtime.LaunchedEffect(initialSiteId) {
        if (initialSiteId != null) {
            backStack.openComparison(initialSiteId)
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
    val motionScheme = MaterialTheme.motionScheme
    val forwardTransitionMetadata = remember(motionScheme) {
        NavDisplay.transitionSpec {
            ContentTransform(
                targetContentEnter = slideInHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    initialOffsetX = { fullWidth -> fullWidth },
                ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec()),
                initialContentExit = ExitTransition.None,
            )
        } + NavDisplay.popTransitionSpec {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = slideOutHorizontally(
                    animationSpec = motionScheme.defaultSpatialSpec(),
                    targetOffsetX = { fullWidth -> fullWidth },
                ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
            )
        }
    }
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
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategies = listOf(listDetailStrategy),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MainDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            SignalEmptyState(
                                title = stringResource(Res.string.select_sites),
                                description = stringResource(Res.string.select_site_to_view_details),
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    ),
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        selectedSiteId = (backStack.lastOrNull() as? ComparisonDestination)?.siteId,
                        onAddSite = { openAddPage() },
                        onLearnHowItWorks = { backStack.add(HowItWorksDestination) },
                        onSiteClick = { site -> backStack.openComparison(site.id) },
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
                        onAddSite = { openAddPage() },
                        onStartTurbo = { selectedUrl, interval ->
                            platformActions.startTurbo?.invoke(selectedUrl, interval)
                                ?: mainViewModel.startQuickMonitor(selectedUrl, interval)
                        },
                        onStopTurbo = {
                            platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor()
                        },
                    )
                }

                entry<LogsDestination> {
                    CenteredScreen {
                        LogsScreen(
                            onSnapClick = { snapId, url ->
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
        )
    }
}
