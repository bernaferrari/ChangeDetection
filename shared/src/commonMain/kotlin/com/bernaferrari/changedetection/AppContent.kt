@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.graphics.toArgb
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
import com.bernaferrari.changedetection.localization.stringResource

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
    startDestination: NavKey = MainDestination,
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
                startDestination = startDestination,
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
    /** The first screen; native iOS navigation hosts one destination per view controller. */
    startDestination: NavKey = MainDestination,
) {
    val native = com.bernaferrari.changedetection.ui.navigation.LocalNativeNavigation.current
    // Root tab entries are replaced on navigation; keep their reading positions with the app.
    val homeListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val activityListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val homeBarState = androidx.compose.material3.rememberTopAppBarState()
    val activityBarState = androidx.compose.material3.rememberTopAppBarState()
    val backStack = rememberNavBackStack(appNavSavedStateConfiguration, startDestination)
    // One MainViewModel for the whole app, even when every screen is its own view controller (iOS).
    val mainViewModel: MainViewModel = koinViewModel(
        viewModelStoreOwner = LocalAppViewModelStoreOwner.current ?: requireNotNull(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current),
    )
    // Activity is a tab: switching tabs replaces its entry, so an entry-scoped model would reload the
    // feed from scratch on every visit and pop its content in after the transition.
    val logsViewModel: com.bernaferrari.changedetection.ui.logs.LogsViewModel = koinViewModel(
        viewModelStoreOwner = LocalAppViewModelStoreOwner.current ?: requireNotNull(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current),
    )
    var turboSetup by remember { mutableStateOf<Set<String>?>(null) }

    fun openAddPage(url: String? = null) {
        if (backStack.lastOrNull() is AddEditDestination) backStack.pop()
        backStack.add(AddEditDestination(url))
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
    // With native navigation, Compose only ever shows this host's own screen: anything opened from it is
    // handed to UIKit (pushed, or presented as a sheet). On wide screens, Home keeps its list-detail panes.
    if (native != null) {
        androidx.compose.runtime.LaunchedEffect(backStack, singlePaneLayout) {
            androidx.compose.runtime.snapshotFlow { backStack.toList() }.collect { keys ->
                val forward = keys.drop(1).filterNot { key ->
                    !singlePaneLayout && startDestination == MainDestination && (key is ComparisonDestination || key is PageDetailDestination)
                }
                if (forward.isEmpty()) return@collect
                forward.forEach { key -> if (key is AddEditDestination || key is EditSiteDestination) native.present(key) else native.push(key) }
                backStack.removeAll(forward.toSet())
            }
        }
    }

    /** Closes [key]; with native navigation, closing this host's only screen pops or dismisses it in UIKit. */
    fun close(key: NavKey) {
        if (native != null && backStack.size <= 1) native.close() else backStack.pop(key)
    }

    /** Detail panes shown next to Home on wide screens draw their own bar, even under native navigation. */
    @Composable
    fun InPane(content: @Composable () -> Unit) {
        if (native != null && !singlePaneLayout && startDestination == MainDestination) {
            androidx.compose.runtime.CompositionLocalProvider(com.bernaferrari.changedetection.ui.navigation.LocalNativeNavigation provides null, content = content)
        } else {
            content()
        }
    }

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
    if (native != null && startDestination == MainDestination) {
        androidx.compose.runtime.LaunchedEffect(unreadCount) { native.setHomeBadge(unreadCount) }
    }
    if (native != null) {
        val background = MaterialTheme.colorScheme.background
        androidx.compose.runtime.LaunchedEffect(background) { native.setBackground(background.toArgb()) }
    }
    // The native tab bar replaces the Compose navigation bar and rail.
    val suite: @Composable (@Composable () -> Unit) -> Unit = { content ->
        if (native != null) {
            content()
        } else {
            ChangeDetectionNavigationSuite(
                backStack = backStack,
                homeBadge = unreadCount,
                retainNavigationForDetail = !singlePaneLayout && (backStack.lastOrNull() is ComparisonDestination || backStack.lastOrNull() is PageDetailDestination),
                content = content,
            )
        }
    }
    // Tabs replace the root entry, so Activity or Settings alone has nothing to pop. Back returns to Home
    // first, as on any bottom-navigation app, and only Home leaves the app. Screens that handle back
    // themselves (search, pickers) register later and still take precedence.
    com.bernaferrari.changedetection.util.BackHandler(
        enabled = native == null && backStack.size == 1 && (backStack.first() is LogsDestination || backStack.first() is SettingsDestination),
    ) { backStack[0] = MainDestination }
    suite {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.pop() },
            // On phones every screen is its own scene, so pushes and pops animate (inside one list-detail
            // scene the content swaps instantly) and Home keeps its scroll position.
            sceneStrategies = if (singlePaneLayout) emptyList() else listOf(listDetailStrategy),
            // Tab switches replace the root entry and carry no motion of their own; the library default
            // is a slow 700ms cross-fade, so they use Material's fade through instead.
            transitionSpec = fadeThrough(),
            popTransitionSpec = fadeThrough(),
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
                        listState = homeListState,
                        topBarState = homeBarState,
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

                entry<TurboDestination> { navKey ->
                    TurboRoute(
                        mainViewModel = mainViewModel,
                        onBack = { close(navKey) },
                        onStop = { platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor() },
                        onRunAgain = { ids -> turboSetup = ids },
                        onOpenPage = { backStack.openPage(it) },
                        onOpenChange = { id, snapId -> backStack.openComparison(id, snapId) },
                    )
                }

                entry<LogsDestination> { navKey ->
                    ActivityScreen(
                        onOpenChange = { siteId, snapId -> backStack.openComparison(siteId, snapId) },
                        onOpenPage = { backStack.openPage(it) },
                        viewModel = logsViewModel,
                        listState = activityListState,
                        topBarState = activityBarState,
                    )
                }

                entry<SettingsDestination> { navKey ->
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
                    InPane {
                        PageDetailScreen(
                            siteId = key.siteId,
                            viewModel = mainViewModel,
                            onBack = { close(key) },
                            onCompare = { snapId -> backStack.openComparison(key.siteId, snapId) },
                            onHistory = { snapId -> backStack.openComparison(key.siteId, snapId, showHistory = true) },
                            onEdit = { backStack.add(EditSiteDestination(key.siteId)) },
                            onTurbo = { turboSetup = setOf(key.siteId) },
                            onSchedule = { showSchedule = true },
                            onOpenUrl = platformActions.openUrl,
                            onEnableNotifications = platformActions.requestNotifications,
                        )
                    }
                    if (showSchedule) BackgroundSyncDialog(onDismiss = { showSchedule = false })
                }

                entry<PageTurboDestination>(metadata = forwardTransitionMetadata) { navKey ->
                    TurboRoute(
                        mainViewModel = mainViewModel,
                        onBack = { close(navKey) },
                        onStop = { platformActions.stopTurbo?.invoke() ?: mainViewModel.stopQuickMonitor() },
                        onRunAgain = { ids -> turboSetup = ids },
                        onOpenPage = { backStack.openPage(it) },
                        onOpenChange = { id, snapId -> backStack.openComparison(id, snapId) },
                    )
                }

                entry<AddEditDestination>(metadata = forwardTransitionMetadata) { key ->
                    AddEditFlow(
                        selectedUrl = key.initialUrl,
                        onNavigateUp = { close(key) },
                        onSaveComplete = {
                            platformActions.requestNotifications()
                            close(key)
                        },
                        onOpenSite = { siteId ->
                            close(key)
                            backStack.openPage(siteId)
                        },
                        onPageAdded = { siteId, name ->
                            close(key)
                            mainViewModel.announcePageAdded(siteId, name)
                            platformActions.requestNotifications()
                        },
                    )
                }

                entry<EditSiteDestination>(metadata = forwardTransitionMetadata) { key ->
                    AddEditFlow(
                        siteId = key.siteId,
                        onNavigateUp = { close(key) },
                        onSaveComplete = { close(key) },
                        onOpenSite = { siteId ->
                            close(key)
                            backStack.openPage(siteId)
                        },
                    )
                }

                entry<AboutDestination>(metadata = forwardTransitionMetadata) { navKey ->
                    AboutScreen(
                        onBack = { close(navKey) },
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
                        appVersion = platformActions.appVersion,
                    )
                }

                entry<HowItWorksDestination>(metadata = forwardTransitionMetadata) { navKey ->
                    HowItWorksScreen(
                        onBackClick = { close(navKey) },
                    )
                }

                entry<BrowserDestination>(metadata = forwardTransitionMetadata) { key ->
                    LocalBrowserScreen(
                        snapId = key.snapId,
                        url = key.url,
                        onDismiss = { close(key) },
                    )
                }

                entry<ComparisonDestination>(metadata = comparisonMetadata) { key ->
                    InPane {
                        ComparisonScreen(
                            siteId = key.siteId,
                            onNavigateUp = { close(key) },
                            showNavigateUp = singlePaneLayout,
                            initialSnapId = key.initialSnapId,
                            showHistoryInitially = key.showHistory,
                            onSiteEdit = { id -> backStack.add(EditSiteDestination(siteId = id)) },
                            onOpenPage = { id -> backStack.openPage(id) },
                            onSiteDelete = { close(key) },
                        )
                    }
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

/** The app-wide owner for view models shared by every screen (set by the iOS shell, where each screen has its own host). */
val LocalAppViewModelStoreOwner = androidx.compose.runtime.staticCompositionLocalOf<androidx.lifecycle.ViewModelStoreOwner?> { null }

/**
 * Closes [screen] if it is still on top (taps that land during the closing animation do nothing), and never
 * removes the last screen: an empty back stack crashes navigation.
 */
internal fun androidx.navigation3.runtime.NavBackStack<NavKey>.pop(screen: NavKey? = null) {
    if (size <= 1) return
    if (screen != null && lastOrNull() != screen) return
    removeLastOrNull()
}
