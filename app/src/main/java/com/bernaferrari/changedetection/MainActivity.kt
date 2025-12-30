package com.bernaferrari.changedetection

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.logs.LogsScreen
import com.bernaferrari.changedetection.navigation.AboutDestination
import com.bernaferrari.changedetection.navigation.AddEditDestination
import com.bernaferrari.changedetection.navigation.BrowserDestination
import com.bernaferrari.changedetection.navigation.ComparisonDestination
import com.bernaferrari.changedetection.navigation.EditSiteDestination
import com.bernaferrari.changedetection.navigation.HowItWorksDestination
import com.bernaferrari.changedetection.navigation.LogsDestination
import com.bernaferrari.changedetection.navigation.MainDestination
import com.bernaferrari.changedetection.navigation.SettingsDestination
import com.bernaferrari.changedetection.settings.SettingsScreen
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi

import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import com.bernaferrari.changedetection.ui.main.MainViewModel
import com.bernaferrari.changedetection.ui.main.TurboScreen
import com.bernaferrari.changedetection.navigation.TurboDestination
import com.bernaferrari.changedetection.QuickMonitorService
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Permission granted or denied - notifications will work if granted
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }

                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val windowSizeClass = calculateWindowSizeClass(this)

            ChangeDetectionTheme(
                darkTheme = themeViewModel.shouldUseDarkTheme(),
                dynamicColor = themeViewModel.shouldUseDynamicColor(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChangeDetectionApp(windowSizeClass)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ChangeDetectionApp(windowSizeClass: WindowSizeClass) {
    val backStack = rememberNavBackStack(MainDestination)
    val mainViewModel: MainViewModel = hiltViewModel()
    val context = LocalContext.current
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    ChangeDetectionBottomNavigation(
        backStack = backStack,
        windowSizeClass = windowSizeClass
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            sceneStrategy = listDetailStrategy,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<MainDestination>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("Select a site to view details")
                            }
                        }
                    )
                ) {
                    MainScreen(backStack = backStack, viewModel = mainViewModel)
                }

                entry<TurboDestination> {
                    val sites by mainViewModel.sitesFlow.collectAsState()
                    val quickMonitorState by mainViewModel.quickMonitorState.collectAsState()

                    TurboScreen(
                        sites = sites,
                        quickMonitorState = quickMonitorState,
                        onStartTurbo = { selectedUrl, interval ->
                            mainViewModel.startQuickMonitor(selectedUrl, interval)
                            Toast.makeText(context, "Turbo Mode Started", Toast.LENGTH_SHORT).show()
                        },
                        onStopTurbo = {
                            QuickMonitorService.stop(context)
                            mainViewModel.stopQuickMonitor()
                        }
                    )
                }

                entry<LogsDestination> {
                    LogsScreen(
                        onSnapClick = { snapId, url ->
                            backStack.add(BrowserDestination(snapId = snapId, url = url))
                        }
                    )
                }

                entry<SettingsDestination> {
                    SettingsScreen(
                        onAboutClick = { backStack.add(AboutDestination) }
                    )
                }

            // Full-screen destinations (no bottom navigation)
            entry<AddEditDestination>(
                metadata = NavDisplay.transitionSpec {
                    slideInVertically(initialOffsetY = { it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                } + NavDisplay.popTransitionSpec {
                    slideInVertically(initialOffsetY = { -it }) togetherWith
                            slideOutVertically(targetOffsetY = { it })
                }
            ) {
                AddEditFlow(
                    onNavigateUp = { backStack.removeLastOrNull() },
                    onSaveComplete = { backStack.removeLastOrNull() }
                )
            }

            entry<EditSiteDestination>(
                metadata = NavDisplay.transitionSpec {
                    slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                } + NavDisplay.popTransitionSpec {
                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                }
            ) { key ->
                AddEditFlow(
                    siteId = key.siteId,
                    onNavigateUp = { backStack.removeLastOrNull() },
                    onSaveComplete = { backStack.removeLastOrNull() }
                )
            }

            entry<AboutDestination> {
                AboutScreen(backStack = backStack)
            }

            entry<HowItWorksDestination> {
                HowItWorksScreen(
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }

            entry<BrowserDestination> { key ->
                LocalBrowserScreen(
                    snapId = key.snapId,
                    url = key.url.ifEmpty { "https://google.com" },
                    onDismiss = { backStack.removeLastOrNull() }
                )
            }

            entry<ComparisonDestination>(
                metadata = ListDetailSceneStrategy.detailPane() + NavDisplay.transitionSpec {
                    slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                } + NavDisplay.popTransitionSpec {
                    slideInHorizontally(initialOffsetX = { -it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { it })
                }
            ) { key ->
                ComparisonScreen(
                    siteId = key.siteId,
                    onNavigateUp = { backStack.removeLastOrNull() }
                )
            }
            },
            transitionSpec = {
                // Default forward navigation: slide in from right
                slideInHorizontally(initialOffsetX = { it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it })
            },
            popTransitionSpec = {
                // Default back navigation: slide in from left
                slideInHorizontally(initialOffsetX = { -it }) togetherWith
                        slideOutHorizontally(targetOffsetX = { it })
            }
        )
    }
}
