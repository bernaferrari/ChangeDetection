/*
 * Copyright (c) 2024 ChangeDetection Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.bernaferrari.changedetection

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.di.initKoinIos
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.navigation.*
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.logs.LogsScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.TurboScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.platform.openEmail
import com.bernaferrari.changedetection.platform.openUrl
import org.koin.compose.viewmodel.koinViewModel
import platform.UIKit.UIViewController

/**
 * Creates the main view controller for iOS.
 * Initializes Koin and loads the Change Detection app with full navigation.
 */
fun MainViewController(): UIViewController {
    // Initialize Koin with iOS modules
    initKoinIos()

    return ComposeUIViewController {
        val themeViewModel: ThemeViewModel = koinViewModel()

        ChangeDetectionTheme(
            darkTheme = themeViewModel.shouldUseDarkTheme(),
            dynamicColor = false, // No dynamic color on iOS
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                IosNavigationApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun IosNavigationApp() {
    val backStack = remember { NavBackStack<NavKey>(MainDestination) }
    val mainViewModel: MainViewModel = koinViewModel()

    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    ChangeDetectionBottomNavigation(
        backStack = backStack,
        useNavigationRail = false, // iOS uses bottom bar, not rail
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategy = listDetailStrategy,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MainDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Select a site to view details")
                            }
                        },
                    ),
                ) {
                    MainScreen(
                        viewModel = mainViewModel,
                        onAddSite = { backStack.add(AddEditDestination()) },
                        onLearnHowItWorks = { backStack.add(HowItWorksDestination) },
                        onSiteClick = { site -> backStack.add(ComparisonDestination(siteId = site.id)) },
                        onSiteEdit = { site -> backStack.add(EditSiteDestination(siteId = site.id)) },
                    )
                }

                entry<TurboDestination> {
                    val sites by mainViewModel.sitesFlow.collectAsState()
                    val quickMonitorState by mainViewModel.quickMonitorState.collectAsState()

                    TurboScreen(
                        sites = sites ?: emptyList(),
                        quickMonitorState = quickMonitorState,
                        isLoading = sites == null,
                        onAddSite = { backStack.add(AddEditDestination()) },
                        onStartTurbo = { selectedUrl, interval ->
                            mainViewModel.startQuickMonitor(selectedUrl, interval)
                        },
                        onStopTurbo = {
                            mainViewModel.stopQuickMonitor()
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
                            openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                        },
                        appVersion = platform.Foundation.NSBundle.mainBundle
                            .objectForInfoDictionaryKey("CFBundleShortVersionString")
                            ?.toString()
                            .orEmpty(),
                    )
                }

                // Full-screen destinations
                entry<AddEditDestination>(
                    metadata = NavDisplay.transitionSpec {
                        slideInVertically(initialOffsetY = { it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    } + NavDisplay.popTransitionSpec {
                        slideInVertically(initialOffsetY = { -it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    },
                ) { key ->
                    AddEditFlow(
                        selectedUrl = key.initialUrl,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSaveComplete = { backStack.removeLastOrNull() },
                    )
                }

                entry<EditSiteDestination>(
                    metadata = NavDisplay.transitionSpec {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    } + NavDisplay.popTransitionSpec {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                    },
                ) { key ->
                    AddEditFlow(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSaveComplete = { backStack.removeLastOrNull() },
                    )
                }

                entry<AboutDestination>(
                    metadata = NavDisplay.transitionSpec {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    } + NavDisplay.popTransitionSpec {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                    },
                ) {
                    AboutScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onSocialClick = ::openUrl,
                        onGitHubClick = ::openUrl,
                        onContactClick = {
                            openEmail("bernaferrari2@gmail.com", "Change Detection Feedback")
                        },
                        onLicenseUrlClick = ::openUrl,
                    )
                }

                entry<HowItWorksDestination>(
                    metadata = NavDisplay.transitionSpec {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    } + NavDisplay.popTransitionSpec {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                    },
                ) {
                    HowItWorksScreen(onBackClick = { backStack.removeLastOrNull() })
                }

                entry<BrowserDestination>(
                    metadata = NavDisplay.transitionSpec {
                        slideInVertically(initialOffsetY = { it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    } + NavDisplay.popTransitionSpec {
                        slideInVertically(initialOffsetY = { -it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                    },
                ) { key ->
                    LocalBrowserScreen(
                        snapId = key.snapId,
                        url = key.url.ifEmpty { "https://google.com" },
                        onDismiss = { backStack.removeLastOrNull() },
                    )
                }

                entry<ComparisonDestination>(
                    metadata = ListDetailSceneStrategy.detailPane() + NavDisplay.transitionSpec {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    } + NavDisplay.popTransitionSpec {
                        slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                    },
                ) { key ->
                    ComparisonScreen(
                        siteId = key.siteId,
                        onNavigateUp = { backStack.removeLastOrNull() },
                        onSiteEdit = { id -> backStack.add(EditSiteDestination(siteId = id)) },
                        onSiteDelete = { backStack.removeLastOrNull() },
                    )
                }
            },
            transitionSpec = {
                androidx.compose.animation.fadeIn(tween(300)) togetherWith
                    androidx.compose.animation.fadeOut(tween(300))
            },
        )
    }
}
