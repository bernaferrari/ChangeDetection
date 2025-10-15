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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.bernaferrari.changedetection.addedit.AddEditFlow
import com.bernaferrari.changedetection.localBrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.logs.LogsScreen
import com.bernaferrari.changedetection.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.comparison.ComparisonScreen
import com.bernaferrari.changedetection.ui.howItWorks.HowItWorksScreen
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

@Composable
fun ChangeDetectionApp(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Main app with bottom navigation
        composable("main") {
            ChangeDetectionBottomNavigation(
                navController = navController,
                windowSizeClass = windowSizeClass
            ) {
                MainScreen(navController = navController)
            }
        }

        composable("logs") {
            ChangeDetectionBottomNavigation(
                navController = navController,
                windowSizeClass = windowSizeClass
            ) {
                LogsScreen(
                    onSnapClick = { snapId, url ->
                        val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
                        val encodedSnapId = URLEncoder.encode(snapId, StandardCharsets.UTF_8.toString())
                        navController.navigate("browser?snapId=$encodedSnapId&url=$encodedUrl")
                    }
                )
            }
        }

        composable("settings") {
            ChangeDetectionBottomNavigation(
                navController = navController,
                windowSizeClass = windowSizeClass
            ) {
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

            AddEditFlow(
                selectedUrl = selectedUrl,
                onNavigateUp = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
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

            AddEditFlow(
                siteId = siteId,
                selectedUrl = selectedUrl,
                onNavigateUp = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
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

        // Unified browser route - combines local_browser and webview functionality
        composable(
            route = "browser?snapId={snapId}&url={url}",
            arguments = listOf(
                navArgument("snapId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("url") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
            val encodedSnapId = backStackEntry.arguments?.getString("snapId").orEmpty()

            val url = if (encodedUrl.isNotEmpty()) {
                URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
            } else {
                "https://google.com"
            }

            val snapId = if (encodedSnapId.isNotEmpty()) {
                URLDecoder.decode(encodedSnapId, StandardCharsets.UTF_8.toString())
            } else {
                ""
            }

            LocalBrowserScreen(
                snapId = snapId,
                url = url,
                onDismiss = { navController.popBackStack() }
            )
        }

        // Comparison screen for viewing site snapshots and changes
        composable(
            "comparison/{siteId}",
            arguments = listOf(navArgument("siteId") { type = NavType.StringType }),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
            
            ComparisonScreen(
                siteId = siteId,
                onNavigateUp = { navController.popBackStack() }
            )
        }
    }
}
