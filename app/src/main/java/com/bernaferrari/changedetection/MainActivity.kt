package com.bernaferrari.changedetection

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bernaferrari.changedetection.addedit.AddEditScreen
import com.bernaferrari.changedetection.addedit.WebViewScreen
import com.bernaferrari.changedetection.detailsText.TextScreen
import com.bernaferrari.changedetection.detailsvisual.VisualScreen
import com.bernaferrari.changedetection.di.PreferencesModule
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.logs.LogsScreen
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.about.HowItWorksScreen
import com.bernaferrari.changedetection.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()

            ChangeDetectionTheme(themeViewModel = themeViewModel) {
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

    ChangeDetectionBottomNavigation(navController = navController) {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
            composable("main") {
                MainScreen(navController = navController)
            }

            composable("logs") {
                LogsScreen(
                    onSnapClick = { snapId, url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("local_browser/$snapId/$encodedUrl")
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onAboutClick = { navController.navigate("about") }
                )
            }

            composable("about") {
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
                    selectedUrl = selectedUrl,
                    onNavigateUp = { navController.popBackStack() },
                    onNavigateToWebView = { url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        navController.navigate("webview/$encodedUrl")
                    },
                    onShowCompareDialog = { url ->
                        // TODO: Implement compare dialog
                    }
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

                VisualScreen(
                    siteId = siteId,
                    contentType = contentType,
                    url = url,
                    onNavigateUp = { navController.popBackStack() }
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

                TextScreen(
                    siteId = siteId,
                    url = url,
                    contentType = "text/html", // Default content type for text screen
                    onNavigateUp = { navController.popBackStack() }
                )
            }
        }
    }
}
