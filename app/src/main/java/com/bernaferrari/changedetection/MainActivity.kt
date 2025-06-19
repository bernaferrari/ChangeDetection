package com.bernaferrari.changedetection

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bernaferrari.changedetection.addedit.AddEditScreen
import com.bernaferrari.changedetection.addedit.WebViewScreen
import com.bernaferrari.changedetection.detailsText.TextScreen
import com.bernaferrari.changedetection.detailsWebview.TextDiffScreen
import com.bernaferrari.changedetection.detailsWebview.VisualComparisonScreen
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.logs.LogsScreen
import com.bernaferrari.changedetection.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.about.HowItWorksScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()

            ChangeDetectionTheme(
                darkTheme = themeViewModel.shouldUseDarkTheme(),
                dynamicColor = themeViewModel.shouldUseDynamicColor(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChangeDetectionApp()
                }
            }
        }
    }
}

@Composable
fun ChangeDetectionApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Main app with bottom navigation
        composable("main") {
            ChangeDetectionBottomNavigation(navController = navController) {
                MainScreen(navController = navController)
            }
        }

        composable("logs") {
            ChangeDetectionBottomNavigation(navController = navController) {
                LogsScreen(
                    onSnapClick = { snapId, url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("local_browser/$snapId/$encodedUrl")
                    }
                )
            }
        }

        composable("settings") {
            ChangeDetectionBottomNavigation(navController = navController) {
                SettingsScreen(
                    onAboutClick = { navController.navigate("about") }
                )
            }
        }

        // Full-screen routes (no bottom navigation)
        composable(
            "add_edit",
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) }
        ) {
            val selectedUrl = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<String>("selected_url")

            AddEditScreen(
                selectedUrl = selectedUrl,
                onNavigateUp = { navController.popBackStack() },
                onNavigateToWebView = { url ->
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$encodedUrl")
                },
                onNavigateToVisualComparison = { siteId ->
                    navController.navigate("visual_comparison/$siteId")
                },
                onShowCompareDialog = { url ->
                    // TODO: Implement compare dialog or navigate to compare screen
                }
            )
        }

        composable(
            "add_edit/{siteId}",
            arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId")
            val selectedUrl = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.get<String>("selected_url")

            AddEditScreen(
                siteId = siteId,
                selectedUrl = selectedUrl,
                onNavigateUp = { navController.popBackStack() },
                onNavigateToWebView = { url ->
                    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                    navController.navigate("webview/$encodedUrl")
                },
                onNavigateToVisualComparison = { siteId ->
                    navController.navigate("visual_comparison/$siteId")
                },
                onShowCompareDialog = { url ->
                    // TODO: Implement compare dialog
                }
            )
        }

        composable(
            "about"
        ) {
            AboutScreen(navController)
        }

        composable("how_it_works") {
            HowItWorksScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            "local_browser/{snapId}/{url}",
            arguments = listOf(
                navArgument("snapId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val snapId = backStackEntry.arguments?.getString("snapId") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

            LocalBrowserScreen(
                snapId = snapId,
                url = url,
                onDismiss = { navController.popBackStack() }
            )
        }

        composable(
            "webview/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = if (encodedUrl.isNotEmpty()) {
                URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            } else {
                "https://google.com"
            }

            WebViewScreen(
                initialUrl = url,
                onNavigateUp = { navController.popBackStack() },
                onUrlSelected = { selectedUrl ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(
                        "selected_url",
                        selectedUrl
                    )
                    navController.popBackStack()
                }
            )
        }

        composable(
            "visual/{siteId}/{contentType}/{url}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("contentType") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
            val contentType = backStackEntry.arguments?.getString("contentType") ?: "image"
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

            VisualComparisonScreen(
                siteId = siteId,
                onNavigateUp = { navController.popBackStack() },
                onShowTextDiff = { comparison ->
                    navController.navigate("text_diff/$siteId")
                }
            )
        }

        composable(
            "text/{siteId}/{url}",
            arguments = listOf(
                navArgument("siteId") { type = NavType.StringType },
                navArgument("url") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())

            VisualComparisonScreen(
                siteId = siteId,
                onNavigateUp = { navController.popBackStack() },
                onShowTextDiff = { comparison ->
                    navController.navigate("text_diff/$siteId")
                }
            )
        }

        composable(
            "visual_comparison/{siteId}",
            arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""

            VisualComparisonScreen(
                siteId = siteId,
                onNavigateUp = { navController.popBackStack() },
                onShowTextDiff = { comparison ->
                    navController.navigate("webview_text_diff/$siteId")
                }
            )
        }

        composable(
            "webview_text_diff/{siteId}",
            arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) }
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""

            TextDiffScreen(
                siteId = siteId,
                onNavigateUp = { navController.popBackStack() }
            )
        }

        composable(
            "text_diff/{siteId}",
            arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
            enterTransition = { slideInVertically(initialOffsetY = { it }) },
            exitTransition = { slideOutVertically(targetOffsetY = { it }) }
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""

            TextScreen(
                siteId = siteId,
                url = "", // We'll get this from the comparison data
                contentType = "text/html",
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
