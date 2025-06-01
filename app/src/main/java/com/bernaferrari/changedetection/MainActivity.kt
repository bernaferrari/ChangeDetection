package com.bernaferrari.changedetection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bernaferrari.changedetection.di.PreferencesModule
import com.bernaferrari.changedetection.ui.main.MainScreen
import com.bernaferrari.changedetection.ui.navigation.ChangeDetectionBottomNavigation
import com.bernaferrari.changedetection.ui.theme.ChangeDetectionTheme
import com.bernaferrari.changedetection.ui.settings.SettingsScreen
import com.bernaferrari.changedetection.ui.logs.LogsScreen
import com.bernaferrari.changedetection.ui.about.AboutScreen
import com.bernaferrari.changedetection.ui.about.AboutLicensesScreen
import com.bernaferrari.changedetection.ui.addedit.AddEditScreen
import com.bernaferrari.changedetection.ui.localbrowser.LocalBrowserScreen
import com.bernaferrari.changedetection.ui.webview.WebViewScreen
import com.bernaferrari.changedetection.ui.visual.VisualScreen
import com.bernaferrari.changedetection.ui.text.TextScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkMode by dataStore.data
                .map { preferences -> !preferences[PreferencesModule.PreferenceKeys.LIGHT_MODE]!! ?: false }
                .collectAsState(initial = false)

            ChangeDetectionTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChangeDetectionApp()
                }
            }
        }
    }

    companion object {
        const val DARKMODE = "dark mode"
        const val LASTCHANGE = "LASTCHANGE"
        const val SITEID = "SITEID"
        const val SNAPID = "SNAPID"
        const val TITLE = "TITLE"
        const val TYPE = "TYPE"
        const val URL = "URL"
        const val TRANSITION = 175L
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
                MainScreen(
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }
            composable("logs") {
                LogsScreen(
                    navController = navController,
                    viewModel = hiltViewModel()
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = hiltViewModel()
                )
            }
            composable("about") {
                AboutScreen(navController = navController)
            }
            composable("about_licenses") {
                AboutLicensesScreen(navController = navController)
            }
            composable("local_browser/{snapId}/{url}") { backStackEntry ->
                val snapId = backStackEntry.arguments?.getString("snapId") ?: ""
                val url = backStackEntry.arguments?.getString("url") ?: ""
                LocalBrowserScreen(
                    snapId = snapId,
                    url = url,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("add_edit") {
                AddEditScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWebView = { url ->
                        navController.navigate("webview/$url")
                    },
                    onNavigateToCompare = { url ->
                        // TODO: Implement compare dialog
                    }
                )
            }
            composable("add_edit/{siteId}") { backStackEntry ->
                val siteId = backStackEntry.arguments?.getString("siteId")
                AddEditScreen(
                    siteId = siteId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToWebView = { url ->
                        navController.navigate("webview/$url")
                    },
                    onNavigateToCompare = { url ->
                        // TODO: Implement compare dialog
                    }
                )
            }
            composable("webview/{url}") { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: "https://google.com"
                WebViewScreen(
                    initialUrl = url,
                    onNavigateBack = { navController.popBackStack() },
                    onUrlSelected = { selectedUrl ->
                        // Return the selected URL to the previous screen
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_url", selectedUrl)
                        navController.popBackStack()
                    }
                )
            }
            composable("visual/{siteId}/{contentType}/{url}") { backStackEntry ->
                val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
                val contentType = backStackEntry.arguments?.getString("contentType") ?: "image"
                val url = backStackEntry.arguments?.getString("url") ?: ""
                VisualScreen(
                    siteId = siteId,
                    contentType = contentType,
                    url = url,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("text/{siteId}/{url}") { backStackEntry ->
                val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
                val url = backStackEntry.arguments?.getString("url") ?: ""
                TextScreen(
                    siteId = siteId,
                    url = url,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
