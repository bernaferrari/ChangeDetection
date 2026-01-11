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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import com.bernaferrari.changedetection.ui.main.MainViewModel
import org.koin.compose.viewmodel.koinViewModel

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
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Read deep link extras from notification
        val initialSiteId = if (intent.getBooleanExtra("openComparison", false)) {
            intent.getStringExtra("siteId")
        } else {
            null
        }

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val mainViewModel: MainViewModel = koinViewModel()
            val context = LocalContext.current

            ChangeDetectionRootApp(
                windowSizeClass = windowSizeClass,
                platformActions = PlatformActions(
                    openUrl = { url -> openUrl(context, url) },
                    openEmail = { email, subject -> openEmail(context, email, subject) },
                    startTurbo = { siteIds, intervalSeconds ->
                        mainViewModel.startQuickMonitor(siteIds, intervalSeconds)
                    },
                    stopTurbo = {
                        QuickMonitorService.stop(context)
                        mainViewModel.stopQuickMonitor()
                    }
                ),
                initialSiteId = initialSiteId
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}

private fun openEmail(context: android.content.Context, email: String, subject: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = "mailto:$email?subject=${android.net.Uri.encode(subject)}".toUri()
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle error
    }
}
